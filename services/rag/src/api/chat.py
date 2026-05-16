import uuid
import time
import json
from fastapi import APIRouter, HTTPException
from fastapi.responses import StreamingResponse
from typing import Optional
from loguru import logger
from ..schemas import ChatRequest, ChatResponse, ChatHistoryItem
from ..services.rag_chain import RAGChain
from ..config import get_settings
from ..persistence.session_store import get_session_store, SessionStore
from ..persistence.cache_manager import get_cache_manager, reset_cache_manager

router = APIRouter(prefix="/chat", tags=["chat"])


@router.post("/", response_model=ChatResponse)
async def chat(request: ChatRequest):
    """Process a chat query and return a response with sources."""
    settings = get_settings()
    session_store = get_session_store()

    session_id = request.session_id or str(uuid.uuid4())

    # Ensure session exists
    session = session_store.get_session(session_id)
    if not session:
        session_store.create_session(session_id)

    try:
        rag_chain = RAGChain(top_k=request.top_k)
        response = await rag_chain.query(request)

        # Persist messages to database
        session_store.add_message(
            session_id=session_id,
            role="user",
            content=request.query,
        )
        session_store.add_message(
            session_id=session_id,
            role="assistant",
            content=response.answer,
            sources=[s.model_dump() for s in response.sources],
        )

        logger.info(f"Chat session {session_id}: query processed successfully")

        return ChatResponse(
            answer=response.answer,
            sources=response.sources,
            session_id=session_id,
            model=settings.LLM_MODEL,
            processing_time_ms=response.processing_time_ms,
        )

    except Exception as e:
        logger.error(f"Error processing chat: {e}")
        raise HTTPException(status_code=500, detail=f"Failed to process query: {str(e)}")


@router.post("/stream")
async def stream_chat(request: ChatRequest):
    """Stream a chat response token by token."""
    settings = get_settings()

    session_id = request.session_id or str(uuid.uuid4())

    if session_id not in _chat_histories:
        _chat_histories[session_id] = []

    try:
        rag_chain = RAGChain(top_k=request.top_k)

        full_response = []
        sources = []
        processing_time_ms = 0

        async def generate():
            nonlocal sources, processing_time_ms
            try:
                result = await rag_chain.query(request)
                sources = result.sources
                processing_time_ms = result.processing_time_ms

                # Stream the answer content
                if hasattr(result.answer, '__iter__') and not isinstance(result.answer, str):
                    for part in result.answer:
                        yield f"data: {part.replace(chr(10), '<br>')}\n\n"
                else:
                    # For non-streaming responses, send in chunks
                    answer_text = result.answer
                    chunk_size = 20  # Send 20 chars at a time for streaming effect
                    for i in range(0, len(answer_text), chunk_size):
                        chunk = answer_text[i:i+chunk_size]
                        yield f"data: {chunk.replace(chr(10), '<br>')}\n\n"
                        # Small delay for smoother streaming effect
                        import asyncio
                        await asyncio.sleep(0.02)
            except Exception as e:
                logger.error(f"Stream error: {e}")
                yield f"data: Error: {str(e)}\n\n"
            # Send sources and metadata at the end
            yield f"data: [DONE]\n\n"
            yield f"event: sources\n"
            yield f"data: {json.dumps([s.model_dump() for s in sources])}\n\n"
            yield f"event: meta\n"
            yield f"data: {json.dumps({'processing_time_ms': processing_time_ms, 'model': settings.LLM_MODEL})}\n\n"

        return StreamingResponse(
            generate(),
            media_type="text/event-stream",
            headers={
                "Cache-Control": "no-cache",
                "Connection": "keep-alive",
                "X-Session-Id": session_id,
            },
        )

    except Exception as e:
        logger.error(f"Error in stream chat: {e}")
        raise HTTPException(status_code=500, detail=f"Failed to stream response: {str(e)}")


@router.get("/history/{session_id}")
async def get_history(session_id: str):
    """Get chat history for a session."""
    session_store = get_session_store()
    messages = session_store.get_messages(session_id)

    if not messages:
        return {"session_id": session_id, "messages": [], "total": 0}

    return {
        "session_id": session_id,
        "messages": [
            {
                "role": msg.role,
                "content": msg.content,
                "timestamp": msg.timestamp,
                "sources": json.loads(msg.sources) if msg.sources else [],
            }
            for msg in messages
        ],
        "total": len(messages),
    }


@router.delete("/history/{session_id}")
async def clear_history(session_id: str):
    """Clear chat history for a session."""
    session_store = get_session_store()
    success = session_store.delete_session(session_id)
    if success:
        return {"status": "success", "message": f"History for session {session_id} cleared"}
    return {"status": "error", "message": "Failed to clear history"}


@router.post("/ingest-text")
async def ingest_text(
    text: str,
    title: str = "Text Document",
    doc_id: Optional[str] = None,
):
    """Ingest raw text as a document."""
    from ..document_loader.loader import DocumentLoaderFactory
    from ..services.ingestion import IngestionService
    from ..schemas import DocumentMetadata, DocumentSource

    doc_id = doc_id or str(uuid.uuid4())

    metadata = DocumentMetadata(
        source=DocumentSource.TEXT,
        title=title,
        doc_id=doc_id,
    )

    try:
        chunks = await DocumentLoaderFactory.load(
            content=text.encode("utf-8"),
            metadata=metadata,
        )

        ingestion_service = IngestionService()
        full_text = "\n\n".join(chunk["text"] for chunk in chunks)

        result = await ingestion_service.ingest(
            text=full_text,
            metadata=metadata.model_dump(exclude_none=True),
            doc_id=doc_id,
        )

        logger.info(f"Ingested text {doc_id}: {title} with {result['chunks']} chunks")

        return {
            "doc_id": doc_id,
            "title": title,
            "chunks": result["chunks"],
            "status": "success",
        }

    except Exception as e:
        logger.error(f"Error ingesting text: {e}")
        raise HTTPException(status_code=500, detail=f"Failed to ingest text: {str(e)}")


_chat_histories: dict = {}
