from enum import Enum
from pydantic_settings import BaseSettings
from functools import lru_cache


class VideoProvider(str, Enum):
    REPLICATE = "replicate"
    KLING = "kling"
    RUNWAY = "runway"
    PIKA = "pika"
    SORA = "sora"
    MOCK = "mock"


class Settings(BaseSettings):
    VIDEO_PROVIDER: VideoProvider = VideoProvider.MOCK
    REPLICATE_API_TOKEN: str = ""
    KLING_API_KEY: str = ""
    KLING_API_SECRET: str = ""
    RUNWAY_API_KEY: str = ""
    PIKA_API_KEY: str = ""
    SORA_API_KEY: str = ""
    VIDEO_MAX_DURATION: int = 10
    VIDEO_DEFAULT_FPS: int = 24
    VIDEO_DEFAULT_RESOLUTION: str = "720p"
    VIDEO_WEBHOOK_URL: str = ""

    class Config:
        env_file = ".env"
        env_file_encoding = "utf-8"


@lru_cache
def get_settings() -> Settings:
    return Settings()
