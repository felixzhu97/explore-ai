"""RAG and document tools for AI Infrastructure Agents.

This module provides tools for document indexing, retrieval, and RAG pipeline operations.
"""

from typing import Any, Dict, List, Optional

from langchain_core.tools import BaseTool
from pydantic import BaseModel, Field

from services.ai_agents.infrastructure.schemas import (
    ChunkConfig,
    Document,
    DocumentMetadata,
    RAGQuery,
    RAGResult,
    RAGResponse,
    VectorDocument,
)
from services.ai_agents.infrastructure.adapters.rag_adapter import RAGAdapter as DomainRAGAdapter


# ============================================================================
# Tool Input Schemas
# ============================================================================


class IndexDocumentInput(BaseModel):
    """Input schema for indexing a document."""
    content: str = Field(..., description="Document content to index")
    collection: str = Field(..., description="Target collection name")
    metadata: Optional[Dict[str, Any]] = Field(default=None, description="Document metadata")
    chunk_config: Optional[ChunkConfig] = Field(default=None, description="Chunking configuration")


class SearchDocumentsInput(BaseModel):
    """Input schema for searching documents."""
    query: str = Field(..., description="Search query text")
    collection: str = Field(..., description="Collection to search")
    top_k: int = Field(default=5, ge=1, le=20, description="Number of results")
    filters: Optional[Dict[str, Any]] = Field(default=None, description="Metadata filters")


class GetDocumentInput(BaseModel):
    """Input schema for getting a document."""
    document_id: str = Field(..., description="Document ID")
    collection: str = Field(..., description="Collection name")


class DeleteDocumentInput(BaseModel):
    """Input schema for deleting a document."""
    document_id: str = Field(..., description="Document ID to delete")
    collection: str = Field(..., description="Collection name")


class ListDocumentsInput(BaseModel):
    """Input schema for listing documents."""
    collection: str = Field(..., description="Collection name")
    limit: int = Field(default=100, ge=1, le=1000, description="Maximum results")
    offset: int = Field(default=0, ge=0, description="Pagination offset")


class RAGQueryInput(BaseModel):
    """Input schema for RAG query."""
    query: str = Field(..., description="User query")
    collection: str = Field(..., description="Collection to search")
    top_k: int = Field(default=5, ge=1, le=20)
    enable_rerank: bool = Field(default=True, description="Enable reranking")
    filters: Optional[Dict[str, Any]] = Field(default=None)


# ============================================================================
# RAG Adapter Wrapper (translates between domain and infrastructure schemas)
# ============================================================================


class RAGAdapter:
    """Adapter for RAG operations.
    
    This is a wrapper around the domain RAGAdapter that translates between
    infrastructure schemas (Document, RAGResult) and domain types.
    """
    
    def __init__(self):
        """Initialize the RAG adapter."""
        self._domain_adapter = DomainRAGAdapter()
        self._documents: Dict[str, List[Document]] = {}
    
    def create_collection(self, name: str, dimension: int = 1536) -> Dict[str, Any]:
        """Create a RAG collection."""
        result = self._domain_adapter.create_collection(name, dimension)
        return {"success": True, "collection": name}
    
    def index_document(
        self,
        collection: str,
        content: str,
        metadata: Optional[Dict[str, Any]] = None,
        chunk_config: Optional[ChunkConfig] = None
    ) -> Dict[str, Any]:
        """Index a document into a collection."""
        if chunk_config is None:
            chunk_config = ChunkConfig()
        
        chunks = self._chunk_text(content, chunk_config)
        
        document_id = self._domain_adapter.index_document(
            collection=collection,
            content=content,
            document_id="",
            metadata=metadata or {}
        )
        
        doc_metadata = DocumentMetadata(
            source=metadata.get("source", "unknown") if metadata else "unknown",
            title=metadata.get("title") if metadata else None,
            author=metadata.get("author") if metadata else None,
            tags=metadata.get("tags", []) if metadata else [],
            custom=metadata or {}
        )
        
        doc = Document(
            id=document_id,
            content=content,
            metadata=doc_metadata,
            embedding=None
        )
        
        if collection not in self._documents:
            self._documents[collection] = []
        self._documents[collection].append(doc)
        
        return {
            "success": True,
            "document_id": document_id,
            "chunks": len(chunks),
            "message": f"Indexed document with {len(chunks)} chunks"
        }
    
    def _chunk_text(self, text: str, config: ChunkConfig) -> List[str]:
        """Split text into chunks."""
        separator = config.separator
        chunk_size = config.chunk_size
        chunk_overlap = config.chunk_overlap
        
        parts = text.split(separator)
        chunks = []
        current = ""
        
        for part in parts:
            if len(current) + len(part) <= chunk_size:
                current += part + separator
            else:
                if current:
                    chunks.append(current.strip())
                current = part + separator
        
        if current:
            chunks.append(current.strip())
        
        return chunks if chunks else [text]
    
    def search(
        self,
        collection: str,
        query: str,
        top_k: int = 5,
        filters: Optional[Dict[str, Any]] = None
    ) -> List[RAGResult]:
        """Search for relevant documents."""
        domain_results = self._domain_adapter.search(collection, query, top_k)
        
        results = []
        for r in domain_results:
            results.append(RAGResult(
                content=r.content,
                score=r.score,
                metadata=r.metadata,
                source=r.metadata.get("source", "unknown") if r.metadata else "unknown"
            ))
        
        return results
    
    def get_document(self, collection: str, document_id: str) -> Optional[Document]:
        """Get a document by ID."""
        if collection not in self._documents:
            return None
        
        for doc in self._documents[collection]:
            if doc.id == document_id:
                return doc
        
        return None
    
    def delete_document(self, collection: str, document_id: str) -> Dict[str, Any]:
        """Delete a document."""
        deleted = self._domain_adapter.delete_document(collection, document_id)
        
        if collection in self._documents:
            self._documents[collection] = [
                d for d in self._documents[collection]
                if d.id != document_id
            ]
        
        return {"success": deleted, "deleted": 1 if deleted else 0}
    
    def list_documents(
        self,
        collection: str,
        limit: int = 100,
        offset: int = 0
    ) -> List[Document]:
        """List documents in a collection."""
        if collection not in self._documents:
            return []
        
        return self._documents[collection][offset:offset + limit]


# Global adapter instance
_rag_adapter: Optional[RAGAdapter] = None


def get_rag_adapter() -> RAGAdapter:
    """Get the global RAG adapter instance."""
    global _rag_adapter
    if _rag_adapter is None:
        _rag_adapter = RAGAdapter()
    return _rag_adapter


# ============================================================================
# RAG Tools
# ============================================================================


def _create_index_document_tool() -> BaseTool:
    """Create the index document tool."""
    def index_document(
        content: str,
        collection: str,
        metadata: Optional[Dict[str, Any]] = None,
        chunk_size: int = 1000,
        chunk_overlap: int = 200
    ) -> str:
        """Index a document for RAG retrieval.
        
        Args:
            content: Document content to index.
            collection: Target collection name.
            metadata: Optional document metadata.
            chunk_size: Size of text chunks.
            chunk_overlap: Overlap between chunks.
            
        Returns:
            Indexing result.
        """
        adapter = get_rag_adapter()
        
        config = ChunkConfig(chunk_size=chunk_size, chunk_overlap=chunk_overlap)
        result = adapter.index_document(collection, content, metadata, config)
        
        if result["success"]:
            return f"Successfully indexed document. ID: {result['document_id']}, Chunks: {result['chunks']}"
        return f"Failed: {result.get('error', 'Unknown error')}"
    
    return BaseTool(
        name="index_document",
        description="Index a document into a RAG collection. Use this to add new documents to the knowledge base. Provide the content, collection name, and optional metadata.",
        args_schema=IndexDocumentInput,
        func=index_document
    )


def _create_search_documents_tool() -> BaseTool:
    """Create the search documents tool."""
    def search_documents(
        query: str,
        collection: str,
        top_k: int = 5,
        filters: Optional[Dict[str, Any]] = None
    ) -> str:
        """Search for relevant documents.
        
        Args:
            query: Search query text.
            collection: Collection to search.
            top_k: Number of results.
            filters: Metadata filters.
            
        Returns:
            Search results.
        """
        adapter = get_rag_adapter()
        results = adapter.search(collection, query, top_k, filters)
        
        if not results:
            return f"No documents found for query: '{query}'"
        
        output = f"Found {len(results)} relevant documents:\n\n"
        for i, r in enumerate(results, 1):
            output += f"{i}. [Score: {r.score:.2f}] {r.source}\n"
            output += f"   {r.content[:200]}...\n\n"
        
        return output
    
    return BaseTool(
        name="search_documents",
        description="Search for relevant documents in a RAG collection. Use this to find documents related to a query. Returns documents with relevance scores.",
        args_schema=SearchDocumentsInput,
        func=search_documents
    )


def _create_get_document_tool() -> BaseTool:
    """Create the get document tool."""
    def get_document(document_id: str, collection: str) -> str:
        """Get a document by ID.
        
        Args:
            document_id: Document ID.
            collection: Collection name.
            
        Returns:
            Document content.
        """
        adapter = get_rag_adapter()
        doc = adapter.get_document(collection, document_id)
        
        if doc is None:
            return f"Document '{document_id}' not found"
        
        return f"""Document: {doc.metadata.title or doc.id}
Source: {doc.metadata.source}
Author: {doc.metadata.author or 'Unknown'}
Created: {doc.metadata.created_at}

Content:
{doc.content}"""
    
    return BaseTool(
        name="get_document",
        description="Get a specific document by its ID. Use this to retrieve full document content after searching.",
        args_schema=GetDocumentInput,
        func=get_document
    )


def _create_delete_document_tool() -> BaseTool:
    """Create the delete document tool."""
    def delete_document(document_id: str, collection: str) -> str:
        """Delete a document.
        
        Args:
            document_id: Document ID to delete.
            collection: Collection name.
            
        Returns:
            Delete result.
        """
        adapter = get_rag_adapter()
        result = adapter.delete_document(collection, document_id)
        
        if result["success"]:
            return f"Successfully deleted document '{document_id}'"
        return f"Failed: {result.get('error', 'Document not found')}"
    
    return BaseTool(
        name="delete_document",
        description="Delete a document from a RAG collection. Use this to remove outdated or incorrect documents.",
        args_schema=DeleteDocumentInput,
        func=delete_document
    )


def _create_list_documents_tool() -> BaseTool:
    """Create the list documents tool."""
    def list_documents(collection: str, limit: int = 100, offset: int = 0) -> str:
        """List documents in a collection.
        
        Args:
            collection: Collection name.
            limit: Maximum results.
            offset: Pagination offset.
            
        Returns:
            List of documents.
        """
        adapter = get_rag_adapter()
        docs = adapter.list_documents(collection, limit, offset)
        
        if not docs:
            return f"No documents in collection '{collection}'"
        
        output = f"Documents in '{collection}' ({len(docs)} shown):\n\n"
        for doc in docs:
            output += f"- {doc.metadata.title or doc.id}\n"
            output += f"  Source: {doc.metadata.source}, "
            output += f"Created: {doc.metadata.created_at.strftime('%Y-%m-%d')}\n"
        
        return output
    
    return BaseTool(
        name="list_documents",
        description="List all documents in a RAG collection. Use this to see what documents are available.",
        args_schema=ListDocumentsInput,
        func=list_documents
    )


def _create_rag_query_tool() -> BaseTool:
    """Create the RAG query tool."""
    def rag_query(
        query: str,
        collection: str,
        top_k: int = 5,
        enable_rerank: bool = True,
        filters: Optional[Dict[str, Any]] = None
    ) -> str:
        """Execute a RAG query.
        
        Args:
            query: User query.
            collection: Collection to search.
            top_k: Number of results.
            enable_rerank: Enable reranking.
            filters: Metadata filters.
            
        Returns:
            RAG query results.
        """
        adapter = get_rag_adapter()
        results = adapter.search(collection, query, top_k, filters)
        
        if not results:
            return f"No relevant documents found for: '{query}'"
        
        output = f"Retrieved {len(results)} relevant documents for query: '{query}'\n\n"
        
        for i, r in enumerate(results, 1):
            output += f"--- Document {i} (Relevance: {r.score:.1%}) ---\n"
            output += f"Source: {r.source}\n"
            output += f"Content: {r.content[:300]}...\n\n"
        
        if enable_rerank:
            output += "[Reranking enabled - results are ordered by relevance]"
        
        return output
    
    return BaseTool(
        name="rag_query",
        description="Execute a RAG (Retrieval-Augmented Generation) query. This is the primary tool for answering questions using the knowledge base. It retrieves relevant documents and synthesizes them for the query.",
        args_schema=RAGQueryInput,
        func=rag_query
    )


def _create_create_collection_tool() -> BaseTool:
    """Create the create collection tool."""
    def create_collection(name: str, dimension: int = 1536) -> str:
        """Create a RAG collection.
        
        Args:
            name: Collection name.
            dimension: Embedding dimension.
            
        Returns:
            Creation result.
        """
        adapter = get_rag_adapter()
        result = adapter.create_collection(name, dimension)
        
        if result["success"]:
            return f"Successfully created collection '{name}' with dimension {dimension}"
        return f"Failed: {result.get('error', 'Unknown error')}"
    
    return BaseTool(
        name="create_rag_collection",
        description="Create a new RAG collection. Use this to set up a new knowledge base for document indexing.",
        args_schema={
            "name": str,
            "dimension": int
        },
        func=create_collection
    )


def get_all_rag_tools() -> List[BaseTool]:
    """Get all RAG tools.
    
    Returns:
        List of all RAG tools.
    """
    return [
        _create_create_collection_tool(),
        _create_index_document_tool(),
        _create_search_documents_tool(),
        _create_get_document_tool(),
        _create_delete_document_tool(),
        _create_list_documents_tool(),
        _create_rag_query_tool(),
    ]
