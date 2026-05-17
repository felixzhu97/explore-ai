"""Video Agent implementation.

This agent specializes in text-to-video generation operations.
"""

from typing import Any, Dict, List, Optional

from langchain_core.language_models import BaseChatModel
from langchain_core.messages import BaseMessage, HumanMessage
from langchain_core.runnables import Runnable
from langchain_core.tools import BaseTool
from langgraph.graph import StateGraph, END

from services.ai_agents.infrastructure.base import BaseInfraAgent, AgentState
from services.ai_agents.presentation.agents.prompts import VIDEO_SYSTEM_PROMPT


class VideoAgent(BaseInfraAgent):
    """Agent for text-to-video generation operations.
    
    This agent provides capabilities for:
    - Generating videos from text descriptions
    - Checking generation status and retrieving results
    - Managing video parameters (duration, quality, aspect ratio)
    - Supporting multiple video generation providers
    - Advanced generation with style presets and fine-tuned parameters
    
    Example:
        ```python
        from langchain_openai import ChatOpenAI
        from services.ai_agents.presentation.agents import VideoAgent
        
        llm = ChatOpenAI(model="gpt-4")
        agent = VideoAgent(llm=llm)
        
        # Generate a video from text
        result = agent.invoke({
            "messages": [HumanMessage(
                content="Generate a video of a sunset over the ocean with waves"
            )]
        })
        
        # Check status with task ID
        result = agent.invoke({
            "messages": [HumanMessage(
                content="Check the status of task abc123"
            )]
        })
        
        # Generate with advanced parameters
        result = agent.invoke({
            "messages": [HumanMessage(
                content="Generate a cinematic video of a forest in autumn with style realistic"
            )]
        })
        
        # List available providers
        result = agent.invoke({
            "messages": [HumanMessage(
                content="Show me available video generation providers"
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
        """Initialize the Video Agent.
        
        Args:
            llm: The language model for reasoning.
            tools: Optional list of tools (defaults to all video tools).
            system_prompt: Optional custom system prompt.
        """
        _tools = tools if tools is not None else []
        _prompt = system_prompt or VIDEO_SYSTEM_PROMPT
        
        super().__init__(
            llm=llm,
            tools=_tools,
            system_prompt=_prompt,
            name="VideoAgent",
            description="Text-to-video generation, video status tracking, and video provider management"
        )
    
    def create_graph(self) -> Runnable:
        """Create the LangGraph workflow for the Video Agent.
        
        Returns:
            A compiled Runnable instance.
        """
        workflow = StateGraph(AgentState)
        
        workflow.add_node("video_processor", self._create_video_node())
        
        workflow.set_entry_point("video_processor")
        workflow.add_edge("video_processor", END)
        
        return workflow.compile()
    
    def _create_video_node(self):
        """Create the video processing node."""
        def video_node(state: AgentState) -> Dict[str, Any]:
            """Process video generation requests."""
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
        
        return video_node
    
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
1. Understand the user's video generation requirements
2. Extract key parameters from natural language requests
3. Provide appropriate tool calls for video generation or status checking
4. Return clear, actionable feedback on generation results
5. Help users find optimal parameters for their use case
"""
        return SystemMessage(content=full_prompt)
    
    def generate(
        self,
        prompt: str,
        duration: int = 5,
        aspect_ratio: str = "16:9",
        quality: str = "high",
        negative_prompt: Optional[str] = None,
    ) -> Dict[str, Any]:
        """Convenience method for video generation.
        
        Args:
            prompt: Text description of the video content.
            duration: Video duration in seconds (5-10).
            aspect_ratio: Video aspect ratio.
            quality: Video quality level.
            negative_prompt: Elements to avoid.
            
        Returns:
            Generation result with task_id.
        """
        content = f"Generate a video with prompt: '{prompt}'"
        if negative_prompt:
            content += f", negative_prompt: '{negative_prompt}'"
        content += f", duration: {duration}, aspect_ratio: {aspect_ratio}, quality: {quality}"
        
        result = self.invoke({
            "messages": [HumanMessage(content=content)]
        })
        
        return result
    
    def check_status(self, task_id: str) -> Dict[str, Any]:
        """Convenience method for checking video status.
        
        Args:
            task_id: The task ID to check.
            
        Returns:
            Status result.
        """
        result = self.invoke({
            "messages": [HumanMessage(
                content=f"Check the status of video generation task: {task_id}"
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
                content="Get information about available video generation providers"
            )]
        })
        
        return result
    
    def generate_advanced(
        self,
        prompt: str,
        duration: int = 5,
        aspect_ratio: str = "16:9",
        fps: int = 24,
        quality: str = "high",
        style: str = "none",
        seed: Optional[int] = None,
        cfg_scale: float = 7.5,
        motion_intensity: float = 1.0,
    ) -> Dict[str, Any]:
        """Convenience method for advanced video generation.
        
        Args:
            prompt: Text description of the video content.
            duration: Video duration in seconds.
            aspect_ratio: Video aspect ratio.
            fps: Frames per second.
            quality: Video quality level.
            style: Visual style preset.
            seed: Random seed for reproducibility.
            cfg_scale: Guidance scale for prompt adherence.
            motion_intensity: Motion intensity multiplier.
            
        Returns:
            Generation result with task_id.
        """
        content = f"""Generate an advanced video with:
- Prompt: "{prompt}"
- Duration: {duration}s
- Aspect Ratio: {aspect_ratio}
- FPS: {fps}
- Quality: {quality}
- Style: {style}
- CFG Scale: {cfg_scale}
- Motion Intensity: {motion_intensity}
"""
        if seed:
            content += f"- Seed: {seed}"
        
        result = self.invoke({
            "messages": [HumanMessage(content=content)]
        })
        
        return result
