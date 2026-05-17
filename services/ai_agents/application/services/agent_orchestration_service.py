"""Application service for agent orchestration.

This service handles the business logic for invoking agents and executing tool calls,
separating it from the HTTP layer following Clean Architecture principles.
"""

import asyncio
import json
import re
import time
import uuid
from typing import TYPE_CHECKING, Any, Dict, List, Optional, AsyncIterator

from loguru import logger

if TYPE_CHECKING:
    from services.ai_agents.domain.ports import AgentOrchestrator


def extract_clean_content(data: Any) -> str:
    """Extract clean text content from various data formats.
    
    Handles:
    - LangChain AIMessage objects
    - Dict with 'messages' key
    - Plain strings
    - Nested dicts with 'content' field
    """
    if isinstance(data, str):
        content = data
    elif hasattr(data, 'content'):
        content = data.content
    elif isinstance(data, dict):
        if 'content' in data:
            content = data['content']
            if not isinstance(content, str):
                content = str(content)
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
    
    if re.match(r"^\{'[^']*':\s*\[", content) or content.startswith('AIMessage('):
        try:
            match = re.search(r'content\s*=\s*"([^"]*)"', content)
            if match:
                content = match.group(1)
            else:
                match = re.search(r'"([^"]*response_metadata[^"]*)"', content)
                if match:
                    content = ""
        except Exception:
            pass
    
    content = re.sub(
        r'\n?```json\s*\{[^}]*"type"[^}]*\}[^}]*```\s*$',
        '',
        content
    ).strip()
    
    return content


class AgentOrchestrationService:
    """Orchestrates agent execution and tool calls.
    
    This service encapsulates all business logic for:
    - Invoking the supervisor agent with messages
    - Invoking specific agents directly
    - Executing tool calls
    - Handling streaming responses
    """
    
    def __init__(self, orchestrator: Optional["AgentOrchestrator"] = None):
        """Initialize the service with an orchestrator.
        
        Args:
            orchestrator: The AgentOrchestrator implementation for agent orchestration
        """
        self._orchestrator = orchestrator
    
    @property
    def orchestrator(self):
        """Get the orchestrator."""
        return self._orchestrator
    
    @property
    def is_initialized(self) -> bool:
        """Check if orchestrator is initialized."""
        return self._orchestrator is not None
    
    @property
    def available_agents(self) -> List[str]:
        """Get list of available agent names."""
        if self._orchestrator is None:
            return []
        return self._orchestrator.available_agents
    
    @property
    def agent_descriptions(self) -> Dict[str, str]:
        """Get agent descriptions."""
        if self._orchestrator is None:
            return {}
        return self._orchestrator.agent_descriptions
    
    async def invoke_supervisor_stream(
        self, 
        messages: List[Dict[str, str]]
    ) -> AsyncIterator[Dict[str, Any]]:
        """Stream agent execution for supervisor.
        
        Args:
            messages: List of message dicts with 'role' and 'content'
            
        Yields:
            Event dicts with 'event' type and 'data' content
        """
        if self._orchestrator is None:
            # Mock response when not initialized
            user_message = messages[-1].get("content", "") if messages else ""
            
            yield {"event": "message", "data": f"I understand you're asking about: {user_message[:50]}...\n"}
            
            yield {"event": "message", "data": "However, the AI Agents service is running in demo mode. "}
            yield {"event": "message", "data": "The supervisor agent would route this request to the appropriate specialized agent.\n"}
            
            yield {"event": "message", "data": "To enable full functionality, please configure your LLM provider (OpenAI API key) in the .env file.\n"}
            
            yield {"event": "done", "data": ""}
            return
        
        try:
            from langchain_core.messages import HumanMessage, AIMessage
            
            lc_messages = []
            for msg in messages:
                if msg.get("role") == "user":
                    lc_messages.append(HumanMessage(content=msg.get("content", "")))
                elif msg.get("role") == "assistant":
                    lc_messages.append(AIMessage(content=msg.get("content", "")))
            
            if not lc_messages:
                yield {"event": "message", "data": "No messages provided\n"}
                yield {"event": "done", "data": ""}
                return
            
            yield {"event": "message", "data": "Starting analysis...\n"}
            
            result_holder = [None]
            
            def run_agent():
                try:
                    result = self._orchestrator.invoke({"messages": lc_messages})
                    result_holder[0] = (result, False)
                except Exception as e:
                    logger.error(f"Error invoking supervisor: {e}")
                    result_holder[0] = (str(e), True)
            
            loop = asyncio.get_event_loop()
            future = loop.run_in_executor(None, run_agent)
            
            start_time = time.time()
            poll_interval = 0.5
            
            while not future.done():
                if time.time() - start_time > 60:
                    logger.warning("Agent invocation timed out")
                    break
                await asyncio.sleep(poll_interval)
            
            if result_holder[0] is None:
                if future.done() and not future.cancelled():
                    try:
                        future.result()
                    except Exception as e:
                        result_holder[0] = (str(e), True)
            
            if result_holder[0] is None:
                yield {"event": "message", "data": "Timeout waiting for response\n"}
            else:
                data, is_error = result_holder[0]
                
                if is_error:
                    yield {"event": "error", "data": str(data)}
                else:
                    result_messages = data.get("messages", [])
                    if result_messages:
                        last_message = result_messages[-1]
                        content = last_message.content if hasattr(last_message, 'content') else str(last_message)
                        yield {"event": "message", "data": content}
                    
                    agent_results = data.get("agent_results", {})
                    for agent_name, agent_result in agent_results.items():
                        yield {"event": "tool_output", "data": f"Agent '{agent_name}' completed"}
            
            yield {"event": "done", "data": ""}
            
        except Exception as e:
            logger.error(f"Error invoking supervisor: {e}")
            yield {"event": "error", "data": f"Error: {str(e)}"}
    
    async def invoke_agent_stream(
        self, 
        agent_name: str,
        messages: List[Dict[str, str]]
    ) -> AsyncIterator[Dict[str, Any]]:
        """Stream agent execution for a specific agent.
        
        Args:
            agent_name: Name of the agent to invoke
            messages: List of message dicts with 'role' and 'content'
            
        Yields:
            Event dicts with 'event' type and 'data' content
        """
        if self._orchestrator is None:
            yield {"event": "error", "data": "Agents not initialized"}
            yield {"event": "done", "data": ""}
            return
        
        if agent_name not in self._orchestrator.available_agents:
            yield {"event": "error", "data": f"Agent '{agent_name}' not found"}
            yield {"event": "done", "data": ""}
            return
        
        try:
            from langchain_core.messages import HumanMessage
            
            task = messages[-1].get("content", "") if messages else ""
            
            yield {"event": "message", "data": "Processing...\n"}
            
            result_holder = [None]
            tool_events = []
            
            def run_agent():
                try:
                    agent = self._orchestrator.agents.get(agent_name)
                    if agent is None:
                        result_holder[0] = (f"Agent '{agent_name}' not found", True)
                        return
                    
                    tools = agent.tools
                    tool_map = {t.name: t for t in tools}
                    
                    system_msg = agent._format_system_message()
                    messages = [HumanMessage(content=task)]
                    response = self._orchestrator.llm.invoke([system_msg] + messages)
                    content = response.content if hasattr(response, 'content') else str(response)
                    
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
                            except Exception:
                                pass
                    
                    if tool_call and 'tool' in tool_call:
                        tool_name = tool_call.get('tool', '')
                        tool_args = tool_call.get('args', {})
                        
                        if tool_name in tool_map:
                            try:
                                result = tool_map[tool_name].invoke(tool_args)
                                result_str = str(result)
                                tool_events.append(('tool', tool_name, result_str))
                                messages.append(HumanMessage(content=f"Result: {result}"))
                            except Exception as e:
                                tool_events.append(('tool_error', tool_name, str(e)))
                                messages.append(HumanMessage(content=f"Error: {str(e)}"))
                        
                        response = self._orchestrator.llm.invoke([system_msg] + messages)
                        final_content = response.content if hasattr(response, 'content') else str(response)
                        result_holder[0] = (final_content, False)
                    else:
                        result_holder[0] = (content, False)
                    
                except Exception as e:
                    logger.error(f"Error invoking agent {agent_name}: {e}")
                    result_holder[0] = (str(e), True)
            
            loop = asyncio.get_event_loop()
            future = loop.run_in_executor(None, run_agent)
            
            start_time = time.time()
            poll_interval = 0.5
            
            while not future.done():
                if time.time() - start_time > 120:
                    logger.warning(f"Agent '{agent_name}' invocation timed out")
                    break
                await asyncio.sleep(poll_interval)
            
            if result_holder[0] is None:
                if future.done() and not future.cancelled():
                    try:
                        future.result()
                    except Exception as e:
                        result_holder[0] = (str(e), True)
            
            for event_type, tool_name, result in tool_events:
                if event_type == 'tool':
                    tool_id = str(uuid.uuid4())[:8]
                    yield {"event": "tool_call", "data": {"id": tool_id, "name": tool_name, "input": {}}}
                    yield {"event": "tool_output", "data": str(result)}
                elif event_type == 'tool_error':
                    yield {"event": "tool_error", "data": f"[Tool Error: {tool_name}] {result}"}
            
            if result_holder[0] is None:
                yield {"event": "error", "data": "Timeout waiting for agent response"}
            else:
                content, is_error = result_holder[0]
                if is_error:
                    yield {"event": "error", "data": str(content)}
                else:
                    yield {"event": "message", "data": str(content)}
            
            yield {"event": "done", "data": ""}
            
        except Exception as e:
            logger.error(f"Error invoking agent {agent_name}: {e}")
            yield {"event": "error", "data": f"Error: {str(e)}"}
    
    def execute_tool_calls(
        self, 
        tool_calls: List[Dict[str, Any]], 
        tools: List
    ) -> List[Dict[str, Any]]:
        """Execute tool calls and return results.
        
        Args:
            tool_calls: List of tool call dicts with 'name', 'args', and 'id'
            tools: List of available tool instances
            
        Returns:
            List of result dicts with 'id', 'name', 'output', and 'status'
        """
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
