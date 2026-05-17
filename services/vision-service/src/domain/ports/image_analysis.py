"""Image analysis ports (domain layer interfaces).

This module defines the Protocol interfaces for image analysis operations
including object detection, image captioning, and OCR.
Infrastructure implementations must satisfy these protocols.

Architecture:
    UseCase -> IObjectDetector / IImageCaptioner / IOCRProcessor (impl in infrastructure)
    (app)           (domain port)
"""

from typing import Protocol, runtime_checkable
from PIL import Image


@runtime_checkable
class IObjectDetector(Protocol):
    """Protocol for object detection models.

    Infrastructure implementations (e.g., YOLODetector) must implement this protocol.
    """

    async def detect(
        self, image: Image.Image, conf_threshold: float = 0.25
    ) -> dict:
        """Detect objects in an image.

        Args:
            image: PIL Image to analyze.
            conf_threshold: Confidence threshold for detections.

        Returns:
            Dictionary containing detections with bounding boxes and classes.
        """
        ...


@runtime_checkable
class IImageCaptioner(Protocol):
    """Protocol for image captioning models.

    Infrastructure implementations (e.g., BLIPCaptioner) must implement this protocol.
    """

    async def caption(self, image: Image.Image) -> dict:
        """Generate a caption for an image.

        Args:
            image: PIL Image to analyze.

        Returns:
            Dictionary containing the generated caption.
        """
        ...


@runtime_checkable
class IOCRProcessor(Protocol):
    """Protocol for OCR models.

    Infrastructure implementations (e.g., EasyOCRProcessor) must implement this protocol.
    """

    async def extract_text(
        self, image: Image.Image, engine: str = "easyocr"
    ) -> dict:
        """Extract text from an image.

        Args:
            image: PIL Image to analyze.
            engine: OCR engine to use (for engines that support multiple).

        Returns:
            Dictionary containing extracted text and bounding boxes.
        """
        ...
