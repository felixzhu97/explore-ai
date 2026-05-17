"""Runway Gen-3 Video Provider implementation.

This provider integrates with Runway's Gen-3 Alpha API for text-to-video generation.
"""

from typing import Optional
from .base import BaseVideoProvider
from loguru import logger
import httpx
import asyncio


class RunwayVideoProvider(BaseVideoProvider):
    """Runway Gen-3 Alpha text-to-video provider.
    
    Runway provides state-of-the-art video generation models including Gen-3 Alpha,
    which offers high-quality, motion-rich video generation from text prompts.
    
    API Documentation: https://docs.runwayml.com/
    """
    
    def __init__(self):
        self.api_key: str = ""
        self.base_url = "https://api.runwayml.com/v1"
        self._load_credentials()
    
    def _load_credentials(self):
        """Load API credentials from environment/settings."""
        try:
            from ..core.video_config import get_settings
            settings = get_settings()
            self.api_key = settings.get("RUNWAY_API_KEY", "")
        except Exception:
            self.api_key = ""
    
    @property
    def provider_name(self) -> str:
        return "runway"
    
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
        """Generate video using Runway Gen-3.
        
        Args:
            prompt: Text description of the video content.
            negative_prompt: Elements to avoid in the video.
            duration: Video duration in seconds (5-10).
            aspect_ratio: Video aspect ratio.
            fps: Frames per second.
            quality: Video quality level.
            
        Returns:
            Dictionary with task_id and status.
        """
        if not self.api_key:
            raise ValueError("RUNWAY_API_KEY is not configured. Please set it in your environment.")
        
        # Map aspect ratio to Runway format
        aspect_mapping = {
            "16:9": "16:9",
            "9:16": "9:16", 
            "1:1": "1:1",
            "4:3": "4:3",
        }
        runway_aspect = aspect_mapping.get(aspect_ratio, "16:9")
        
        payload = {
            "prompt": prompt,
            "duration": duration,
            "aspect_ratio": runway_aspect,
            "fps": fps,
        }
        
        if negative_prompt:
            payload["negative_prompt"] = negative_prompt
        
        try:
            async with httpx.AsyncClient(timeout=120.0) as client:
                # Create task
                response = await client.post(
                    f"{self.base_url}/gen3_turbo/text_to_video",
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
                    "message": "Runway Gen-3 video generation started"
                }
                
        except httpx.HTTPStatusError as e:
            logger.error(f"Runway API error: {e.response.text}")
            raise
        except Exception as e:
            logger.error(f"Runway generation error: {e}")
            raise
    
    async def get_task_status(self, task_id: str) -> dict:
        """Get the status of a Runway video generation task.
        
        Args:
            task_id: The task ID from generate_video.
            
        Returns:
            Dictionary with status and video_url if completed.
        """
        if not self.api_key:
            raise ValueError("RUNWAY_API_KEY is not configured")
        
        try:
            async with httpx.AsyncClient(timeout=30.0) as client:
                response = await client.get(
                    f"{self.base_url}/tasks/{task_id}",
                    headers={
                        "Authorization": f"Bearer {self.api_key}",
                    }
                )
                response.raise_for_status()
                data = response.json()
                
                status_mapping = {
                    "pending": "pending",
                    "processing": "processing",
                    "completed": "completed",
                    "failed": "failed",
                }
                
                status = status_mapping.get(data.get("status", ""), "processing")
                
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
            logger.error(f"Runway API error: {e.response.text}")
            raise
