"""Video provider ports (domain layer interfaces).

This module defines the Protocol interfaces for video generation.
Infrastructure implementations must satisfy these protocols.

Architecture:
    API -> UseCase -> IVideoGenerationService (impl in infrastructure)
    (api)   (app)                (infrastructure)

    Infrastructure -> IVideoProvider -> External API
    (infrastructure)    (domain port)
"""

from typing import Protocol, Optional, runtime_checkable

from ..entities.video_task import VideoTask
from ..value_objects.common import VideoConfig


@runtime_checkable
class IVideoProvider(Protocol):
    """Protocol for video generation providers (infrastructure concern).

    This protocol defines the interface that video providers must implement.
    Providers are infrastructure concerns and handle actual HTTP calls.
    """

    @property
    def provider_name(self) -> str: ...

    async def generate_video(self, prompt: str, **kwargs) -> dict: ...
    async def get_task_status(self, task_id: str) -> dict: ...


@runtime_checkable
class IVideoGenerationService(Protocol):
    """Protocol for video generation service (application/infrastructure layer).

    Defines the interface for video generation operations.
    Concrete implementation resides in infrastructure layer.
    """

    async def create_task(
        self,
        prompt: str,
        config: VideoConfig,
    ) -> VideoTask:
        """Create a new video generation task."""
        ...

    async def get_task_status(self, task_id: str) -> VideoTask:
        """Get updated task status from provider."""
        ...
