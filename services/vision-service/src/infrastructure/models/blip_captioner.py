"""BLIP image captioner."""
import time
from PIL import Image
from transformers import BlipProcessor, BlipForConditionalGeneration
from loguru import logger


class BLIPCaptioner:
    """Image captioner using BLIP."""

    def __init__(self, model_name: str = "Salesforce/blip-image-captioning-base"):
        self.model_name = model_name
        self._processor = None
        self._model = None

    @property
    def processor(self) -> BlipProcessor:
        if self._processor is None:
            logger.info(f"Loading BLIP processor: {self.model_name}")
            self._processor = BlipProcessor.from_pretrained(
                self.model_name,
                use_fast=False,
                local_files_only=False,
            )
        return self._processor

    @property
    def model(self) -> BlipForConditionalGeneration:
        if self._model is None:
            logger.info(f"Loading BLIP model: {self.model_name}")
            self._model = BlipForConditionalGeneration.from_pretrained(self.model_name)
            logger.info("BLIP model loaded successfully")
        return self._model

    async def caption(self, image: Image.Image) -> dict:
        """Generate caption for image using BLIP."""
        start = time.time()

        # Prepare inputs
        inputs = self.processor(image, return_tensors="pt")

        # Generate caption
        output = self.model.generate(**inputs, max_new_tokens=50)
        caption = self.processor.decode(output[0], skip_special_tokens=True)

        return {
            "task": "caption_image",
            "model": self.model_name,
            "caption": caption,
            "processing_time_ms": (time.time() - start) * 1000,
        }
