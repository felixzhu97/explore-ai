"""RAG Agent implementation.

This agent specializes in document retrieval, knowledge base management, and RAG pipeline operations.
"""

from typing import Any, Dict, List, Optional

from langchain_core.language_models import BaseChatModel
from langchain_core.messages import BaseMessage, HumanMessage
from langchain_core.runnables import Runnable
from langchain_core.tools import BaseTool
from langgraph.graph import StateGraph, END

from services.ai_agents.infrastructure.base import BaseInfraAgent, AgentState
from services.ai_agents.presentation.agents.prompts import RAG_SYSTEM_PROMPT


class RAGAgent(BaseInfraAgent):
    """Agent for Retrieval-Augmented Generation and document operations.
    
    This agent provides capabilities for:
    - Document indexing and management
    - Semantic similarity search
    - RAG pipeline configuration
    - Knowledge base administration
    - Multi-hop reasoning
    
    Example:
        ```python
        from langchain_openai import ChatOpenAI
        from services.ai_agents.presentation.agents import RAGAgent
        
        llm = ChatOpenAI(model="gpt-4")
        agent = RAGAgent(llm=llm)
        
        # Index a document
        result = agent.invoke({
            "messages": [HumanMessage(
                content="Index the documentation for our API into the 'docs' collection"
            )]
        })
        
        # Query the knowledge base
        result = agent.invoke({
            "messages": [HumanMessage(
                content="Find information about authentication in our API docs"
            )]
        })
        ```
    """
    
    def __init__(
        self,
        llm: BaseChatModel,
        tools: Optional[List[BaseTool]] = None,
        system_prompt: Optional[str] = None,
        default_collection: str = "default"
    ):
        """Initialize the RAG Agent.
        
        Args:
            llm: The language model for reasoning.
            tools: Optional list of tools (defaults to all RAG tools).
            system_prompt: Optional custom system prompt.
            default_collection: Default collection for operations.
        """
        _tools = tools if tools is not None else []
        _prompt = system_prompt or RAG_SYSTEM_PROMPT
        
        super().__init__(
            llm=llm,
            tools=_tools,
            system_prompt=_prompt,
            name="RAGAgent",
            description="Handles document retrieval, knowledge bases, and RAG pipelines"
        )
        
        self.default_collection = default_collection
    
    def create_graph(self) -> Runnable:
        """Create the LangGraph workflow for the RAG Agent.
        
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
                    "collection": self.default_collection,
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

Default Collection: {self.default_collection}

Available Tools:
{tool_descriptions}

Instructions:
1. Understand the user's information need
2. Select appropriate tools for retrieval
3. Retrieve relevant documents with proper filtering
4. Synthesize information into a coherent response
5. Cite sources when referencing specific information
"""
        return SystemMessage(content=full_prompt)
    
    def index_document(
        self,
        content: str,
        collection: Optional[str] = None,
        metadata: Optional[Dict[str, Any]] = None,
        chunk_size: int = 1000,
        chunk_overlap: int = 200
    ) -> Dict[str, Any]:
        """Convenience method for indexing a document.
        
        Args:
            content: Document content.
            collection: Target collection.
            metadata: Document metadata.
            chunk_size: Chunk size.
            chunk_overlap: Chunk overlap.
            
        Returns:
            Indexing result.
        """
        coll = collection or self.default_collection
        
        result = self.invoke({
            "messages": [HumanMessage(
                content=f"Index document into collection '{coll}'"
            )]
        })
        
        return result
    
    def search(
        self,
        query: str,
        collection: Optional[str] = None,
        top_k: int = 5
    ) -> Dict[str, Any]:
        """Convenience method for searching documents.
        
        Args:
            query: Search query.
            collection: Target collection.
            top_k: Number of results.
            
        Returns:
            Search results.
        """
        coll = collection or self.default_collection
        
        result = self.invoke({
            "messages": [HumanMessage(
                content=f"Search for: '{query}' in collection '{coll}'"
            )]
        })
        
        return result
    
    def query_rag(
        self,
        question: str,
        collection: Optional[str] = None,
        enable_rerank: bool = True
    ) -> Dict[str, Any]:
        """Convenience method for RAG query.
        
        Args:
            question: User question.
            collection: Target collection.
            enable_rerank: Enable reranking.
            
        Returns:
            RAG response.
        """
        coll = collection or self.default_collection
        rerank_str = "with reranking" if enable_rerank else "without reranking"
        
        result = self.invoke({
            "messages": [HumanMessage(
                content=f"Answer the following question using the knowledge base in '{coll}' {rerank_str}: {question}"
            )]
        })
        
        return result
    
    def create_collection(
        self,
        name: str,
        dimension: int = 1536
    ) -> Dict[str, Any]:
        """Convenience method for creating a collection.
        
        Args:
            name: Collection name.
            dimension: Embedding dimension.
            
        Returns:
            Creation result.
        """
        result = self.invoke({
            "messages": [HumanMessage(
                content=f"Create a new RAG collection named '{name}' with dimension {dimension}"
            )]
        })
        
        return result
    
    def list_documents(
        self,
        collection: Optional[str] = None
    ) -> Dict[str, Any]:
        """Convenience method for listing documents.
        
        Args:
            collection: Target collection.
            
        Returns:
            Document list.
        """
        coll = collection or self.default_collection
        
        result = self.invoke({
            "messages": [HumanMessage(
                content=f"List all documents in collection '{coll}'"
            )]
        })
        
        return result
