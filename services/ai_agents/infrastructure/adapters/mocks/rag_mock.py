"""Mock implementations for RAG operations.

This module contains mock/stub implementations for testing and development.
These are not meant for production use.
"""

from typing import Any, Dict, List

from services.ai_agents.domain.repositories.rag_repository import (
    CollectionInfo,
    RAGRepository,
    RAGResult,
)
from services.ai_agents.domain.values import (
    ChunkConfig,
    Document,
    DocumentMetadata,
    VectorDocument,
)


class RAGMockStore:
    """Mock data store for RAG operations.
    
    Provides in-memory storage for testing without external dependencies.
    """
    
    def __init__(self):
        self._collections: Dict[str, Dict[str, Any]] = {}
        self._documents: Dict[str, List[Document]] = {}
        self._vectors: Dict[str, List[VectorDocument]] = {}


class MockRAGAdapter(RAGRepository):
    """Adapter for RAG operations (mock implementation).
    
    This is a mock implementation that simulates RAG operations.
    In production, replace with actual vector store and document store adapters.
    """
    
    def __init__(self):
        self._store = RAGMockStore()
    
    def _simple_chunk(self, content: str, chunk_size: int = 500) -> List[Dict[str, Any]]:
        """Simple chunking for mock purposes - no domain service dependency."""
        import uuid
        chunks = []
        for i in range(0, len(content), chunk_size):
            chunks.append({
                "id": str(uuid.uuid4()),
                "text": content[i:i+chunk_size],
                "start_char": i,
                "end_char": min(i+chunk_size, len(content)),
                "length": min(chunk_size, len(content)-i)
            })
        return chunks
    
    def create_collection(self, name: str, dimension: int = 1536) -> CollectionInfo:
        if name in self._store._collections:
            raise ValueError(f"Collection '{name}' already exists")
        
        self._store._collections[name] = {
            "name": name,
            "dimension": dimension,
            "document_count": 0
        }
        self._store._documents[name] = []
        self._store._vectors[name] = []
        
        return CollectionInfo(name=name, dimension=dimension, doc_count=0)
    
    def delete_collection(self, name: str) -> bool:
        if name not in self._store._collections:
            return False
        
        del self._store._collections[name]
        self._store._documents.pop(name, None)
        self._store._vectors.pop(name, None)
        return True
    
    def list_collections(self) -> List[CollectionInfo]:
        return [
            CollectionInfo(
                name=info["name"],
                dimension=info["dimension"],
                doc_count=info["document_count"]
            )
            for info in self._store._collections.values()
        ]
    
    def index_document(
        self,
        collection: str,
        content: str,
        document_id: str,
        metadata: Dict[str, Any]
    ) -> str:
        if collection not in self._store._collections:
            raise ValueError(f"Collection '{collection}' not found")
        
        doc_metadata = DocumentMetadata(
            source=metadata.get("source", "unknown"),
            title=metadata.get("title"),
            author=metadata.get("author"),
            tags=metadata.get("tags", []),
            custom=metadata
        )
        
        doc = Document(
            id=document_id,
            content=content,
            metadata=doc_metadata
        )
        
        self._store._documents[collection].append(doc)
        
        chunk_config = ChunkConfig()
        chunks = self._simple_chunk(content, chunk_size=chunk_config.chunk_size)
        
        chunk_metadata = doc_metadata.model_dump()
        chunk_metadata["document_id"] = document_id
        chunk_metadata["collection"] = collection
        
        for idx, chunk in enumerate(chunks):
            chunk_meta = chunk_metadata.copy()
            chunk_meta["chunk_index"] = idx
            vector = VectorDocument(
                id=chunk["id"],
                content=chunk["text"],
                metadata=chunk_meta
            )
            self._store._vectors[collection].append(vector)
        
        self._store._collections[collection]["document_count"] += 1
        return document_id
    
    def search(
        self,
        collection: str,
        query: str,
        top_k: int = 5
    ) -> List[RAGResult]:
        if collection not in self._store._collections:
            return []
        
        vectors = self._store._vectors.get(collection, [])
        
        results = []
        for vec in vectors:
            relevance = 0.7 + (hash(vec.content + query) % 30) / 100
            results.append(RAGResult(
                content=vec.content,
                score=round(relevance, 4),
                metadata={"id": vec.id, "document_id": vec.metadata.get("document_id")}
            ))
        
        results.sort(key=lambda x: x.score, reverse=True)
        return results[:top_k]
    
    def delete_document(self, collection: str, document_id: str) -> bool:
        if collection not in self._store._collections:
            return False
        
        docs = self._store._documents.get(collection, [])
        self._store._documents[collection] = [d for d in docs if d.id != document_id]
        
        vectors = self._store._vectors.get(collection, [])
        self._store._vectors[collection] = [v for v in vectors if v.metadata.get("document_id") != document_id]
        
        self._store._collections[collection]["document_count"] -= 1
        return True
