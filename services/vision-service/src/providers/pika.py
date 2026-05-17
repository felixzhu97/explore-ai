"""Pika Labs Video Provider implementation.

This provider integrates with Pika Labs API for text-to-video generation.
"""

from typing import Optional
from .base import BaseVideoProvider
from loguru import logger
import httpx


class PikaVideoProvider(BaseVideoProvider):
    """Pika Labs text-to-video provider.
    
    Pika Labs provides AI-powered video generation with unique artistic styles
    and smooth motion capabilities.
    
    API Documentation: https://docs.pika.art/
    """
    
    def __init__(self):
        self.api_key: str = ""
        self.base_url = "https://api.pika.art/v1"
        self._load_credentials()
    
    def _load_credentials(self):
        """Load API credentials from environment/settings."""
        try:
            from ..core.video_config import get_settings
            settings = get_settings()
            self.api_key = settings.get("PIKA_API_KEY", "")
        except Exception:
            self.api_key = ""
    
    @property
    def provider_name(self) -> str:
        return "pika"
    
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
        """Generate video using Pika Labs.
        
        Args:
            prompt: Text description of the video content.
            negative_prompt: Elements to avoid in the video.
            duration: Video duration in seconds.
            aspect_ratio: Video aspect ratio.
            fps: Frames per second.
            quality: Video quality level.
            
        Returns:
            Dictionary with task_id and status.
        """
        if not self.api_key:
            raise ValueError("PIKA_API_KEY is not configured. Please set it in your environment.")
        
        # Pika specific parameters
        aspect_mapping = {
            "16:9": "16:9",
            "9:16": "9:16",
            "1:1": "1:1",
            "4:3": "4:3",
        }
        
        quality_mapping = {
            "standard": "standard",
            "high": "hd",
        }
        
        payload = {
            "prompt": prompt,
            "duration": duration,
            "aspect_ratio": aspect_mapping.get(aspect_ratio, "16:9"),
            "fps": fps,
            "quality": quality_mapping.get(quality, "standard"),
        }
        
        if negative_prompt:
            payload["negative_prompt"] = negative_prompt
        
        try:
            async with httpx.AsyncClient(timeout=120.0) as client:
                response = await client.post(
                    f"{self.base_url}/generate",
                    headers={
                        "Authorization": f"Bearer {self.api_key}",
                        "Content-Type": "application/json",
                    },
                    json=payload
                )
                response.raise_for_status()
                data = response.json()
                
                return {
                    "task_id": data.get("id", data.get("task_id")),
                    "status": "pending",
                    "message": "Pika video generation started"
                }
                
        except httpx.HTTPStatusError as e:
            logger.error(f"Pika API error: {e.response.text}")
            raise
        except Exception as e:
            logger.error(f"Pika generation error: {e}")
            raise
    
    async def get_task_status(self, task_id: str) -> dict:
        """Get the status of a Pika video generation task.
        
        Args:
            task_id: The task ID from generate_video.
            
        Returns:
            Dictionary with status and video_url if completed.
        """
        if not self.api_key:
            raise ValueError("PIKA_API_KEY is not configured")
        
        try:
            async with httpx.AsyncClient(timeout=30.0) as client:
                response = await client.get(
                    f"{self.base_url}/generate/{task_id}",
                    headers={
                        "Authorization": f"Bearer {self.api_key}",
                    }
                )
                response.raise_for_status()
                data = response.json()
                
                status = data.get("status", "processing")
                
                result = {
                    "task_id": task_id,
                    "status": status,
                }
                
                if status == "completed":
                    result["video_url"] = data.get("output", {}).get("video_url")
                    result["thumbnail_url"] = data.get("output", {}).get("thumbnail_url")
                
                elif status == "failed":
                    result["error"] = data.get("error", "Generation failed")
                
                return result
                
        except httpx.HTTPStatusError as e:
            logger.error(f"Pika API error: {e.response.text}")
            raise
