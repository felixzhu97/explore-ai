import pytest
from src.schemas.vision import (
    TaskType,
    DetectionResult,
    DetectionResponse,
    CaptionResponse,
    OCRResult,
    OCRResponse,
)


class TestTaskType:
    def test_task_type_values(self):
        assert TaskType.DETECT_OBJECTS.value == "detect_objects"
        assert TaskType.CAPTION_IMAGE.value == "caption_image"
        assert TaskType.EXTRACT_TEXT.value == "extract_text"
        assert TaskType.ANALYZE_IMAGE.value == "analyze_image"

    def test_task_type_is_string_enum(self):
        assert isinstance(TaskType.DETECT_OBJECTS, str)


class TestDetectionResult:
    def test_detection_result_valid(self):
        result = DetectionResult(
            class_name="person",
            confidence=0.95,
            bbox=(10, 20, 100, 200),
        )
        assert result.class_name == "person"
        assert result.confidence == 0.95
        assert result.bbox == (10, 20, 100, 200)

    def test_detection_result_confidence_bounds(self):
        assert DetectionResult(
            class_name="car",
            confidence=0.0,
            bbox=(0, 0, 100, 100),
        )
        assert DetectionResult(
            class_name="car",
            confidence=1.0,
            bbox=(0, 0, 100, 100),
        )

    def test_detection_result_invalid_confidence(self):
        with pytest.raises(ValueError):
            DetectionResult(
                class_name="car",
                confidence=1.5,
                bbox=(0, 0, 100, 100),
            )
        with pytest.raises(ValueError):
            DetectionResult(
                class_name="car",
                confidence=-0.1,
                bbox=(0, 0, 100, 100),
            )


class TestDetectionResponse:
    def test_detection_response_valid(self):
        response = DetectionResponse(
            model="yolo11n.pt",
            detections=[
                DetectionResult(class_name="person", confidence=0.9, bbox=(10, 20, 100, 200))
            ],
            image_width=640,
            image_height=480,
            processing_time_ms=50.5,
        )
        assert response.task == "detect_objects"
        assert response.model == "yolo11n.pt"
        assert len(response.detections) == 1

    def test_detection_response_empty_detections(self):
        response = DetectionResponse(
            model="yolo11n.pt",
            detections=[],
            image_width=640,
            image_height=480,
            processing_time_ms=10.0,
        )
        assert len(response.detections) == 0


class TestCaptionResponse:
    def test_caption_response_valid(self):
        response = CaptionResponse(
            model="Salesforce/blip-image-captioning-large",
            caption="A red square image",
            processing_time_ms=123.45,
        )
        assert response.task == "caption_image"
        assert response.caption == "A red square image"

    def test_caption_response_empty_caption(self):
        response = CaptionResponse(
            model="test-model",
            caption="",
            processing_time_ms=0.0,
        )
        assert response.caption == ""


class TestOCRResult:
    def test_ocr_result_with_bbox(self):
        result = OCRResult(
            text="Hello World",
            confidence=0.99,
            bbox=[(0, 0), (100, 0), (100, 50), (0, 50)],
        )
        assert result.text == "Hello World"
        assert result.bbox is not None
        assert len(result.bbox) == 4

    def test_ocr_result_without_bbox(self):
        result = OCRResult(
            text="Hello World",
            confidence=0.99,
            bbox=None,
        )
        assert result.bbox is None


class TestOCRResponse:
    def test_ocr_response_valid(self):
        response = OCRResponse(
            model="PaddleOCR",
            results=[
                OCRResult(text="Line 1", confidence=0.95, bbox=None),
                OCRResult(text="Line 2", confidence=0.90, bbox=None),
            ],
            full_text="Line 1\nLine 2",
            processing_time_ms=75.0,
        )
        assert response.task == "extract_text"
        assert len(response.results) == 2
        assert response.full_text == "Line 1\nLine 2"

    def test_ocr_response_full_text_join(self):
        response = OCRResponse(
            model="PaddleOCR",
            results=[
                OCRResult(text="First", confidence=1.0, bbox=None),
                OCRResult(text="Second", confidence=1.0, bbox=None),
                OCRResult(text="Third", confidence=1.0, bbox=None),
            ],
            full_text="First\nSecond\nThird",
            processing_time_ms=10.0,
        )
        assert response.full_text.count("\n") == 2
