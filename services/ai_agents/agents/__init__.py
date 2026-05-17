"""Agents module initialization."""

from services.ai_agents.agents.vector_db_agent import VectorDBAgent
from services.ai_agents.agents.k8s_agent import K8sAgent
from services.ai_agents.agents.monitoring_agent import MonitoringAgent
from services.ai_agents.agents.model_agent import ModelAgent
from services.ai_agents.agents.supervisor import (
    SupervisorAgent,
    SupervisorState,
    create_supervisor_with_defaults,
)
from services.ai_agents.agents.rag_agent import RAGAgent
from services.ai_agents.agents.llmops_agent import LLMOpsAgent
from services.ai_agents.agents.feature_store_agent import FeatureStoreAgent
from services.ai_agents.agents.pipeline_agent import PipelineAgent
from services.ai_agents.agents.aiops_agent import AIOpsAgent
from services.ai_agents.agents.tts_agent import TTSAgent
from services.ai_agents.agents.video_agent import VideoAgent

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
