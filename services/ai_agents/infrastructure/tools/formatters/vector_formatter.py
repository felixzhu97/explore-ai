"""Output formatters for agent tools.

This module contains formatting functions for human-readable output
from agent tool operations.
"""

from typing import Any, Dict, List, Optional


def format_vector_results(results: List[Dict[str, Any]]) -> str:
    """Format vector search results for display.
    
    Args:
        results: List of search results.
        
    Returns:
        Formatted string.
    """
    if not results:
        return "No results found."
    
    lines = []
    for i, result in enumerate(results, 1):
        content = result.get("content", "")[:200]
        score = result.get("score", 0)
        doc_id = result.get("id", "unknown")
        
        lines.append(
            f"{i}. [Score: {score:.4f}] {content}..."
            f"\n   Document ID: {doc_id}"
        )
    
    return "\n\n".join(lines)


def format_collections(collections: List[str]) -> str:
    """Format list of collections.
    
    Args:
        collections: List of collection names.
        
    Returns:
        Formatted string.
    """
    if not collections:
        return "No collections found."
    
    return "Available collections:\n" + "\n".join(f"  - {c}" for c in collections)


def format_collection_info(info: Dict[str, Any]) -> str:
    """Format collection information.
    
    Args:
        info: Collection info dictionary.
        
    Returns:
        Formatted string.
    """
    if not info:
        return "Collection not found."
    
    lines = [
        f"📚 Collection: {info.get('name', 'unknown')}",
        f"   Dimension: {info.get('dimension', 'N/A')}",
        f"   Documents: {info.get('document_count', 0)}",
    ]
    
    return "\n".join(lines)


def format_document(doc: Any) -> str:
    """Format document for display.
    
    Args:
        doc: Document object.
        
    Returns:
        Formatted string.
    """
    if not doc:
        return "Document not found."
    
    metadata = getattr(doc, 'metadata', None)
    if metadata:
        source = getattr(metadata, 'source', 'unknown')
        title = getattr(metadata, 'title', None)
        author = getattr(metadata, 'author', None)
    else:
        source = title = author = 'N/A'
    
    lines = [
        f"📄 Document: {getattr(doc, 'id', 'unknown')}",
        f"   Source: {source}",
    ]
    
    if title:
        lines.append(f"   Title: {title}")
    if author:
        lines.append(f"   Author: {author}")
    
    content = getattr(doc, 'content', '')
    if content:
        lines.append(f"   Content Preview: {content[:150]}...")
    
    return "\n".join(lines)
