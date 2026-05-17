"""Video generation service implementation (infrastructure layer).

This module contains the infrastructure implementation that handles
I/O operations, HTTP calls to video generation providers, and
orchestrates between the domain layer and external services.
"""

from typing import Optional

from ...domain.entities.video_task import VideoTask, VideoTaskStatus, TaskNotFoundError
from ...domain.value_objects.common import VideoConfig
from ...domain.ports.video_providers import IVideoProvider, IVideoGenerationService


class VideoGenerationServiceImpl(IVideoGenerationService):
    """Infrastructure implementation of video generation service.

    Handles async I/O operations and HTTP calls to video providers.
    This class belongs in the infrastructure layer, not the domain layer.

    Architecture:
        API -> UseCase -> VideoGenerationServiceImpl -> VideoProvider (HTTP)
                            (application)  (infrastructure)    (infrastructure)
    """

    def __init__(self, provider: IVideoProvider):
        """Initialize with a video provider.

        Args:
            provider: Video provider implementation (e.g., ReplicateVideoProvider).
        """
        self._provider = provider

    async def create_task(
        self,
        prompt: str,
        config: VideoConfig,
    ) -> VideoTask:
        """Create a new video generation task via provider.

        Args:
            prompt: Text description for video generation.
            config: Video configuration (duration, aspect ratio, etc.).

        Returns:
            Created VideoTask entity.

        Raises:
            ValueError: If prompt is empty.
        """
        if not prompt or not prompt.strip():
            raise ValueError("Prompt cannot be empty")

        result = await self._provider.generate_video(
            prompt=prompt,
            negative_prompt=config.negative_prompt,
            duration=config.duration,
            aspect_ratio=config.aspect_ratio.value,
            fps=config.fps,
            quality=config.quality.value,
        )

        return VideoTask(
            task_id=result["task_id"],
            prompt=prompt,
            status=VideoTaskStatus.PENDING,
        )

    async def get_task_status(self, task_id: str) -> VideoTask:
        """Get updated task status from provider.

        Args:
            task_id: The task ID to check.

        Returns:
            Updated VideoTask entity with current status.

        Raises:
            ValueError: If task_id is empty.
        """
        if not task_id or not task_id.strip():
            raise ValueError("Task ID cannot be empty")

        result = await self._provider.get_task_status(task_id)

        if "error" in result and "Task not found" in result.get("error", ""):
            raise TaskNotFoundError(f"Task not found: {task_id}")

        provider_status = result["status"]

        # Convert string status to enum
        status_mapping = {
            "pending": VideoTaskStatus.PENDING,
            "processing": VideoTaskStatus.PROCESSING,
            "completed": VideoTaskStatus.COMPLETED,
            "failed": VideoTaskStatus.FAILED,
        }
        status = status_mapping.get(provider_status, VideoTaskStatus.PENDING)

        task = VideoTask(
            task_id=task_id,
            prompt="",  # Not returned by status check
            status=status,
        )

        if provider_status == "completed":
            task.mark_completed(
                video_url=result.get("video_url", ""),
                thumbnail_url=result.get("thumbnail_url"),
            )
        elif provider_status == "failed":
            task.mark_failed(result.get("error", "Unknown error"))

        return task
