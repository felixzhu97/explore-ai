"""Image generation business rules (domain layer).

This module contains pure business rules for image generation,
ensuring that validation logic is centralized in the domain layer
and not scattered across application services.
"""

import base64
import io
import uuid
from dataclasses import dataclass
from typing import Optional, Tuple

from ..value_objects.common import Dimensions


class ImageGenerationRulesError(Exception):
    """Base exception for image generation rules violations."""

    pass


class InvalidPromptError(ImageGenerationRulesError):
    """Raised when prompt validation fails."""

    pass


class InvalidScaleError(ImageGenerationRulesError):
    """Raised when scale validation fails."""

    pass


class InvalidImageDataError(ImageGenerationRulesError):
    """Raised when image data is invalid."""

    pass


@dataclass(frozen=True)
class ImageGenerationRules:
    """Domain service containing image generation business rules.

    This class is a pure domain service with no infrastructure dependencies.
    It encapsulates all validation and business rule logic for image generation.

    Usage:
        rules = ImageGenerationRules()
        rules.validate_prompt("a beautiful sunset")
        rules.validate_scale(2)
        dimensions = rules.calculate_dimensions(image_b64, 2)
    """

    DEFAULT_MODEL: str = "stabilityai/stable-diffusion-2-1"
    VALID_SCALES: Tuple[int, ...] = (2, 4)

    def validate_prompt(self, prompt: str) -> None:
        """Validate that the prompt is non-empty.

        Args:
            prompt: The text prompt for image generation.

        Raises:
            InvalidPromptError: If prompt is empty or contains only whitespace.
        """
        if not prompt or not prompt.strip():
            raise InvalidPromptError("Prompt cannot be empty")

    def validate_scale(self, scale: int) -> None:
        """Validate that the scale factor is allowed.

        Args:
            scale: The scale factor for upscaling (2 or 4).

        Raises:
            InvalidScaleError: If scale is not 2 or 4.
        """
        if scale not in self.VALID_SCALES:
            raise InvalidScaleError(f"Scale must be one of {self.VALID_SCALES}")

    def generate_seed(self, provided_seed: Optional[int] = None) -> int:
        """Generate a seed for image generation.

        If a seed is provided, returns it; otherwise generates a new one
        using UUID's time_low component for reproducibility.

        Args:
            provided_seed: Optional seed value provided by the user.

        Returns:
            The seed value to use for generation.
        """
        if provided_seed is not None:
            return provided_seed
        return uuid.uuid4().time_low

    def calculate_dimensions(
        self, image_base64: str, scale: int
    ) -> Tuple[Dimensions, Dimensions]:
        """Calculate original and scaled dimensions from a base64-encoded image.

        Args:
            image_base64: Base64-encoded image string.
            scale: Scale factor to apply.

        Returns:
            Tuple of (original_dimensions, scaled_dimensions).

        Raises:
            InvalidImageDataError: If image data is invalid or corrupted.
        """
        from PIL import Image

        self.validate_scale(scale)

        try:
            image_data = base64.b64decode(image_base64)
            original_img = Image.open(io.BytesIO(image_data))
            original_width, original_height = original_img.size
        except Exception as e:
            raise InvalidImageDataError(f"Invalid base64 image data: {e}")

        # Create original dimensions (already validated by Dimensions VO)
        original_dims = Dimensions(original_width, original_height)

        # Calculate scaled dimensions
        scaled_dims = original_dims.scale(scale)

        return original_dims, scaled_dims
