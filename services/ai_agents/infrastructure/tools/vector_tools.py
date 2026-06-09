"""Vector database tools with mock operations."""

from typing import List, Dict, Any, Optional
from pydantic import BaseModel
from langchain_core.tools import BaseTool, tool


# In-memory mock store for demo purposes
_vector_store: Dict[str, List[Dict[str, Any]]] = {
    "default": [
        {"id": "1", "content": "Machine learning is a subset of AI", "metadata": {"source": "wiki"}},
        {"id": "2", "content": "Deep learning uses neural networks", "metadata": {"source": "wiki"}},
        {"id": "3", "content": "Python is popular for data science", "metadata": {"source": "blog"}},
    ],
    "documents": [
        {"id": "doc1", "content": "Kubernetes container orchestration", "metadata": {"type": "tech"}},
        {"id": "doc2", "content": "Docker container technology", "metadata": {"type": "tech"}},
    ],
}


@tool("list_collections")
def list_collections() -> str:
    """List all available vector collections."""
    collections = list(_vector_store.keys())
    if not collections:
        return "No collections found."
    return f"Available collections: {', '.join(collections)}"


@tool("create_collection")
def create_collection(name: str, dimension: int = 1536, description: str = "") -> str:
    """Create a new vector collection.
    
    Args:
        name: Collection name
        dimension: Embedding dimension (default: 1536)
        description: Optional description
    """
    if name in _vector_store:
        return f"Collection '{name}' already exists."
    _vector_store[name] = []
    return f"Collection '{name}' created successfully (dimension={dimension})."


@tool("search_vectors")
def search_vectors(query: str, collection: str = "default", top_k: int = 5) -> str:
    """Search for similar vectors in a collection.
    
    Args:
        query: Search query
        collection: Collection name (default: default)
        top_k: Number of results (default: 5)
    """
    if collection not in _vector_store:
        return f"Collection '{collection}' not found."
    
    items = _vector_store[collection]
    if not items:
        return f"Collection '{collection}' is empty."
    
    # Simple mock search - just return all items for demo
    results = items[:top_k]
    
    formatted = [f"[{i+1}] {item['content']} (score: {0.9 - i*0.1:.2f})" 
                 for i, item in enumerate(results)]
    
    return f"Found {len(results)} results in '{collection}':\n" + "\n".join(formatted)


@tool("insert_vector")
def insert_vector(content: str, collection: str = "default", metadata: Optional[dict] = None) -> str:
    """Insert a vector into a collection.
    
    Args:
        content: Text content to embed
        collection: Collection name (default: default)
        metadata: Optional metadata
    """
    if collection not in _vector_store:
        _vector_store[collection] = []
    
    import uuid
    doc_id = str(uuid.uuid4())[:8]
    _vector_store[collection].append({
        "id": doc_id,
        "content": content,
        "metadata": metadata or {}
    })
    
    return f"Inserted document '{doc_id}' into '{collection}'."


@tool("get_collection_info")
def get_collection_info(collection: str = "default") -> str:
    """Get information about a collection."""
    if collection not in _vector_store:
        return f"Collection '{collection}' not found."
    
    count = len(_vector_store[collection])
    return f"Collection: {collection}\nDocuments: {count}\nDimension: 1536"


@tool("delete_collection")
def delete_collection(name: str) -> str:
    """Delete a vector collection."""
    if name not in _vector_store:
        return f"Collection '{name}' not found."
    
    del _vector_store[name]
    return f"Collection '{name}' deleted."


def get_all_vector_tools() -> List[BaseTool]:
    """Get all vector database tools."""
    return [
        list_collections,
        create_collection,
        search_vectors,
        insert_vector,
        get_collection_info,
        delete_collection,
    ]
