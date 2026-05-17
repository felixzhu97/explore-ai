"""
Text-to-Image Client for Vision Service

A Python client library for generating images using the Vision Service's
Text-to-Image API. Supports Stable Diffusion XL and SD3 models.

Usage:
    from services.vision_service.src.clients.image_gen_client import ImageGenClient
    
    client = ImageGenClient(base_url="http://localhost:8000")
    result = client.generate(
        prompt="A beautiful sunset over mountains",
        width=1024,
        height=768
    )
    client.save_images(result, output_dir="./output")
"""

import base64
import io
import logging
import os
import time
from pathlib import Path
from typing import Optional, Union
from dataclasses import dataclass, field

import httpx
from PIL import Image

logger = logging.getLogger(__name__)


@dataclass
class GenerationConfig:
    """Configuration for image generation."""
    prompt: str
    negative_prompt: str = "blurry, ugly, distorted, low quality, watermark, text, signature"
    model: str = "sdxl"
    width: int = 1024
    height: int = 1024
    num_inference_steps: int = 30
    guidance_scale: float = 7.5
    seed: Optional[int] = None
    num_images: int = 1
    style_preset: Optional[str] = None


@dataclass
class GenerationResult:
    """Result from image generation."""
    images: list[Image.Image]
    seeds: list[int]
    model: str
    prompt: str
    inference_steps: int
    guidance_scale: float
    width: int
    height: int
    processing_time_ms: float
    metadata: dict = field(default_factory=dict)

    def save(self, output_dir: Union[str, Path], prefix: str = "generated") -> list[str]:
        """Save generated images to disk.
        
        Args:
            output_dir: Directory to save images
            prefix: Filename prefix
            
        Returns:
            List of saved file paths
        """
        output_dir = Path(output_dir)
        output_dir.mkdir(parents=True, exist_ok=True)
        
        paths = []
        for i, img in enumerate(self.images):
            timestamp = int(time.time() * 1000)
            filename = f"{prefix}_{timestamp}_{i}.png"
            filepath = output_dir / filename
            img.save(filepath, "PNG")
            paths.append(str(filepath))
            logger.info(f"Saved image to {filepath}")
        
        return paths


class ImageGenClient:
    """
    Python client for Vision Service Text-to-Image API.
    
    Example:
        >>> client = ImageGenClient()
        >>> result = client.generate("A cat sitting on a windowsill")
        >>> result.save("./images")
        
        >>> # With custom parameters
        >>> result = client.generate(
        ...     prompt="Abstract art",
        ...     width=2048,
        ...     height=1024,
        ...     num_inference_steps=50,
        ...     seed=42
        ... )
    """

    DEFAULT_TIMEOUT = 300  # 5 minutes for generation

    def __init__(
        self,
        base_url: str = "http://localhost:8000",
        timeout: int = DEFAULT_TIMEOUT,
        api_key: Optional[str] = None,
    ):
        """
        Initialize the client.
        
        Args:
            base_url: Base URL of the Vision Service
            timeout: Request timeout in seconds
            api_key: Optional API key for authentication
        """
        self.base_url = base_url.rstrip("/")
        self.timeout = timeout
        self._client = httpx.Client(timeout=timeout)
        
        if api_key:
            self._headers = {"Authorization": f"Bearer {api_key}"}
        else:
            self._headers = {}

    def _post(self, endpoint: str, data: dict) -> dict:
        """Make a POST request to the API."""
        url = f"{self.base_url}{endpoint}"
        try:
            response = self._client.post(url, json=data, headers=self._headers)
            response.raise_for_status()
            return response.json()
        except httpx.HTTPStatusError as e:
            logger.error(f"HTTP error {e.response.status_code}: {e.response.text}")
            raise
        except httpx.RequestError as e:
            logger.error(f"Request error: {e}")
            raise

    def _get(self, endpoint: str) -> dict:
        """Make a GET request to the API."""
        url = f"{self.base_url}{endpoint}"
        try:
            response = self._client.get(url, headers=self._headers)
            response.raise_for_status()
            return response.json()
        except httpx.HTTPStatusError as e:
            logger.error(f"HTTP error {e.response.status_code}: {e.response.text}")
            raise
        except httpx.RequestError as e:
            logger.error(f"Request error: {e}")
            raise

    def generate(
        self,
        prompt: str,
        negative_prompt: str = "blurry, ugly, distorted, low quality",
        model: str = "sdxl",
        width: int = 1024,
        height: int = 1024,
        num_inference_steps: int = 30,
        guidance_scale: float = 7.5,
        seed: Optional[int] = None,
        num_images: int = 1,
        style_preset: Optional[str] = None,
    ) -> GenerationResult:
        """
        Generate images from text prompt.
        
        Args:
            prompt: Text description of the desired image
            negative_prompt: Things to avoid in the image
            model: Model to use ('sdxl' or 'sd3')
            width: Image width (256-2048, divisible by 8)
            height: Image height (256-2048, divisible by 8)
            num_inference_steps: Quality vs speed tradeoff (1-150)
            guidance_scale: How closely to follow the prompt (1-20)
            seed: Random seed for reproducibility
            num_images: Number of images to generate (1-4)
            style_preset: Optional style preset
            
        Returns:
            GenerationResult with generated PIL Images and metadata
            
        Raises:
            httpx.HTTPStatusError: If the API returns an error
        """
        data = {
            "prompt": prompt,
            "negative_prompt": negative_prompt,
            "model": model,
            "width": width,
            "height": height,
            "num_inference_steps": num_inference_steps,
            "guidance_scale": guidance_scale,
            "num_images": num_images,
        }
        
        if seed is not None:
            data["seed"] = seed
        if style_preset:
            data["style_preset"] = style_preset

        logger.info(f"Generating {num_images} image(s) with {model}")
        start_time = time.perf_counter()
        
        response = self._post("/image-gen/generate", data)
        
        # Decode base64 images
        images = []
        seeds = []
        for img_b64 in response["images"]:
            img_data = base64.b64decode(img_b64)
            images.append(Image.open(io.BytesIO(img_data)))
            seeds.append(response["seed"])

        processing_time = (time.perf_counter() - start_time) * 1000

        return GenerationResult(
            images=images,
            seeds=seeds,
            model=response["model"],
            prompt=response["prompt"],
            inference_steps=response["inference_steps"],
            guidance_scale=response["guidance_scale"],
            width=response["width"],
            height=response["height"],
            processing_time_ms=processing_time,
            metadata=response.get("metadata", {}),
        )

    def generate_variation(
        self,
        image: Union[str, Path, Image.Image],
        prompt: str,
        strength: float = 0.5,
        num_inference_steps: int = 30,
        guidance_scale: float = 7.5,
        seed: Optional[int] = None,
        num_images: int = 1,
    ) -> GenerationResult:
        """
        Generate variations of an existing image.
        
        Args:
            image: Source image (file path, URL, or PIL Image)
            prompt: Description of the desired variation
            strength: How much to transform (0.0-1.0)
            num_inference_steps: Quality vs speed (1-150)
            guidance_scale: Prompt adherence (1-20)
            seed: Random seed
            num_images: Number of variations (1-4)
            
        Returns:
            GenerationResult with generated variations
        """
        # Load and encode image
        if isinstance(image, Image.Image):
            img = image
        elif isinstance(image, (str, Path)):
            img = Image.open(image)
        else:
            raise ValueError(f"Unsupported image type: {type(image)}")
        
        buffered = io.BytesIO()
        img.save(buffered, format="PNG")
        img_b64 = base64.b64encode(buffered.getvalue()).decode("utf-8")

        data = {
            "image": img_b64,
            "prompt": prompt,
            "strength": strength,
            "num_inference_steps": num_inference_steps,
            "guidance_scale": guidance_scale,
            "num_images": num_images,
        }
        
        if seed is not None:
            data["seed"] = seed

        logger.info(f"Generating {num_images} variation(s)")
        
        response = self._post("/image-gen/variation", data)
        
        # Decode images
        images = []
        seeds = []
        for img_b64 in response["images"]:
            img_data = base64.b64decode(img_b64)
            images.append(Image.open(io.BytesIO(img_data)))
            seeds.append(response["seed"])

        return GenerationResult(
            images=images,
            seeds=seeds,
            model=response["model"],
            prompt=response["prompt"],
            inference_steps=response["inference_steps"],
            guidance_scale=response["guidance_scale"],
            width=response["width"],
            height=response["height"],
            processing_time_ms=response["processing_time_ms"],
            metadata=response.get("metadata", {}),
        )

    def upscale(
        self,
        image: Union[str, Path, Image.Image],
        scale: int = 2,
        prompt: Optional[str] = None,
    ) -> GenerationResult:
        """
        Upscale an image using AI.
        
        Args:
            image: Image to upscale (file path, URL, or PIL Image)
            scale: Upscaling factor (2 or 4)
            prompt: Optional guidance prompt
            
        Returns:
            GenerationResult with upscaled image
        """
        # Load and encode image
        if isinstance(image, Image.Image):
            img = image
        elif isinstance(image, (str, Path)):
            img = Image.open(image)
        else:
            raise ValueError(f"Unsupported image type: {type(image)}")
        
        buffered = io.BytesIO()
        img.save(buffered, format="PNG")
        img_b64 = base64.b64encode(buffered.getvalue()).decode("utf-8")

        data = {
            "image": img_b64,
            "scale": scale,
        }
        
        if prompt:
            data["prompt"] = prompt

        logger.info(f"Upscaling image {scale}x")
        
        response = self._post("/image-gen/upscale", data)
        
        # Decode images
        images = []
        seeds = []
        for img_b64 in response["images"]:
            img_data = base64.b64decode(img_b64)
            images.append(Image.open(io.BytesIO(img_data)))
            seeds.append(response["seed"])

        return GenerationResult(
            images=images,
            seeds=seeds,
            model=response["model"],
            prompt=response["prompt"],
            inference_steps=response["inference_steps"],
            guidance_scale=response["guidance_scale"],
            width=response["width"],
            height=response["height"],
            processing_time_ms=response["processing_time_ms"],
            metadata=response.get("metadata", {}),
        )

    def get_models(self) -> dict:
        """Get information about available models."""
        return self._get("/image-gen/models")

    def health_check(self) -> dict:
        """Check service health."""
        return self._get("/image-gen/health")

    def clear_cache(self) -> dict:
        """Clear model cache to free memory."""
        return self._post("/image-gen/cache/clear", {})

    def close(self):
        """Close the HTTP client."""
        self._client.close()

    def __enter__(self):
        return self

    def __exit__(self, exc_type, exc_val, exc_tb):
        self.close()


# Convenience functions for simple usage
def generate_image(
    prompt: str,
    **kwargs
) -> GenerationResult:
    """
    Generate an image with default settings.
    
    Args:
        prompt: Text description of the image
        **kwargs: Additional generation parameters
        
    Returns:
        GenerationResult with generated image
    """
    with ImageGenClient() as client:
        return client.generate(prompt, **kwargs)


def save_images(
    result: GenerationResult,
    output_dir: Union[str, Path] = "./output",
    prefix: str = "image"
) -> list[str]:
    """
    Save generation result to disk.
    
    Args:
        result: GenerationResult from generate()
        output_dir: Output directory
        prefix: Filename prefix
        
    Returns:
        List of saved file paths
    """
    return result.save(output_dir, prefix)


# Example usage
if __name__ == "__main__":
    # Configure logging
    logging.basicConfig(level=logging.INFO)
    
    # Basic usage
    print("Generating image...")
    result = generate_image(
        prompt="A serene mountain landscape at sunset",
        width=1024,
        height=768,
        num_inference_steps=30
    )
    
    print(f"Generated {len(result.images)} image(s) in {result.processing_time_ms:.0f}ms")
    print(f"Seed: {result.seeds[0]}")
    
    # Save to disk
    saved_paths = save_images(result, "./generated_images", "mountain")
    print(f"Saved to: {saved_paths}")
    
    # Batch generation
    print("\nGenerating variations...")
    with ImageGenClient() as client:
        # Generate multiple images with same seed
        result_batch = client.generate(
            prompt="Abstract geometric art",
            seed=12345,
            num_images=3,
            width=512,
            height=512,
            num_inference_steps=20
        )
        
        for i, img in enumerate(result_batch.images):
            img.save(f"./generated_images/variation_{i}.png")
        print(f"Generated {len(result_batch.images)} variations")
