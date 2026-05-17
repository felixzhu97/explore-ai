"""Image generation service implementation (infrastructure layer).

This module contains the infrastructure implementation that handles
I/O operations, model inference, and image encoding for image generation.
"""

from typing import List, Optional
from PIL import Image
import io
import base64
import random
import time

from ...domain.ports.image_providers import IImageGenerationService


class ImageGenerationServiceImpl(IImageGenerationService):
    """Infrastructure implementation of image generation service.

    Handles model inference, image processing, and base64 encoding.
    This class belongs in the infrastructure layer, not the domain layer.

    Architecture:
        API -> UseCase -> ImageGenerationServiceImpl
        (api)   (app)              (infrastructure)
    """

    def __init__(self, model: str = "stabilityai/stable-diffusion-2-1"):
        """Initialize the image generation service.

        Args:
            model: HuggingFace model identifier for image generation.
        """
        self._model = model
        self._device = "cpu"  # Would be "cuda" if GPU available

    @property
    def device(self) -> str:
        """Get the current device (cpu/cuda)."""
        return self._device

    async def generate(
        self,
        prompt: str,
        negative_prompt: Optional[str] = None,
        width: int = 1024,
        height: int = 1024,
        num_inference_steps: int = 30,
        guidance_scale: float = 7.5,
        seed: Optional[int] = None,
        num_images: int = 1,
        **kwargs
    ) -> List[str]:
        """Generate images from text prompt.

        This is a mock implementation for development.
        In production, this would load and run a diffusion model.
        """
        if not prompt or not prompt.strip():
            raise ValueError("Prompt cannot be empty")

        if width > 2048 or height > 2048:
            raise ValueError("Image dimensions cannot exceed 2048px")

        if num_inference_steps > 150:
            raise ValueError("Maximum 150 inference steps allowed")

        if num_images > 4:
            raise ValueError("Maximum 4 images per request")

        images = []
        for _ in range(num_images):
            # Mock: Generate placeholder images with random colors
            img = Image.new(
                "RGB",
                (width, height),
                color=(
                    random.randint(100, 200),
                    random.randint(100, 200),
                    random.randint(150, 255),
                ),
            )
            images.append(self._encode_image(img))

        return images

    async def generate_variation(
        self,
        image: str,
        prompt: str,
        strength: float = 0.5,
        num_inference_steps: int = 30,
        guidance_scale: float = 7.5,
        seed: Optional[int] = None,
        num_images: int = 1,
        **kwargs
    ) -> List[str]:
        """Generate variations of an existing image."""
        if not prompt or not prompt.strip():
            raise ValueError("Prompt cannot be empty")

        if not 0.0 <= strength <= 1.0:
            raise ValueError("Strength must be between 0.0 and 1.0")

        # Decode input image
        try:
            img_data = base64.b64decode(image)
            input_img = Image.open(io.BytesIO(img_data))
        except Exception as e:
            raise ValueError(f"Invalid base64 image: {e}")

        # Mock: Resize and return with slight color variation
        variations = []
        for i in range(num_images):
            # Create a slightly modified version
            w, h = input_img.size
            variation = input_img.resize(
                (min(w + 10, 2048), min(h + 10, 2048)),
                Image.Resampling.LANCZOS,
            )
            variations.append(self._encode_image(variation))

        return variations

    async def upscale(
        self,
        image: str,
        scale: int = 2,
        prompt: Optional[str] = None,
        **kwargs
    ) -> str:
        """Upscale an image using AI-powered upsampling."""
        if scale not in (2, 4):
            raise ValueError("Scale must be 2 or 4")

        # Decode input image
        try:
            img_data = base64.b64decode(image)
            input_img = Image.open(io.BytesIO(img_data))
        except Exception as e:
            raise ValueError(f"Invalid base64 image: {e}")

        # Calculate new dimensions
        w, h = input_img.size
        new_w, new_h = w * scale, h * scale

        if new_w > 2048 or new_h > 2048:
            raise ValueError("Upscaled dimensions cannot exceed 2048px")

        # Mock: Simple resize (in production would use Real-ESRGAN or similar)
        upscaled = input_img.resize((new_w, new_h), Image.Resampling.LANCZOS)

        return self._encode_image(upscaled)

    def get_available_models(self) -> List[dict]:
        """Get information about available image generation models."""
        return [
            {
                "model_id": "stabilityai/stable-diffusion-3-medium",
                "model_type": "sd3",
                "capabilities": ["text-to-image", "image-to-image", "inpainting"],
                "max_dimensions": (1024, 1024),
                "recommended_steps": (25, 50),
                "vram_required_gb": 8.0,
                "supports_attention_slicing": True,
                "supports_vae_slicing": True,
            },
            {
                "model_id": "stabilityai/stable-diffusion-xl-base-1.0",
                "model_type": "sdxl",
                "capabilities": ["text-to-image", "image-to-image"],
                "max_dimensions": (1024, 1024),
                "recommended_steps": (20, 50),
                "vram_required_gb": 6.0,
                "supports_attention_slicing": True,
                "supports_vae_slicing": True,
            },
            {
                "model_id": "stabilityai/stable-diffusion-2-1",
                "model_type": "sd2",
                "capabilities": ["text-to-image", "image-to-image"],
                "max_dimensions": (768, 768),
                "recommended_steps": (30, 50),
                "vram_required_gb": 4.0,
                "supports_attention_slicing": True,
                "supports_vae_slicing": False,
            },
        ]

    def clear_cache(self) -> None:
        """Clear the model cache to free GPU memory."""
        # In production, this would unload models from GPU memory
        pass

    def _encode_image(self, image: Image.Image) -> str:
        """Convert PIL Image to base64 string."""
        buffer = io.BytesIO()
        image.save(buffer, format="PNG")
        return base64.b64encode(buffer.getvalue()).decode()


# Singleton instance
_service_instance: Optional[ImageGenerationServiceImpl] = None


def get_image_generation_service(
    model: str = "stabilityai/stable-diffusion-2-1",
) -> ImageGenerationServiceImpl:
    """Get singleton image generation service instance.

    Args:
        model: HuggingFace model identifier.

    Returns:
        ImageGenerationServiceImpl instance.
    """
    global _service_instance
    if _service_instance is None:
        _service_instance = ImageGenerationServiceImpl(model=model)
    return _service_instance
