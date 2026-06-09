"""Agents module initialization."""

from services.ai_agents.presentation.agents.vector_db_agent import VectorDBAgent
from services.ai_agents.presentation.agents.k8s_agent import K8sAgent
from services.ai_agents.presentation.agents.monitoring_agent import MonitoringAgent
from services.ai_agents.presentation.agents.model_agent import ModelAgent
from services.ai_agents.presentation.agents.supervisor import (
    SupervisorAgent,
    SupervisorState,
    create_supervisor_with_defaults,
)
from services.ai_agents.presentation.agents.rag_agent import RAGAgent
from services.ai_agents.presentation.agents.llmops_agent import LLMOpsAgent
from services.ai_agents.presentation.agents.feature_store_agent import FeatureStoreAgent
from services.ai_agents.presentation.agents.pipeline_agent import PipelineAgent
from services.ai_agents.presentation.agents.aiops_agent import AIOpsAgent
from services.ai_agents.presentation.agents.tts_agent import TTSAgent
from services.ai_agents.presentation.agents.video_agent import VideoAgent

__all__ = [
    # Original agents
    "VectorDBAgent",
    "K8sAgent",
    "MonitoringAgent",
    "ModelAgent",
    "SupervisorAgent",
    "SupervisorState",
    "create_supervisor_with_defaults",
    # New agents
    "RAGAgent",
    "LLMOpsAgent",
    "FeatureStoreAgent",
    "PipelineAgent",
    "AIOpsAgent",
    "TTSAgent",
    "VideoAgent",
]
