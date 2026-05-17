from pydantic import BaseModel, Field, field_validator
from typing import Optional
from enum import Enum


class ImageModel(str, Enum):
    SD3 = "sd3"
    SDXL = "sdxl"
    SD35_MEDIUM = "sd35_medium"
    SD35_LARGE = "sd35_large"


class AspectRatio(str, Enum):
    RATIO_1_1 = "1:1"
    RATIO_4_3 = "4:3"
    RATIO_3_2 = "3:2"
    RATIO_16_9 = "16:9"
    RATIO_9_16 = "9:16"
    RATIO_21_9 = "21:9"


ASPECT_RATIO_DIMENSIONS = {
    AspectRatio.RATIO_1_1: (1024, 1024),
    AspectRatio.RATIO_4_3: (1024, 768),
    AspectRatio.RATIO_3_2: (1024, 683),
    AspectRatio.RATIO_16_9: (1024, 576),
    AspectRatio.RATIO_9_16: (768, 1024),
    AspectRatio.RATIO_21_9: (1280, 544),
}


class ImageGenRequest(BaseModel):
    prompt: str = Field(..., min_length=1, max_length=4000, description="Text prompt for image generation")
    negative_prompt: Optional[str] = Field(
        default="blurry, ugly, distorted, low quality, watermark, text, signature, copyright",
        max_length=2000,
        description="Negative prompt to avoid certain features"
    )
    model: ImageModel = Field(default=ImageModel.SDXL, description="Model to use for generation")
    width: int = Field(default=1024, ge=256, le=2048, description="Image width in pixels")
    height: int = Field(default=1024, ge=256, le=2048, description="Image height in pixels")
    num_inference_steps: int = Field(default=30, ge=1, le=150, description="Number of denoising steps")
    guidance_scale: float = Field(default=7.5, ge=1.0, le=20.0, description="Guidance scale for generation")
    seed: Optional[int] = Field(default=None, description="Random seed for reproducibility")
    num_images: int = Field(default=1, ge=1, le=4, description="Number of images to generate")
    style_preset: Optional[str] = Field(default=None, description="Style preset for the image")

    @field_validator("width", "height")
    @classmethod
    def validate_dimensions(cls, v: int) -> int:
        if v % 8 != 0:
            raise ValueError("Dimensions must be divisible by 8")
        return v

    model_config = {
        "json_schema_extra": {
            "examples": [
                {
                    "prompt": "A serene mountain landscape at sunset with vibrant orange and purple skies, realistic photography style",
                    "model": "sdxl",
                    "width": 1024,
                    "height": 768,
                    "num_inference_steps": 30,
                    "guidance_scale": 7.5
                }
            ]
        }
    }


class ImageGenResponse(BaseModel):
    images: list[str] = Field(..., description="Base64 encoded generated images")
    seed: int = Field(..., description="Seed used for generation")
    model: str = Field(..., description="Model used for generation")
    prompt: str = Field(..., description="Original prompt used")
    inference_steps: int = Field(..., description="Number of inference steps used")
    guidance_scale: float = Field(..., description="Guidance scale used")
    width: int = Field(..., description="Generated image width")
    height: int = Field(..., description="Generated image height")
    processing_time_ms: float = Field(..., description="Total processing time in milliseconds")
    metadata: dict = Field(default_factory=dict, description="Additional generation metadata")


class ImageVariationRequest(BaseModel):
    image: str = Field(..., description="Base64 encoded source image")
    prompt: str = Field(..., min_length=1, max_length=4000, description="Prompt to guide variation")
    strength: float = Field(default=0.5, ge=0.0, le=1.0, description="Transformation strength")
    num_inference_steps: int = Field(default=30, ge=1, le=150)
    guidance_scale: float = Field(default=7.5, ge=1.0, le=20.0)
    seed: Optional[int] = None
    num_images: int = Field(default=1, ge=1, le=4)

    model_config = {
        "json_schema_extra": {
            "examples": [
                {
                    "image": "<base64_encoded_image>",
                    "prompt": "Same scene in winter with snow",
                    "strength": 0.6,
                    "num_inference_steps": 30
                }
            ]
        }
    }


class ImageUpscaleRequest(BaseModel):
    image: str = Field(..., description="Base64 encoded image to upscale")
    scale: int = Field(default=2, ge=2, le=4, description="Upscaling factor (2x or 4x)")
    prompt: Optional[str] = Field(default=None, max_length=1000, description="Optional prompt for guided upscaling")

    model_config = {
        "json_schema_extra": {
            "examples": [
                {
                    "image": "<base64_encoded_image>",
                    "scale": 2,
                    "prompt": "Enhance details and sharpness"
                }
            ]
        }
    }


class ModelInfo(BaseModel):
    model_id: str
    model_type: str
    capabilities: list[str]
    max_dimensions: tuple[int, int]
    recommended_steps: tuple[int, int]
    vram_required_gb: float
    supports_attention_slicing: bool
    supports_vae_slicing: bool


class AvailableModelsResponse(BaseModel):
    models: list[ModelInfo]
    default_model: str
