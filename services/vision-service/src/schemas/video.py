from pydantic import BaseModel, Field
from typing import Optional
from enum import Enum


class VideoModel(str, Enum):
    KLING_V1_0 = "kling-v1-0"
    KLING_V1_5 = "kling-v1-5"
    REPLICATE_KLING = "kling"


class VideoAspectRatio(str, Enum):
    RATIO_16_9 = "16:9"
    RATIO_9_16 = "9:16"
    RATIO_1_1 = "1:1"
    RATIO_4_3 = "4:3"


class VideoQuality(str, Enum):
    STANDARD = "standard"
    HIGH = "high"


class VideoGenerateRequest(BaseModel):
    prompt: str = Field(..., min_length=1, max_length=500, description="Text description of the video to generate")
    negative_prompt: Optional[str] = Field(None, max_length=500, description="Elements to avoid in the video")
    duration: int = Field(default=5, ge=5, le=10, description="Video duration in seconds")
    aspect_ratio: VideoAspectRatio = Field(default=VideoAspectRatio.RATIO_16_9)
    fps: int = Field(default=24, ge=24, le=60)
    quality: VideoQuality = Field(default=VideoQuality.HIGH)
    model: VideoModel = Field(default=VideoModel.KLING_V1_5)
    callback_url: Optional[str] = None


class VideoTaskStatus(str, Enum):
    PENDING = "pending"
    PROCESSING = "processing"
    COMPLETED = "completed"
    FAILED = "failed"


class VideoGenerateResponse(BaseModel):
    task_id: str
    status: VideoTaskStatus
    message: str
    created_at: str


class VideoTaskResponse(BaseModel):
    task_id: str
    status: VideoTaskStatus
    video_url: Optional[str] = None
    thumbnail_url: Optional[str] = None
    error: Optional[str] = None
    processing_time_seconds: Optional[float] = None
    metadata: Optional[dict] = None


class VideoStylePreset(str, Enum):
    REALISTIC = "realistic"
    ANIMATION = "animation"
    CINEMATIC = "cinematic"
    ABSTRACT = "abstract"
    NONE = "none"


class AdvancedVideoRequest(VideoGenerateRequest):
    style: VideoStylePreset = Field(default=VideoStylePreset.NONE)
    seed: Optional[int] = None
    cfg_scale: float = Field(default=7.5, ge=1.0, le=20.0)
    motion_intensity: float = Field(default=1.0, ge=0.1, le=2.0)
