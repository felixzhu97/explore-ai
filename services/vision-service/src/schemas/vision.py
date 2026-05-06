from pydantic import BaseModel, Field
from typing import Optional
from enum import Enum


class TaskType(str, Enum):
    DETECT_OBJECTS = "detect_objects"
    CAPTION_IMAGE = "caption_image"
    EXTRACT_TEXT = "extract_text"
    ANALYZE_IMAGE = "analyze_image"


class DetectionResult(BaseModel):
    class_name: str
    confidence: float = Field(ge=0.0, le=1.0)
    bbox: tuple[int, int, int, int]


class DetectionResponse(BaseModel):
    task: str = "detect_objects"
    model: str
    detections: list[DetectionResult]
    image_width: int
    image_height: int
    processing_time_ms: float


class CaptionResponse(BaseModel):
    task: str = "caption_image"
    model: str
    caption: str
    processing_time_ms: float


class OCRResult(BaseModel):
    text: str
    confidence: float
    bbox: Optional[list[list[float]]] = None


class OCRResponse(BaseModel):
    task: str = "extract_text"
    model: str
    results: list[OCRResult]
    full_text: str
    processing_time_ms: float
