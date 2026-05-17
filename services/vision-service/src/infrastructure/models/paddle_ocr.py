"""PaddleOCR text extractor."""
import asyncio
import time
import numpy as np
from PIL import Image
from paddleocr import PaddleOCR
from loguru import logger


class PaddleOCRProcessor:
    """OCR processor using PaddleOCR."""

    def __init__(self, lang: str = "en"):
        self.lang = lang
        self._ocr = None

    @property
    def ocr(self) -> PaddleOCR:
        if self._ocr is None:
            logger.info(f"Loading PaddleOCR model (lang={self.lang})")
            self._ocr = PaddleOCR(lang=self.lang, use_angle_cls=False)
            logger.info("PaddleOCR model loaded successfully")
        return self._ocr

    async def extract_text(
        self, image: Image.Image, engine: str = "paddleocr"
    ) -> dict:
        """Extract text from image using PaddleOCR."""
        start = time.time()

        # Run OCR in thread pool to avoid blocking
        loop = asyncio.get_event_loop()
        result = await loop.run_in_executor(None, self._run_ocr, image)

        results = []
        full_text_parts = []

        if result and len(result) > 0:
            for line in result[0]:
                if len(line) >= 2:
                    bbox = line[0] if isinstance(line[0], list) else []
                    text_info = line[1]
                    if isinstance(text_info, (list, tuple)) and len(text_info) >= 2:
                        text = text_info[0]
                        confidence = text_info[1]
                    else:
                        text = str(text_info)
                        confidence = 0.9

                    results.append({
                        "text": text,
                        "confidence": round(float(confidence), 4),
                        "bbox": bbox if bbox else []
                    })
                    full_text_parts.append(text)

        return {
            "task": "extract_text",
            "model": "PaddleOCR",
            "results": results,
            "full_text": " ".join(full_text_parts),
            "processing_time_ms": (time.time() - start) * 1000,
        }

    def _run_ocr(self, image: Image.Image) -> list:
        """Synchronous OCR run."""
        img = image.convert("RGB")

        # Scale up small images for better OCR accuracy
        width, height = img.size
        if height < 500:
            scale = 500 / height
            new_width = int(width * scale)
            img = img.resize((new_width, 500), Image.LANCZOS)

        img_array = np.array(img)
        return self.ocr.ocr(img_array)
