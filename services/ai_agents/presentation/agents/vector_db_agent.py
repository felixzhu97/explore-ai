"""Vector Database Agent implementation with real operations."""

from typing import Any, Dict, List, Optional

from langchain_core.language_models import BaseChatModel
from langchain_core.runnables import Runnable
from langchain_core.tools import BaseTool

from services.ai_agents.infrastructure.base import BaseInfraAgent, AgentState
from services.ai_agents.infrastructure.tools.vector_tools import get_all_vector_tools


class VectorDBAgent(BaseInfraAgent):
    """Agent for vector database operations."""
    
    def __init__(
        self,
        llm: BaseChatModel,
        tools: Optional[List[BaseTool]] = None,
    ):
        tools = tools if tools is not None else get_all_vector_tools()
        super().__init__(
            llm=llm,
            tools=tools,
            system_prompt=VECTOR_DB_SYSTEM_PROMPT,
            name="VectorDBAgent",
            description="Handles vector database operations, embeddings, and similarity search",
        )
    
    def create_graph(self) -> Runnable:
        return super().create_graph()


VECTOR_DB_SYSTEM_PROMPT = """You are a Vector Database expert. Your role is to help manage vector collections and perform similarity searches.

IMPORTANT: When you need to perform an action, respond with ONLY a JSON object in this exact format:
{"tool": "tool_name", "args": {"arg1": "value1", "arg2": "value2"}}

Available tools:
- list_collections: No arguments needed
- create_collection: args: name (string), dimension (int, optional, default 1536)
- search_vectors: args: query (string), collection (string, optional, default "default"), top_k (int, optional, default 5)
- insert_vector: args: content (string), collection (string, optional)
- get_collection_info: args: collection (string)
- delete_collection: args: name (string)

Example responses:
User: What collections do I have?
{"tool": "list_collections", "args": {}}

User: Search for "machine learning"
{"tool": "search_vectors", "args": {"query": "machine learning"}}

User: Search in my collection
{"tool": "search_vectors", "args": {"query": "machine learning", "collection": "my_collection", "top_k": 10}}

After tool execution, I will show you the results. Then summarize the results concisely.
"""
