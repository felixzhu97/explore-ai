"""AIOps Agent implementation.

This agent specializes in intelligent operations, anomaly detection, and automated incident response.
"""

from typing import Any, Dict, List, Optional

from langchain_core.language_models import BaseChatModel
from langchain_core.messages import BaseMessage, HumanMessage
from langchain_core.runnables import Runnable
from langchain_core.tools import BaseTool
from langgraph.graph import StateGraph, END

from services.ai_agents.infrastructure.base import BaseInfraAgent, AgentState
from services.ai_agents.presentation.agents.prompts import AIOPS_SYSTEM_PROMPT


class AIOpsAgent(BaseInfraAgent):
    """Agent for intelligent operations and automated incident response.
    
    This agent provides capabilities for:
    - Anomaly detection in metrics and logs
    - Root cause analysis across systems
    - Incident creation and management
    - Automated remediation
    - System health monitoring
    - Event correlation and alerting
    
    Example:
        ```python
        from langchain_openai import ChatOpenAI
        from services.ai_agents.presentation.agents import AIOpsAgent
        
        llm = ChatOpenAI(model="gpt-4")
        agent = AIOpsAgent(llm=llm)
        
        # Detect anomalies
        result = agent.invoke({
            "messages": [HumanMessage(
                content="Check for anomalies in 'api_request_latency' metric"
            )]
        })
        
        # Perform root cause analysis
        result = agent.invoke({
            "messages": [HumanMessage(
                content="Analyze root cause for incident 'inc_abc123' "
                       "affecting 'api-gateway' and 'auth-service'"
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
        """Initialize the AIOps Agent.
        
        Args:
            llm: The language model for reasoning.
            tools: Optional list of tools (defaults to all AIOps tools).
            system_prompt: Optional custom system prompt.
        """
        _tools = tools if tools is not None else []
        _prompt = system_prompt or AIOPS_SYSTEM_PROMPT
        
        super().__init__(
            llm=llm,
            tools=_tools,
            system_prompt=_prompt,
            name="AIOpsAgent",
            description="Intelligent operations, anomaly detection, and incident response"
        )
    
    def create_graph(self) -> Runnable:
        """Create the LangGraph workflow for the AIOps Agent.
        
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
1. Prioritize minimizing user impact
2. Provide clear communication
3. Enable human oversight for critical decisions
4. Correlate events across systems
5. Document findings and actions
"""
        return SystemMessage(content=full_prompt)
    
    def detect_anomaly(
        self,
        metric: str,
        time_range: str = "1h",
        sensitivity: float = 0.5
    ) -> Dict[str, Any]:
        """Convenience method for anomaly detection.
        
        Args:
            metric: Metric name.
            time_range: Time range for analysis.
            sensitivity: Detection sensitivity.
            
        Returns:
            Anomaly detection result.
        """
        result = self.invoke({
            "messages": [HumanMessage(
                content=f"Detect anomalies in metric '{metric}' for the last {time_range}"
            )]
        })
        
        return result
    
    def search_logs(
        self,
        query: str,
        time_range: str = "1h",
        limit: int = 100
    ) -> Dict[str, Any]:
        """Convenience method for log search.
        
        Args:
            query: Search query.
            time_range: Time range.
            limit: Maximum results.
            
        Returns:
            Log search results.
        """
        result = self.invoke({
            "messages": [HumanMessage(
                content=f"Search logs for: '{query}' in the last {time_range}"
            )]
        })
        
        return result
    
    def query_metrics(
        self,
        metric: str,
        time_range: str = "5m",
        aggregation: str = "avg"
    ) -> Dict[str, Any]:
        """Convenience method for metric query.
        
        Args:
            metric: Metric name.
            time_range: Time range.
            aggregation: Aggregation method.
            
        Returns:
            Metric data.
        """
        result = self.invoke({
            "messages": [HumanMessage(
                content=f"Query metric '{metric}' with {aggregation} aggregation for {time_range}"
            )]
        })
        
        return result
    
    def create_incident(
        self,
        title: str,
        severity: str,
        description: str,
        affected_systems: List[str],
        labels: Optional[Dict[str, str]] = None
    ) -> Dict[str, Any]:
        """Convenience method for creating an incident.
        
        Args:
            title: Incident title.
            severity: Severity level.
            description: Incident description.
            affected_systems: Affected systems.
            labels: Optional labels.
            
        Returns:
            Created incident.
        """
        result = self.invoke({
            "messages": [HumanMessage(
                content=f"Create incident '{title}' with severity '{severity}' "
                       f"affecting: {', '.join(affected_systems)}"
            )]
        })
        
        return result
    
    def get_incident(self, incident_id: str) -> Dict[str, Any]:
        """Convenience method for getting incident details.
        
        Args:
            incident_id: Incident ID.
            
        Returns:
            Incident details.
        """
        result = self.invoke({
            "messages": [HumanMessage(
                content=f"Get details for incident '{incident_id}'"
            )]
        })
        
        return result
    
    def list_incidents(
        self,
        status: Optional[str] = None,
        severity: Optional[str] = None
    ) -> Dict[str, Any]:
        """Convenience method for listing incidents.
        
        Args:
            status: Status filter.
            severity: Severity filter.
            
        Returns:
            List of incidents.
        """
        filters = []
        if status:
            filters.append(f"status '{status}'")
        if severity:
            filters.append(f"severity '{severity}'")
        
        filter_str = f" with {', '.join(filters)}" if filters else ""
        
        result = self.invoke({
            "messages": [HumanMessage(
                content=f"List incidents{filter_str}"
            )]
        })
        
        return result
    
    def update_incident(
        self,
        incident_id: str,
        status: str,
        message: Optional[str] = None
    ) -> Dict[str, Any]:
        """Convenience method for updating incident status.
        
        Args:
            incident_id: Incident ID.
            status: New status.
            message: Optional message.
            
        Returns:
            Update result.
        """
        msg_str = f" with message: {message}" if message else ""
        
        result = self.invoke({
            "messages": [HumanMessage(
                content=f"Update incident '{incident_id}' to status '{status}'{msg_str}"
            )]
        })
        
        return result
    
    def analyze_root_cause(
        self,
        incident_id: str,
        time_range: str = "1h",
        affected_services: Optional[List[str]] = None,
        include_logs: bool = True
    ) -> Dict[str, Any]:
        """Convenience method for root cause analysis.
        
        Args:
            incident_id: Incident ID.
            time_range: Time range for analysis.
            affected_services: Affected services.
            include_logs: Include logs in analysis.
            
        Returns:
            Root cause analysis result.
        """
        services_str = ", ".join(affected_services) if affected_services else "the affected systems"
        logs_str = "including related logs" if include_logs else "using metrics only"
        
        result = self.invoke({
            "messages": [HumanMessage(
                content=f"Analyze root cause for incident '{incident_id}' affecting {services_str} "
                       f"{logs_str} over the last {time_range}"
            )]
        })
        
        return result
    
    def get_system_health(self) -> Dict[str, Any]:
        """Convenience method for getting system health.
        
        Returns:
            System health overview.
        """
        result = self.invoke({
            "messages": [HumanMessage(
                content="Provide an overview of system health across all services"
            )]
        })
        
        return result
    
    def acknowledge_alert(
        self,
        alert_id: str,
        user: str
    ) -> Dict[str, Any]:
        """Convenience method for acknowledging an alert.
        
        Args:
            alert_id: Alert ID.
            user: User acknowledging.
            
        Returns:
            Acknowledge result.
        """
        result = self.invoke({
            "messages": [HumanMessage(
                content=f"Acknowledge alert '{alert_id}'"
            )]
        })
        
        return result
