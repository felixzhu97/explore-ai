"""Generate image use case (application layer).

This use case orchestrates image generation by delegating to
the image generation service (infrastructure layer).

Business rules (validation, seed generation, etc.) are delegated
to ImageGenerationRules in the domain layer.
"""

import time

from ...domain.ports.image_providers import IImageGenerationService
from ...domain.services.image_generation_rules import (
    ImageGenerationRules,
    InvalidPromptError,
    InvalidScaleError,
)
from ..dtos.image_gen_dtos import (
    GenerateImageInputDTO,
    GenerateImageOutputDTO,
    GenerateVariationInputDTO,
    GenerateVariationOutputDTO,
    UpscaleImageInputDTO,
    UpscaleImageOutputDTO,
    ListModelsOutputDTO,
)


class GenerateImageUseCase:
    """Application use case for text-to-image generation.

    This use case handles orchestration of image generation,
    delegating business rules to the domain layer.

    Architecture:
        API -> GenerateImageUseCase -> IImageGenerationService (implementation)
        (api)       (application)              (infrastructure)

    The use case is responsible for:
    - Orchestrating the workflow
    - Timing and metrics
    - Response formatting
    - Delegating business rules to ImageGenerationRules (domain)
    """

    def __init__(
        self,
        image_service: IImageGenerationService,
        rules: ImageGenerationRules | None = None,
    ):
        """Initialize the use case with an image generation service.

        Args:
            image_service: Implementation of IImageGenerationService protocol.
            rules: Optional ImageGenerationRules instance. If not provided,
                   uses the default ImageGenerationRules.
        """
        self._service = image_service
        self._rules = rules if rules is not None else ImageGenerationRules()

    async def execute(self, input_dto: GenerateImageInputDTO) -> GenerateImageOutputDTO:
        """Execute image generation.

        Args:
            input_dto: Input data transfer object with generation parameters.

        Returns:
            Output data transfer object with generated images and metadata.

        Raises:
            InvalidPromptError: If prompt is empty or contains only whitespace.
        """
        start_time = time.time()

        self._rules.validate_prompt(input_dto.prompt)

        seed = self._rules.generate_seed(input_dto.seed)

        images = await self._service.generate(
            prompt=input_dto.prompt,
            negative_prompt=input_dto.negative_prompt,
            width=input_dto.width,
            height=input_dto.height,
            num_inference_steps=input_dto.num_inference_steps,
            guidance_scale=input_dto.guidance_scale,
            seed=seed,
            num_images=input_dto.num_images,
            style_preset=input_dto.style_preset,
        )

        processing_time_ms = (time.time() - start_time) * 1000

        return GenerateImageOutputDTO(
            images=images,
            seed=seed,
            model=self._rules.DEFAULT_MODEL,
            prompt=input_dto.prompt,
            inference_steps=input_dto.num_inference_steps,
            guidance_scale=input_dto.guidance_scale,
            width=input_dto.width,
            height=input_dto.height,
            processing_time_ms=processing_time_ms,
        )


class GenerateVariationUseCase:
    """Application use case for generating image variations.

    Handles orchestration of image variation generation,
    delegating business rules to the domain layer.
    """

    def __init__(
        self,
        image_service: IImageGenerationService,
        rules: ImageGenerationRules | None = None,
    ):
        """Initialize the use case with an image generation service."""
        self._service = image_service
        self._rules = rules if rules is not None else ImageGenerationRules()

    async def execute(
        self, input_dto: GenerateVariationInputDTO
    ) -> GenerateVariationOutputDTO:
        """Execute image variation generation.

        Args:
            input_dto: Input data transfer object with variation parameters.

        Returns:
            Output data transfer object with generated variations.

        Raises:
            InvalidPromptError: If prompt is empty or contains only whitespace.
        """
        start_time = time.time()

        self._rules.validate_prompt(input_dto.prompt)

        seed = self._rules.generate_seed(input_dto.seed)

        images = await self._service.generate_variation(
            image=input_dto.image,
            prompt=input_dto.prompt,
            strength=input_dto.strength,
            num_inference_steps=input_dto.num_inference_steps,
            guidance_scale=input_dto.guidance_scale,
            seed=seed,
            num_images=input_dto.num_images,
        )

        processing_time_ms = (time.time() - start_time) * 1000

        return GenerateVariationOutputDTO(
            images=images,
            seed=seed,
            prompt=input_dto.prompt,
            strength=input_dto.strength,
            inference_steps=input_dto.num_inference_steps,
            processing_time_ms=processing_time_ms,
        )


class UpscaleImageUseCase:
    """Application use case for image upscaling.

    Handles orchestration of AI-powered image upscaling,
    delegating business rules to the domain layer.
    """

    def __init__(
        self,
        image_service: IImageGenerationService,
        rules: ImageGenerationRules | None = None,
    ):
        """Initialize the use case with an image generation service."""
        self._service = image_service
        self._rules = rules if rules is not None else ImageGenerationRules()

    async def execute(self, input_dto: UpscaleImageInputDTO) -> UpscaleImageOutputDTO:
        """Execute image upscaling.

        Args:
            input_dto: Input data transfer object with upscale parameters.

        Returns:
            Output data transfer object with upscaled image.

        Raises:
            InvalidScaleError: If scale is not 2 or 4.
            InvalidImageDataError: If image data is invalid.
        """
        start_time = time.time()

        self._rules.validate_scale(input_dto.scale)

        original_dims, scaled_dims = self._rules.calculate_dimensions(
            input_dto.image, input_dto.scale
        )

        upscaled_b64 = await self._service.upscale(
            image=input_dto.image,
            scale=input_dto.scale,
            prompt=input_dto.prompt,
        )

        processing_time_ms = (time.time() - start_time) * 1000

        return UpscaleImageOutputDTO(
            image=upscaled_b64,
            scale=input_dto.scale,
            original_width=original_dims.width,
            original_height=original_dims.height,
            new_width=scaled_dims.width,
            new_height=scaled_dims.height,
            processing_time_ms=processing_time_ms,
        )


class ListModelsUseCase:
    """Application use case for listing available models."""

    def __init__(self, image_service: IImageGenerationService):
        """Initialize the use case with an image generation service."""
        self._service = image_service

    def execute(self) -> ListModelsOutputDTO:
        """Get available image generation models.

        Returns:
            Output DTO with list of available models.
        """
        models = self._service.get_available_models()
        return ListModelsOutputDTO(
            models=models,
            default_model=ImageGenerationRules.DEFAULT_MODEL,
        )
