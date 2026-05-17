from typing import Optional
from .base import BaseVideoProvider
from loguru import logger
import uuid
import asyncio
from datetime import datetime


class MockVideoProvider(BaseVideoProvider):
    _tasks: dict[str, dict] = {}

    @property
    def provider_name(self) -> str:
        return "mock"

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
        task_id = f"mock_task_{uuid.uuid4().hex[:12]}"
        self._tasks[task_id] = {
            "task_id": task_id,
            "status": "pending",
            "prompt": prompt,
            "created_at": datetime.now().isoformat(),
            "duration": duration,
            "aspect_ratio": aspect_ratio,
        }
        asyncio.create_task(self._simulate_processing(task_id))
        return {
            "task_id": task_id,
            "status": "pending",
            "message": f"Mock video generation task created. Use task_id to poll status."
        }

    async def _simulate_processing(self, task_id: str):
        await asyncio.sleep(3)
        if task_id in self._tasks:
            self._tasks[task_id]["status"] = "processing"
        await asyncio.sleep(2)
        if task_id in self._tasks:
            self._tasks[task_id].update({
                "status": "completed",
                "video_url": f"https://example.com/videos/{task_id}.mp4",
                "thumbnail_url": f"https://example.com/thumbnails/{task_id}.jpg",
                "processing_time_seconds": 5.0,
            })

    async def get_task_status(self, task_id: str) -> dict:
        if task_id not in self._tasks:
            return {"error": "Task not found"}
        task = self._tasks[task_id]
        response = {
            "task_id": task_id,
            "status": task["status"],
        }
        if task["status"] == "completed":
            response.update({
                "video_url": task.get("video_url"),
                "thumbnail_url": task.get("thumbnail_url"),
                "processing_time_seconds": task.get("processing_time_seconds"),
            })
        elif task["status"] == "failed":
            response["error"] = task.get("error", "Unknown error")
        return response
