"""Image generation domain service."""

from typing import Optional, List
from ..entities.image import ImageGeneration, ImageModel
from ..value_objects.common import Dimensions


class ImageGenerationService:
    """Domain service for image generation orchestration."""

    def validate_request(
        self,
        prompt: str,
        width: int,
        height: int,
        num_inference_steps: int,
        guidance_scale: float,
        num_images: int,
    ) -> ImageGeneration:
        """Validate and create an image generation request."""
        return ImageGeneration(
            prompt=prompt,
            width=width,
            height=height,
            num_inference_steps=num_inference_steps,
            guidance_scale=guidance_scale,
            num_images=num_images,
        )

    def upscale(
        self,
        original_width: int,
        original_height: int,
        scale: int,
    ) -> Dimensions:
        """Calculate upscaled dimensions."""
        if scale not in (2, 4):
            raise ValueError("Scale must be 2 or 4")

        return Dimensions(
            width=original_width * scale,
            height=original_height * scale,
        )
