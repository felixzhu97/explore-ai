"""Tests for dependency injection container."""

import pytest
from unittest.mock import MagicMock, patch, AsyncMock
from src.core.di import container as di_container
from src.core.di import (
    ModelContainer,
    get_video_provider,
    _reset_instances,
    get_yolo,
    get_blip,
    get_easyocr,
)


class TestModelContainer:
    """Tests for ModelContainer (backward compatibility)."""

    def setup_method(self):
        """Reset container state before each test."""
        _reset_instances()

    def teardown_method(self):
        """Reset container state after each test."""
        _reset_instances()

    def test_get_yolo_lazy_initialization(self):
        """Should initialize YOLO on first access."""
        with patch("src.models.yolo_detector.YOLODetector") as mock_class:
            mock_instance = MagicMock()
            mock_class.return_value = mock_instance

            result = ModelContainer.get_yolo()

            assert result == mock_instance
            mock_class.assert_called_once()

    def test_get_yolo_cached(self):
        """Should return cached instance on subsequent calls."""
        mock_instance = MagicMock()
        di_container._yolo_instance = mock_instance

        with patch("src.models.yolo_detector.YOLODetector") as mock_class:
            result = ModelContainer.get_yolo()

            assert result == mock_instance
            mock_class.assert_not_called()

    def test_get_blip_lazy_initialization(self):
        """Should initialize BLIP on first access."""
        with patch("src.models.blip_captioner.BLIPCaptioner") as mock_class:
            mock_instance = MagicMock()
            mock_class.return_value = mock_instance

            result = ModelContainer.get_blip()

            assert result == mock_instance
            mock_class.assert_called_once()

    def test_get_blip_cached(self):
        """Should return cached instance on subsequent calls."""
        mock_instance = MagicMock()
        di_container._blip_instance = mock_instance

        with patch("src.models.blip_captioner.BLIPCaptioner") as mock_class:
            result = ModelContainer.get_blip()

            assert result == mock_instance
            mock_class.assert_not_called()

    def test_get_easyocr_lazy_initialization(self):
        """Should initialize EasyOCR on first access."""
        with patch("src.models.easy_ocr.EasyOCRProcessor") as mock_class:
            mock_instance = MagicMock()
            mock_class.return_value = mock_instance

            result = ModelContainer.get_easyocr()

            assert result == mock_instance
            mock_class.assert_called_once()

    def test_get_easyocr_cached(self):
        """Should return cached instance on subsequent calls."""
        mock_instance = MagicMock()
        di_container._easyocr_instance = mock_instance

        with patch("src.models.easy_ocr.EasyOCRProcessor") as mock_class:
            result = ModelContainer.get_easyocr()

            assert result == mock_instance
            mock_class.assert_not_called()

    def test_reset_clears_all_instances(self):
        """Should clear all cached instances."""
        di_container._yolo_instance = MagicMock()
        di_container._blip_instance = MagicMock()
        di_container._easyocr_instance = MagicMock()
        di_container._generator_instance = MagicMock()

        ModelContainer.reset()

        assert di_container._yolo_instance is None
        assert di_container._blip_instance is None
        assert di_container._easyocr_instance is None
        assert di_container._generator_instance is None

    def test_reset_when_empty(self):
        """Should handle reset when all instances are None."""
        _reset_instances()
        ModelContainer.reset()
        assert di_container._yolo_instance is None


class TestDependencyFunctions:
    """Tests for direct dependency functions."""

    def setup_method(self):
        """Reset container state before each test."""
        _reset_instances()

    def teardown_method(self):
        """Reset container state after each test."""
        _reset_instances()

    def test_get_yolo_lazy_initialization(self):
        """Should initialize YOLO on first access."""
        with patch("src.models.yolo_detector.YOLODetector") as mock_class:
            mock_instance = MagicMock()
            mock_class.return_value = mock_instance

            result = get_yolo()

            assert result == mock_instance
            mock_class.assert_called_once()

    def test_get_yolo_cached(self):
        """Should return cached instance on subsequent calls."""
        mock_instance = MagicMock()
        di_container._yolo_instance = mock_instance

        with patch("src.models.yolo_detector.YOLODetector") as mock_class:
            result = get_yolo()

            assert result == mock_instance
            mock_class.assert_not_called()

    def test_get_blip_lazy_initialization(self):
        """Should initialize BLIP on first access."""
        with patch("src.models.blip_captioner.BLIPCaptioner") as mock_class:
            mock_instance = MagicMock()
            mock_class.return_value = mock_instance

            result = get_blip()

            assert result == mock_instance
            mock_class.assert_called_once()

    def test_get_blip_cached(self):
        """Should return cached instance on subsequent calls."""
        mock_instance = MagicMock()
        di_container._blip_instance = mock_instance

        with patch("src.models.blip_captioner.BLIPCaptioner") as mock_class:
            result = get_blip()

            assert result == mock_instance
            mock_class.assert_not_called()

    def test_get_easyocr_lazy_initialization(self):
        """Should initialize EasyOCR on first access."""
        with patch("src.models.easy_ocr.EasyOCRProcessor") as mock_class:
            mock_instance = MagicMock()
            mock_class.return_value = mock_instance

            result = get_easyocr()

            assert result == mock_instance
            mock_class.assert_called_once()

    def test_get_easyocr_cached(self):
        """Should return cached instance on subsequent calls."""
        mock_instance = MagicMock()
        di_container._easyocr_instance = mock_instance

        with patch("src.models.easy_ocr.EasyOCRProcessor") as mock_class:
            result = get_easyocr()

            assert result == mock_instance
            mock_class.assert_not_called()

    def test_reset_clears_all_instances(self):
        """Should clear all cached instances via _reset_instances."""
        di_container._yolo_instance = MagicMock()
        di_container._blip_instance = MagicMock()
        di_container._easyocr_instance = MagicMock()
        di_container._generator_instance = MagicMock()

        _reset_instances()

        assert di_container._yolo_instance is None
        assert di_container._blip_instance is None
        assert di_container._easyocr_instance is None
        assert di_container._generator_instance is None


class TestVideoProviderDependency:
    """Tests for video provider dependency."""

    @patch("src.providers.get_provider")
    @patch("src.core.video_config.get_settings")
    def test_get_video_provider_mock(self, mock_get_settings, mock_get_provider):
        """Should return mock provider when configured."""
        mock_settings = MagicMock()
        mock_settings.VIDEO_PROVIDER.value = "mock"
        mock_settings.REPLICATE_API_TOKEN = ""
        mock_settings.KLING_API_KEY = ""
        mock_settings.KLING_API_SECRET = ""
        mock_settings.RUNWAY_API_KEY = ""
        mock_settings.PIKA_API_KEY = ""
        mock_settings.SORA_API_KEY = ""
        mock_get_settings.return_value = mock_settings

        mock_provider = MagicMock()
        mock_get_provider.return_value = mock_provider

        result = get_video_provider()

        mock_get_provider.assert_called_once()
        assert result == mock_provider

    @patch("src.providers.get_provider")
    @patch("src.core.video_config.get_settings")
    def test_get_video_provider_replicate(self, mock_get_settings, mock_get_provider):
        """Should pass API token for replicate provider."""
        mock_settings = MagicMock()
        mock_settings.VIDEO_PROVIDER.value = "replicate"
        mock_settings.REPLICATE_API_TOKEN = "test_token_123"
        mock_settings.KLING_API_KEY = ""
        mock_settings.KLING_API_SECRET = ""
        mock_settings.RUNWAY_API_KEY = ""
        mock_settings.PIKA_API_KEY = ""
        mock_settings.SORA_API_KEY = ""
        mock_get_settings.return_value = mock_settings

        mock_provider = MagicMock()
        mock_get_provider.return_value = mock_provider

        result = get_video_provider()

        mock_get_provider.assert_called_once_with(
            provider_name="replicate",
            api_key="test_token_123",
            api_secret=""
        )
        assert result == mock_provider

    @patch("src.providers.get_provider")
    @patch("src.core.video_config.get_settings")
    def test_get_video_provider_kling(self, mock_get_settings, mock_get_provider):
        """Should pass API credentials for kling provider."""
        mock_settings = MagicMock()
        mock_settings.VIDEO_PROVIDER.value = "kling"
        mock_settings.REPLICATE_API_TOKEN = ""
        mock_settings.KLING_API_KEY = "kling_key"
        mock_settings.KLING_API_SECRET = "kling_secret"
        mock_settings.RUNWAY_API_KEY = ""
        mock_settings.PIKA_API_KEY = ""
        mock_settings.SORA_API_KEY = ""
        mock_get_settings.return_value = mock_settings

        mock_provider = MagicMock()
        mock_get_provider.return_value = mock_provider

        result = get_video_provider()

        mock_get_provider.assert_called_once_with(
            provider_name="kling",
            api_key="kling_key",
            api_secret="kling_secret"
        )
        assert result == mock_provider

    @patch("src.providers.get_provider")
    @patch("src.core.video_config.get_settings")
    def test_get_video_provider_runway(self, mock_get_settings, mock_get_provider):
        """Should pass API key for runway provider."""
        mock_settings = MagicMock()
        mock_settings.VIDEO_PROVIDER.value = "runway"
        mock_settings.REPLICATE_API_TOKEN = ""
        mock_settings.KLING_API_KEY = ""
        mock_settings.KLING_API_SECRET = ""
        mock_settings.RUNWAY_API_KEY = "runway_key"
        mock_settings.PIKA_API_KEY = ""
        mock_settings.SORA_API_KEY = ""
        mock_get_settings.return_value = mock_settings

        mock_provider = MagicMock()
        mock_get_provider.return_value = mock_provider

        result = get_video_provider()

        mock_get_provider.assert_called_once_with(
            provider_name="runway",
            api_key="runway_key",
            api_secret=""
        )
        assert result == mock_provider

    @patch("src.providers.get_provider")
    @patch("src.core.video_config.get_settings")
    def test_get_video_provider_pika(self, mock_get_settings, mock_get_provider):
        """Should pass API key for pika provider."""
        mock_settings = MagicMock()
        mock_settings.VIDEO_PROVIDER.value = "pika"
        mock_settings.REPLICATE_API_TOKEN = ""
        mock_settings.KLING_API_KEY = ""
        mock_settings.KLING_API_SECRET = ""
        mock_settings.RUNWAY_API_KEY = ""
        mock_settings.PIKA_API_KEY = "pika_key"
        mock_settings.SORA_API_KEY = ""
        mock_get_settings.return_value = mock_settings

        mock_provider = MagicMock()
        mock_get_provider.return_value = mock_provider

        result = get_video_provider()

        mock_get_provider.assert_called_once_with(
            provider_name="pika",
            api_key="pika_key",
            api_secret=""
        )
        assert result == mock_provider

    @patch("src.providers.get_provider")
    @patch("src.core.video_config.get_settings")
    def test_get_video_provider_sora(self, mock_get_settings, mock_get_provider):
        """Should pass API key for sora provider."""
        mock_settings = MagicMock()
        mock_settings.VIDEO_PROVIDER.value = "sora"
        mock_settings.REPLICATE_API_TOKEN = ""
        mock_settings.KLING_API_KEY = ""
        mock_settings.KLING_API_SECRET = ""
        mock_settings.RUNWAY_API_KEY = ""
        mock_settings.PIKA_API_KEY = ""
        mock_settings.SORA_API_KEY = "sora_key"
        mock_get_settings.return_value = mock_settings

        mock_provider = MagicMock()
        mock_get_provider.return_value = mock_provider

        result = get_video_provider()

        mock_get_provider.assert_called_once_with(
            provider_name="sora",
            api_key="sora_key",
            api_secret=""
        )
        assert result == mock_provider
