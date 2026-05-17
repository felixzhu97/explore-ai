"""Video Generation Tools for AI Agents Service."""

import httpx
from typing import Optional

from langchain_core.tools import tool


VIDEO_SERVICE_URL = "http://localhost:8003"


@tool("video_generate")
def video_generate(
    prompt: str,
    negative_prompt: Optional[str] = None,
    duration: int = 5,
    aspect_ratio: str = "16:9",
    fps: int = 24,
    quality: str = "high",
    model: str = "kling-v1-5",
) -> str:
    """Generate a video from text description.
    
    Creates a video based on a textual description of the scene.
    Use this when you need to:
    - Create video content from text prompts
    - Generate animations or scenes
    - Produce visual content for applications
    - Create promotional or educational videos
    
    Args:
        prompt: Detailed text description of the video content.
                Be specific about subjects, actions, environment, and mood.
        negative_prompt: Elements to avoid in the video (optional).
        duration: Video duration in seconds (5-10 seconds).
        aspect_ratio: Video aspect ratio. Options: "16:9", "9:16", "1:1", "4:3".
        fps: Frames per second (24-60, default 24).
        quality: Video quality. Options: "standard", "high".
        model: Model to use. Options: "kling-v1-0", "kling-v1-5", "kling".
    
    Returns:
        A message with task_id and status for video generation.
        Use video_check_status with the task_id to get the result.
    """
    try:
        with httpx.Client(timeout=30.0) as client:
            response = client.post(
                f"{VIDEO_SERVICE_URL}/video/generate",
                json={
                    "prompt": prompt,
                    "negative_prompt": negative_prompt,
                    "duration": duration,
                    "aspect_ratio": aspect_ratio,
                    "fps": fps,
                    "quality": quality,
                    "model": model,
                }
            )
            
            if response.status_code == 200:
                data = response.json()
                return f"""Video generation task created successfully.

Task Details:
- Task ID: {data['task_id']}
- Status: {data['status']}
- Message: {data['message']}
- Created At: {data['created_at']}

Use video_check_status tool with task_id "{data['task_id']}" to check the generation progress and get the video URL when completed.
"""
            else:
                return f"Video generation failed with status code: {response.status_code}\n{response.text}"
                
    except httpx.ConnectError:
        return "Error: Cannot connect to Video service. Please ensure the Video service is running on port 8003."
    except Exception as e:
        return f"Error during video generation: {str(e)}"


@tool("video_generate_advanced")
def video_generate_advanced(
    prompt: str,
    duration: int = 5,
    aspect_ratio: str = "16:9",
    fps: int = 24,
    quality: str = "high",
    style: str = "none",
    seed: Optional[int] = None,
    cfg_scale: float = 7.5,
    motion_intensity: float = 1.0,
) -> str:
    """Generate a video with advanced parameters.
    
    Creates a video with fine-tuned control over generation parameters.
    Use this when you need precise control over the video output.
    
    Args:
        prompt: Detailed text description of the video content.
        duration: Video duration in seconds (5-10 seconds).
        aspect_ratio: Video aspect ratio. Options: "16:9", "9:16", "1:1", "4:3".
        fps: Frames per second (24-60).
        quality: Video quality. Options: "standard", "high".
        style: Visual style preset. Options: "realistic", "animation", "cinematic", "abstract", "none".
        seed: Random seed for reproducible generation (optional).
        cfg_scale: Guidance scale for prompt adherence (1.0-20.0, default 7.5).
        motion_intensity: Motion intensity multiplier (0.1-2.0, default 1.0).
    
    Returns:
        A message with task_id and status for video generation.
        Use video_check_status with the task_id to get the result.
    """
    try:
        with httpx.Client(timeout=30.0) as client:
            response = client.post(
                f"{VIDEO_SERVICE_URL}/video/generate/advanced",
                json={
                    "prompt": prompt,
                    "duration": duration,
                    "aspect_ratio": aspect_ratio,
                    "fps": fps,
                    "quality": quality,
                    "style": style,
                    "seed": seed,
                    "cfg_scale": cfg_scale,
                    "motion_intensity": motion_intensity,
                }
            )
            
            if response.status_code == 200:
                data = response.json()
                return f"""Advanced video generation task created successfully.

Task Details:
- Task ID: {data['task_id']}
- Status: {data['status']}
- Message: {data['message']}
- Created At: {data['created_at']}

Advanced Settings Applied:
- Style: {style}
- CFG Scale: {cfg_scale}
- Motion Intensity: {motion_intensity}
{f'- Seed: {seed}' if seed else ''}

Use video_check_status tool with task_id "{data['task_id']}" to check the generation progress and get the video URL when completed.
"""
            else:
                return f"Video generation failed with status code: {response.status_code}\n{response.text}"
                
    except httpx.ConnectError:
        return "Error: Cannot connect to Video service. Please ensure the Video service is running on port 8003."
    except Exception as e:
        return f"Error during video generation: {str(e)}"


@tool("video_check_status")
def video_check_status(task_id: str) -> str:
    """Check the status of a video generation task.
    
    Retrieves the current status and result of a video generation task.
    Use this to poll for completion or check error details.
    
    Args:
        task_id: The task ID returned from video_generate or video_generate_advanced.
    
    Returns:
        Detailed status information including:
        - Current status (pending, processing, completed, failed)
        - Video URL (when completed)
        - Thumbnail URL (when completed)
        - Error message (if failed)
        - Processing time
    """
    try:
        with httpx.Client(timeout=30.0) as client:
            response = client.get(f"{VIDEO_SERVICE_URL}/video/status/{task_id}")
            
            if response.status_code == 200:
                data = response.json()
                
                status_emoji = {
                    "pending": "⏳",
                    "processing": "🔄",
                    "completed": "✅",
                    "failed": "❌",
                }.get(data['status'], "❓")
                
                output = f"""Video Generation Status {status_emoji}

Task ID: {data['task_id']}
Status: {data['status']}
"""
                
                if data['status'] == 'completed':
                    output += f"""
Video URL: {data.get('video_url', 'N/A')}
Thumbnail URL: {data.get('thumbnail_url', 'N/A')}
Processing Time: {data.get('processing_time_seconds', 0):.1f} seconds
"""
                elif data['status'] == 'failed':
                    output += f"\nError: {data.get('error', 'Unknown error')}"
                elif data['status'] == 'processing':
                    output += "\nVideo is still being generated. Please check again in a few seconds."
                else:
                    output += "\nTask is queued for processing."
                
                return output
            elif response.status_code == 404:
                return f"Task not found: {task_id}. Please verify the task ID."
            else:
                return f"Failed to check status. Status: {response.status_code}"
                
    except httpx.ConnectError:
        return "Error: Cannot connect to Video service. Please ensure the Video service is running on port 8003."
    except Exception as e:
        return f"Error checking video status: {str(e)}"


@tool("video_get_providers")
def video_get_providers() -> str:
    """Get information about available video generation providers.
    
    Returns details about each supported video generation provider including:
    - Supported features
    - Model capabilities
    - Current configuration
    
    Returns:
        Formatted information about all video generation providers.
    """
    try:
        settings_info = {
            "kling": {
                "name": "Kling AI",
                "models": ["kling-v1-0", "kling-v1-5"],
                "features": [
                    "Text-to-Video generation",
                    "High-quality output",
                    "Variable duration (5-10s)",
                    "Multiple aspect ratios",
                    "Adjustable FPS",
                    "Motion intensity control",
                ],
                "max_duration": 10,
                "supported_aspect_ratios": ["16:9", "9:16", "1:1", "4:3"],
            },
            "replicate": {
                "name": "Replicate (Kling via API)",
                "models": ["kling"],
                "features": [
                    "Text-to-Video generation",
                    "Cloud-based processing",
                    "Quick generation",
                ],
                "max_duration": 5,
                "supported_aspect_ratios": ["16:9"],
            },
            "mock": {
                "name": "Mock Provider (Development)",
                "models": ["mock-v1"],
                "features": [
                    "Mock generation for testing",
                    "Simulated delays",
                    "No actual video generation",
                ],
                "max_duration": 10,
                "supported_aspect_ratios": ["16:9", "9:16", "1:1"],
            },
        }
        
        output = "Available Video Generation Providers\n"
        output += "=" * 60 + "\n\n"
        
        for key, info in settings_info.items():
            output += f"## {info['name']}\n"
            output += f"- Provider ID: `{key}`\n"
            output += f"- Max Duration: {info['max_duration']}s\n"
            output += "- Models:\n"
            for model in info['models']:
                output += f"  - {model}\n"
            output += "- Features:\n"
            for feature in info['features']:
                output += f"  - {feature}\n"
            output += "- Aspect Ratios:\n"
            for ratio in info['supported_aspect_ratios']:
                output += f"  - {ratio}\n"
            output += "\n"
        
        return output
        
    except Exception as e:
        return f"Error getting provider information: {str(e)}"


def get_all_video_tools():
    """Get all video generation tools.
    
    Returns:
        List of all video-related LangChain tools.
    """
    return [
        video_generate,
        video_generate_advanced,
        video_check_status,
        video_get_providers,
    ]
