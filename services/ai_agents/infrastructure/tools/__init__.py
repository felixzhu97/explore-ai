"""Tools module initialization."""

# Tools are imported lazily to avoid instantiation issues
# Each agent imports its own tools as needed

__all__ = [
    "get_all_vector_tools",
    "get_all_k8s_tools",
    "get_all_monitoring_tools",
    "get_all_model_tools",
    "get_all_rag_tools",
    "get_all_llmops_tools",
    "get_all_feature_store_tools",
    "get_all_pipeline_tools",
    "get_all_aiops_tools",
    "get_all_tts_tools",
    "get_all_video_tools",
]


def get_all_vector_tools():
    from services.ai_agents.infrastructure.tools.vector_tools import get_all_vector_tools as _get
    return _get()


def get_all_k8s_tools():
    from services.ai_agents.infrastructure.tools.k8s_tools import get_all_k8s_tools as _get
    return _get()


def get_all_monitoring_tools():
    from services.ai_agents.infrastructure.tools.monitoring_tools import get_all_monitoring_tools as _get
    return _get()


def get_all_model_tools():
    from services.ai_agents.infrastructure.tools.model_tools import get_all_model_tools as _get
    return _get()


def get_all_rag_tools():
    from services.ai_agents.infrastructure.tools.rag_tools import get_all_rag_tools as _get
    return _get()


def get_all_llmops_tools():
    from services.ai_agents.infrastructure.tools.llmops_tools import get_all_llmops_tools as _get
    return _get()


def get_all_feature_store_tools():
    from services.ai_agents.infrastructure.tools.feature_store_tools import get_all_feature_store_tools as _get
    return _get()


def get_all_pipeline_tools():
    from services.ai_agents.infrastructure.tools.pipeline_tools import get_all_pipeline_tools as _get
    return _get()


def get_all_aiops_tools():
    from services.ai_agents.infrastructure.tools.aiops_tools import get_all_aiops_tools as _get
    return _get()


def get_all_tts_tools():
    from services.ai_agents.infrastructure.tools.tts_tools import get_all_tts_tools as _get
    return _get()


def get_all_video_tools():
    from services.ai_agents.infrastructure.tools.video_tools import get_all_video_tools as _get
    return _get()
