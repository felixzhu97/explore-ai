from abc import ABC, abstractmethod
from typing import Optional
import httpx
from loguru import logger


class BaseVideoProvider(ABC):
    @property
    @abstractmethod
    def provider_name(self) -> str:
        pass

    @abstractmethod
    async def generate_video(
        self,
        prompt: str,
        negative_prompt: Optional[str] = None,
        duration: int = 5,
        aspect_ratio: str = "16:9",
        fps: int = 24,
        quality: str = "high",
        **kwargs
    ) -> dict:
        pass

    @abstractmethod
    async def get_task_status(self, task_id: str) -> dict:
        pass

    async def _make_request(self, method: str, url: str, **kwargs) -> dict:
        async with httpx.AsyncClient(timeout=120.0) as client:
            response = await client.request(method, url, **kwargs)
            response.raise_for_status()
            return response.json()
