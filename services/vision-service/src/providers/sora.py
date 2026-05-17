"""Sora Video Provider implementation.

This provider integrates with OpenAI's Sora API for text-to-video generation.
"""

from typing import Optional
from .base import BaseVideoProvider
from loguru import logger
import httpx


class SoraVideoProvider(BaseVideoProvider):
    """OpenAI Sora text-to-video provider.
    
    Sora is OpenAI's text-to-video model capable of generating realistic
    and imaginative scenes from text instructions.
    
    API Documentation: https://platform.openai.com/docs/api-reference/videos
    """
    
    def __init__(self):
        self.api_key: str = ""
        self.base_url = "https://api.openai.com/v1"
        self._load_credentials()
    
    def _load_credentials(self):
        """Load API credentials from environment/settings."""
        try:
            from ..core.video_config import get_settings
            settings = get_settings()
            self.api_key = settings.get("OPENAI_API_KEY", "")
        except Exception:
            self.api_key = ""
    
    @property
    def provider_name(self) -> str:
        return "sora"
    
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
        """Generate video using OpenAI Sora.
        
        Args:
            prompt: Text description of the video content.
            negative_prompt: Elements to avoid in the video (not supported by Sora).
            duration: Video duration in seconds (up to 20 seconds).
            aspect_ratio: Video aspect ratio.
            fps: Frames per second.
            quality: Video quality level (standard, high).
            
        Returns:
            Dictionary with task_id and status.
        """
        if not self.api_key:
            raise ValueError("OPENAI_API_KEY is not configured. Please set it in your environment.")
        
        # Sora aspect ratio mapping
        aspect_mapping = {
            "16:9": "1920x1080",
            "9:16": "1080x1920",
            "1:1": "1024x1024",
            "4:3": "1024x768",
        }
        
        # Sora quality mapping
        quality_mapping = {
            "standard": "540p",
            "high": "1080p",
        }
        
        payload = {
            "model": "sora-720p",  # Sora model variant
            "prompt": prompt,
            "duration": duration,
            "resolution": aspect_mapping.get(aspect_ratio, "1920x1080"),
            "quality": quality_mapping.get(quality, "1080p"),
        }
        
        # Remove negative_prompt if provided (Sora doesn't support it)
        # but log it as it might be useful for future versions
        
        try:
            async with httpx.AsyncClient(timeout=180.0) as client:
                response = await client.post(
                    f"{self.base_url}/videos/generations",
                    headers={
                        "Authorization": f"Bearer {self.api_key}",
                        "Content-Type": "application/json",
                    },
                    json=payload
                )
                response.raise_for_status()
                data = response.json()
                
                return {
                    "task_id": data.get("id"),
                    "status": "pending",
                    "message": "Sora video generation started"
                }
                
        except httpx.HTTPStatusError as e:
            logger.error(f"Sora API error: {e.response.text}")
            raise
        except Exception as e:
            logger.error(f"Sora generation error: {e}")
            raise
    
    async def get_task_status(self, task_id: str) -> dict:
        """Get the status of a Sora video generation task.
        
        Args:
            task_id: The task ID from generate_video.
            
        Returns:
            Dictionary with status and video_url if completed.
        """
        if not self.api_key:
            raise ValueError("OPENAI_API_KEY is not configured")
        
        try:
            async with httpx.AsyncClient(timeout=30.0) as client:
                response = await client.get(
                    f"{self.base_url}/videos/{task_id}",
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
                    # Get the video output
                    video_data = data.get("data", [{}])[0]
                    result["video_url"] = video_data.get("url")
                
                elif status == "failed":
                    result["error"] = data.get("error", {}).get("message", "Generation failed")
                
                return result
                
        except httpx.HTTPStatusError as e:
            logger.error(f"Sora API error: {e.response.text}")
            raise
