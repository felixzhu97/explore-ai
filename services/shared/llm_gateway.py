"""Unified LLM Gateway for AI Services.

Provides a single interface for interacting with multiple LLM providers:
- OpenAI (GPT-4, GPT-4o, GPT-3.5)
- Anthropic (Claude)
- Ollama (Local models)

Usage:
    from services.shared.llm_gateway import LLMGateway, get_llm

    llm = LLMGateway.get_llm(provider="openai", model="gpt-4o")
"""

from typing import Optional, List, Dict, Any, Iterator, Union
from enum import Enum

from langchain_core.language_models import BaseChatModel
from langchain_core.messages import BaseMessage, HumanMessage, AIMessage, SystemMessage, ToolMessage
from langchain_openai import ChatOpenAI
from langchain_anthropic import ChatAnthropic
from langchain_ollama import ChatOllama
from loguru import logger

from .config import get_settings


class LLMProvider(Enum):
    """Supported LLM providers."""
    OPENAI = "openai"
    ANTHROPIC = "anthropic"
    OLLAMA = "ollama"


class LLMGateway:
    """Unified gateway for multiple LLM providers with caching.

    Implements singleton pattern with temperature-aware caching to ensure
    the correct LLM instance is returned based on provider, model, and temperature.
    """

    _instance: Optional[BaseChatModel] = None
    _provider: Optional[str] = None
    _model: Optional[str] = None
    _temperature: Optional[float] = None

    @classmethod
    def get_llm(
        cls,
        provider: Optional[str] = None,
        model: Optional[str] = None,
        temperature: float = 0.7,
        max_tokens: Optional[int] = None,
    ) -> BaseChatModel:
        """Get or create an LLM instance for the specified provider.

        Args:
            provider: LLM provider (openai, anthropic, ollama)
            model: Model name (defaults to settings)
            temperature: Sampling temperature (0-2)
            max_tokens: Maximum tokens to generate

        Returns:
            Configured LLM instance
        """
        settings = get_settings()
        provider = provider or settings.LLM_PROVIDER

        if provider == "openai":
            model = model or settings.LLM_MODEL
        elif provider == "anthropic":
            model = model or settings.LLM_MODEL
        elif provider == "ollama":
            model = model or settings.OLLAMA_MODEL
        else:
            model = model or settings.LLM_MODEL

        # Use cache only if all parameters match
        if (cls._instance is not None
                and cls._provider == provider
                and cls._model == model
                and cls._temperature == temperature):
            return cls._instance

        logger.info(f"Initializing LLM: provider={provider}, model={model}, temperature={temperature}")

        try:
            if provider == "openai":
                cls._instance = ChatOpenAI(
                    model=model,
                    api_key=settings.OPENAI_API_KEY or None,
                    base_url=settings.OPENAI_BASE_URL or None,
                    temperature=temperature,
                    max_tokens=max_tokens or settings.LLM_MAX_TOKENS,
                    timeout=settings.OPENAI_TIMEOUT,
                )
            elif provider == "anthropic":
                cls._instance = ChatAnthropic(
                    model=model,
                    anthropic_api_key=settings.ANTHROPIC_API_KEY or None,
                    temperature=temperature,
                    max_tokens=max_tokens or settings.LLM_MAX_TOKENS,
                    timeout=settings.ANTHROPIC_TIMEOUT,
                )
            elif provider == "ollama":
                cls._instance = ChatOllama(
                    base_url=settings.OLLAMA_BASE_URL,
                    model=model,
                    temperature=temperature,
                    timeout=settings.OLLAMA_TIMEOUT,
                )
            else:
                raise ValueError(f"Unknown LLM provider: {provider}")

            cls._provider = provider
            cls._model = model
            cls._temperature = temperature
            return cls._instance

        except Exception as e:
            logger.error(f"Failed to initialize {provider} LLM: {e}")
            raise

    @classmethod
    def reset(cls):
        """Reset the cached LLM instance."""
        cls._instance = None
        cls._provider = None
        cls._model = None
        cls._temperature = None


def get_llm(
    provider: Optional[str] = None,
    model: Optional[str] = None,
    temperature: float = 0.7,
    max_tokens: Optional[int] = None,
) -> BaseChatModel:
    """Factory function for getting LLM instance.

    Usage:
        llm = get_llm(provider="ollama", model="qwen2.5", temperature=0.7)
    """
    return LLMGateway.get_llm(provider, model, temperature, max_tokens)


class TextService:
    """High-level service for text-to-text generation.

    Provides a simple interface for:
    - Basic text completion
    - Chat-style conversations
    - Streaming responses
    - System prompt support
    """

    def __init__(
        self,
        provider: Optional[str] = None,
        model: Optional[str] = None,
        temperature: float = 0.7,
        max_tokens: int = 4096,
        system_prompt: Optional[str] = None,
    ):
        """Initialize the text service.

        Args:
            provider: LLM provider (openai, anthropic, ollama)
            model: Model name
            temperature: Sampling temperature (0-2)
            max_tokens: Maximum tokens to generate
            system_prompt: Optional system prompt for all requests
        """
        self.provider = provider
        self.model = model
        self.temperature = temperature
        self.max_tokens = max_tokens
        self.system_prompt = system_prompt
        self._llm: Optional[BaseChatModel] = None

    @property
    def llm(self) -> BaseChatModel:
        """Get or create the LLM instance."""
        if self._llm is None:
            self._llm = LLMGateway.get_llm(
                provider=self.provider,
                model=self.model,
                temperature=self.temperature,
                max_tokens=self.max_tokens,
            )
        return self._llm

    def complete(
        self,
        prompt: str,
        system_prompt: Optional[str] = None,
        **kwargs,
    ) -> str:
        """Generate a text completion.

        Args:
            prompt: The input prompt
            system_prompt: Optional system prompt (overrides instance default)
            **kwargs: Additional arguments passed to the LLM

        Returns:
            Generated text completion
        """
        messages = []

        effective_system = system_prompt or self.system_prompt
        if effective_system:
            messages.append(SystemMessage(content=effective_system))

        messages.append(HumanMessage(content=prompt))

        response = self.llm.invoke(messages, **kwargs)
        return self._extract_content(response)

    def chat(
        self,
        messages: List[Union[str, Dict[str, str]]],
        system_prompt: Optional[str] = None,
        **kwargs,
    ) -> str:
        """Generate a chat completion.

        Args:
            messages: List of conversation messages
            system_prompt: Optional system prompt
            **kwargs: Additional arguments passed to the LLM

        Returns:
            Generated response text
        """
        lc_messages = self._prepare_messages(messages, system_prompt)
        response = self.llm.invoke(lc_messages, **kwargs)
        return self._extract_content(response)

    def stream(
        self,
        prompt: str,
        system_prompt: Optional[str] = None,
        **kwargs,
    ) -> Iterator[str]:
        """Generate a streaming text completion.

        Args:
            prompt: The input prompt
            system_prompt: Optional system prompt
            **kwargs: Additional arguments passed to the LLM

        Yields:
            Text chunks as they are generated
        """
        messages = []

        effective_system = system_prompt or self.system_prompt
        if effective_system:
            messages.append(SystemMessage(content=effective_system))

        messages.append(HumanMessage(content=prompt))

        for chunk in self.llm.stream(messages, **kwargs):
            content = self._extract_content(chunk)
            if content:
                yield content

    def chat_stream(
        self,
        messages: List[Union[str, Dict[str, str]]],
        system_prompt: Optional[str] = None,
        **kwargs,
    ) -> Iterator[str]:
        """Generate a streaming chat completion.

        Args:
            messages: List of conversation messages
            system_prompt: Optional system prompt
            **kwargs: Additional arguments passed to the LLM

        Yields:
            Text chunks as they are generated
        """
        lc_messages = self._prepare_messages(messages, system_prompt)

        for chunk in self.llm.stream(lc_messages, **kwargs):
            content = self._extract_content(chunk)
            if content:
                yield content

    def _prepare_messages(
        self,
        messages: List[Union[str, Dict[str, str]]],
        system_prompt: Optional[str] = None,
    ) -> List[BaseMessage]:
        """Prepare messages for LLM invocation.

        Args:
            messages: Raw message list
            system_prompt: Optional system prompt

        Returns:
            Formatted LangChain messages
        """
        lc_messages = []

        effective_system = system_prompt or self.system_prompt
        if effective_system:
            lc_messages.append(SystemMessage(content=effective_system))

        for msg in messages:
            if isinstance(msg, str):
                lc_messages.append(HumanMessage(content=msg))
            elif isinstance(msg, dict):
                role = msg.get("role", "user")
                content = msg.get("content", "")
                if role == "system":
                    lc_messages.append(SystemMessage(content=content))
                elif role == "user":
                    lc_messages.append(HumanMessage(content=content))
                elif role == "assistant":
                    lc_messages.append(AIMessage(content=content))
            elif hasattr(msg, "role"):
                lc_messages.append(msg)

        return lc_messages

    def _extract_content(self, response: Any) -> str:
        """Extract text content from LLM response.

        Handles various response formats from different providers.
        """
        if isinstance(response, str):
            return response

        if isinstance(response, dict):
            return response.get("content", str(response))

        if hasattr(response, "content"):
            return response.content

        if hasattr(response, "generations"):
            generations = response.generations
            if generations and len(generations) > 0:
                first_gen = generations[0]
                if isinstance(first_gen, list) and len(first_gen) > 0:
                    return first_gen[0].text if hasattr(first_gen[0], "text") else str(first_gen[0])
                elif hasattr(first_gen, "text"):
                    return first_gen.text

        return str(response)


def get_text_service(
    provider: Optional[str] = None,
    model: Optional[str] = None,
    temperature: float = 0.7,
    system_prompt: Optional[str] = None,
) -> TextService:
    """Create a TextService instance.

    Args:
        provider: LLM provider
        model: Model name
        temperature: Sampling temperature
        system_prompt: Default system prompt

    Returns:
        Configured TextService instance
    """
    return TextService(
        provider=provider,
        model=model,
        temperature=temperature,
        system_prompt=system_prompt,
    )
