"""Feature Store Agent implementation.

This agent specializes in feature engineering, feature management, and feature serving for ML models.
"""

from typing import Any, Dict, List, Optional

from langchain_core.language_models import BaseChatModel
from langchain_core.messages import BaseMessage, HumanMessage
from langchain_core.runnables import Runnable
from langchain_core.tools import BaseTool
from langgraph.graph import StateGraph, END

from services.ai_agents.infrastructure.base import BaseInfraAgent, AgentState
from services.ai_agents.presentation.agents.prompts import FEATURE_STORE_SYSTEM_PROMPT


class FeatureStoreAgent(BaseInfraAgent):
    """Agent for feature engineering and feature store operations.
    
    This agent provides capabilities for:
    - Feature group creation and management
    - Feature registration and versioning
    - Feature transformations and aggregations
    - Feature materialization for training and serving
    - Point-in-time correctness for training
    - Feature lineage tracking
    
    Example:
        ```python
        from langchain_openai import ChatOpenAI
        from services.ai_agents.presentation.agents import FeatureStoreAgent
        
        llm = ChatOpenAI(model="gpt-4")
        agent = FeatureStoreAgent(llm=llm)
        
        # Create a feature group
        result = agent.invoke({
            "messages": [HumanMessage(
                content="Create feature group 'user_features' with entity 'user_id' "
                       "and features: age, country, account_age"
            )]
        })
        
        # Get feature vector
        result = agent.invoke({
            "messages": [HumanMessage(
                content="Get feature vector for user_id='12345' from 'user_features'"
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
        """Initialize the Feature Store Agent.
        
        Args:
            llm: The language model for reasoning.
            tools: Optional list of tools (defaults to all feature store tools).
            system_prompt: Optional custom system prompt.
        """
        _tools = tools if tools is not None else []
        _prompt = system_prompt or FEATURE_STORE_SYSTEM_PROMPT
        
        super().__init__(
            llm=llm,
            tools=_tools,
            system_prompt=_prompt,
            name="FeatureStoreAgent",
            description="Manages feature engineering, feature stores, and feature serving"
        )
    
    def create_graph(self) -> Runnable:
        """Create the LangGraph workflow for the Feature Store Agent.
        
        Returns:
            A compiled Runnable instance.
        """
        workflow = StateGraph(AgentState)
        
        workflow.add_node("supervisor", self._create_supervisor_node())
        
        workflow.set_entry_point("supervisor")
        workflow.add_edge("supervisor", END)
        
        return workflow.compile()
    
    def _create_supervisor_node(self):
        """Create the supervisor node that coordinates tool usage."""
        def supervisor_node(state: AgentState) -> Dict[str, Any]:
            """Supervisor node for routing and coordination."""
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
        
        return supervisor_node
    
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
1. Understand the feature requirements
2. Select appropriate tools for feature operations
3. Ensure point-in-time correctness
4. Maintain feature consistency offline and online
5. Document feature semantics and lineage
"""
        return SystemMessage(content=full_prompt)
    
    def create_feature_group(
        self,
        name: str,
        entities: List[str],
        features: List[Dict[str, str]],
        description: Optional[str] = None,
        online_enabled: bool = True
    ) -> Dict[str, Any]:
        """Convenience method for creating a feature group.
        
        Args:
            name: Feature group name.
            entities: Entity identifiers.
            features: Feature definitions.
            description: Optional description.
            online_enabled: Enable online serving.
            
        Returns:
            Creation result.
        """
        feature_str = ", ".join(f["name"] for f in features)
        
        result = self.invoke({
            "messages": [HumanMessage(
                content=f"Create feature group '{name}' with entities: {', '.join(entities)} "
                       f"and features: {feature_str}"
            )]
        })
        
        return result
    
    def register_feature(
        self,
        feature_group: str,
        feature_name: str,
        dtype: str,
        description: Optional[str] = None
    ) -> Dict[str, Any]:
        """Convenience method for registering a feature.
        
        Args:
            feature_group: Feature group name.
            feature_name: Feature name.
            dtype: Data type.
            description: Optional description.
            
        Returns:
            Registration result.
        """
        result = self.invoke({
            "messages": [HumanMessage(
                content=f"Register feature '{feature_name}' with type '{dtype}' "
                       f"in feature group '{feature_group}'"
            )]
        })
        
        return result
    
    def get_feature_vector(
        self,
        feature_group: str,
        entity_id: str,
        features: Optional[List[str]] = None
    ) -> Dict[str, Any]:
        """Convenience method for getting a feature vector.
        
        Args:
            feature_group: Feature group name.
            entity_id: Entity identifier.
            features: Feature names to retrieve.
            
        Returns:
            Feature vector.
        """
        features_str = ", ".join(features) if features else "all features"
        
        result = self.invoke({
            "messages": [HumanMessage(
                content=f"Get feature vector for {entity_id} from '{feature_group}' "
                       f"with features: {features_str}"
            )]
        })
        
        return result
    
    def write_features(
        self,
        feature_group: str,
        entity_id: str,
        features: Dict[str, Any]
    ) -> Dict[str, Any]:
        """Convenience method for writing feature values.
        
        Args:
            feature_group: Feature group name.
            entity_id: Entity identifier.
            features: Feature values to write.
            
        Returns:
            Write result.
        """
        result = self.invoke({
            "messages": [HumanMessage(
                content=f"Write features for entity '{entity_id}' in '{feature_group}'"
            )]
        })
        
        return result
    
    def materialize_features(
        self,
        feature_group: str,
        start_time: str,
        end_time: Optional[str] = None
    ) -> Dict[str, Any]:
        """Convenience method for materializing features.
        
        Args:
            feature_group: Feature group name.
            start_time: Start time.
            end_time: Optional end time.
            
        Returns:
            Materialization result.
        """
        time_range = f"from {start_time} to {end_time}" if end_time else f"from {start_time}"
        
        result = self.invoke({
            "messages": [HumanMessage(
                content=f"Materialize features for '{feature_group}' {time_range}"
            )]
        })
        
        return result
    
    def list_feature_groups(self) -> Dict[str, Any]:
        """Convenience method for listing feature groups.
        
        Returns:
            List of feature groups.
        """
        result = self.invoke({
            "messages": [HumanMessage(
                content="List all available feature groups"
            )]
        })
        
        return result
    
    def create_transformation(
        self,
        name: str,
        input_features: List[str],
        transformation_type: str,
        code: Optional[str] = None
    ) -> Dict[str, Any]:
        """Convenience method for creating a transformation.
        
        Args:
            name: Transformation name.
            input_features: Input feature names.
            transformation_type: Type of transformation.
            code: Transformation code.
            
        Returns:
            Creation result.
        """
        result = self.invoke({
            "messages": [HumanMessage(
                content=f"Create transformation '{name}' with type '{transformation_type}' "
                       f"using features: {', '.join(input_features)}"
            )]
        })
        
        return result
