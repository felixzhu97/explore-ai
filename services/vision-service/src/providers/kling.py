from typing import Optional
from .base import BaseVideoProvider
from ..core.video_config import get_settings
from loguru import logger
import httpx
import time
import hashlib
import base64
import json


class KlingVideoProvider(BaseVideoProvider):
    def __init__(self):
        self.settings = get_settings()
        self.api_key = self.settings.KLING_API_KEY
        self.api_secret = self.settings.KLING_API_SECRET
        self.base_url = "https://api.kling.ai/v1/videos"

    @property
    def provider_name(self) -> str:
        return "kling"

    def _generate_auth_token(self) -> str:
        timestamp = str(int(time.time()))
        signature_str = f"{self.api_key}{self.api_secret}{timestamp}"
        signature = hashlib.sha256(signature_str.encode()).hexdigest()
        token = base64.b64encode(
            json.dumps({
                "api_key": self.api_key,
                "timestamp": timestamp,
                "signature": signature
            }).encode()
        ).decode()
        return token

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
        if not self.api_key or not self.api_secret:
            raise ValueError("KLING_API_KEY and KLING_API_SECRET are not configured")

        token = self._generate_auth_token()
        headers = {
            "Authorization": f"Bearer {token}",
            "Content-Type": "application/json",
        }

        payload = {
            "prompt": prompt,
            "duration": duration,
            "aspect_ratio": aspect_ratio,
            "fps": fps,
            "quality": quality,
        }

        if negative_prompt:
            payload["negative_prompt"] = negative_prompt

        try:
            async with httpx.AsyncClient(timeout=60.0) as client:
                response = await client.post(
                    f"{self.base_url}/text2video",
                    headers=headers,
                    json=payload
                )
                response.raise_for_status()
                data = response.json()
                return {
                    "task_id": data["data"]["task_id"],
                    "status": "pending",
                    "message": "Kling video generation started"
                }
        except httpx.HTTPStatusError as e:
            logger.error(f"Kling API error: {e.response.text}")
            raise

    async def get_task_status(self, task_id: str) -> dict:
        if not self.api_key or not self.api_secret:
            raise ValueError("KLING_API_KEY and KLING_API_SECRET are not configured")

        token = self._generate_auth_token()
        headers = {
            "Authorization": f"Bearer {token}",
        }

        try:
            async with httpx.AsyncClient(timeout=30.0) as client:
                response = await client.get(
                    f"{self.base_url}/text2video/{task_id}",
                    headers=headers
                )
                response.raise_for_status()
                data = response.json()

                result = {
                    "task_id": task_id,
                    "status": data["data"]["task_status"],
                }

                if result["status"] == "completed":
                    result["video_url"] = data["data"].get("video_url")
                    result["thumbnail_url"] = data["data"].get("cover_image_url")

                elif result["status"] == "failed":
                    result["error"] = data["data"].get("fail_reason", "Generation failed")

                return result

        except httpx.HTTPStatusError as e:
            logger.error(f"Kling API error: {e.response.text}")
            raise
