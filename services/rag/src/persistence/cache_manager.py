"""
Cache manager providing LLM response cache, retrieval result cache, and embedding cache.
Supports both Redis and in-memory backends.
"""
import hashlib
import json
import pickle
from abc import ABC, abstractmethod
from typing import Optional, Any
from dataclasses import dataclass, field
from loguru import logger
from datetime import datetime, timedelta
from ..config import get_settings


@dataclass
class CacheEntry:
    """Cache entry"""
    key: str
    value: Any
    created_at: datetime = field(default_factory=datetime.now)
    ttl_seconds: int = 3600
    hits: int = 0

    def is_expired(self) -> bool:
        return datetime.now() > self.created_at + timedelta(seconds=self.ttl_seconds)


class CacheBackend(ABC):
    """Abstract cache backend"""

    @abstractmethod
    def get(self, key: str) -> Optional[Any]:
        pass

    @abstractmethod
    def set(self, key: str, value: Any, ttl_seconds: int = 3600) -> None:
        pass

    @abstractmethod
    def delete(self, key: str) -> None:
        pass

    @abstractmethod
    def clear(self) -> None:
        pass

    @abstractmethod
    def stats(self) -> dict:
        pass


class MemoryCache(CacheBackend):
    """In-memory cache backend"""

    def __init__(self):
        self._cache: dict[str, CacheEntry] = {}
        self._hits = 0
        self._misses = 0

    def get(self, key: str) -> Optional[Any]:
        entry = self._cache.get(key)
        if entry is None:
            self._misses += 1
            return None
        if entry.is_expired():
            del self._cache[key]
            self._misses += 1
            return None
        entry.hits += 1
        self._hits += 1
        return entry.value

    def set(self, key: str, value: Any, ttl_seconds: int = 3600) -> None:
        self._cache[key] = CacheEntry(key=key, value=value, ttl_seconds=ttl_seconds)

    def delete(self, key: str) -> None:
        self._cache.pop(key, None)

    def clear(self) -> None:
        self._cache.clear()
        self._hits = 0
        self._misses = 0

    def stats(self) -> dict:
        total = self._hits + self._misses
        hit_rate = self._hits / total if total > 0 else 0.0
        return {
            "hits": self._hits,
            "misses": self._misses,
            "hit_rate": round(hit_rate, 4),
            "size": len(self._cache),
        }


class RedisCache(CacheBackend):
    """Redis cache backend"""

    def __init__(self, host: str = "localhost", port: int = 6379, db: int = 0):
        import redis
        self._client = redis.Redis(host=host, port=port, db=db, decode_responses=False)
        self._hits = 0
        self._misses = 0
        self._prefix = "rag_cache:"

    def _make_key(self, key: str) -> bytes:
        return f"{self._prefix}{key}".encode()

    def get(self, key: str) -> Optional[Any]:
        try:
            data = self._client.get(self._make_key(key))
            if data is None:
                self._misses += 1
                return None
            self._hits += 1
            return pickle.loads(data)
        except Exception as e:
            logger.warning(f"Redis get error: {e}")
            self._misses += 1
            return None

    def set(self, key: str, value: Any, ttl_seconds: int = 3600) -> None:
        try:
            data = pickle.dumps(value)
            self._client.setex(self._make_key(key), ttl_seconds, data)
        except Exception as e:
            logger.warning(f"Redis set error: {e}")

    def delete(self, key: str) -> None:
        try:
            self._client.delete(self._make_key(key))
        except Exception as e:
            logger.warning(f"Redis delete error: {e}")

    def clear(self) -> None:
        try:
            for key in self._client.scan_iter(f"{self._prefix}*"):
                self._client.delete(key)
        except Exception as e:
            logger.warning(f"Redis clear error: {e}")
        self._hits = 0
        self._misses = 0

    def stats(self) -> dict:
        try:
            count = sum(1 for _ in self._client.scan_iter(f"{self._prefix}*"))
        except Exception:
            count = 0
        total = self._hits + self._misses
        hit_rate = self._hits / total if total > 0 else 0.0
        return {
            "hits": self._hits,
            "misses": self._misses,
            "hit_rate": round(hit_rate, 4),
            "size": count,
        }


class CacheManager:
    """
    Multi-layer cache manager.

    Provides three cache layers:
    1. LLM response cache - caches complete LLM responses
    2. Retrieval result cache - caches vector retrieval results
    3. Embedding cache - caches query embedding vectors
    """

    def __init__(self, backend: Optional[CacheBackend] = None):
        settings = get_settings()

        if backend:
            self._backend = backend
        elif settings.REDIS_HOST:
            try:
                self._backend = RedisCache(
                    host=settings.REDIS_HOST,
                    port=settings.REDIS_PORT,
                    db=settings.REDIS_CACHE_DB,
                )
                logger.info("Using Redis cache backend")
            except Exception as e:
                logger.warning(f"Redis unavailable, falling back to memory: {e}")
                self._backend = MemoryCache()
        else:
            self._backend = MemoryCache()
            logger.info("Using memory cache backend")

        self._llm_cache: dict[str, Any] = {}
        self._retrieval_cache: dict[str, Any] = {}
        self._embedding_cache: dict[str, Any] = {}

    @staticmethod
    def _hash_key(data: str) -> str:
        """Generate hash key for cache"""
        return hashlib.sha256(data.encode()).hexdigest()[:32]

    def get_llm_response(self, query: str, doc_ids: Optional[list[str]] = None) -> Optional[dict]:
        """Get LLM response from cache"""
        key_parts = [query, str(sorted(doc_ids)) if doc_ids else "all"]
        key = self._hash_key("|".join(key_parts))
        return self._backend.get(f"llm:{key}")

    def set_llm_response(
        self,
        query: str,
        response: dict,
        doc_ids: Optional[list[str]] = None,
        ttl_seconds: int = 3600,
    ) -> None:
        """Set LLM response in cache"""
        key_parts = [query, str(sorted(doc_ids)) if doc_ids else "all"]
        key = self._hash_key("|".join(key_parts))
        self._backend.set(f"llm:{key}", response, ttl_seconds)

    def get_retrieval_results(
        self,
        query: str,
        doc_ids: Optional[list[str]] = None,
    ) -> Optional[list[dict]]:
        """Get retrieval results from cache"""
        key_parts = [query, str(sorted(doc_ids)) if doc_ids else "all"]
        key = self._hash_key("|".join(key_parts))
        return self._backend.get(f"retrieval:{key}")

    def set_retrieval_results(
        self,
        query: str,
        results: list[dict],
        doc_ids: Optional[list[str]] = None,
        ttl_seconds: int = 1800,
    ) -> None:
        """Set retrieval results in cache"""
        key_parts = [query, str(sorted(doc_ids)) if doc_ids else "all"]
        key = self._hash_key("|".join(key_parts))
        self._backend.set(f"retrieval:{key}", results, ttl_seconds)

    def get_embedding(self, text: str) -> Optional[list[float]]:
        """Get embedding from cache"""
        key = self._hash_key(text)
        return self._backend.get(f"embed:{key}")

    def set_embedding(self, text: str, embedding: list[float], ttl_seconds: int = 86400) -> None:
        """Set embedding in cache"""
        key = self._hash_key(text)
        self._backend.set(f"embed:{key}", embedding, ttl_seconds)

    def invalidate_query_cache(self, query: str) -> None:
        """Invalidate cache entries containing specific query terms"""
        prefix = self._hash_key(query[:50])
        for key in list(self._backend._cache.keys() if hasattr(self._backend, '_cache') else []):
            if key.startswith(f"llm:{prefix}") or key.startswith(f"retrieval:{prefix}"):
                self._backend.delete(key)

    def clear_all(self) -> None:
        """Clear all caches"""
        self._backend.clear()
        self._llm_cache.clear()
        self._retrieval_cache.clear()
        self._embedding_cache.clear()

    def get_stats(self) -> dict:
        """Get cache statistics"""
        return self._backend.stats()


_cache_manager: Optional[CacheManager] = None


def get_cache_manager() -> CacheManager:
    """Get global cache manager instance"""
    global _cache_manager
    if _cache_manager is None:
        _cache_manager = CacheManager()
    return _cache_manager


def reset_cache_manager() -> None:
    """Reset global cache manager"""
    global _cache_manager
    if _cache_manager:
        _cache_manager.clear_all()
    _cache_manager = None
