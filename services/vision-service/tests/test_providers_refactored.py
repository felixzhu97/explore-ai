"""Tests for refactored video providers."""

import pytest
from unittest.mock import AsyncMock, MagicMock, patch
import httpx

from src.infrastructure.providers.base import BaseVideoProvider
from src.infrastructure.providers.mock import MockVideoProvider
from src.infrastructure.providers.replicate import ReplicateVideoProvider
from src.infrastructure.providers.kling import KlingVideoProvider
from src.infrastructure.providers.pika import PikaVideoProvider
from src.infrastructure.providers.runway import RunwayVideoProvider
from src.infrastructure.providers.sora import SoraVideoProvider
from src.infrastructure.providers import get_provider


class TestReplicateVideoProvider:
    """Tests for ReplicateVideoProvider."""

    def test_constructor_accepts_api_token(self):
        """Provider should accept api_token in constructor."""
        provider = ReplicateVideoProvider(api_token="test_token_123")
        assert provider.api_token == "test_token_123"
        assert provider.base_url == "https://api.replicate.com/v1"
        assert "Bearer test_token_123" in provider._headers["Authorization"]

    def test_constructor_default_empty_token(self):
        """Provider should work with empty token for testing."""
        provider = ReplicateVideoProvider()
        assert provider.api_token == ""

    def test_provider_name(self):
        """Provider should report correct name."""
        provider = ReplicateVideoProvider()
        assert provider.provider_name == "replicate"

    @pytest.mark.asyncio
    async def test_generate_video_without_token_raises_error(self):
        """Should raise ValueError when API token is not set."""
        provider = ReplicateVideoProvider()
        with pytest.raises(ValueError, match="REPLICATE_API_TOKEN"):
            await provider.generate_video("test prompt")

    @pytest.mark.asyncio
    async def test_generate_video_with_token(self):
        """Should successfully call API with valid token."""
        provider = ReplicateVideoProvider(api_token="valid_token")

        mock_response = {"id": "test_task_123", "status": "starting", "urls": {}}

        with patch("httpx.AsyncClient") as mock_client:
            mock_instance = AsyncMock()
            mock_instance.post = AsyncMock(
                return_value=MagicMock(
                    raise_for_status=MagicMock(),
                    json=MagicMock(return_value=mock_response)
                )
            )
            mock_instance.__aenter__ = AsyncMock(return_value=mock_instance)
            mock_instance.__aexit__ = AsyncMock(return_value=None)
            mock_client.return_value = mock_instance

            result = await provider.generate_video("test prompt", duration=5)

            assert result["task_id"] == "test_task_123"
            assert result["status"] == "pending"


class TestKlingVideoProvider:
    """Tests for KlingVideoProvider."""

    def test_constructor_accepts_credentials(self):
        """Provider should accept api_key and api_secret."""
        provider = KlingVideoProvider(api_key="key_123", api_secret="secret_456")
        assert provider.api_token == "key_123"
        assert provider.api_secret == "secret_456"
        assert provider.base_url == "https://api.kling.ai/v1/videos"

    def test_auth_token_generation(self):
        """Should generate valid auth token."""
        provider = KlingVideoProvider(api_key="key_123", api_secret="secret_456")
        token = provider._generate_auth_token()
        assert token is not None
        assert len(token) > 0

    def test_constructor_default_credentials(self):
        """Provider should work with empty credentials for testing."""
        provider = KlingVideoProvider()
        assert provider.api_token == ""
        assert provider.api_secret == ""

    def test_provider_name(self):
        """Provider should report correct name."""
        provider = KlingVideoProvider()
        assert provider.provider_name == "kling"


class TestPikaVideoProvider:
    """Tests for PikaVideoProvider."""

    def test_constructor_accepts_api_key(self):
        """Provider should accept api_key in constructor."""
        provider = PikaVideoProvider(api_key="pika_key_123")
        assert provider.api_token == "pika_key_123"
        assert provider.base_url == "https://api.pika.art/v1"

    def test_constructor_default_empty_key(self):
        """Provider should work with empty key for testing."""
        provider = PikaVideoProvider()
        assert provider.api_token == ""

    def test_provider_name(self):
        """Provider should report correct name."""
        provider = PikaVideoProvider()
        assert provider.provider_name == "pika"

    @pytest.mark.asyncio
    async def test_generate_video_without_key_raises_error(self):
        """Should raise ValueError when API key is not set."""
        provider = PikaVideoProvider()
        with pytest.raises(ValueError, match="PIKA_API_KEY"):
            await provider.generate_video("test prompt")


class TestRunwayVideoProvider:
    """Tests for RunwayVideoProvider."""

    def test_constructor_accepts_api_key(self):
        """Provider should accept api_key in constructor."""
        provider = RunwayVideoProvider(api_key="runway_key_123")
        assert provider.api_token == "runway_key_123"
        assert provider.base_url == "https://api.runwayml.com/v1"

    def test_constructor_default_empty_key(self):
        """Provider should work with empty key for testing."""
        provider = RunwayVideoProvider()
        assert provider.api_token == ""

    def test_provider_name(self):
        """Provider should report correct name."""
        provider = RunwayVideoProvider()
        assert provider.provider_name == "runway"

    @pytest.mark.asyncio
    async def test_generate_video_without_key_raises_error(self):
        """Should raise ValueError when API key is not set."""
        provider = RunwayVideoProvider()
        with pytest.raises(ValueError, match="RUNWAY_API_KEY"):
            await provider.generate_video("test prompt")


class TestSoraVideoProvider:
    """Tests for SoraVideoProvider."""

    def test_constructor_accepts_api_key(self):
        """Provider should accept api_key in constructor."""
        provider = SoraVideoProvider(api_key="sora_key_123")
        assert provider.api_token == "sora_key_123"
        assert provider.base_url == "https://api.openai.com/v1"

    def test_constructor_default_empty_key(self):
        """Provider should work with empty key for testing."""
        provider = SoraVideoProvider()
        assert provider.api_token == ""

    def test_provider_name(self):
        """Provider should report correct name."""
        provider = SoraVideoProvider()
        assert provider.provider_name == "sora"

    @pytest.mark.asyncio
    async def test_generate_video_without_key_raises_error(self):
        """Should raise ValueError when API key is not set."""
        provider = SoraVideoProvider()
        with pytest.raises(ValueError, match="OPENAI_API_KEY"):
            await provider.generate_video("test prompt")


class TestMockVideoProvider:
    """Tests for MockVideoProvider."""

    def test_constructor(self):
        """Provider should have a constructor."""
        provider = MockVideoProvider()
        assert provider is not None

    def test_provider_name(self):
        """Provider should report correct name."""
        provider = MockVideoProvider()
        assert provider.provider_name == "mock"

    @pytest.mark.asyncio
    async def test_generate_video_returns_task(self):
        """Should return valid task response."""
        provider = MockVideoProvider()
        result = await provider.generate_video("test prompt")

        assert "task_id" in result
        assert result["task_id"].startswith("mock_task_")
        assert result["status"] == "pending"

    @pytest.mark.asyncio
    async def test_get_task_status_pending(self):
        """Should return pending status for new task."""
        provider = MockVideoProvider()
        result = await provider.generate_video("test prompt")

        status = await provider.get_task_status(result["task_id"])
        assert status["task_id"] == result["task_id"]
        assert status["status"] in ("pending", "processing", "completed")


class TestProviderFactory:
    """Tests for get_provider factory function."""

    def test_get_provider_mock_default(self):
        """Mock provider should be returned by default."""
        provider = get_provider(provider_name="mock")
        assert isinstance(provider, MockVideoProvider)

    def test_get_provider_replicate_with_token(self):
        """Factory should create Replicate provider with token."""
        provider = get_provider(
            provider_name="replicate",
            api_key="test_token"
        )
        assert isinstance(provider, ReplicateVideoProvider)
        assert provider.api_token == "test_token"

    def test_get_provider_kling_with_credentials(self):
        """Factory should create Kling provider with credentials."""
        provider = get_provider(
            provider_name="kling",
            api_key="test_key",
            api_secret="test_secret"
        )
        assert isinstance(provider, KlingVideoProvider)
        assert provider.api_token == "test_key"
        assert provider.api_secret == "test_secret"

    def test_get_provider_runway_with_key(self):
        """Factory should create Runway provider with key."""
        provider = get_provider(
            provider_name="runway",
            api_key="test_key"
        )
        assert isinstance(provider, RunwayVideoProvider)
        assert provider.api_token == "test_key"

    def test_get_provider_pika_with_key(self):
        """Factory should create Pika provider with key."""
        provider = get_provider(
            provider_name="pika",
            api_key="test_key"
        )
        assert isinstance(provider, PikaVideoProvider)
        assert provider.api_token == "test_key"

    def test_get_provider_sora_with_key(self):
        """Factory should create Sora provider with key."""
        provider = get_provider(
            provider_name="sora",
            api_key="test_key"
        )
        assert isinstance(provider, SoraVideoProvider)
        assert provider.api_token == "test_key"
