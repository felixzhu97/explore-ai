"""Repository interface for RAG operations."""
from abc import ABC, abstractmethod
from dataclasses import dataclass
from typing import List, Dict, Any

@dataclass(frozen=True)
class RAGResult:
    """Value object for RAG search results."""
    content: str
    score: float
    metadata: Dict[str, Any]
    
    def __repr__(self) -> str:
        return f"RAGResult(score={self.score:.2f}, content={self.content[:50]}...)"

@dataclass(frozen=True)
class CollectionInfo:
    """Value object for collection metadata."""
    name: str
    dimension: int
    doc_count: int

class RAGRepository(ABC):
    """Abstract repository for RAG operations."""
    
    @abstractmethod
    def create_collection(self, name: str, dimension: int) -> CollectionInfo:
        """Create a new collection."""
        pass
    
    @abstractmethod
    def delete_collection(self, name: str) -> bool:
        """Delete a collection."""
        pass
    
    @abstractmethod
    def list_collections(self) -> List[CollectionInfo]:
        """List all collections."""
        pass
    
    @abstractmethod
    def index_document(
        self, 
        collection: str, 
        content: str, 
        document_id: str,
        metadata: Dict[str, Any]
    ) -> str:
        """Index a document in a collection. Returns document ID."""
        pass
    
    @abstractmethod
    def search(
        self, 
        collection: str, 
        query: str, 
        top_k: int = 5
    ) -> List[RAGResult]:
        """Search for similar documents."""
        pass
    
    @abstractmethod
    def delete_document(self, collection: str, document_id: str) -> bool:
        """Delete a document from collection."""
        pass
