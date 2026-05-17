from typing import Optional
from .base import BaseVideoProvider
from ..core.video_config import get_settings
from loguru import logger
import httpx


class ReplicateVideoProvider(BaseVideoProvider):
    def __init__(self):
        self.settings = get_settings()
        self.api_token = self.settings.REPLICATE_API_TOKEN
        self.base_url = "https://api.replicate.com/v1"
        self._headers = {
            "Authorization": f"Bearer {self.api_token}",
            "Content-Type": "application/json",
        }

    @property
    def provider_name(self) -> str:
        return "replicate"

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
        if not self.api_token:
            raise ValueError("REPLICATE_API_TOKEN is not configured")

        resolution_map = {"720p": "720p", "1080p": "1080p"}
        duration_map = {5: 5, 10: 10}

        payload = {
            "version": "s欣581c69f7b9989a89a2c7e31dae6d43f57c02d9a51e7a6c55b41c8c6c35c7e6a",
            "input": {
                "prompt": prompt,
                "duration": duration_map.get(duration, 5),
                "aspect_ratio": aspect_ratio,
                "fps": fps,
                "resolution": resolution_map.get(quality, "720p"),
            }
        }

        if negative_prompt:
            payload["input"]["negative_prompt"] = negative_prompt

        try:
            async with httpx.AsyncClient(timeout=30.0) as client:
                response = await client.post(
                    f"{self.base_url}/predictions",
                    headers=self._headers,
                    json=payload
                )
                response.raise_for_status()
                data = response.json()
                return {
                    "task_id": data["id"],
                    "status": "pending",
                    "urls": data.get("urls", {}),
                    "message": "Video generation started"
                }
        except httpx.HTTPStatusError as e:
            logger.error(f"Replicate API error: {e.response.text}")
            raise

    async def get_task_status(self, task_id: str) -> dict:
        if not self.api_token:
            raise ValueError("REPLICATE_API_TOKEN is not configured")

        try:
            async with httpx.AsyncClient(timeout=30.0) as client:
                response = await client.get(
                    f"{self.base_url}/predictions/{task_id}",
                    headers=self._headers
                )
                response.raise_for_status()
                data = response.json()

                status_map = {
                    "starting": "pending",
                    "processing": "processing",
                    "succeeded": "completed",
                    "failed": "failed",
                    "canceled": "failed",
                }

                result = {
                    "task_id": task_id,
                    "status": status_map.get(data["status"], data["status"]),
                }

                if data["status"] == "succeeded":
                    output = data.get("output", {})
                    if isinstance(output, list):
                        result["video_url"] = output[0] if output else None
                    else:
                        result["video_url"] = output

                elif data["status"] == "failed":
                    result["error"] = data.get("error", "Generation failed")

                return result

        except httpx.HTTPStatusError as e:
            logger.error(f"Replicate API error: {e.response.text}")
            raise
