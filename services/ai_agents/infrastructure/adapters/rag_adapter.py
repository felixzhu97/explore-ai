"""RAG Adapter - Anti-Corruption Layer for RAG operations.

This adapter implements the RAGRepository interface and translates
domain concepts to external vector store operations.
"""

import uuid
from typing import Any, Dict, List, Optional

from services.ai_agents.domain.repositories.rag_repository import (
    CollectionInfo,
    RAGRepository,
    RAGResult,
)
from services.ai_agents.domain.ports import RAGPort


class RAGAdapter(RAGRepository):
    """Adapter for RAG operations implementing the RAGRepository interface.

    This adapter translates domain operations to external vector store operations,
    acting as an Anti-Corruption Layer (ACL) between the domain and infrastructure.
    """

    def __init__(self, vector_store: Optional[RAGPort] = None):
        """Initialize the RAG adapter.

        Args:
            vector_store: External vector store port for actual operations.
                          If None, uses in-memory storage.
        """
        self._vector_store = vector_store
        self._collections: Dict[str, Dict[str, Any]] = {}
        self._documents: Dict[str, List[Dict[str, Any]]] = {}

    def create_collection(self, name: str, dimension: int) -> CollectionInfo:
        """Create a new RAG collection.

        Args:
            name: Collection name.
            dimension: Embedding dimension.

        Returns:
            CollectionInfo with collection metadata.

        Raises:
            ValueError: If collection already exists.
        """
        if name in self._collections:
            raise ValueError(f"Collection '{name}' already exists")

        if self._vector_store is not None:
            self._vector_store.create_collection(name=name, dimension=dimension)

        self._collections[name] = {
            "name": name,
            "dimension": dimension,
            "document_count": 0
        }
        self._documents[name] = []

        return CollectionInfo(
            name=name,
            dimension=dimension,
            doc_count=0
        )

    def delete_collection(self, name: str) -> bool:
        """Delete a RAG collection.

        Args:
            name: Collection name.

        Returns:
            True if deleted, False if not found.
        """
        if name not in self._collections:
            return False

        if self._vector_store is not None:
            self._vector_store.delete_document(collection=name, document_id="")

        del self._collections[name]
        if name in self._documents:
            del self._documents[name]
        return True

    def list_collections(self) -> List[CollectionInfo]:
        """List all RAG collections.

        Returns:
            List of CollectionInfo objects.
        """
        return [
            CollectionInfo(
                name=name,
                dimension=info["dimension"],
                doc_count=info["document_count"]
            )
            for name, info in self._collections.items()
        ]

    def index_document(
        self,
        collection: str,
        content: str,
        document_id: str,
        metadata: Dict[str, Any]
    ) -> str:
        """Index a document in a RAG collection.

        Args:
            collection: Target collection name.
            content: Document content.
            document_id: Document ID (generated if empty).
            metadata: Document metadata.

        Returns:
            The document ID.
        """
        if not document_id:
            document_id = str(uuid.uuid4())

        if collection not in self._collections:
            self.create_collection(collection, dimension=1536)

        doc = {
            "id": document_id,
            "content": content,
            "metadata": metadata
        }

        if collection not in self._documents:
            self._documents[collection] = []
        self._documents[collection].append(doc)

        self._collections[collection]["document_count"] = len(self._documents[collection])

        if self._vector_store is not None:
            self._vector_store.index_document(
                collection=collection,
                content=content,
                metadata=metadata
            )

        return document_id

    def search(
        self,
        collection: str,
        query: str,
        top_k: int = 5
    ) -> List[RAGResult]:
        """Search for relevant documents in a collection.

        Args:
            collection: Collection to search.
            query: Search query.
            top_k: Number of results.

        Returns:
            List of RAGResult objects.
        """
        if self._vector_store is not None:
            results = self._vector_store.search(
                collection=collection,
                query=query,
                top_k=top_k
            )
            return [
                RAGResult(
                    content=r.get("content", ""),
                    score=r.get("score", 0.0),
                    metadata=r.get("metadata", {})
                )
                for r in results
            ]

        if collection not in self._documents:
            return []

        docs = self._documents[collection]
        results = []
        query_lower = query.lower()

        for doc in docs[:top_k]:
            content = doc.get("content", "")
            content_lower = content.lower()
            score = 0.5

            if query_lower in content_lower:
                score = 0.9
            elif any(word in content_lower for word in query_lower.split()):
                score = 0.7

            results.append(RAGResult(
                content=content[:500] + "..." if len(content) > 500 else content,
                score=score,
                metadata=doc.get("metadata", {})
            ))

        return results

    def delete_document(self, collection: str, document_id: str) -> bool:
        """Delete a document from a collection.

        Args:
            collection: Collection name.
            document_id: Document ID.

        Returns:
            True if deleted, False if not found.
        """
        if collection not in self._documents:
            return False

        original_len = len(self._documents[collection])
        self._documents[collection] = [
            d for d in self._documents[collection]
            if d["id"] != document_id
        ]

        deleted = original_len - len(self._documents[collection])
        if deleted > 0:
            self._collections[collection]["document_count"] = len(self._documents[collection])

        if deleted > 0 and self._vector_store is not None:
            self._vector_store.delete_document(
                collection=collection,
                document_id=document_id
            )

        return deleted > 0
