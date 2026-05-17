"""Kling Video Provider implementation.

This provider integrates with Kling AI API for text-to-video generation.
"""

from typing import Optional
from .base import BaseVideoProvider
from loguru import logger
import httpx


class KlingVideoProvider(BaseVideoProvider):
    """Kling AI text-to-video provider.
    
    Kling AI provides high-quality video generation with smooth motion
    and realistic rendering.
    """
    
    def __init__(self, api_key: str = "", api_secret: str = ""):
        """Initialize Kling provider.
        
        Args:
            api_key: Kling API key.
            api_secret: Kling API secret.
        """
        super().__init__(api_token=api_key, base_url="https://api.kling.ai/v1/videos")
        self.api_secret = api_secret

    @property
    def provider_name(self) -> str:
        return "kling"

    def _generate_auth_token(self) -> str:
        timestamp = str(int(time.time()))
        signature_str = f"{self.api_token}{self.api_secret}{timestamp}"
        signature = hashlib.sha256(signature_str.encode()).hexdigest()
        token = base64.b64encode(
            json.dumps({
                "api_key": self.api_token,
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
        if not self.api_token or not self.api_secret:
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
        if not self.api_token or not self.api_secret:
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
