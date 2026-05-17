from fastapi import APIRouter, HTTPException
from ..schemas.video import (
    VideoGenerateRequest,
    VideoGenerateResponse,
    VideoTaskResponse,
    AdvancedVideoRequest,
)
from ..providers import get_provider
from loguru import logger
from datetime import datetime

router = APIRouter(prefix="/video", tags=["Video Generation"])


@router.post("/generate", response_model=VideoGenerateResponse)
async def generate_video(request: VideoGenerateRequest):
    provider = get_provider()
    try:
        result = await provider.generate_video(
            prompt=request.prompt,
            negative_prompt=request.negative_prompt,
            duration=request.duration,
            aspect_ratio=request.aspect_ratio.value,
            fps=request.fps,
            quality=request.quality.value,
        )
        return VideoGenerateResponse(
            task_id=result["task_id"],
            status=result.get("status", "pending"),
            message=result.get("message", "Video generation started"),
            created_at=datetime.now().isoformat()
        )
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))
    except Exception as e:
        logger.error(f"Video generation error: {e}")
        raise HTTPException(status_code=500, detail=f"Video generation failed: {str(e)}")


@router.post("/generate/advanced", response_model=VideoGenerateResponse)
async def generate_video_advanced(request: AdvancedVideoRequest):
    provider = get_provider()
    try:
        result = await provider.generate_video(
            prompt=request.prompt,
            negative_prompt=request.negative_prompt,
            duration=request.duration,
            aspect_ratio=request.aspect_ratio.value,
            fps=request.fps,
            quality=request.quality.value,
            style=request.style.value,
            seed=request.seed,
            cfg_scale=request.cfg_scale,
            motion_intensity=request.motion_intensity,
        )
        return VideoGenerateResponse(
            task_id=result["task_id"],
            status=result.get("status", "pending"),
            message=result.get("message", "Video generation started"),
            created_at=datetime.now().isoformat()
        )
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))
    except Exception as e:
        logger.error(f"Video generation error: {e}")
        raise HTTPException(status_code=500, detail=f"Video generation failed: {str(e)}")


@router.get("/status/{task_id}", response_model=VideoTaskResponse)
async def get_video_status(task_id: str):
    provider = get_provider()
    try:
        result = await provider.get_task_status(task_id)
        if "error" in result and result.get("error") == "Task not found":
            raise HTTPException(status_code=404, detail="Task not found")
        return VideoTaskResponse(
            task_id=result["task_id"],
            status=result["status"],
            video_url=result.get("video_url"),
            thumbnail_url=result.get("thumbnail_url"),
            error=result.get("error"),
            processing_time_seconds=result.get("processing_time_seconds"),
        )
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))
    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"Status check error: {e}")
        raise HTTPException(status_code=500, detail=f"Status check failed: {str(e)}")
