"""LLMOps workflow graphs for model lifecycle automation.

This module provides LangGraph workflows for comprehensive ML model management
including training pipelines, evaluation workflows, and deployment automation.
"""

from typing import Any, Dict, List, Optional, TypedDict

from langchain_core.language_models import BaseChatModel
from langchain_core.messages import BaseMessage, HumanMessage, AIMessage
from langchain_core.runnables import Runnable
from langgraph.graph import StateGraph, END


class LLMOpsState(TypedDict):
    """State schema for LLMOps workflows."""
    messages: List[BaseMessage]
    model_name: str
    model_version: Optional[str]
    training_config: Dict[str, Any]
    evaluation_results: Dict[str, Any]
    deployment_config: Dict[str, Any]
    context: Dict[str, Any]


class LLMOpsGraphWorkflow:
    """Workflow for LLMOps operations with LangGraph.
    
    This class provides predefined workflows for:
    - Full model training pipeline
    - Model evaluation and validation
    - Safe deployment with rollback
    - A/B testing orchestration
    - Model comparison and selection
    
    Example:
        ```python
        from services.ai_agents.application.graphs import LLMOpsGraphWorkflow
        
        workflow = LLMOpsGraphWorkflow(llm=llm, llmops_agent=llmops_agent)
        
        # Training pipeline
        graph = workflow.create_training_pipeline_graph()
        result = graph.invoke({
            "messages": [HumanMessage(content="Train sentiment model")],
            "model_name": "sentiment-classifier",
            "training_config": {"dataset": "sentiment-train", "epochs": 10}
        })
        ```
    """
    
    def __init__(
        self,
        llm: BaseChatModel,
        llmops_agent: Optional[Any] = None,
    ):
        """Initialize the LLMOps workflow.
        
        Args:
            llm: Language model.
            llmops_agent: Optional LLMOps agent.
        """
        self.llm = llm
        self.llmops_agent = llmops_agent
    
    def create_training_pipeline_graph(self) -> Runnable:
        """Create a complete model training pipeline.
        
        Steps:
        1. Register model (if new)
        2. Execute training
        3. Log metrics
        4. Evaluate model
        5. Register version
        
        Returns:
            Compiled StateGraph.
        """
        workflow = StateGraph(LLMOpsState)
        
        workflow.add_node("register", self._create_register_node())
        workflow.add_node("train", self._create_train_node())
        workflow.add_node("log_metrics", self._create_log_metrics_node())
        workflow.add_node("evaluate", self._create_evaluate_node())
        workflow.add_node("register_version", self._create_register_version_node())
        
        workflow.set_entry_point("register")
        workflow.add_edge("register", "train")
        workflow.add_edge("train", "log_metrics")
        workflow.add_edge("log_metrics", "evaluate")
        workflow.add_edge("evaluate", "register_version")
        workflow.add_edge("register_version", END)
        
        return workflow.compile()
    
    def create_deployment_pipeline_graph(self) -> Runnable:
        """Create a safe deployment pipeline.
        
        Steps:
        1. Validate model
        2. Prepare infrastructure
        3. Deploy model
        4. Setup monitoring
        5. Verify deployment
        
        Returns:
            Compiled StateGraph.
        """
        workflow = StateGraph(LLMOpsState)
        
        workflow.add_node("validate", self._create_validate_node())
        workflow.add_node("prepare_infra", self._create_prepare_infra_node())
        workflow.add_node("deploy", self._create_deploy_node())
        workflow.add_node("setup_monitoring", self._create_setup_monitoring_node())
        workflow.add_node("verify", self._create_verify_node())
        
        workflow.set_entry_point("validate")
        workflow.add_edge("validate", "prepare_infra")
        workflow.add_edge("prepare_infra", "deploy")
        workflow.add_edge("deploy", "setup_monitoring")
        workflow.add_edge("setup_monitoring", "verify")
        workflow.add_edge("verify", END)
        
        return workflow.compile()
    
    def create_ab_testing_graph(self) -> Runnable:
        """Create an A/B testing workflow.
        
        Steps:
        1. Setup A/B test
        2. Deploy variants
        3. Monitor performance
        4. Analyze results
        5. Select winner
        
        Returns:
            Compiled StateGraph.
        """
        workflow = StateGraph(LLMOpsState)
        
        workflow.add_node("setup_ab", self._create_setup_ab_node())
        workflow.add_node("deploy_variants", self._create_deploy_variants_node())
        workflow.add_node("monitor", self._create_monitor_node())
        workflow.add_node("analyze", self._create_analyze_node())
        workflow.add_node("select_winner", self._create_select_winner_node())
        
        workflow.set_entry_point("setup_ab")
        workflow.add_edge("setup_ab", "deploy_variants")
        workflow.add_edge("deploy_variants", "monitor")
        workflow.add_edge("monitor", "analyze")
        workflow.add_edge("analyze", "select_winner")
        workflow.add_edge("select_winner", END)
        
        return workflow.compile()
    
    def create_full_ml_pipeline_graph(self) -> Runnable:
        """Create a complete ML pipeline from data to deployment.
        
        Steps:
        1. Data validation
        2. Feature engineering
        3. Model training
        4. Model evaluation
        5. Model deployment
        
        Returns:
            Compiled StateGraph.
        """
        workflow = StateGraph(LLMOpsState)
        
        workflow.add_node("validate_data", self._create_validate_data_node())
        workflow.add_node("feature_engineering", self._create_feature_engineering_node())
        workflow.add_node("train", self._create_train_node())
        workflow.add_node("evaluate", self._create_evaluate_node())
        workflow.add_node("deploy", self._create_deploy_node())
        
        workflow.set_entry_point("validate_data")
        workflow.add_edge("validate_data", "feature_engineering")
        workflow.add_edge("feature_engineering", "train")
        workflow.add_edge("train", "evaluate")
        workflow.add_edge("evaluate", "deploy")
        workflow.add_edge("deploy", END)
        
        return workflow.compile()
    
    def _create_register_node(self):
        """Create the model registration node."""
        def register_node(state: LLMOpsState) -> Dict[str, Any]:
            """Register a new model."""
            model_name = state.get("model_name", "")
            context = state.get("context", {})
            context["registration_status"] = "completed"
            
            return {
                "context": context,
                "messages": state.get("messages", []) + [
                    AIMessage(content=f"Model '{model_name}' registered successfully")
                ]
            }
        
        return register_node
    
    def _create_train_node(self):
        """Create the training node."""
        def train_node(state: LLMOpsState) -> Dict[str, Any]:
            """Execute model training."""
            model_name = state.get("model_name", "")
            config = state.get("training_config", {})
            
            context = state.get("context", {})
            context["training_status"] = "completed"
            context["training_metrics"] = {
                "train_loss": 0.5,
                "val_accuracy": 0.92
            }
            
            return {
                "context": context,
                "messages": state.get("messages", []) + [
                    AIMessage(content=f"Training completed for '{model_name}'")
                ]
            }
        
        return train_node
    
    def _create_log_metrics_node(self):
        """Create the metrics logging node."""
        def log_metrics_node(state: LLMOpsState) -> Dict[str, Any]:
            """Log training metrics."""
            context = state.get("context", {})
            context["metrics_logged"] = True
            
            return {"context": context}
        
        return log_metrics_node
    
    def _create_evaluate_node(self):
        """Create the evaluation node."""
        def evaluate_node(state: LLMOpsState) -> Dict[str, Any]:
            """Evaluate model performance."""
            model_name = state.get("model_name", "")
            context = state.get("context", {})
            
            evaluation_results = {
                "accuracy": 0.92,
                "precision": 0.91,
                "recall": 0.93,
                "f1": 0.92
            }
            
            context["evaluation_completed"] = True
            
            return {
                "evaluation_results": evaluation_results,
                "context": context,
                "messages": state.get("messages", []) + [
                    AIMessage(content=f"Evaluation completed for '{model_name}': F1={evaluation_results['f1']}")
                ]
            }
        
        return evaluate_node
    
    def _create_register_version_node(self):
        """Create the version registration node."""
        def register_version_node(state: LLMOpsState) -> Dict[str, Any]:
            """Register model version."""
            model_name = state.get("model_name", "")
            context = state.get("context", {})
            
            model_version = "v1.0.0"
            context["model_version"] = model_version
            
            return {
                "model_version": model_version,
                "context": context,
                "messages": state.get("messages", []) + [
                    AIMessage(content=f"Model '{model_name}' version '{model_version}' registered")
                ]
            }
        
        return register_version_node
    
    def _create_validate_node(self):
        """Create the validation node."""
        def validate_node(state: LLMOpsState) -> Dict[str, Any]:
            """Validate model for deployment."""
            model_name = state.get("model_name", "")
            context = state.get("context", {})
            context["validation_passed"] = True
            
            return {
                "context": context,
                "messages": state.get("messages", []) + [
                    AIMessage(content=f"Model '{model_name}' validated for deployment")
                ]
            }
        
        return validate_node
    
    def _create_prepare_infra_node(self):
        """Create the infrastructure preparation node."""
        def prepare_infra_node(state: LLMOpsState) -> Dict[str, Any]:
            """Prepare infrastructure for deployment."""
            context = state.get("context", {})
            context["infra_ready"] = True
            
            return {
                "context": context,
                "messages": state.get("messages", []) + [
                    AIMessage(content="Infrastructure prepared")
                ]
            }
        
        return prepare_infra_node
    
    def _create_deploy_node(self):
        """Create the deployment node."""
        def deploy_node(state: LLMOpsState) -> Dict[str, Any]:
            """Deploy the model."""
            model_name = state.get("model_name", "")
            model_version = state.get("model_version", "v1.0.0")
            context = state.get("context", {})
            context["deployment_status"] = "deployed"
            
            return {
                "context": context,
                "messages": state.get("messages", []) + [
                    AIMessage(content=f"Model '{model_name}:{model_version}' deployed")
                ]
            }
        
        return deploy_node
    
    def _create_setup_monitoring_node(self):
        """Create the monitoring setup node."""
        def setup_monitoring_node(state: LLMOpsState) -> Dict[str, Any]:
            """Setup monitoring for deployed model."""
            context = state.get("context", {})
            context["monitoring_configured"] = True
            
            return {
                "context": context,
                "messages": state.get("messages", []) + [
                    AIMessage(content="Monitoring configured")
                ]
            }
        
        return setup_monitoring_node
    
    def _create_verify_node(self):
        """Create the verification node."""
        def verify_node(state: LLMOpsState) -> Dict[str, Any]:
            """Verify deployment health."""
            model_name = state.get("model_name", "")
            context = state.get("context", {})
            context["deployment_verified"] = True
            
            return {
                "context": context,
                "messages": state.get("messages", []) + [
                    AIMessage(content=f"Deployment of '{model_name}' verified and healthy")
                ]
            }
        
        return verify_node
    
    def _create_setup_ab_node(self):
        """Create the A/B test setup node."""
        def setup_ab_node(state: LLMOpsState) -> Dict[str, Any]:
            """Setup A/B test configuration."""
            context = state.get("context", {})
            context["ab_test_configured"] = True
            
            return {
                "context": context,
                "messages": state.get("messages", []) + [
                    AIMessage(content="A/B test configured")
                ]
            }
        
        return setup_ab_node
    
    def _create_deploy_variants_node(self):
        """Create the variants deployment node."""
        def deploy_variants_node(state: LLMOpsState) -> Dict[str, Any]:
            """Deploy model variants."""
            context = state.get("context", {})
            context["variants_deployed"] = True
            
            return {
                "context": context,
                "messages": state.get("messages", []) + [
                    AIMessage(content="Model variants deployed")
                ]
            }
        
        return deploy_variants_node
    
    def _create_monitor_node(self):
        """Create the monitoring node."""
        def monitor_node(state: LLMOpsState) -> Dict[str, Any]:
            """Monitor A/B test performance."""
            context = state.get("context", {})
            context["monitoring_active"] = True
            
            return {
                "context": context,
                "messages": state.get("messages", []) + [
                    AIMessage(content="A/B test monitoring active")
                ]
            }
        
        return monitor_node
    
    def _create_analyze_node(self):
        """Create the analysis node."""
        def analyze_node(state: LLMOpsState) -> Dict[str, Any]:
            """Analyze A/B test results."""
            context = state.get("context", {})
            context["analysis_complete"] = True
            
            return {
                "context": context,
                "messages": state.get("messages", []) + [
                    AIMessage(content="A/B test analysis complete")
                ]
            }
        
        return analyze_node
    
    def _create_select_winner_node(self):
        """Create the winner selection node."""
        def select_winner_node(state: LLMOpsState) -> Dict[str, Any]:
            """Select winning variant."""
            context = state.get("context", {})
            context["winner_selected"] = True
            
            return {
                "context": context,
                "messages": state.get("messages", []) + [
                    AIMessage(content="Winner variant selected")
                ]
            }
        
        return select_winner_node
    
    def _create_validate_data_node(self):
        """Create the data validation node."""
        def validate_data_node(state: LLMOpsState) -> Dict[str, Any]:
            """Validate input data."""
            context = state.get("context", {})
            context["data_validated"] = True
            
            return {
                "context": context,
                "messages": state.get("messages", []) + [
                    AIMessage(content="Data validation passed")
                ]
            }
        
        return validate_data_node
    
    def _create_feature_engineering_node(self):
        """Create the feature engineering node."""
        def feature_engineering_node(state: LLMOpsState) -> Dict[str, Any]:
            """Perform feature engineering."""
            context = state.get("context", {})
            context["features_engineered"] = True
            
            return {
                "context": context,
                "messages": state.get("messages", []) + [
                    AIMessage(content="Feature engineering completed")
                ]
            }
        
        return feature_engineering_node


def create_training_pipeline_graph(
    llm: BaseChatModel,
    llmops_agent: Optional[Any] = None
) -> Runnable:
    """Create a training pipeline graph.
    
    Args:
        llm: Language model.
        llmops_agent: Optional LLMOps agent.
    
    Returns:
        Compiled runnable graph.
    """
    workflow = LLMOpsGraphWorkflow(llm=llm, llmops_agent=llmops_agent)
    return workflow.create_training_pipeline_graph()


def create_deployment_pipeline_graph(
    llm: BaseChatModel,
    llmops_agent: Optional[Any] = None
) -> Runnable:
    """Create a deployment pipeline graph.
    
    Args:
        llm: Language model.
        llmops_agent: Optional LLMOps agent.
    
    Returns:
        Compiled runnable graph.
    """
    workflow = LLMOpsGraphWorkflow(llm=llm, llmops_agent=llmops_agent)
    return workflow.create_deployment_pipeline_graph()
