"""Tests for API endpoints with dependency injection support."""

import pytest
from unittest.mock import AsyncMock, MagicMock, patch
from fastapi.testclient import TestClient
from PIL import Image
import io

from src.application.dtos.vision_dtos import (
    TaskType,
    DetectionResult,
    DetectionResponseDTO as DetectionResponse,
    CaptionResponseDTO as CaptionResponse,
    OCRResult,
    OCRResponseDTO as OCRResponse,
)


@pytest.fixture
def mock_yolo():
    """Mock YOLO detector."""
    mock = MagicMock()
    mock.detect = AsyncMock(return_value=DetectionResponse(
        model="yolo11n.pt",
        detections=[
            DetectionResult(class_name="person", confidence=0.9, bbox=(10, 20, 100, 200))
        ],
        image_width=640,
        image_height=480,
        processing_time_ms=50.0,
    ))
    return mock


@pytest.fixture
def mock_blip():
    """Mock BLIP captioner."""
    mock = MagicMock()
    mock.caption = AsyncMock(return_value=CaptionResponse(
        model="test-model",
        caption="A test image caption",
        processing_time_ms=100.0,
    ))
    return mock


@pytest.fixture
def mock_ocr():
    """Mock OCR processor."""
    mock = MagicMock()
    mock.extract_text = AsyncMock(return_value=OCRResponse(
        model="PaddleOCR",
        results=[OCRResult(text="Sample text", confidence=0.95, bbox=None)],
        full_text="Sample text",
        processing_time_ms=30.0,
    ))
    return mock


@pytest.fixture
def client_with_mocks(mock_yolo, mock_blip, mock_ocr):
    """Create test client with mocked model dependencies."""
    from src.main import app
    from src.core.di import get_yolo, get_blip, get_easyocr

    # Use FastAPI's dependency override mechanism
    app.dependency_overrides[get_yolo] = lambda: mock_yolo
    app.dependency_overrides[get_blip] = lambda: mock_blip
    app.dependency_overrides[get_easyocr] = lambda: mock_ocr

    client = TestClient(app)
    yield client

    # Clean up overrides
    app.dependency_overrides.clear()


@pytest.fixture
def sample_image_bytes():
    """Create sample image bytes."""
    img = Image.new("RGB", (100, 100), color="red")
    buf = io.BytesIO()
    img.save(buf, format="JPEG")
    buf.seek(0)
    return buf.getvalue()


class TestLoadImage:
    """Tests for load_image utility function."""

    @pytest.mark.asyncio
    async def test_load_image_valid(self, sample_image_bytes):
        from src.api.vision import load_image
        from fastapi import UploadFile

        mock_file = MagicMock(spec=UploadFile)
        mock_file.read = AsyncMock(return_value=sample_image_bytes)

        with patch("src.api.vision.get_settings") as mock_settings:
            mock_settings.return_value.MAX_IMAGE_SIZE = 10 * 1024 * 1024
            image = await load_image(mock_file)
            assert isinstance(image, Image.Image)
            assert image.size == (100, 100)

    @pytest.mark.asyncio
    async def test_load_image_too_large(self):
        from src.api.vision import load_image
        from fastapi import UploadFile, HTTPException

        mock_file = MagicMock(spec=UploadFile)
        mock_file.read = AsyncMock(return_value=b"x" * (11 * 1024 * 1024))

        with patch("src.api.vision.get_settings") as mock_settings:
            mock_settings.return_value.MAX_IMAGE_SIZE = 10 * 1024 * 1024
            with pytest.raises(HTTPException) as exc_info:
                await load_image(mock_file)
            assert exc_info.value.status_code == 400
            assert "too large" in exc_info.value.detail

    @pytest.mark.asyncio
    async def test_load_image_invalid_format(self):
        from src.api.vision import load_image
        from fastapi import UploadFile, HTTPException

        mock_file = MagicMock(spec=UploadFile)
        mock_file.read = AsyncMock(return_value=b"not an image")

        with patch("src.api.vision.get_settings") as mock_settings:
            mock_settings.return_value.MAX_IMAGE_SIZE = 10 * 1024 * 1024
            with pytest.raises(HTTPException) as exc_info:
                await load_image(mock_file)
            assert exc_info.value.status_code == 400
            assert "Invalid image" in exc_info.value.detail


class TestDetectEndpoint:
    """Tests for detect endpoint."""

    def test_detect_success(self, client_with_mocks, sample_image_bytes):
        response = client_with_mocks.post(
            "/vision/detect",
            files={"file": ("test.jpg", sample_image_bytes, "image/jpeg")},
            data={"conf": 0.5},
        )
        assert response.status_code == 200
        data = response.json()
        assert data["task"] == "detect_objects"
        assert data["model"] == "yolo11n.pt"
        assert "detections" in data

    def test_detect_no_file(self, client_with_mocks):
        response = client_with_mocks.post("/vision/detect")
        assert response.status_code == 422


class TestCaptionEndpoint:
    """Tests for caption endpoint."""

    def test_caption_success(self, client_with_mocks, sample_image_bytes):
        response = client_with_mocks.post(
            "/vision/caption",
            files={"file": ("test.jpg", sample_image_bytes, "image/jpeg")},
        )
        assert response.status_code == 200
        data = response.json()
        assert data["task"] == "caption_image"
        assert "caption" in data

    def test_caption_no_file(self, client_with_mocks):
        response = client_with_mocks.post("/vision/caption")
        assert response.status_code == 422


class TestOCRAEndpoint:
    """Tests for OCR endpoint."""

    def test_ocr_success(self, client_with_mocks, sample_image_bytes):
        response = client_with_mocks.post(
            "/vision/ocr",
            files={"file": ("test.jpg", sample_image_bytes, "image/jpeg")},
        )
        assert response.status_code == 200
        data = response.json()
        assert data["task"] == "extract_text"
        assert "results" in data

    def test_ocr_no_file(self, client_with_mocks):
        response = client_with_mocks.post("/vision/ocr")
        assert response.status_code == 422


class TestAnalyzeEndpoint:
    """Tests for analyze endpoint."""

    def test_analyze_caption_task(self, client_with_mocks, sample_image_bytes):
        response = client_with_mocks.post(
            "/vision/analyze?task=caption_image",
            files={"file": ("test.jpg", sample_image_bytes, "image/jpeg")},
        )
        assert response.status_code == 200
        data = response.json()
        assert "caption" in data

    def test_analyze_detect_task(self, client_with_mocks, sample_image_bytes):
        response = client_with_mocks.post(
            "/vision/analyze?task=detect_objects",
            files={"file": ("test.jpg", sample_image_bytes, "image/jpeg")},
        )
        assert response.status_code == 200
        data = response.json()
        assert "detections" in data

    def test_analyze_ocr_task(self, client_with_mocks, sample_image_bytes):
        response = client_with_mocks.post(
            "/vision/analyze?task=extract_text",
            files={"file": ("test.jpg", sample_image_bytes, "image/jpeg")},
        )
        assert response.status_code == 200
        data = response.json()
        assert "results" in data

    def test_analyze_full_task(self, client_with_mocks, sample_image_bytes):
        response = client_with_mocks.post(
            "/vision/analyze?task=analyze_image",
            files={"file": ("test.jpg", sample_image_bytes, "image/jpeg")},
        )
        assert response.status_code == 200
        data = response.json()
        assert "caption" in data
        assert "detections" in data
        assert "ocr" in data


class TestHealthEndpoint:
    """Tests for health check endpoints."""

    def test_health_check(self):
        """Should return health status."""
        from src.main import app
        client = TestClient(app)
        response = client.get("/health")
        assert response.status_code == 200
        assert response.json()["status"] == "ok"

    def test_root_endpoint(self):
        """Should return service info."""
        from src.main import app
        client = TestClient(app)
        response = client.get("/")
        assert response.status_code == 200
        data = response.json()
        assert "name" in data
        assert "version" in data
