import pytest
from unittest.mock import patch
from src.core.config.settings import Settings, get_settings


class TestSettings:
    def test_default_values(self):
        with patch.dict("os.environ", {}, clear=True):
            settings = Settings()
            assert settings.YOLO_MODEL == "yolo11n.pt"
            assert settings.BLIP_MODEL == "Salesforce/blip-image-captioning-large"
            assert settings.OCR_LANG == "ch"
            assert settings.MAX_IMAGE_SIZE == 10 * 1024 * 1024
            assert settings.DEVICE == "cuda"
            assert settings.MODEL_CACHE_DIR == "./models"
            assert settings.MAX_CONCURRENT_REQUESTS == 4

    def test_env_override(self):
        with patch.dict("os.environ", {
            "YOLO_MODEL": "yolo11s.pt",
            "BLIP_MODEL": "test-model",
            "MAX_IMAGE_SIZE": "5242880",
        }):
            settings = Settings()
            assert settings.YOLO_MODEL == "yolo11s.pt"
            assert settings.BLIP_MODEL == "test-model"
            assert settings.MAX_IMAGE_SIZE == 5242880


class TestGetSettings:
    def test_get_settings_returns_settings(self):
        with patch.dict("os.environ", {}, clear=True):
            settings = get_settings()
            assert isinstance(settings, Settings)

    def test_get_settings_cached(self):
        with patch.dict("os.environ", {}, clear=True):
            settings1 = get_settings()
            settings2 = get_settings()
            assert settings1 is settings2
