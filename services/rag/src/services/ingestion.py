import tiktoken
from typing import Optional
from loguru import logger
from ..core.embedding import get_embedding_model, EmbeddingModel
from ..core.vector_store import get_vector_store, VectorStore
from ..config import get_settings
from ..persistence.document_metadata import get_document_store, DocumentRecord, DocumentMetadataStore


class IngestionService:
    """Service for ingesting documents into the vector store."""

    def __init__(
        self,
        vector_store: Optional[VectorStore] = None,
        embedding_model: Optional[EmbeddingModel] = None,
        chunk_size: Optional[int] = None,
        chunk_overlap: Optional[int] = None,
        document_store: Optional[DocumentMetadataStore] = None,
    ):
        settings = get_settings()
        self.vector_store = vector_store or get_vector_store()
        self.embedding_model = embedding_model or get_embedding_model()
        self.chunk_size = chunk_size or settings.CHUNK_SIZE
        self.chunk_overlap = chunk_overlap or settings.CHUNK_OVERLAP
        self.document_store = document_store or get_document_store()

        try:
            self.encoding = tiktoken.get_encoding("cl100k_base")
        except Exception:
            logger.warning("tiktoken not available, using basic tokenization")
            self.encoding = None

    def _split_text(self, text: str) -> list[str]:
        """Split text into chunks based on token count."""
        if self.encoding:
            tokens = self.encoding.encode(text)
            chunks = []
            start = 0

            while start < len(tokens):
                end = start + self.chunk_size
                chunk_tokens = tokens[start:end]
                chunk_text = self.encoding.decode(chunk_tokens)
                if chunk_text.strip():
                    chunks.append(chunk_text.strip())
                start = end - self.chunk_overlap

            return chunks
        else:
            words = text.split()
            chunks = []
            for i in range(0, len(words), self.chunk_size):
                chunk = " ".join(words[i : i + self.chunk_size])
                if chunk.strip():
                    chunks.append(chunk.strip())
            return chunks

    async def ingest(
        self,
        text: str,
        metadata: dict,
        doc_id: str,
    ) -> dict:
        """Ingest a document into the vector store."""
        metadata_with_id = {**metadata, "doc_id": doc_id}

        # Record indexing start
        self.document_store.add_indexing_history(
            doc_id=doc_id,
            action="ingest",
            status="in_progress",
        )

        chunks = self._split_text(text)
        if not chunks:
            logger.warning(f"No chunks generated for doc_id: {doc_id}")
            self.document_store.complete_indexing_history(
                doc_id=doc_id,
                status="failed",
                error_message="No chunks generated",
            )
            return {"doc_id": doc_id, "chunks": 0}

        # Update document status to indexing
        self.document_store.update_document(
            doc_id=doc_id,
            status="indexing",
            chunk_count=len(chunks),
        )

        logger.info(f"Generating embeddings for {len(chunks)} chunks")
        vectors = self.embedding_model.embed(chunks)

        metadata_list = [metadata_with_id] * len(chunks)

        chunk_ids = self.vector_store.insert(
            vectors=vectors,
            texts=chunks,
            metadata=metadata_list,
        )

        # Update document status to completed
        self.document_store.update_document(
            doc_id=doc_id,
            status="completed",
            chunk_count=len(chunks),
            indexed_at=metadata_with_id.get("indexed_at"),
        )
        self.document_store.complete_indexing_history(
            doc_id=doc_id,
            status="completed",
        )

        logger.info(f"Ingested {len(chunks)} chunks for doc_id: {doc_id}")

        return {
            "doc_id": doc_id,
            "chunks": len(chunks),
            "chunk_ids": chunk_ids,
        }

    async def ingest_with_chunks(
        self,
        chunks: list[str],
        metadata: dict,
        doc_id: str,
    ) -> dict:
        """Ingest pre-chunked text into the vector store."""
        if not chunks:
            return {"doc_id": doc_id, "chunks": 0}

        metadata_with_id = {**metadata, "doc_id": doc_id}

        vectors = self.embedding_model.embed(chunks)
        metadata_list = [metadata_with_id] * len(chunks)

        chunk_ids = self.vector_store.insert(
            vectors=vectors,
            texts=chunks,
            metadata=metadata_list,
        )

        logger.info(f"Ingested {len(chunks)} pre-chunked segments for doc_id: {doc_id}")

        return {
            "doc_id": doc_id,
            "chunks": len(chunks),
            "chunk_ids": chunk_ids,
        }
