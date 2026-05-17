from pydantic_settings import BaseSettings
from pydantic import ConfigDict
from functools import lru_cache


class ImageGenSettings(BaseSettings):
    model_config = ConfigDict(extra='allow')
    
    # Model settings
    DIFFUSION_MODEL: str = "stabilityai/stable-diffusion-3-medium"
    SDXL_MODEL: str = "stabilityai/stable-diffusion-xl-base-1.0"
    SD_VAE_MODEL: str = "stabilityai/sdxl-vae"
    
    # Generation parameters
    DEFAULT_STEPS: int = 30
    DEFAULT_GUIDANCE_SCALE: float = 7.5
    DEFAULT_HEIGHT: int = 1024
    DEFAULT_WIDTH: int = 1024
    DEFAULT_SEED: int = 42
    MAX_IMAGE_SIZE: int = 2048
    MIN_IMAGE_SIZE: int = 256
    
    # Performance settings
    USE_ATTENTION_SLICING: bool = True
    USE_SDPA: bool = True
    ENABLE_CPU_OFFLOAD: bool = False
    ENABLE_VAE_SPLITTING: bool = True
    
    # Rate limiting
    MAX_CONCURRENT_REQUESTS: int = 2
    REQUEST_TIMEOUT: int = 300
    
    # HuggingFace settings
    HF_ENDPOINT: str = ""


@lru_cache
def get_image_gen_settings() -> ImageGenSettings:
    return ImageGenSettings()
