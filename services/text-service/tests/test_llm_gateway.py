"""Tests for LLM Gateway."""

import pytest
from unittest.mock import patch, MagicMock
from core.llm_gateway import TextToTextService, LLMGateway, ChatMessage
from core.config import Settings


class TestChatMessage:
    """Tests for ChatMessage helper class."""

    def test_create_message(self):
        """Test creating a chat message."""
        msg = ChatMessage(role="user", content="Hello")
        assert msg["role"] == "user"
        assert msg["content"] == "Hello"

    def test_message_with_extra_fields(self):
        """Test creating a message with extra fields."""
        msg = ChatMessage(role="assistant", content="Hi", name="assistant")
        assert msg["role"] == "assistant"
        assert msg["content"] == "Hi"
        assert msg["name"] == "assistant"


class TestTextToTextService:
    """Tests for TextToTextService class."""

    def test_init_default_values(self):
        """Test service initialization with defaults."""
        service = TextToTextService()
        assert service.provider is None
        assert service.model is None
        assert service.temperature == 0.7
        assert service.max_tokens == 4096
        assert service.system_prompt is None

    def test_init_custom_values(self):
        """Test service initialization with custom values."""
        service = TextToTextService(
            provider="openai",
            model="gpt-4",
            temperature=0.5,
            max_tokens=1000,
            system_prompt="You are helpful."
        )
        assert service.provider == "openai"
        assert service.model == "gpt-4"
        assert service.temperature == 0.5
        assert service.max_tokens == 1000
        assert service.system_prompt == "You are helpful."

    @patch('core.llm_gateway.LLMGateway.get_llm')
    def test_complete_calls_llm(self, mock_get_llm):
        """Test that complete() calls the LLM."""
        mock_llm = MagicMock()
        mock_response = MagicMock()
        mock_response.content = "Test response"
        mock_llm.invoke.return_value = mock_response
        mock_get_llm.return_value = mock_llm

        service = TextToTextService(provider="openai", model="gpt-4")
        result = service.complete("Hello")

        assert result == "Test response"
        mock_llm.invoke.assert_called_once()

    @patch('core.llm_gateway.LLMGateway.get_llm')
    def test_chat_with_string_messages(self, mock_get_llm):
        """Test chat with string messages."""
        mock_llm = MagicMock()
        mock_response = MagicMock()
        mock_response.content = "Test response"
        mock_llm.invoke.return_value = mock_response
        mock_get_llm.return_value = mock_llm

        service = TextToTextService(provider="openai", model="gpt-4")
        result = service.chat(["Hello", "How are you?"])

        assert result == "Test response"
        assert mock_llm.invoke.call_count == 1

    @patch('core.llm_gateway.LLMGateway.get_llm')
    def test_chat_with_dict_messages(self, mock_get_llm):
        """Test chat with dict messages."""
        mock_llm = MagicMock()
        mock_response = MagicMock()
        mock_response.content = "Test response"
        mock_llm.invoke.return_value = mock_response
        mock_get_llm.return_value = mock_llm

        service = TextToTextService(provider="openai", model="gpt-4")
        result = service.chat([
            {"role": "user", "content": "Hello"},
            {"role": "assistant", "content": "Hi there!"},
        ])

        assert result == "Test response"

    @patch('core.llm_gateway.LLMGateway.get_llm')
    def test_extract_content_from_string(self, mock_get_llm):
        """Test content extraction from string response."""
        service = TextToTextService()
        result = service._extract_content("Simple string")
        assert result == "Simple string"

    @patch('core.llm_gateway.LLMGateway.get_llm')
    def test_extract_content_from_dict(self, mock_get_llm):
        """Test content extraction from dict response."""
        service = TextToTextService()
        result = service._extract_content({"content": "Dict content"})
        assert result == "Dict content"

    @patch('core.llm_gateway.LLMGateway.get_llm')
    def test_extract_content_from_object(self, mock_get_llm):
        """Test content extraction from object with content attr."""
        service = TextToTextService()
        mock_obj = MagicMock()
        mock_obj.content = "Object content"
        result = service._extract_content(mock_obj)
        assert result == "Object content"


class TestLLMGateway:
    """Tests for LLMGateway class."""

    def test_reset_clears_instance(self):
        """Test that reset clears the cached instance."""
        LLMGateway._instance = MagicMock()
        LLMGateway._provider = "openai"
        LLMGateway._model = "gpt-4"
        LLMGateway._temperature = 0.7

        LLMGateway.reset()

        assert LLMGateway._instance is None
        assert LLMGateway._provider is None
        assert LLMGateway._model is None
        assert LLMGateway._temperature is None


class TestSettings:
    """Tests for Settings configuration."""

    def test_default_values(self):
        """Test settings have correct defaults."""
        settings = Settings()
        assert settings.HOST == "0.0.0.0"
        assert settings.PORT == 8004
        assert settings.LLM_PROVIDER == "openai"
        assert settings.LLM_MODEL == "gpt-4o-mini"
        assert settings.LLM_TEMPERATURE == 0.7
        assert settings.LLM_MAX_TOKENS == 4096

    def test_model_lists(self):
        """Test model lists are defined."""
        settings = Settings()
        assert len(settings.OPENAI_MODELS) > 0
        assert len(settings.ANTHROPIC_MODELS) > 0
        assert len(settings.OLLAMA_MODELS) > 0
