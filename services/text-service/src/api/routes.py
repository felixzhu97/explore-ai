"""API routes for Text-to-Text service."""

import os
import uuid
import time
import json
from typing import Optional
from fastapi import APIRouter, HTTPException, Depends
from fastapi.responses import StreamingResponse
from loguru import logger

from .schemas import (
    CompletionRequest, CompletionResponse,
    ChatRequest, ChatResponse,
    ModelInfo, ProviderInfo, HealthResponse,
)
from ..core.llm_gateway import TextToTextService, LLMGateway
from ..core.config import get_settings

# Import shared authentication
from services.shared.auth import verify_api_key

router = APIRouter(prefix="/api/text", tags=["text-to-text"])

# Enable/disable authentication based on environment
ENABLE_AUTH = os.getenv("ENABLE_API_AUTH", "false").lower() in ("true", "1", "yes")


# Session storage for chat history
_chat_sessions: dict[str, list] = {}


@router.get("/health", response_model=HealthResponse)
async def health_check():
    """Health check endpoint."""
    settings = get_settings()
    return HealthResponse(
        status="ok",
        provider=settings.LLM_PROVIDER,
        model=settings.LLM_MODEL,
        version="0.1.0",
    )


@router.get("/providers", response_model=list[ProviderInfo])
async def list_providers(api_key: str = Depends(verify_api_key) if ENABLE_AUTH else None):
    """List all available LLM providers and their models."""
    settings = get_settings()
    
    return [
        ProviderInfo(
            name="openai",
            display_name="OpenAI",
            models=settings.OPENAI_MODELS.split(","),
            status="available" if settings.OPENAI_API_KEY else "configured",
        ),
        ProviderInfo(
            name="anthropic",
            display_name="Anthropic Claude",
            models=settings.ANTHROPIC_MODELS.split(","),
            status="available" if settings.ANTHROPIC_API_KEY else "configured",
        ),
        ProviderInfo(
            name="ollama",
            display_name="Ollama (Local)",
            models=settings.OLLAMA_MODELS.split(","),
            status="available",
        ),
    ]


@router.get("/models", response_model=list[ModelInfo])
async def list_models(provider: Optional[str] = None, api_key: str = Depends(verify_api_key) if ENABLE_AUTH else None):
    """List available models, optionally filtered by provider."""
    settings = get_settings()
    
    models = []
    
    if provider is None or provider == "openai":
        for model_name in settings.OPENAI_MODELS.split(","):
            models.append(ModelInfo(
                name=model_name.strip(),
                provider="openai",
                description=f"OpenAI {model_name.strip()}",
                max_tokens=128000 if "o" in model_name else 16385,
            ))
    
    if provider is None or provider == "anthropic":
        for model_name in settings.ANTHROPIC_MODELS.split(","):
            models.append(ModelInfo(
                name=model_name.strip(),
                provider="anthropic",
                description=f"Anthropic {model_name.strip()}",
                max_tokens=200000,
            ))
    
    if provider is None or provider == "ollama":
        for model_name in settings.OLLAMA_MODELS.split(","):
            models.append(ModelInfo(
                name=model_name.strip(),
                provider="ollama",
                description=f"Ollama {model_name.strip()} (Local)",
                max_tokens=None,
            ))
    
    return models


@router.post("/complete", response_model=CompletionResponse)
async def complete(request: CompletionRequest, api_key: str = Depends(verify_api_key) if ENABLE_AUTH else None):
    """Generate a text completion.
    
    Simple prompt -> completion endpoint for single-turn text generation.
    """
    settings = get_settings()
    start_time = time.time()
    
    try:
        service = TextToTextService(
            provider=request.provider or settings.LLM_PROVIDER,
            model=request.model or settings.LLM_MODEL,
            temperature=request.temperature,
            max_tokens=request.max_tokens,
            system_prompt=request.system_prompt,
        )
        
        text = service.complete(
            prompt=request.prompt,
            system_prompt=request.system_prompt,
        )
        
        elapsed_ms = int((time.time() - start_time) * 1000)
        
        return CompletionResponse(
            text=text,
            provider=service.provider or settings.LLM_PROVIDER,
            model=service.model or settings.LLM_MODEL,
            usage={"latency_ms": elapsed_ms},
            finish_reason="stop",
        )
        
    except Exception as e:
        logger.error(f"Completion error: {e}")
        raise HTTPException(status_code=500, detail=f"Completion failed: {str(e)}")


@router.post("/complete/stream")
async def complete_stream(request: CompletionRequest, api_key: str = Depends(verify_api_key) if ENABLE_AUTH else None):
    """Stream a text completion.
    
    Returns a Server-Sent Events stream of generated text chunks.
    """
    settings = get_settings()
    
    try:
        service = TextToTextService(
            provider=request.provider or settings.LLM_PROVIDER,
            model=request.model or settings.LLM_MODEL,
            temperature=request.temperature,
            max_tokens=request.max_tokens,
        )
        
        async def generate():
            try:
                # Send metadata
                yield f"event: meta\n"
                yield f"data: {json.dumps({'provider': service.provider, 'model': service.model})}\n\n"
                
                # Stream tokens
                for token in service.stream(
                    prompt=request.prompt,
                    system_prompt=request.system_prompt,
                ):
                    yield f"data: {json.dumps({'token': token})}\n\n"
                
                yield f"event: done\n"
                yield f"data: {json.dumps({'finish_reason': 'stop'})}\n\n"
                
            except Exception as e:
                logger.error(f"Stream error: {e}")
                yield f"event: error\n"
                yield f"data: {json.dumps({'error': str(e)})}\n\n"
        
        return StreamingResponse(
            generate(),
            media_type="text/event-stream",
            headers={
                "Cache-Control": "no-cache",
                "Connection": "keep-alive",
            },
        )
        
    except Exception as e:
        logger.error(f"Stream setup error: {e}")
        raise HTTPException(status_code=500, detail=f"Stream failed: {str(e)}")


@router.post("/chat", response_model=ChatResponse)
async def chat(request: ChatRequest, api_key: str = Depends(verify_api_key) if ENABLE_AUTH else None):
    """Generate a chat completion.
    
    Multi-turn conversation endpoint with optional session management.
    """
    settings = get_settings()
    start_time = time.time()
    
    # Get or create session
    session_id = request.session_id
    if not session_id:
        session_id = str(uuid.uuid4())
    
    # Load session history
    if session_id not in _chat_sessions:
        _chat_sessions[session_id] = []
    
    # Convert messages to dict format
    messages = [msg.model_dump() for msg in request.messages]
    
    # Build conversation context
    conversation = _chat_sessions[session_id] + messages
    if request.system_prompt:
        conversation = [{"role": "system", "content": request.system_prompt}] + conversation
    
    try:
        service = TextToTextService(
            provider=request.provider or settings.LLM_PROVIDER,
            model=request.model or settings.LLM_MODEL,
            temperature=request.temperature,
            max_tokens=request.max_tokens,
        )
        
        text = service.chat(messages=conversation)
        
        # Save to session history
        _chat_sessions[session_id].extend(messages)
        _chat_sessions[session_id].append({"role": "assistant", "content": text})
        
        # Trim history if too long (keep last 20 messages)
        if len(_chat_sessions[session_id]) > 20:
            _chat_sessions[session_id] = _chat_sessions[session_id][-20:]
        
        elapsed_ms = int((time.time() - start_time) * 1000)
        
        return ChatResponse(
            text=text,
            provider=service.provider or settings.LLM_PROVIDER,
            model=service.model or settings.LLM_MODEL,
            session_id=session_id,
            usage={"latency_ms": elapsed_ms, "history_length": len(_chat_sessions[session_id])},
            finish_reason="stop",
        )
        
    except Exception as e:
        logger.error(f"Chat error: {e}")
        raise HTTPException(status_code=500, detail=f"Chat failed: {str(e)}")


@router.post("/chat/stream")
async def chat_stream(request: ChatRequest, api_key: str = Depends(verify_api_key) if ENABLE_AUTH else None):
    """Stream a chat completion.
    
    Returns a Server-Sent Events stream for real-time response generation.
    """
    settings = get_settings()
    
    session_id = request.session_id or str(uuid.uuid4())
    
    # Load session history
    if session_id not in _chat_sessions:
        _chat_sessions[session_id] = []
    
    # Convert messages to dict format
    messages = [msg.model_dump() for msg in request.messages]
    
    # Build conversation context
    conversation = _chat_sessions[session_id] + messages
    if request.system_prompt:
        conversation = [{"role": "system", "content": request.system_prompt}] + conversation
    
    try:
        service = TextToTextService(
            provider=request.provider or settings.LLM_PROVIDER,
            model=request.model or settings.LLM_MODEL,
            temperature=request.temperature,
            max_tokens=request.max_tokens,
        )
        
        async def generate():
            try:
                full_response = []
                
                # Send metadata
                yield f"event: meta\n"
                yield f"data: {json.dumps({'session_id': session_id, 'provider': service.provider, 'model': service.model})}\n\n"
                
                # Stream tokens
                for token in service.chat_stream(messages=conversation):
                    full_response.append(token)
                    yield f"data: {json.dumps({'token': token})}\n\n"
                
                # Save to session
                _chat_sessions[session_id].extend(messages)
                _chat_sessions[session_id].append({"role": "assistant", "content": "".join(full_response)})
                
                # Trim history
                if len(_chat_sessions[session_id]) > 20:
                    _chat_sessions[session_id] = _chat_sessions[session_id][-20:]
                
                yield f"event: done\n"
                yield f"data: {json.dumps({'session_id': session_id, 'finish_reason': 'stop'})}\n\n"
                
            except Exception as e:
                logger.error(f"Stream error: {e}")
                yield f"event: error\n"
                yield f"data: {json.dumps({'error': str(e)})}\n\n"
        
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
        logger.error(f"Stream setup error: {e}")
        raise HTTPException(status_code=500, detail=f"Stream failed: {str(e)}")


@router.get("/session/{session_id}")
async def get_session(session_id: str, api_key: str = Depends(verify_api_key) if ENABLE_AUTH else None):
    """Get chat history for a session."""
    messages = _chat_sessions.get(session_id, [])
    return {
        "session_id": session_id,
        "messages": messages,
        "count": len(messages),
    }


@router.delete("/session/{session_id}")
async def clear_session(session_id: str, api_key: str = Depends(verify_api_key) if ENABLE_AUTH else None):
    """Clear chat history for a session."""
    if session_id in _chat_sessions:
        del _chat_sessions[session_id]
        return {"status": "success", "message": f"Session {session_id} cleared"}
    return {"status": "not_found", "message": "Session not found"}


@router.post("/reset")
async def reset_llm(api_key: str = Depends(verify_api_key) if ENABLE_AUTH else None):
    """Reset the cached LLM instance (for model switching)."""
    LLMGateway.reset()
    return {"status": "success", "message": "LLM cache reset"}
