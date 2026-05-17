"""Generate video use case."""

from typing import Optional
from ...domain.entities.video_task import VideoTask, VideoTaskStatus
from ...domain.value_objects.common import VideoConfig, AspectRatio, VideoQuality
from ...domain.ports.video_providers import IVideoGenerationService


def _normalize_aspect_ratio(value: str) -> AspectRatio:
    """Convert string to AspectRatio domain enum."""
    mapping = {
        "16:9": AspectRatio.RATIO_16_9,
        "9:16": AspectRatio.RATIO_9_16,
        "1:1": AspectRatio.RATIO_1_1,
        "4:3": AspectRatio.RATIO_4_3,
    }
    return mapping.get(value, AspectRatio.RATIO_16_9)


def _normalize_video_quality(value: str) -> VideoQuality:
    """Convert string to VideoQuality domain enum."""
    mapping = {
        "standard": VideoQuality.STANDARD,
        "high": VideoQuality.HIGH,
    }
    return mapping.get(value, VideoQuality.HIGH)


class GenerateVideoInput:
    """Input for video generation use case."""

    def __init__(
        self,
        prompt: str,
        negative_prompt: Optional[str] = None,
        duration: int = 5,
        aspect_ratio: str = "16:9",
        fps: int = 24,
        quality: str = "high",
        seed: Optional[int] = None,
    ):
        self.prompt = prompt
        self.negative_prompt = negative_prompt
        self.duration = duration
        self._aspect_ratio_raw = aspect_ratio
        self.fps = fps
        self._quality_raw = quality
        self.seed = seed

    @property
    def aspect_ratio(self) -> AspectRatio:
        return _normalize_aspect_ratio(self._aspect_ratio_raw)

    @property
    def quality(self) -> VideoQuality:
        return _normalize_video_quality(self._quality_raw)


class GenerateVideoOutput:
    """Output from video generation use case."""

    def __init__(self, task: VideoTask):
        self.task_id = task.task_id
        self.status = task.status.value
        self.message = "Video generation started"


class GenerateVideoUseCase:
    """Application use case for video generation.

    This use case orchestrates video generation by delegating to
    the video generation service (infrastructure layer).
    """

    def __init__(self, service: IVideoGenerationService):
        self._service = service

    async def execute(self, input_data: GenerateVideoInput) -> GenerateVideoOutput:
        """Execute the video generation use case."""
        config = VideoConfig(
            duration=input_data.duration,
            aspect_ratio=input_data.aspect_ratio,
            fps=input_data.fps,
            quality=input_data.quality,
            negative_prompt=input_data.negative_prompt,
            seed=input_data.seed,
        )

        task = await self._service.create_task(
            prompt=input_data.prompt,
            config=config,
        )

        return GenerateVideoOutput(task)
