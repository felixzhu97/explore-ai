"""TTS Agent implementation.

This agent specializes in text-to-speech synthesis operations.
"""

from typing import Any, Dict, List, Optional

from langchain_core.language_models import BaseChatModel
from langchain_core.messages import BaseMessage, HumanMessage
from langchain_core.runnables import Runnable
from langchain_core.tools import BaseTool
from langgraph.graph import StateGraph, END

from services.ai_agents.infrastructure.base import BaseInfraAgent, AgentState
from services.ai_agents.presentation.agents.prompts import TTS_SYSTEM_PROMPT


class TTSAgent(BaseInfraAgent):
    """Agent for text-to-speech synthesis operations.
    
    This agent provides capabilities for:
    - Synthesizing text to speech
    - Listing and selecting voices
    - Managing audio output formats
    - Voice customization (speed, pitch)
    - Streaming synthesis
    - Provider management
    
    Example:
        ```python
        from langchain_openai import ChatOpenAI
        from services.ai_agents.presentation.agents import TTSAgent
        
        llm = ChatOpenAI(model="gpt-4")
        agent = TTSAgent(llm=llm)
        
        # Synthesize text
        result = agent.invoke({
            "messages": [HumanMessage(
                content="Synthesize 'Hello, world!' with Jenny voice"
            )]
        })
        
        # List available voices
        result = agent.invoke({
            "messages": [HumanMessage(
                content="List all available English voices"
            )]
        })
        
        # Stream speech
        result = agent.invoke({
            "messages": [HumanMessage(
                content="Stream 'Welcome!' in Spanish with speed 1.2"
            )]
        })
        ```
    """
    
    def __init__(
        self,
        llm: BaseChatModel,
        tools: Optional[List[BaseTool]] = None,
        system_prompt: Optional[str] = None,
    ):
        """Initialize the TTS Agent.
        
        Args:
            llm: The language model for reasoning.
            tools: Optional list of tools (defaults to all TTS tools).
            system_prompt: Optional custom system prompt.
        """
        _tools = tools if tools is not None else []
        _prompt = system_prompt or TTS_SYSTEM_PROMPT
        
        super().__init__(
            llm=llm,
            tools=_tools,
            system_prompt=_prompt,
            name="TTSAgent",
            description="Text-to-speech synthesis, voice management, and audio generation"
        )
    
    def create_graph(self) -> Runnable:
        """Create the LangGraph workflow for the TTS Agent.
        
        Returns:
            A compiled Runnable instance.
        """
        workflow = StateGraph(AgentState)
        
        workflow.add_node("tts_processor", self._create_tts_node())
        
        workflow.set_entry_point("tts_processor")
        workflow.add_edge("tts_processor", END)
        
        return workflow.compile()
    
    def _create_tts_node(self):
        """Create the TTS processing node."""
        def tts_node(state: AgentState) -> Dict[str, Any]:
            """Process TTS requests."""
            messages = state.get("messages", [])
            if not messages:
                return {"messages": [], "context": {}}
            
            last_message = messages[-1]
            
            response = self.llm.bind_tools(
                self.tools,
                tool_choice="auto"
            ).invoke(
                [self._format_system_message()] + messages
            )
            
            return {
                "messages": [response],
                "context": {
                    "agent": self.name,
                    "task": str(last_message.content) if hasattr(last_message, 'content') else str(last_message)
                }
            }
        
        return tts_node
    
    def _format_system_message(self) -> BaseMessage:
        """Format the system message for the agent."""
        from langchain_core.messages import SystemMessage
        
        tool_descriptions = "\n".join(
            f"- {tool.name}: {tool.description}"
            for tool in self.tools
        )
        
        full_prompt = f"""{self.system_prompt}

Available Tools:
{tool_descriptions}

Instructions:
1. Understand the user's speech synthesis requirements
2. Select appropriate voice based on language and content type
3. Configure speed and pitch for optimal output
4. Provide clear feedback on synthesis results
5. Help users find the right voice for their use case
"""
        return SystemMessage(content=full_prompt)
    
    def synthesize(
        self,
        text: str,
        voice: Optional[str] = None,
        language: Optional[str] = None,
        speed: float = 1.0,
        pitch: float = 0
    ) -> Dict[str, Any]:
        """Convenience method for text synthesis.
        
        Args:
            text: Text to synthesize.
            voice: Voice identifier.
            language: Language code.
            speed: Speech speed.
            pitch: Pitch adjustment.
            
        Returns:
            Synthesis result.
        """
        result = self.invoke({
            "messages": [HumanMessage(
                content=f"Synthesize: '{text}'" +
                        (f" with voice {voice}" if voice else "") +
                        (f" in language {language}" if language else "") +
                        f" at speed {speed}" +
                        (f" with pitch {pitch}" if pitch else "")
            )]
        })
        
        return result
    
    def list_voices(self, language: Optional[str] = None) -> Dict[str, Any]:
        """Convenience method for listing voices.
        
        Args:
            language: Optional language filter.
            
        Returns:
            List of voices.
        """
        lang_filter = f" for language '{language}'" if language else ""
        result = self.invoke({
            "messages": [HumanMessage(
                content=f"List all available voices{lang_filter}"
            )]
        })
        
        return result
    
    def get_providers(self) -> Dict[str, Any]:
        """Convenience method for getting provider info.
        
        Returns:
            Provider information.
        """
        result = self.invoke({
            "messages": [HumanMessage(
                content="Get information about available TTS providers"
            )]
        })
        
        return result
    
    def stream_synthesize(
        self,
        text: str,
        voice: Optional[str] = None,
        speed: float = 1.0
    ) -> Dict[str, Any]:
        """Convenience method for streaming synthesis.
        
        Args:
            text: Text to synthesize.
            voice: Voice identifier.
            speed: Speech speed.
            
        Returns:
            Streaming result.
        """
        result = self.invoke({
            "messages": [HumanMessage(
                content=f"Stream synthesize: '{text}'" +
                        (f" with voice {voice}" if voice else "") +
                        f" at speed {speed}"
            )]
        })
        
        return result
