from typing import Optional
from langchain_core.prompts import PromptTemplate
from langchain_core.language_models import BaseChatModel
from loguru import logger
from ..core.embedding import EmbeddingModel, get_embedding_model
from ..core.vector_store import VectorStore, get_vector_store
from ..core.llm_gateway import get_llm
from ..schemas import ChatRequest, ChatResponse, SourceDocument
from ..config import get_settings
from ..persistence.cache_manager import get_cache_manager, CacheManager


SYSTEM_PROMPT = """You are a professional AI assistant. Please answer the user's question based on the following context.

Context:
{context}

User question: {question}

Please provide an accurate and detailed answer based on the context. If the context doesn't contain relevant information, please clearly inform the user.
"""

FALLBACK_PROMPT = """You are a professional AI assistant. Please answer the user's question.

User question: {question}

Please provide an accurate and detailed answer.
"""


class RAGChain:
    """RAG chain for question answering with context retrieval."""

    def __init__(
        self,
        vector_store: Optional[VectorStore] = None,
        embedding_model: Optional[EmbeddingModel] = None,
        llm: Optional[BaseChatModel] = None,
        top_k: int = 5,
        cache_manager: Optional[CacheManager] = None,
    ):
        self.vector_store = vector_store or get_vector_store()
        self.embedding_model = embedding_model or get_embedding_model()
        self.llm = llm
        self.top_k = top_k
        self.cache = cache_manager or get_cache_manager()

        self.prompt_template = PromptTemplate(
            template=SYSTEM_PROMPT,
            input_variables=["context", "question"],
        )

        self.fallback_template = PromptTemplate(
            template=FALLBACK_PROMPT,
            input_variables=["question"],
        )

    def _get_llm(self, temperature: float = 0.7) -> BaseChatModel:
        """Get LLM instance with specified temperature."""
        if self.llm is None:
            return get_llm(temperature=temperature)
        return self.llm

    async def query(self, request: ChatRequest) -> ChatResponse:
        """Process a query and return a response with sources."""
        import time
        start = time.perf_counter()

        # Check retrieval result cache
        cached_results = self.cache.get_retrieval_results(
            query=request.query,
            doc_ids=request.doc_ids,
        )

        if cached_results is not None:
            logger.info(f"Retrieval cache hit for query: {request.query[:50]}...")
            results = cached_results
        else:
            # Generate query embedding
            cached_embedding = self.cache.get_embedding(request.query)
            if cached_embedding is not None:
                query_vector = cached_embedding
                logger.info(f"Embedding cache hit for query: {request.query[:50]}...")
            else:
                query_vector = self.embedding_model.embed_query(request.query)
                self.cache.set_embedding(request.query, query_vector)

            # Execute vector retrieval
            results = self.vector_store.search(
                query_vector=query_vector,
                top_k=request.top_k,
                doc_ids=request.doc_ids,
            )
            # Cache retrieval results
            self.cache.set_retrieval_results(
                query=request.query,
                results=results,
                doc_ids=request.doc_ids,
            )

        sources = [
            SourceDocument(
                text=r["text"],
                score=r["distance"],
                metadata=r.get("metadata", {}),
            )
            for r in results
        ]

        # Check LLM response cache
        cached_response = self.cache.get_llm_response(
            query=request.query,
            doc_ids=request.doc_ids,
        )
        if cached_response is not None:
            logger.info(f"LLM cache hit for query: {request.query[:50]}...")
            return ChatResponse(
                answer=cached_response["answer"],
                sources=sources,
                session_id=request.session_id or "default",
                model=cached_response.get("model", "cached"),
                processing_time_ms=(time.perf_counter() - start) * 1000,
            )

        if results:
            context = "\n\n".join(
                f"[Source {i+1}] {r['text']}" for i, r in enumerate(results)
            )
            prompt = self.prompt_template.format(
                context=context,
                question=request.query,
            )
        else:
            prompt = self.fallback_template.format(question=request.query)

        llm = self._get_llm(temperature=request.temperature)

        if hasattr(llm, "ainvoke"):
            response = await llm.ainvoke(prompt)
            answer = response.content if hasattr(response, "content") else str(response)
        elif hasattr(llm, "apredict"):
            answer = await llm.apredict(prompt)
        else:
            answer = llm.invoke(prompt)
            if hasattr(answer, "content"):
                answer = answer.content

        processing_time = (time.perf_counter() - start) * 1000

        settings = get_settings()
        current_model = settings.OLLAMA_MODEL if settings.LLM_PROVIDER == "ollama" else settings.LLM_MODEL
        session_id = request.session_id or "default"

        # Cache LLM response
        self.cache.set_llm_response(
            query=request.query,
            response={"answer": answer, "model": current_model},
            doc_ids=request.doc_ids,
        )

        return ChatResponse(
            answer=answer,
            sources=sources,
            session_id=session_id,
            model=current_model,
            processing_time_ms=processing_time,
        )

    async def stream_query(self, request: ChatRequest):
        """Stream query response token by token."""
        import time
        start = time.perf_counter()

        # Check retrieval result cache
        cached_results = self.cache.get_retrieval_results(
            query=request.query,
            doc_ids=request.doc_ids,
        )

        if cached_results is not None:
            logger.info(f"Retrieval cache hit for stream query: {request.query[:50]}...")
            results = cached_results
        else:
            # Generate query embedding
            cached_embedding = self.cache.get_embedding(request.query)
            if cached_embedding is not None:
                query_vector = cached_embedding
            else:
                query_vector = self.embedding_model.embed_query(request.query)
                self.cache.set_embedding(request.query, query_vector)

            # Execute vector retrieval
            results = self.vector_store.search(
                query_vector=query_vector,
                top_k=request.top_k,
                doc_ids=request.doc_ids,
            )
            # Cache retrieval results
            self.cache.set_retrieval_results(
                query=request.query,
                results=results,
                doc_ids=request.doc_ids,
            )

        sources = [
            SourceDocument(
                text=r["text"],
                score=r["distance"],
                metadata=r.get("metadata", {}),
            )
            for r in results
        ]

        if results:
            context = "\n\n".join(
                f"[Source {i+1}] {r['text']}" for i, r in enumerate(results)
            )
            prompt = self.prompt_template.format(
                context=context,
                question=request.query,
            )
        else:
            prompt = self.fallback_template.format(question=request.query)

        llm = self._get_llm(temperature=request.temperature)

        if hasattr(llm, "astream"):
            async for chunk in llm.astream(prompt):
                yield chunk.content if hasattr(chunk, "content") else str(chunk)
        elif hasattr(llm, "stream"):
            for chunk in llm.stream(prompt):
                yield chunk.content if hasattr(chunk, "content") else str(chunk)
        else:
            response = llm.invoke(prompt)
            yield response.content if hasattr(response, "content") else str(response)
