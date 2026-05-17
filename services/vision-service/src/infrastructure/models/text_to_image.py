"""Mock Text-to-Image generator for development."""
from typing import Optional, List
from PIL import Image
import io
import base64
import random


class TextToImageGenerator:
    """Mock image generator using Stable Diffusion."""

    def __init__(self, model: str = "stabilityai/stable-diffusion-2-1"):
        self.model = model

    def generate(
        self,
        prompt: str,
        negative_prompt: Optional[str] = None,
        width: int = 512,
        height: int = 512,
        num_inference_steps: int = 50,
        guidance_scale: float = 7.5,
        seed: Optional[int] = None,
        num_images: int = 1,
        **kwargs
    ) -> List[Image.Image]:
        """Mock generation - returns placeholder images."""
        images = []
        for _ in range(num_images):
            img = Image.new("RGB", (width, height), color=(
                random.randint(100, 200),
                random.randint(100, 200),
                random.randint(150, 255)
            ))
            images.append(img)
        return images

    def image_to_base64(self, image: Image.Image) -> str:
        """Convert PIL Image to base64 string."""
        buffer = io.BytesIO()
        image.save(buffer, format="PNG")
        return base64.b64encode(buffer.getvalue()).decode()


_generator: Optional[TextToImageGenerator] = None


def get_generator() -> TextToImageGenerator:
    """Get singleton generator instance."""
    global _generator
    if _generator is None:
        _generator = TextToImageGenerator()
    return _generator
