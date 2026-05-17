"""Domain services for RAG operations.

This module contains the pure domain logic for RAG operations,
separated from infrastructure concerns.
"""

import uuid
from typing import TYPE_CHECKING, Any, Dict, List, Optional

from services.ai_agents.domain.values import (
    ChunkConfig,
    Document,
    DocumentMetadata,
    VectorDocument,
)

if TYPE_CHECKING:
    from services.ai_agents.domain.ports import TextProcessorPort


def _default_split_sentences(content: str) -> List[str]:
    """Fallback sentence splitting using simple newline split."""
    return content.replace(".", ".\n").replace("!", "!\n").replace("?", "?\n").split("\n")


def _default_match_headings(content: str) -> List[tuple]:
    """Fallback heading matching using simple line scanning."""
    results = []
    for line in content.split("\n"):
        stripped = line.strip()
        if stripped.startswith("#"):
            parts = stripped.split(" ", 1)
            if len(parts) == 2:
                level = parts[0].count("#")
                results.append((str(level), parts[1]))
    return results


class TextChunkingService:
    """Domain service for text chunking operations.
    
    This service contains the pure business logic for splitting text
    into chunks according to various strategies.
    
    The text_processor parameter allows injection of infrastructure-level
    text processing (e.g., regex-based) while keeping the domain logic pure.
    """
    
    def __init__(self, text_processor: Optional["TextProcessorPort"] = None):
        """Initialize with optional text processor.

        Args:
            text_processor: Infrastructure adapter for text processing operations.
                           If None, uses simple fallback implementations.
        """
        self._text_processor = text_processor

    def _split_sentences(self, content: str) -> List[str]:
        """Split content into sentences using injected processor or fallback."""
        if self._text_processor is not None:
            return self._text_processor.split_sentences(content)
        return _default_split_sentences(content)

    def _match_headings(self, content: str) -> List[tuple]:
        """Match headings using injected processor or fallback."""
        if self._text_processor is not None:
            return self._text_processor.match_headings(content)
        return _default_match_headings(content)
    
    def chunk_by_size(
        self,
        content: str,
        chunk_size: int = 500,
        overlap: int = 50
    ) -> List[Dict[str, Any]]:
        """Split text into chunks by character size with overlap.
        
        Args:
            content: Text to split.
            chunk_size: Target size per chunk.
            overlap: Character overlap between chunks.
            
        Returns:
            List of chunk dictionaries.
        """
        chunks = []
        start = 0
        content_len = len(content)
        
        while start < content_len:
            end = min(start + chunk_size, content_len)
            chunk_text = content[start:end]
            
            chunks.append({
                "id": str(uuid.uuid4()),
                "text": chunk_text,
                "start_char": start,
                "end_char": end,
                "length": len(chunk_text)
            })
            
            start = end - overlap if overlap > 0 else end
        
        return chunks
    
    def chunk_by_sentences(
        self,
        content: str,
        chunk_size: int = 500,
        overlap: int = 50
    ) -> List[Dict[str, Any]]:
        """Split text into chunks by sentences.
        
        Args:
            content: Text to split.
            chunk_size: Target size per chunk.
            overlap: Number of sentences to overlap.
            
        Returns:
            List of chunk dictionaries.
        """
        sentences = self._split_sentences(content)
        
        chunks = []
        current_chunk = ""
        chunk_start = 0
        
        for i, sentence in enumerate(sentences):
            if len(current_chunk) + len(sentence) <= chunk_size:
                current_chunk += sentence + " "
            else:
                if current_chunk:
                    chunks.append({
                        "id": str(uuid.uuid4()),
                        "text": current_chunk.strip(),
                        "start_char": chunk_start,
                        "end_char": chunk_start + len(current_chunk),
                        "length": len(current_chunk)
                    })
                
                if overlap > 0 and chunks:
                    overlap_text = " ".join(sentences[max(0, i-overlap):i])
                    current_chunk = overlap_text + " " + sentence + " "
                    chunk_start = chunk_start + len(current_chunk) - len(sentence) - 1
                else:
                    current_chunk = sentence + " "
                    chunk_start += len(current_chunk)
        
        if current_chunk.strip():
            chunks.append({
                "id": str(uuid.uuid4()),
                "text": current_chunk.strip(),
                "start_char": chunk_start,
                "end_char": chunk_start + len(current_chunk),
                "length": len(current_chunk)
            })
        
        return chunks
    
    def chunk_by_headings(
        self,
        content: str,
        chunk_size: int = 500
    ) -> List[Dict[str, Any]]:
        """Split text by markdown headings.
        
        Args:
            content: Text with markdown headings.
            chunk_size: Target size per chunk.
            
        Returns:
            List of chunk dictionaries.
        """
        lines = content.split('\n')
        
        chunks = []
        current_section = ""
        current_heading = ""
        section_start = 0
        
        for line in lines:
            heading_matches = self._match_headings(line)
            if heading_matches:
                heading_level, heading_text = heading_matches[0]
                
                if current_section.strip():
                    chunks.append({
                        "id": str(uuid.uuid4()),
                        "text": current_section.strip(),
                        "heading": current_heading,
                        "start_char": section_start,
                        "end_char": section_start + len(current_section),
                        "length": len(current_section)
                    })
                
                current_heading = heading_text
                current_section = line + "\n"
                section_start += len(line) + 1
            else:
                current_section += line + "\n"
        
        if current_section.strip():
            chunks.append({
                "id": str(uuid.uuid4()),
                "text": current_section.strip(),
                "heading": current_heading,
                "start_char": section_start,
                "end_char": section_start + len(current_section),
                "length": len(current_section)
            })
        
        return chunks


class DocumentService:
    """Domain service for document operations.
    
    Contains pure business logic for document creation and management.
    """
    
    @staticmethod
    def create_document(
        content: str,
        source: str,
        title: Optional[str] = None,
        author: Optional[str] = None,
        tags: Optional[List[str]] = None,
        custom_metadata: Optional[Dict[str, Any]] = None,
        chunk_config: Optional[ChunkConfig] = None
    ) -> Document:
        """Create a domain document with metadata.
        
        Args:
            content: Document content.
            source: Document source.
            title: Optional title.
            author: Optional author.
            tags: Optional tags.
            custom_metadata: Custom metadata fields.
            chunk_config: Chunking configuration.
            
        Returns:
            Document entity.
        """
        metadata = DocumentMetadata(
            source=source,
            title=title,
            author=author,
            tags=tags or [],
            custom=custom_metadata or {}
        )
        
        return Document(
            id=str(uuid.uuid4()),
            content=content,
            metadata=metadata
        )
    
    @staticmethod
    def chunk_document(
        content: str,
        chunk_config: ChunkConfig
    ) -> List[Dict[str, Any]]:
        """Chunk document content according to configuration.
        
        Args:
            content: Document content.
            chunk_config: Chunking configuration.
            
        Returns:
            List of chunks.
        """
        strategy = chunk_config.strategy or "by_size"
        chunker = TextChunkingService()
        
        if strategy == "by_size":
            return chunker.chunk_by_size(
                content,
                chunk_config.chunk_size,
                chunk_config.chunk_overlap
            )
        elif strategy == "by_sentence":
            return chunker.chunk_by_sentences(
                content,
                chunk_config.chunk_size,
                chunk_config.chunk_overlap
            )
        elif strategy == "by_heading":
            return chunker.chunk_by_headings(
                content,
                chunk_config.chunk_size
            )
        else:
            return chunker.chunk_by_size(
                content,
                chunk_config.chunk_size,
                chunk_config.chunk_overlap
            )
