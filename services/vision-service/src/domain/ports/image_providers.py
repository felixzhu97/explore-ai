"""Image generation service port (domain layer interface).

This module defines the Protocol interface for image generation services.
Infrastructure implementations should implement this protocol.

Architecture:
    API -> UseCase -> IImageGenerationService implementation (infrastructure)
    (api)   (app)              (infrastructure)

The implementation handles I/O operations like:
- Model loading and inference
- Image encoding/decoding
- External API calls (if applicable)
"""

from typing import List, Optional, Protocol, runtime_checkable


@runtime_checkable
class IImageGenerationService(Protocol):
    """Protocol for image generation services.

    This protocol defines the interface that any image generation
    infrastructure implementation must satisfy.
    """

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

        Args:
            prompt: Text description for image generation.
            negative_prompt: Things to avoid in the generated image.
            width: Image width in pixels.
            height: Image height in pixels.
            num_inference_steps: Number of denoising steps.
            guidance_scale: How closely to follow the prompt.
            seed: Random seed for reproducibility.
            num_images: Number of images to generate.
            **kwargs: Additional provider-specific parameters.

        Returns:
            List of base64-encoded PNG images.
        """
        ...

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
        """Generate variations of an existing image.

        Args:
            image: Base64-encoded source image.
            prompt: Text description guiding the variation.
            strength: How much to change the original image (0.0-1.0).
            num_inference_steps: Number of denoising steps.
            guidance_scale: How closely to follow the prompt.
            seed: Random seed for reproducibility.
            num_images: Number of variations to generate.
            **kwargs: Additional provider-specific parameters.

        Returns:
            List of base64-encoded PNG images.
        """
        ...

    async def upscale(
        self,
        image: str,
        scale: int = 2,
        prompt: Optional[str] = None,
        **kwargs
    ) -> str:
        """Upscale an image using AI-powered upsampling.

        Args:
            image: Base64-encoded image to upscale.
            scale: Upscaling factor (2 or 4).
            prompt: Optional text prompt for guidance.
            **kwargs: Additional provider-specific parameters.

        Returns:
            Base64-encoded upscaled PNG image.
        """
        ...

    def get_available_models(self) -> List[dict]:
        """Get information about available image generation models.

        Returns:
            List of model information dictionaries.
        """
        ...
