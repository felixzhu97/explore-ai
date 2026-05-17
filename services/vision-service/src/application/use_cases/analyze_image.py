"""Analyze image use case."""

from PIL import Image
from typing import Optional

from ...domain.ports import IObjectDetector, IImageCaptioner, IOCRProcessor
from ..dtos.vision_dtos import (
    TaskType,
    AnalyzeImageRequestDTO,
    AnalyzeImageResponseDTO,
)


class AnalyzeImageInput:
    """Input for analyze image use case."""

    def __init__(self, image: Image.Image, task: TaskType = TaskType.CAPTION_IMAGE):
        self.image = image
        self.task = task


class AnalyzeImageUseCase:
    """Application use case for combined image analysis.

    Orchestrates object detection, image captioning, and OCR
    based on the requested task type.
    """

    def __init__(
        self,
        detector: IObjectDetector,
        captioner: IImageCaptioner,
        ocr: IOCRProcessor,
    ):
        self._detector = detector
        self._captioner = captioner
        self._ocr = ocr

    async def execute(self, input_data: AnalyzeImageInput) -> AnalyzeImageResponseDTO:
        """Execute the image analysis use case based on task type."""
        if input_data.task == TaskType.DETECT_OBJECTS:
            detections = await self._detector.detect(input_data.image)
            return AnalyzeImageResponseDTO(detections=detections)

        elif input_data.task == TaskType.CAPTION_IMAGE:
            caption = await self._captioner.caption(input_data.image)
            return AnalyzeImageResponseDTO(caption=caption)

        elif input_data.task == TaskType.EXTRACT_TEXT:
            ocr_result = await self._ocr.extract_text(input_data.image)
            return AnalyzeImageResponseDTO(ocr=ocr_result)

        else:
            caption = await self._captioner.caption(input_data.image)
            detections = await self._detector.detect(input_data.image)
            ocr_result = await self._ocr.extract_text(input_data.image)
            return AnalyzeImageResponseDTO(
                caption=caption,
                detections=detections,
                ocr=ocr_result,
            )
