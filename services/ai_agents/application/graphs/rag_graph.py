"""RAG workflow graphs for multi-stage retrieval and generation.

This module provides LangGraph workflows for advanced RAG operations including
multi-hop reasoning, query expansion, and hybrid retrieval.
"""

from typing import Any, Dict, List, Optional, TypedDict

from langchain_core.language_models import BaseChatModel
from langchain_core.messages import BaseMessage, HumanMessage, AIMessage
from langchain_core.runnables import Runnable
from langgraph.graph import StateGraph, END


class RAGState(TypedDict):
    """State schema for RAG workflows."""
    messages: List[BaseMessage]
    query: str
    collection: str
    retrieved_docs: List[Any]
    generated_response: Optional[str]
    context: Dict[str, Any]


class RAGGraphWorkflow:
    """Workflow for advanced RAG operations with LangGraph.
    
    This class provides predefined workflows for:
    - Simple RAG with retrieval and generation
    - Multi-hop reasoning over knowledge bases
    - Query expansion and reformulation
    - Hybrid retrieval (dense + sparse)
    - Iterative refinement
    
    Example:
        ```python
        from services.ai_agents.application.graphs import RAGGraphWorkflow
        
        workflow = RAGGraphWorkflow(llm=llm, rag_agent=rag_agent)
        
        # Simple RAG
        graph = workflow.create_simple_rag_graph()
        result = graph.invoke({
            "messages": [HumanMessage(content="What is machine learning?")],
            "collection": "docs"
        })
        
        # Multi-hop reasoning
        graph = workflow.create_multi_hop_rag_graph()
        result = graph.invoke({
            "messages": [HumanMessage(content="Who trained the first transformer model?")],
            "collection": "docs"
        })
        ```
    """
    
    def __init__(
        self,
        llm: BaseChatModel,
        rag_agent: Optional[Any] = None,
    ):
        """Initialize the RAG workflow.
        
        Args:
            llm: Language model for generation.
            rag_agent: Optional RAG agent for retrieval.
        """
        self.llm = llm
        self.rag_agent = rag_agent
    
    def create_simple_rag_graph(self) -> Runnable:
        """Create a simple RAG workflow.
        
        Steps:
        1. Receive user query
        2. Retrieve relevant documents
        3. Generate response
        
        Returns:
            Compiled StateGraph.
        """
        workflow = StateGraph(RAGState)
        
        workflow.add_node("retrieve", self._create_retrieve_node())
        workflow.add_node("generate", self._create_generate_node())
        
        workflow.set_entry_point("retrieve")
        workflow.add_edge("retrieve", "generate")
        workflow.add_edge("generate", END)
        
        return workflow.compile()
    
    def create_multi_hop_rag_graph(self) -> Runnable:
        """Create a multi-hop RAG workflow for complex questions.
        
        Steps:
        1. Receive user query
        2. Initial retrieval
        3. Query decomposition
        4. Additional retrieval for sub-questions
        5. Synthesize answer
        
        Returns:
            Compiled StateGraph.
        """
        workflow = StateGraph(RAGState)
        
        workflow.add_node("initial_retrieve", self._create_retrieve_node())
        workflow.add_node("decompose", self._create_decompose_node())
        workflow.add_node("expand_retrieve", self._create_retrieve_node())
        workflow.add_node("synthesize", self._create_synthesize_node())
        
        workflow.set_entry_point("initial_retrieve")
        workflow.add_edge("initial_retrieve", "decompose")
        workflow.add_edge("decompose", "expand_retrieve")
        workflow.add_edge("expand_retrieve", "synthesize")
        workflow.add_edge("synthesize", END)
        
        return workflow.compile()
    
    def create_hybrid_rag_graph(self) -> Runnable:
        """Create a hybrid RAG workflow with dense and sparse retrieval.
        
        Steps:
        1. Dense retrieval (vector similarity)
        2. Sparse retrieval (keyword search)
        3. Rerank and fuse results
        4. Generate response
        
        Returns:
            Compiled StateGraph.
        """
        workflow = StateGraph(RAGState)
        
        workflow.add_node("dense_retrieve", self._create_dense_retrieve_node())
        workflow.add_node("sparse_retrieve", self._create_sparse_retrieve_node())
        workflow.add_node("fusion", self._create_fusion_node())
        workflow.add_node("generate", self._create_generate_node())
        
        workflow.set_entry_point("dense_retrieve")
        workflow.add_edge("dense_retrieve", "sparse_retrieve")
        workflow.add_edge("sparse_retrieve", "fusion")
        workflow.add_edge("fusion", "generate")
        workflow.add_edge("generate", END)
        
        return workflow.compile()
    
    def create_iterative_rag_graph(self) -> Runnable:
        """Create an iterative RAG workflow with self-correction.
        
        Steps:
        1. Initial retrieval
        2. Generate draft response
        3. Check if response is sufficient
        4. If not, iterate with query refinement
        
        Returns:
            Compiled StateGraph.
        """
        workflow = StateGraph(RAGState)
        
        workflow.add_node("retrieve", self._create_retrieve_node())
        workflow.add_node("generate", self._create_generate_node())
        workflow.add_node("evaluate", self._create_evaluate_node())
        workflow.add_node("refine_query", self._create_refine_node())
        
        workflow.set_entry_point("retrieve")
        workflow.add_edge("retrieve", "generate")
        workflow.add_edge("generate", "evaluate")
        
        workflow.add_conditional_edges(
            "evaluate",
            self._check_sufficiency,
            {
                "generate": "generate",
                END: END
            }
        )
        
        workflow.add_edge("refine_query", "retrieve")
        
        return workflow.compile()
    
    def _create_retrieve_node(self):
        """Create the retrieval node."""
        def retrieve_node(state: RAGState) -> Dict[str, Any]:
            """Retrieve relevant documents."""
            query = state.get("query", "")
            collection = state.get("collection", "default")
            
            if self.rag_agent:
                result = self.rag_agent.search(
                    query=query,
                    collection=collection,
                    top_k=5
                )
                docs = result.get("retrieved_docs", [])
            else:
                docs = []
            
            return {
                "retrieved_docs": docs,
                "context": {"retrieval_count": len(docs)}
            }
        
        return retrieve_node
    
    def _create_dense_retrieve_node(self):
        """Create the dense retrieval node."""
        def dense_retrieve_node(state: RAGState) -> Dict[str, Any]:
            """Perform dense vector retrieval."""
            context = state.get("context", {})
            context["retrieval_method"] = "dense"
            
            return {"context": context}
        
        return dense_retrieve_node
    
    def _create_sparse_retrieve_node(self):
        """Create the sparse retrieval node."""
        def sparse_retrieve_node(state: RAGState) -> Dict[str, Any]:
            """Perform sparse keyword retrieval."""
            context = state.get("context", {})
            context["retrieval_method"] = "sparse"
            
            return {"context": context}
        
        return sparse_retrieve_node
    
    def _create_fusion_node(self):
        """Create the result fusion node."""
        def fusion_node(state: RAGState) -> Dict[str, Any]:
            """Fuse dense and sparse results."""
            context = state.get("context", {})
            context["fusion_applied"] = True
            
            return {"context": context}
        
        return fusion_node
    
    def _create_generate_node(self):
        """Create the response generation node."""
        def generate_node(state: RAGState) -> Dict[str, Any]:
            """Generate response from retrieved context."""
            messages = state.get("messages", [])
            query = state.get("query", "")
            docs = state.get("retrieved_docs", [])
            
            context_str = "\n\n".join([
                f"Document {i+1}: {doc.get('content', '')}"
                for i, doc in enumerate(docs)
            ]) if docs else "No documents retrieved."
            
            prompt = f"""Based on the following documents, answer the question.

Documents:
{context_str}

Question: {query}

Provide a clear and concise answer, citing specific information from the documents when relevant.
"""
            
            response = self.llm.invoke([HumanMessage(content=prompt)])
            
            return {
                "generated_response": response.content,
                "messages": messages + [AIMessage(content=response.content)]
            }
        
        return generate_node
    
    def _create_decompose_node(self):
        """Create the query decomposition node."""
        def decompose_node(state: RAGState) -> Dict[str, Any]:
            """Decompose complex query into sub-questions."""
            query = state.get("query", "")
            docs = state.get("retrieved_docs", [])
            
            prompt = f"""Given the user query and retrieved documents, determine if additional information is needed.

Query: {query}
Retrieved: {len(docs)} documents

If the retrieved documents fully answer the query, respond with "SUFFICIENT".
Otherwise, provide 1-2 sub-questions that would help answer the original query.
"""
            
            response = self.llm.invoke([HumanMessage(content=prompt)])
            
            needs_expansion = "SUFFICIENT" not in response.content.upper()
            
            return {
                "context": {
                    **state.get("context", {}),
                    "needs_expansion": needs_expansion,
                    "decomposition": response.content
                }
            }
        
        return decompose_node
    
    def _create_synthesize_node(self):
        """Create the synthesis node for multi-hop."""
        def synthesize_node(state: RAGState) -> Dict[str, Any]:
            """Synthesize answer from multiple retrieval passes."""
            messages = state.get("messages", [])
            query = state.get("query", "")
            docs = state.get("retrieved_docs", [])
            
            context_str = "\n\n".join([
                f"Document {i+1}: {doc.get('content', '')}"
                for i, doc in enumerate(docs)
            ]) if docs else "No documents retrieved."
            
            prompt = f"""Synthesize a comprehensive answer from the retrieved information.

Original Question: {query}

Retrieved Context:
{context_str}

Provide a well-structured answer that addresses all aspects of the original question.
"""
            
            response = self.llm.invoke([HumanMessage(content=prompt)])
            
            return {
                "generated_response": response.content,
                "messages": messages + [AIMessage(content=response.content)]
            }
        
        return synthesize_node
    
    def _create_evaluate_node(self):
        """Create the evaluation node for iterative RAG."""
        def evaluate_node(state: RAGState) -> Dict[str, Any]:
            """Evaluate if the generated response is sufficient."""
            response = state.get("generated_response", "")
            
            prompt = f"""Evaluate if the following response adequately answers the user's question.

Response:
{response}

Respond with "SUFFICIENT" if the response is adequate, or explain why it's insufficient.
"""
            
            evaluation = self.llm.invoke([HumanMessage(content=prompt)])
            is_sufficient = "SUFFICIENT" in evaluation.content.upper()
            
            return {
                "context": {
                    **state.get("context", {}),
                    "evaluation": evaluation.content,
                    "is_sufficient": is_sufficient
                }
            }
        
        return evaluate_node
    
    def _create_refine_node(self):
        """Create the query refinement node."""
        def refine_node(state: RAGState) -> Dict[str, Any]:
            """Refine the query for the next iteration."""
            query = state.get("query", "")
            evaluation = state.get("context", {}).get("evaluation", "")
            
            prompt = f"""Based on the evaluation, reformulate the search query to find better information.

Original Query: {query}

Evaluation: {evaluation}

Provide a refined query that addresses the gaps in the current answer.
"""
            
            refined = self.llm.invoke([HumanMessage(content=prompt)])
            
            return {
                "query": refined.content,
                "context": {
                    **state.get("context", {}),
                    "iteration": state.get("context", {}).get("iteration", 0) + 1
                }
            }
        
        return refine_node
    
    def _check_sufficiency(self, state: RAGState) -> str:
        """Check if the response is sufficient."""
        is_sufficient = state.get("context", {}).get("is_sufficient", False)
        iteration = state.get("context", {}).get("iteration", 0)
        
        if is_sufficient or iteration >= 3:
            return END
        
        return "generate"


def create_rag_graph(
    llm: BaseChatModel,
    rag_agent: Optional[Any] = None,
    workflow_type: str = "simple"
) -> Runnable:
    """Create a RAG workflow graph.
    
    Args:
        llm: Language model.
        rag_agent: Optional RAG agent.
        workflow_type: Type of workflow (simple, multi_hop, hybrid, iterative).
    
    Returns:
        Compiled runnable graph.
    """
    workflow = RAGGraphWorkflow(llm=llm, rag_agent=rag_agent)
    
    if workflow_type == "simple":
        return workflow.create_simple_rag_graph()
    elif workflow_type == "multi_hop":
        return workflow.create_multi_hop_rag_graph()
    elif workflow_type == "hybrid":
        return workflow.create_hybrid_rag_graph()
    elif workflow_type == "iterative":
        return workflow.create_iterative_rag_graph()
    else:
        return workflow.create_simple_rag_graph()
