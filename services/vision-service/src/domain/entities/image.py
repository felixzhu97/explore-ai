"""Image generation entity."""

from dataclasses import dataclass, field
from datetime import datetime
from typing import Optional, List
from enum import Enum
import uuid

from ..value_objects.common import Dimensions


class ImageModel(str, Enum):
    SD3 = "sd3"
    SDXL = "sdxl"
    SD35_MEDIUM = "sd35_medium"
    SD35_LARGE = "sd35_large"


@dataclass
class ImageGeneration:
    """Image generation entity.

    Encapsulates the business rules for image generation,
    including validation and seed management.
    """

    prompt: str
    negative_prompt: str = "blurry, ugly, distorted, low quality, watermark, text, signature"
    model: ImageModel = ImageModel.SDXL
    width: int = 1024
    height: int = 1024
    num_inference_steps: int = 30
    guidance_scale: float = 7.5
    num_images: int = 1
    style_preset: Optional[str] = None
    seed: Optional[int] = field(default_factory=lambda: uuid.uuid4().time_low)

    generation_id: str = field(default_factory=lambda: str(uuid.uuid4()))
    created_at: datetime = field(default_factory=datetime.now)
    completed_at: Optional[datetime] = None

    def __post_init__(self):
        self._validate()

    def _validate(self) -> None:
        """Validate generation parameters."""
        if not self.prompt.strip():
            raise ValueError("Prompt cannot be empty")
        if not 1 <= self.num_inference_steps <= 150:
            raise ValueError("Inference steps must be between 1 and 150")
        if not 1.0 <= self.guidance_scale <= 20.0:
            raise ValueError("Guidance scale must be between 1.0 and 20.0")
        if not 1 <= self.num_images <= 4:
            raise ValueError("Number of images must be between 1 and 4")

    @classmethod
    def create_with_validation(
        cls,
        prompt: str,
        negative_prompt: str = "blurry, ugly, distorted, low quality, watermark, text, signature",
        model: ImageModel = ImageModel.SDXL,
        width: int = 1024,
        height: int = 1024,
        num_inference_steps: int = 30,
        guidance_scale: float = 7.5,
        num_images: int = 1,
        style_preset: Optional[str] = None,
        seed: Optional[int] = None,
    ) -> "ImageGeneration":
        """Factory method to create an ImageGeneration with validation.

        This factory delegates prompt validation to ImageGenerationRules,
        centralizing business logic in the domain layer.

        Args:
            prompt: Text prompt for image generation.
            negative_prompt: Negative prompt to avoid certain features.
            model: The image generation model to use.
            width: Image width in pixels.
            height: Image height in pixels.
            num_inference_steps: Number of denoising steps.
            guidance_scale: Guidance scale for generation.
            num_images: Number of images to generate.
            style_preset: Optional style preset.
            seed: Optional seed for reproducibility.

        Returns:
            A new ImageGeneration instance with validated parameters.

        Raises:
            ValueError: If prompt is empty or parameters are invalid.
        """
        from ..services.image_generation_rules import (
            ImageGenerationRules,
            InvalidPromptError,
        )

        rules = ImageGenerationRules()
        rules.validate_prompt(prompt)

        if seed is None:
            seed = rules.generate_seed()

        return cls(
            prompt=prompt,
            negative_prompt=negative_prompt,
            model=model,
            width=width,
            height=height,
            num_inference_steps=num_inference_steps,
            guidance_scale=guidance_scale,
            num_images=num_images,
            style_preset=style_preset,
            seed=seed,
        )

    def mark_completed(self) -> None:
        """Mark generation as completed."""
        self.completed_at = datetime.now()

    @property
    def duration_ms(self) -> Optional[float]:
        """Calculate processing duration in milliseconds."""
        if self.completed_at:
            return (self.completed_at - self.created_at).total_seconds() * 1000
        return None
