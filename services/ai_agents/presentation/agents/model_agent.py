"""Model Agent implementation."""

from typing import Any, Dict, List, Optional

from langchain_core.language_models import BaseChatModel
from langchain_core.messages import BaseMessage, HumanMessage
from langchain_core.runnables import Runnable
from langchain_core.tools import BaseTool
from langgraph.graph import StateGraph, END

from services.ai_agents.infrastructure.base import BaseInfraAgent, AgentState
from services.ai_agents.presentation.agents.prompts import MODEL_SYSTEM_PROMPT


class ModelAgent(BaseInfraAgent):
    """Agent for ML model lifecycle management."""
    
    def __init__(
        self,
        llm: BaseChatModel,
        tools: Optional[List[BaseTool]] = None,
        system_prompt: Optional[str] = None,
    ):
        _prompt = system_prompt or MODEL_SYSTEM_PROMPT
        super().__init__(
            llm=llm,
            tools=tools or [],
            system_prompt=_prompt,
            name="ModelAgent",
            description="Manages ML model lifecycle, deployment, and versioning",
        )
    
    def create_graph(self) -> StateGraph:
        workflow = StateGraph(AgentState)
        workflow.add_node("process", self._create_process_node())
        workflow.set_entry_point("process")
        workflow.add_edge("process", END)
        return workflow.compile()
