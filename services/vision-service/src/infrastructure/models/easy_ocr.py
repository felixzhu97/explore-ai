"""EasyOCR text extractor."""
import asyncio
import time
import numpy as np
from PIL import Image
import easyocr
from loguru import logger


class EasyOCRProcessor:
    """OCR processor using EasyOCR (most powerful)."""

    def __init__(self, lang: str = "en"):
        self.langs = [lang] if lang == "en" else [lang, "en"]
        self._reader = None

    @property
    def reader(self):
        if self._reader is None:
            logger.info(f"Loading EasyOCR model (langs={self.langs})")
            self._reader = easyocr.Reader(
                self.langs,
                gpu=True,
                verbose=False,
            )
            logger.info("EasyOCR model loaded successfully")
        return self._reader

    async def extract_text(
        self, image: Image.Image, engine: str = "easyocr"
    ) -> dict:
        """Extract text from image using EasyOCR."""
        start = time.time()

        loop = asyncio.get_event_loop()
        result = await loop.run_in_executor(None, self._run_ocr, image)

        results = []
        full_text_parts = []

        if result and len(result) > 0:
            for item in result:
                if len(item) >= 2:
                    bbox = item[0] if isinstance(item[0], list) else []
                    text = item[1]
                    confidence = item[2] if len(item) >= 3 else 0.9

                    results.append({
                        "text": text,
                        "confidence": round(float(confidence), 4),
                        "bbox": bbox if bbox else []
                    })
                    full_text_parts.append(text)

        return {
            "task": "extract_text",
            "model": "EasyOCR",
            "results": results,
            "full_text": " ".join(full_text_parts),
            "processing_time_ms": (time.time() - start) * 1000,
        }

    def _run_ocr(self, image: Image.Image) -> list:
        """Synchronous OCR run."""
        img = image.convert("RGB")

        width, height = img.size
        if height < 500:
            scale = 500 / height
            new_width = int(width * scale)
            img = img.resize((new_width, 500), Image.LANCZOS)

        img_array = np.array(img)
        return self.reader.readtext(img_array)
