import uuid
import time
from fastapi import APIRouter, UploadFile, File, HTTPException, Query
from typing import Optional
from loguru import logger
from ..schemas import (
    UploadResponse,
    DocumentListResponse,
    DocumentStats,
    DocumentMetadata,
    DocumentSource,
)
from ..document_loader.loader import DocumentLoaderFactory, load_from_url
from ..services.ingestion import IngestionService
from ..core.vector_store import get_vector_store
from ..config import get_settings
from ..persistence.document_metadata import get_document_store, DocumentRecord

router = APIRouter(prefix="/documents", tags=["documents"])


@router.post("/upload", response_model=UploadResponse)
async def upload_document(
    file: UploadFile = File(...),
    title: Optional[str] = None,
    collection: Optional[str] = None,
):
    """Upload and ingest a document."""
    settings = get_settings()
    doc_store = get_document_store()

    content = await file.read()

    if len(content) > settings.MAX_FILE_SIZE:
        raise HTTPException(
            status_code=400,
            detail=f"File too large. Maximum size is {settings.MAX_FILE_SIZE // (1024*1024)}MB",
        )

    filename = file.filename or "unknown"
    ext = filename.lower().split(".")[-1] if "." in filename else ""

    source_map = {
        "md": DocumentSource.MARKDOWN,
        "markdown": DocumentSource.MARKDOWN,
        "pdf": DocumentSource.PDF,
        "txt": DocumentSource.TEXT,
        "text": DocumentSource.TEXT,
    }
    source = source_map.get(ext, DocumentSource.TEXT)

    doc_id = str(uuid.uuid4())
    metadata = DocumentMetadata(
        source=source,
        filename=filename,
        title=title or filename,
        doc_id=doc_id,
    )

    # Create document record
    record = DocumentRecord(
        doc_id=doc_id,
        title=title or filename,
        source=source.value,
        filename=filename,
        file_size=len(content),
        mime_type=file.content_type,
        status="pending",
        chunk_size=settings.CHUNK_SIZE,
        chunk_overlap=settings.CHUNK_OVERLAP,
    )
    doc_store.add_document(record)

    try:
        chunks = await DocumentLoaderFactory.load(content=content, metadata=metadata)

        if not chunks:
            doc_store.update_document(doc_id=doc_id, status="failed", error_message="No content extracted")
            raise HTTPException(status_code=400, detail="No content could be extracted from the file")

        ingestion_service = IngestionService()
        full_text = "\n\n".join(chunk["text"] for chunk in chunks)

        result = await ingestion_service.ingest(
            text=full_text,
            metadata=metadata.model_dump(exclude_none=True),
            doc_id=doc_id,
        )

        logger.info(f"Uploaded document {doc_id}: {filename} with {result['chunks']} chunks")

        return UploadResponse(
            doc_id=doc_id,
            filename=filename,
            chunks=result["chunks"],
            status="success",
        )

    except Exception as e:
        doc_store.update_document(doc_id=doc_id, status="failed", error_message=str(e))
        logger.error(f"Error uploading document: {e}")
        raise HTTPException(status_code=500, detail=f"Failed to process document: {str(e)}")


@router.post("/ingest-url")
async def ingest_url(
    url: str = Query(..., description="URL to ingest"),
    title: Optional[str] = Query(None, description="Document title"),
):
    """Ingest a document from a URL."""
    doc_store = get_document_store()
    doc_id = str(uuid.uuid4())

    metadata = DocumentMetadata(
        source=DocumentSource.WEB,
        url=url,
        title=title or url,
        doc_id=doc_id,
    )

    # Create document record
    record = DocumentRecord(
        doc_id=doc_id,
        title=title or url,
        source=DocumentSource.WEB.value,
        status="pending",
    )
    doc_store.add_document(record)

    try:
        chunks = await load_from_url(url=url, metadata=metadata)

        if not chunks:
            doc_store.update_document(doc_id=doc_id, status="failed", error_message="No content extracted")
            raise HTTPException(status_code=400, detail="No content could be extracted from the URL")

        ingestion_service = IngestionService()
        full_text = "\n\n".join(chunk["text"] for chunk in chunks)

        result = await ingestion_service.ingest(
            text=full_text,
            metadata=metadata.model_dump(exclude_none=True),
            doc_id=doc_id,
        )

        logger.info(f"Ingested URL {doc_id}: {url} with {result['chunks']} chunks")

        return UploadResponse(
            doc_id=doc_id,
            filename=url,
            chunks=result["chunks"],
            status="success",
        )

    except Exception as e:
        doc_store.update_document(doc_id=doc_id, status="failed", error_message=str(e))
        logger.error(f"Error ingesting URL: {e}")
        raise HTTPException(status_code=500, detail=f"Failed to ingest URL: {str(e)}")


@router.get("/database", response_model=DocumentListResponse)
async def list_documents_from_database():
    """List all documents stored in the vector database."""
    doc_store = get_document_store()

    try:
        # Get document list from persistent store
        doc_records = doc_store.list_documents(limit=500)

        documents = [
            DocumentStats(
                doc_id=doc.doc_id,
                filename=doc.filename or doc.title,
                total_chunks=doc.chunk_count,
                source=doc.source,
                uploaded_at=doc.created_at,
            )
            for doc in doc_records
        ]

        return DocumentListResponse(documents=documents, total=len(documents))
    except Exception as e:
        logger.error(f"Error listing documents from database: {e}")
        raise HTTPException(status_code=500, detail=f"Failed to list documents: {str(e)}")


@router.get("/")
async def list_documents(collection: Optional[str] = None):
    """List all uploaded documents from persistent store."""
    doc_store = get_document_store()

    doc_records = doc_store.list_documents(limit=100)

    documents = [
        DocumentStats(
            doc_id=doc.doc_id,
            filename=doc.filename or doc.title,
            total_chunks=doc.chunk_count,
            source=doc.source,
            uploaded_at=doc.created_at,
        )
        for doc in doc_records
    ]

    return {"documents": documents, "total": len(documents)}


@router.get("/{doc_id}/stats")
async def get_document_stats(doc_id: str):
    """Get statistics for a specific document."""
    doc_store = get_document_store()
    doc = doc_store.get_document(doc_id)

    if not doc:
        raise HTTPException(status_code=404, detail="Document not found")

    vector_store = get_vector_store()

    return {
        "doc_id": doc_id,
        "filename": doc.filename or doc.title,
        "title": doc.title,
        "total_chunks": doc.chunk_count,
        "source": doc.source,
        "status": doc.status,
        "created_at": doc.created_at,
        "indexed_at": doc.indexed_at,
        "file_size": doc.file_size,
        "vector_stats": vector_store.get_stats(),
    }


@router.delete("/{doc_id}")
async def delete_document(doc_id: str):
    """Delete a document and its associated vectors."""
    doc_store = get_document_store()
    doc = doc_store.get_document(doc_id)

    if not doc:
        raise HTTPException(status_code=404, detail="Document not found")

    try:
        vector_store = get_vector_store()
        vector_store.delete_by_doc_id(doc_id)

        # Delete persistent record
        doc_store.delete_document(doc_id)

        logger.info(f"Deleted document: {doc_id}")

        return {"status": "success", "message": f"Document {doc_id} deleted"}

    except Exception as e:
        logger.error(f"Error deleting document {doc_id}: {e}")
        raise HTTPException(status_code=500, detail=f"Failed to delete document: {str(e)}")
