"""AI Agents Service - FastAPI Application.

This service provides REST API endpoints for multi-agent orchestration.
"""

from contextlib import asynccontextmanager
from typing import Any, Dict, List, Optional, Union
from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import StreamingResponse
from pydantic import BaseModel, Field
from loguru import logger
import json
import re

from services.ai_agents.agents.supervisor import SupervisorAgent
from services.ai_agents.agents.rag_agent import RAGAgent
from services.ai_agents.agents.llmops_agent import LLMOpsAgent
from services.ai_agents.agents.aiops_agent import AIOpsAgent
from services.ai_agents.agents.pipeline_agent import PipelineAgent
from services.ai_agents.agents.feature_store_agent import FeatureStoreAgent
from services.ai_agents.agents.k8s_agent import K8sAgent
from services.ai_agents.agents.monitoring_agent import MonitoringAgent
from services.ai_agents.agents.vector_db_agent import VectorDBAgent
from services.ai_agents.agents.model_agent import ModelAgent
from services.ai_agents.agents.tts_agent import TTSAgent

from services.ai_agents.tools.vector_tools import get_all_vector_tools
from services.ai_agents.tools.k8s_tools import get_all_k8s_tools
from services.ai_agents.tools.monitoring_tools import get_all_monitoring_tools
from services.ai_agents.tools.model_tools import get_all_model_tools
from services.ai_agents.tools.llmops_tools import get_all_llmops_tools
from services.ai_agents.tools.aiops_tools import get_all_aiops_tools
from services.ai_agents.tools.tts_tools import get_all_tts_tools


def extract_clean_content(data: Any) -> str:
    """Extract clean text content from various data formats.
    
    Handles:
    - LangChain AIMessage objects
    - Dict with 'messages' key
    - Plain strings
    - Nested dicts with 'content' field
    """
    # If it's a string
    if isinstance(data, str):
        content = data
    # If it has content attribute (LangChain message)
    elif hasattr(data, 'content'):
        content = data.content
    # If it's a dict
    elif isinstance(data, dict):
        # Direct content field
        if 'content' in data:
            content = data['content']
            if isinstance(content, str):
                pass
            else:
                content = str(content)
        # LangChain message format
        elif 'messages' in data:
            messages = data['messages']
            if isinstance(messages, list) and len(messages) > 0:
                return extract_clean_content(messages[-1])
            else:
                return str(data)
        else:
            content = str(data)
    else:
        content = str(data)
    
    # Clean up LangChain internal object strings
    # Pattern like: {'messages': [AIMessage(content="..."), ...]}
    if re.match(r"^\{'[^']*':\s*\[", content) or content.startswith('AIMessage('):
        try:
            # Try to extract content from nested structures
            match = re.search(r'content\s*=\s*"([^"]*)"', content)
            if match:
                content = match.group(1)
            else:
                # Try to find the last clear text block
                match = re.search(r'"([^"]*response_metadata[^"]*)"', content)
                if match:
                    content = ""
        except:
            pass
    
    # Remove trailing JSON metadata blocks
    content = re.sub(
        r'\n?```json\s*\{[^}]*"type"[^}]*\}[^}]*```\s*$',
        '',
        content
    ).strip()
    
    return content


def execute_tool_calls(tool_calls: List[Dict], tools: List) -> List[Dict]:
    """Execute tool calls and return results."""
    results = []
    tool_map = {tool.name: tool for tool in tools}
    
    for call in tool_calls:
        tool_name = call.get('name', '')
        tool_args = call.get('args', {})
        tool_id = call.get('id', '')
        
        if tool_name in tool_map:
            try:
                result = tool_map[tool_name].invoke(tool_args)
                results.append({
                    'id': tool_id,
                    'name': tool_name,
                    'output': str(result),
                    'status': 'success'
                })
            except Exception as e:
                results.append({
                    'id': tool_id,
                    'name': tool_name,
                    'output': f"Error: {str(e)}",
                    'status': 'error'
                })
        else:
            results.append({
                'id': tool_id,
                'name': tool_name,
                'output': f"Tool '{tool_name}' not found",
                'status': 'error'
            })
    
    return results


class Message(BaseModel):
    role: str
    content: str


class InvokeRequest(BaseModel):
    messages: List[Message] = Field(default_factory=list)
    agent_name: Optional[str] = None


class AgentInfo(BaseModel):
    name: str
    description: str
    status: str = "online"


_supervisor: Optional[SupervisorAgent] = None
_initialized = False


async def initialize_agents():
    """Initialize all agents with LLM configuration."""
    global _supervisor, _initialized
    
    if _initialized:
        return
    
    try:
        from langchain_ollama import ChatOllama
        from services.ai_agents.core.config import get_settings
        
        settings = get_settings()
        
        # Create LLM instance using Ollama
        # Note: stream=True enables streaming mode, but the actual streaming
        # behavior depends on the underlying model's support
        llm = ChatOllama(
            model=settings.OLLAMA_MODEL,
            base_url=settings.OLLAMA_BASE_URL,
            temperature=0.7,
            stream=True,  # Enable streaming
            timeout=120,  # 2 minute timeout per request
        )
        
        logger.info(f"Initializing AI Agents with Ollama ({settings.OLLAMA_BASE_URL}/{settings.OLLAMA_MODEL})...")
        
        # Initialize all specialized agents
        rag_agent = RAGAgent(llm=llm)
        llmops_agent = LLMOpsAgent(llm=llm, tools=get_all_llmops_tools())
        aiops_agent = AIOpsAgent(llm=llm, tools=get_all_aiops_tools())
        pipeline_agent = PipelineAgent(llm=llm)
        feature_store_agent = FeatureStoreAgent(llm=llm)
        k8s_agent = K8sAgent(llm=llm, tools=get_all_k8s_tools())
        monitoring_agent = MonitoringAgent(llm=llm, tools=get_all_monitoring_tools())
        vector_agent = VectorDBAgent(llm=llm, tools=get_all_vector_tools())
        model_agent = ModelAgent(llm=llm, tools=get_all_model_tools())
        
        # Create supervisor with all agents
        _supervisor = SupervisorAgent(
            llm=llm,
            rag_agent=rag_agent,
            llmops_agent=llmops_agent,
            aiops_agent=aiops_agent,
            pipeline_agent=pipeline_agent,
            feature_store_agent=feature_store_agent,
            k8s_agent=k8s_agent,
            monitoring_agent=monitoring_agent,
            vector_agent=vector_agent,
            model_agent=model_agent,
        )
        
        logger.info(f"Supervisor initialized with agents: {_supervisor.available_agents}")
        _initialized = True
        
    except Exception as e:
        logger.error(f"Failed to initialize agents: {e}")
        # Create a minimal supervisor without agents for now
        _supervisor = None
        _initialized = True


@asynccontextmanager
async def lifespan(app: FastAPI):
    logger.info("Starting AI Agents Service...")
    await initialize_agents()
    logger.info("AI Agents Service started")
    yield
    logger.info("Shutting down AI Agents Service...")


def create_app() -> FastAPI:
    app = FastAPI(
        title="AI Agents Service",
        description="Multi-agent orchestration service for AI infrastructure",
        version="0.1.0",
        lifespan=lifespan,
    )
    
    app.add_middleware(
        CORSMiddleware,
        allow_origins=["*"],
        allow_credentials=True,
        allow_methods=["*"],
        allow_headers=["*"],
    )
    
    @app.get("/health")
    async def health():
        return {
            "status": "ok",
            "service": "ai_agents",
            "agents_initialized": _supervisor is not None,
            "available_agents": _supervisor.available_agents if _supervisor else [],
        }
    
    @app.get("/agents")
    async def list_agents():
        """List all available agents."""
        if _supervisor is None:
            raise HTTPException(status_code=503, detail="Agents not initialized")
        
        return {
            "agents": [
                {
                    "name": name,
                    "description": desc,
                    "status": "online"
                }
                for name, desc in _supervisor.agent_descriptions.items()
            ]
        }
    
    @app.post("/api/agents/supervisor/invoke")
    async def invoke_supervisor(request: InvokeRequest):
        """Invoke the supervisor agent with a streaming response."""
        if _supervisor is None:
            # Return a mock response if supervisor is not initialized
            async def mock_stream():
                user_message = request.messages[-1].content if request.messages else ""
                
                yield f"event: message\n"
                yield f"data: I understand you're asking about: {user_message[:50]}...\n\n"
                
                yield f"event: message\n"
                yield f"data: However, the AI Agents service is running in demo mode. "
                yield f"data: The supervisor agent would route this request to the appropriate specialized agent.\n\n"
                
                yield f"event: message\n"
                yield f"data: To enable full functionality, please configure your LLM provider (OpenAI API key) in the .env file.\n\n"
                
                yield f"data: [DONE]\n\n"
            
            return StreamingResponse(
                mock_stream(),
                media_type="text/event-stream",
                headers={
                    "Cache-Control": "no-cache",
                    "Connection": "keep-alive",
                },
            )
        
        async def generate():
            try:
                from langchain_core.messages import HumanMessage, AIMessage
                
                # Convert messages to LangChain format
                lc_messages = []
                for msg in request.messages:
                    if msg.role == "user":
                        lc_messages.append(HumanMessage(content=msg.content))
                    elif msg.role == "assistant":
                        lc_messages.append(AIMessage(content=msg.content))
                
                if not lc_messages:
                    yield f"data: No messages provided\n\n"
                    yield f"data: [DONE]\n\n"
                    return
                
                # Send initial message to indicate processing started
                yield f"event: message\n"
                yield f"data: Starting analysis...\n\n"
                
                import asyncio
                
                result_holder = [None]  # [result_or_error, is_error]
                
                def run_agent():
                    try:
                        result = _supervisor.invoke({"messages": lc_messages})
                        result_holder[0] = (result, False)
                    except Exception as e:
                        logger.error(f"Error invoking supervisor: {e}")
                        result_holder[0] = (str(e), True)
                
                # Run agent in background thread using loop.run_in_executor
                loop = asyncio.get_event_loop()
                future = loop.run_in_executor(None, run_agent)
                
                # Wait for the future with timeout, yielding progress
                import time
                start_time = time.time()
                poll_interval = 0.5
                
                # Poll until complete or timeout
                while not future.done():
                    if time.time() - start_time > 60:
                        logger.warning("Agent invocation timed out")
                        break
                    await asyncio.sleep(poll_interval)
                
                # If result not in holder but future done, check for exceptions
                if result_holder[0] is None:
                    if future.done() and not future.cancelled():
                        try:
                            # Wait for any exception from the future
                            future.result()
                        except Exception as e:
                            result_holder[0] = (str(e), True)
                
                if result_holder[0] is None:
                    yield f"event: message\n"
                    yield f"data: Timeout waiting for response\n\n"
                else:
                    data, is_error = result_holder[0]
                    
                    if is_error:
                        yield f"event: error\n"
                        yield f"data: {data}\n\n"
                    else:
                        # Send the response
                        result_messages = data.get("messages", [])
                        if result_messages:
                            last_message = result_messages[-1]
                            content = last_message.content if hasattr(last_message, 'content') else str(last_message)
                            yield f"event: message\n"
                            yield f"data: {content}\n\n"
                        
                        # Send agent results
                        agent_results = data.get("agent_results", {})
                        for agent_name, agent_result in agent_results.items():
                            yield f"event: tool_output\n"
                            yield f"data: Agent '{agent_name}' completed\n\n"
                
                yield f"data: [DONE]\n\n"
                
            except Exception as e:
                logger.error(f"Error invoking supervisor: {e}")
                yield f"event: error\n"
                yield f"data: Error: {str(e)}\n\n"
        
        return StreamingResponse(
            generate(),
            media_type="text/event-stream",
            headers={
                "Cache-Control": "no-cache",
                "Connection": "keep-alive",
            },
        )
    
    @app.post("/api/agents/{agent_name}/invoke")
    async def invoke_agent(agent_name: str, request: InvokeRequest):
        """Invoke a specific agent directly."""
        if _supervisor is None:
            raise HTTPException(status_code=503, detail="Agents not initialized")
        
        if agent_name not in _supervisor.available_agents:
            raise HTTPException(status_code=404, detail=f"Agent '{agent_name}' not found")
        
        async def generate():
            try:
                from langchain_core.messages import HumanMessage
                
                task = request.messages[-1].content if request.messages else ""
                
                # Send initial message
                yield f"event: message\n"
                yield f"data: Processing...\n\n"
                
                # Use asyncio for proper async handling
                result_holder = [None]  # [result_tuple or error_string, is_error]
                tool_events = []  # Store tool events to yield later
                
                def run_agent():
                    try:
                        # Get agent and tools
                        agent = _supervisor.agents.get(agent_name)
                        if agent is None:
                            result_holder[0] = (f"Agent '{agent_name}' not found", True)
                            return
                        
                        tools = agent.tools
                        tool_map = {t.name: t for t in tools}
                        
                        # Import LangChain messages
                        from langchain_core.messages import HumanMessage
                        
                        system_msg = agent._format_system_message()
                        
                        # First LLM call
                        messages = [HumanMessage(content=task)]
                        response = _supervisor.llm.invoke([system_msg] + messages)
                        content = response.content if hasattr(response, 'content') else str(response)
                        
                        # Try to find and execute tool call
                        import json
                        
                        # Look for JSON tool call - try multiple patterns
                        tool_call = None
                        for pattern in [
                            r'\{[^{}]*"tool"[^{}]*\}',
                            r'\{[^{}]*"tool"\s*:\s*"[^"]+"\s*,\s*"args"\s*:\s*\{[^}]*\}\s*\}',
                        ]:
                            json_match = re.search(pattern, content)
                            if json_match:
                                try:
                                    tool_call = json.loads(json_match.group(0))
                                    if 'tool' in tool_call:
                                        break
                                except:
                                    pass
                        
                        if tool_call and 'tool' in tool_call:
                            tool_name = tool_call.get('tool', '')
                            tool_args = tool_call.get('args', {})
                            
                            if tool_name in tool_map:
                                try:
                                    result = tool_map[tool_name].invoke(tool_args)
                                    result_str = str(result)
                                    tool_events.append(('tool', tool_name, result_str))
                                    # Second call with result
                                    messages.append(HumanMessage(content=f"Result: {result}"))
                                except Exception as e:
                                    tool_events.append(('tool_error', tool_name, str(e)))
                                    messages.append(HumanMessage(content=f"Error: {str(e)}"))
                            
                            # Get final response
                            response = _supervisor.llm.invoke([system_msg] + messages)
                            final_content = response.content if hasattr(response, 'content') else str(response)
                            result_holder[0] = (final_content, False)
                        else:
                            # No tool call, return as-is
                            result_holder[0] = (content, False)
                        
                    except Exception as e:
                        logger.error(f"Error invoking agent {agent_name}: {e}")
                        result_holder[0] = (str(e), True)
                
                # Run agent in background thread using run_in_executor
                import asyncio
                loop = asyncio.get_event_loop()
                future = loop.run_in_executor(None, run_agent)
                
                # Wait for the future with timeout, yielding progress
                import time
                start_time = time.time()
                poll_interval = 0.5
                
                # Poll until complete or timeout
                while not future.done():
                    if time.time() - start_time > 120:
                        logger.warning(f"Agent '{agent_name}' invocation timed out")
                        break
                    await asyncio.sleep(poll_interval)
                
                # If result not in holder but future done, check for exceptions
                if result_holder[0] is None:
                    if future.done() and not future.cancelled():
                        try:
                            future.result()
                        except Exception as e:
                            result_holder[0] = (str(e), True)
                
                # Yield tool events
                for event_type, tool_name, result in tool_events:
                    if event_type == 'tool':
                        # Send tool_call event first
                        import uuid
                        tool_id = str(uuid.uuid4())[:8]
                        yield f"event: tool_call\n"
                        yield f"data: {{\"id\": \"{tool_id}\", \"name\": \"{tool_name}\", \"input\": {{}}}}\n\n"
                        # Send tool_output event - properly format multi-line data
                        yield f"event: tool_output\n"
                        for line in str(result).split('\n'):
                            yield f"data: {line}\n"
                        yield f"\n"
                    elif event_type == 'tool_error':
                        yield f"event: tool_error\n"
                        yield f"data: [Tool Error: {tool_name}]\n"
                        for line in str(result).split('\n'):
                            yield f"data: {line}\n"
                        yield f"\n"
                
                if result_holder[0] is None:
                    yield f"event: error\n"
                    yield f"data: Timeout waiting for agent response\n\n"
                else:
                    content, is_error = result_holder[0]
                    if is_error:
                        yield f"event: error\n"
                        yield f"data: {content}\n\n"
                    else:
                        yield f"event: message\n"
                        yield f"data: {content}\n\n"
                
                yield f"data: [DONE]\n\n"
                
            except Exception as e:
                logger.error(f"Error invoking agent {agent_name}: {e}")
                yield f"event: error\n"
                yield f"data: Error: {str(e)}\n\n"
        
        return StreamingResponse(
            generate(),
            media_type="text/event-stream",
            headers={
                "Cache-Control": "no-cache",
                "Connection": "keep-alive",
            },
        )
    
    return app


app = create_app()


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(
        "main:app",
        host="0.0.0.0",
        port=8003,
        reload=True,
    )
