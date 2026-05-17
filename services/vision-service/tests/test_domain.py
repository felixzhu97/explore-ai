"""Tests for domain layer entities and value objects."""

import pytest
from datetime import datetime
from src.domain.entities.video_task import (
    VideoTask,
    VideoTaskStatus,
    InvalidStateTransitionError,
)
from src.domain.entities.image import ImageGeneration, ImageModel
from src.domain.value_objects.common import Dimensions, VideoConfig, AspectRatio, VideoQuality


class TestVideoTask:
    """Tests for VideoTask entity."""

    def test_create_pending_task(self):
        """Should create a task with PENDING status."""
        task = VideoTask(prompt="Test video")
        assert task.status == VideoTaskStatus.PENDING
        assert task.prompt == "Test video"
        assert task.task_id is not None
        assert task.video_url is None

    def test_mark_processing(self):
        """Should transition from PENDING to PROCESSING."""
        task = VideoTask(prompt="Test video")
        task.mark_processing()
        assert task.status == VideoTaskStatus.PROCESSING

    def test_invalid_processing_transition(self):
        """Should raise error if not in PENDING state."""
        task = VideoTask(prompt="Test video")
        task.mark_processing()
        with pytest.raises(InvalidStateTransitionError):
            task.mark_processing()

    def test_mark_completed_from_pending(self):
        """Should transition from PENDING to COMPLETED."""
        task = VideoTask(prompt="Test video")
        task.mark_completed(
            video_url="https://example.com/video.mp4",
            thumbnail_url="https://example.com/thumb.jpg"
        )

        assert task.status == VideoTaskStatus.COMPLETED
        assert task.video_url == "https://example.com/video.mp4"
        assert task.thumbnail_url == "https://example.com/thumb.jpg"
        assert task.processing_time_seconds is not None

    def test_mark_completed_from_processing(self):
        """Should transition from PROCESSING to COMPLETED."""
        task = VideoTask(prompt="Test video")
        task.mark_processing()
        task.mark_completed(video_url="https://example.com/video.mp4")

        assert task.status == VideoTaskStatus.COMPLETED
        assert task.video_url == "https://example.com/video.mp4"

    def test_mark_failed_from_pending(self):
        """Should transition from PENDING to FAILED with error message."""
        task = VideoTask(prompt="Test video")
        task.mark_failed("Generation failed")

        assert task.status == VideoTaskStatus.FAILED
        assert task.error_message == "Generation failed"

    def test_mark_failed_from_processing(self):
        """Should transition from PROCESSING to FAILED."""
        task = VideoTask(prompt="Test video")
        task.mark_processing()
        task.mark_failed("API error")

        assert task.status == VideoTaskStatus.FAILED
        assert task.error_message == "API error"

    def test_cannot_fail_completed(self):
        """Should raise error if trying to fail a completed task."""
        task = VideoTask(prompt="Test video")
        task.mark_completed("https://example.com/video.mp4")

        with pytest.raises(InvalidStateTransitionError):
            task.mark_failed("Late error")

    def test_cannot_complete_failed(self):
        """Should raise error if trying to complete a failed task."""
        task = VideoTask(prompt="Test video")
        task.mark_failed("Initial failure")

        with pytest.raises(InvalidStateTransitionError):
            task.mark_completed("https://example.com/video.mp4")

    def test_is_terminal_pending(self):
        """Should correctly identify PENDING as non-terminal."""
        task = VideoTask(prompt="Test video")
        assert not task.is_terminal

    def test_is_terminal_processing(self):
        """Should correctly identify PROCESSING as non-terminal."""
        task = VideoTask(prompt="Test video")
        task.mark_processing()
        assert not task.is_terminal

    def test_is_terminal_completed(self):
        """Should correctly identify COMPLETED as terminal."""
        task = VideoTask(prompt="Test video")
        task.mark_completed("https://example.com/video.mp4")
        assert task.is_terminal

    def test_is_terminal_failed(self):
        """Should correctly identify FAILED as terminal."""
        task = VideoTask(prompt="Test video")
        task.mark_failed("Error")
        assert task.is_terminal

    def test_invalid_completed_transition_from_processing_twice(self):
        """Should raise error if trying to complete twice."""
        task = VideoTask(prompt="Test video")
        task.mark_processing()
        task.mark_completed("https://example.com/video1.mp4")

        with pytest.raises(InvalidStateTransitionError):
            task.mark_completed("https://example.com/video2.mp4")


class TestVideoTaskStatus:
    """Tests for VideoTaskStatus enum."""

    def test_all_statuses_exist(self):
        """Should have all expected status values."""
        assert VideoTaskStatus.PENDING.value == "pending"
        assert VideoTaskStatus.PROCESSING.value == "processing"
        assert VideoTaskStatus.COMPLETED.value == "completed"
        assert VideoTaskStatus.FAILED.value == "failed"


class TestImageGeneration:
    """Tests for ImageGeneration entity."""

    def test_create_with_defaults(self):
        """Should create with sensible defaults."""
        gen = ImageGeneration(prompt="A beautiful sunset")

        assert gen.model == ImageModel.SDXL
        assert gen.width == 1024
        assert gen.height == 1024
        assert gen.num_inference_steps == 30
        assert gen.guidance_scale == 7.5
        assert gen.num_images == 1

    def test_custom_parameters(self):
        """Should accept custom parameters."""
        gen = ImageGeneration(
            prompt="A cat",
            model=ImageModel.SD3,
            width=512,
            height=512,
            num_images=2
        )

        assert gen.model == ImageModel.SD3
        assert gen.width == 512
        assert gen.height == 512
        assert gen.num_images == 2

    def test_validation_empty_prompt(self):
        """Should reject empty prompt."""
        with pytest.raises(ValueError, match="Prompt cannot be empty"):
            ImageGeneration(prompt="   ")

    def test_validation_empty_prompt_whitespace(self):
        """Should reject whitespace-only prompt."""
        with pytest.raises(ValueError, match="Prompt cannot be empty"):
            ImageGeneration(prompt="  \t\n  ")

    def test_validation_dimensions_too_small(self):
        """Should reject dimensions below 256."""
        with pytest.raises(ValueError, match="at least 256px"):
            ImageGeneration(prompt="Test", width=128, height=128)

    def test_validation_dimensions_exceed_max(self):
        """Should reject dimensions above 2048."""
        with pytest.raises(ValueError, match="cannot exceed"):
            ImageGeneration(prompt="Test", width=4096, height=4096)

    def test_validation_dimensions_not_divisible_by_8(self):
        """Should reject dimensions not divisible by 8."""
        with pytest.raises(ValueError, match="divisible by 8"):
            ImageGeneration(prompt="Test", width=1001, height=1000)

    def test_validation_inference_steps_too_low(self):
        """Should reject inference steps below 1."""
        with pytest.raises(ValueError, match="between 1 and 150"):
            ImageGeneration(prompt="Test", num_inference_steps=0)

    def test_validation_inference_steps_too_high(self):
        """Should reject inference steps above 150."""
        with pytest.raises(ValueError, match="between 1 and 150"):
            ImageGeneration(prompt="Test", num_inference_steps=200)

    def test_validation_guidance_scale_too_low(self):
        """Should reject guidance scale below 1.0."""
        with pytest.raises(ValueError, match="between 1.0 and 20.0"):
            ImageGeneration(prompt="Test", guidance_scale=0.5)

    def test_validation_guidance_scale_too_high(self):
        """Should reject guidance scale above 20.0."""
        with pytest.raises(ValueError, match="between 1.0 and 20.0"):
            ImageGeneration(prompt="Test", guidance_scale=25.0)

    def test_validation_num_images_too_low(self):
        """Should reject num_images below 1."""
        with pytest.raises(ValueError, match="between 1 and 4"):
            ImageGeneration(prompt="Test", num_images=0)

    def test_validation_num_images_too_high(self):
        """Should reject num_images above 4."""
        with pytest.raises(ValueError, match="between 1 and 4"):
            ImageGeneration(prompt="Test", num_images=5)

    def test_mark_completed(self):
        """Should set completed_at timestamp."""
        gen = ImageGeneration(prompt="Test")
        assert gen.completed_at is None

        gen.mark_completed()
        assert gen.completed_at is not None

    def test_duration_ms_none_before_completion(self):
        """Should return None before completion."""
        gen = ImageGeneration(prompt="Test")
        assert gen.duration_ms is None

    def test_duration_ms_after_completion(self):
        """Should calculate processing duration after completion."""
        gen = ImageGeneration(prompt="Test")
        gen.mark_completed()
        assert gen.duration_ms is not None
        assert gen.duration_ms >= 0

    def test_generation_id_is_unique(self):
        """Should generate unique generation IDs."""
        gen1 = ImageGeneration(prompt="Test 1")
        gen2 = ImageGeneration(prompt="Test 2")
        assert gen1.generation_id != gen2.generation_id

    def test_negative_prompt_default(self):
        """Should have sensible default negative prompt."""
        gen = ImageGeneration(prompt="Test")
        assert "blurry" in gen.negative_prompt
        assert "distorted" in gen.negative_prompt


class TestImageModel:
    """Tests for ImageModel enum."""

    def test_all_models_exist(self):
        """Should have all expected model values."""
        assert ImageModel.SD3.value == "sd3"
        assert ImageModel.SDXL.value == "sdxl"
        assert ImageModel.SD35_MEDIUM.value == "sd35_medium"
        assert ImageModel.SD35_LARGE.value == "sd35_large"


class TestDimensions:
    """Tests for Dimensions value object."""

    def test_create_valid_dimensions(self):
        """Should create with valid dimensions."""
        dims = Dimensions(width=1024, height=768)
        assert dims.width == 1024
        assert dims.height == 768

    def test_validation_too_small_width(self):
        """Should reject width below minimum."""
        with pytest.raises(ValueError):
            Dimensions(width=128, height=1024)

    def test_validation_too_small_height(self):
        """Should reject height below minimum."""
        with pytest.raises(ValueError):
            Dimensions(width=1024, height=128)

    def test_validation_not_divisible_by_8(self):
        """Should reject dimensions not divisible by 8."""
        with pytest.raises(ValueError):
            Dimensions(width=1001, height=768)

    def test_validation_exceeds_max(self):
        """Should reject dimensions above maximum."""
        with pytest.raises(ValueError):
            Dimensions(width=4096, height=4096)

    def test_aspect_ratio_16_9(self):
        """Should calculate aspect ratio for 16:9."""
        dims = Dimensions(width=1920, height=1080)
        assert dims.aspect_ratio == pytest.approx(16/9, rel=0.01)

    def test_aspect_ratio_9_16(self):
        """Should calculate aspect ratio for 9:16."""
        dims = Dimensions(width=1080, height=1920)
        assert dims.aspect_ratio == pytest.approx(9/16, rel=0.01)

    def test_aspect_ratio_1_1(self):
        """Should calculate aspect ratio for 1:1."""
        dims = Dimensions(width=1024, height=1024)
        assert dims.aspect_ratio == pytest.approx(1.0, rel=0.01)

    def test_scale_integer(self):
        """Should return scaled dimensions with integer."""
        dims = Dimensions(width=512, height=512)
        scaled = dims.scale(2)

        assert scaled.width == 1024
        assert scaled.height == 1024
        assert isinstance(scaled, Dimensions)

    def test_scale_float(self):
        """Should return scaled dimensions with float."""
        dims = Dimensions(width=1024, height=768)
        scaled = dims.scale(0.5)

        assert scaled.width == 512
        assert scaled.height == 384

    def test_to_tuple(self):
        """Should convert to tuple."""
        dims = Dimensions(width=1024, height=768)
        assert dims.to_tuple() == (1024, 768)

    def test_immutability(self):
        """Should be immutable."""
        dims = Dimensions(width=1024, height=768)

        with pytest.raises(AttributeError):
            dims.width = 2048

    def test_immutability_height(self):
        """Should be immutable for height."""
        dims = Dimensions(width=1024, height=768)

        with pytest.raises(AttributeError):
            dims.height = 576

    def test_equality(self):
        """Should support equality comparison."""
        dims1 = Dimensions(width=1024, height=768)
        dims2 = Dimensions(width=1024, height=768)
        assert dims1 == dims2


class TestVideoConfig:
    """Tests for VideoConfig value object."""

    def test_create_valid_config(self):
        """Should create with valid configuration."""
        config = VideoConfig(
            duration=5,
            aspect_ratio=AspectRatio.RATIO_16_9,
            fps=24,
            quality=VideoQuality.HIGH
        )

        assert config.duration == 5
        assert config.aspect_ratio == AspectRatio.RATIO_16_9
        assert config.fps == 24
        assert config.quality == VideoQuality.HIGH

    def test_validation_duration_too_low(self):
        """Should reject duration below minimum."""
        with pytest.raises(ValueError, match="Duration must be between"):
            VideoConfig(duration=4, aspect_ratio=AspectRatio.RATIO_16_9, fps=24, quality=VideoQuality.HIGH)

    def test_validation_duration_too_high(self):
        """Should reject duration above maximum."""
        with pytest.raises(ValueError, match="Duration must be between"):
            VideoConfig(duration=15, aspect_ratio=AspectRatio.RATIO_16_9, fps=24, quality=VideoQuality.HIGH)

    def test_validation_fps_too_low(self):
        """Should reject FPS below minimum."""
        with pytest.raises(ValueError, match="FPS must be between"):
            VideoConfig(duration=5, aspect_ratio=AspectRatio.RATIO_16_9, fps=20, quality=VideoQuality.HIGH)

    def test_validation_fps_too_high(self):
        """Should reject FPS above maximum."""
        with pytest.raises(ValueError, match="FPS must be between"):
            VideoConfig(duration=5, aspect_ratio=AspectRatio.RATIO_16_9, fps=120, quality=VideoQuality.HIGH)

    def test_optional_negative_prompt(self):
        """Should accept optional negative prompt."""
        config = VideoConfig(
            duration=5,
            aspect_ratio=AspectRatio.RATIO_16_9,
            fps=24,
            quality=VideoQuality.HIGH,
            negative_prompt="blurry"
        )

        assert config.negative_prompt == "blurry"

    def test_optional_seed(self):
        """Should accept optional seed."""
        config = VideoConfig(
            duration=5,
            aspect_ratio=AspectRatio.RATIO_16_9,
            fps=24,
            quality=VideoQuality.HIGH,
            seed=42
        )

        assert config.seed == 42

    def test_immutability(self):
        """Should be immutable."""
        config = VideoConfig(
            duration=5,
            aspect_ratio=AspectRatio.RATIO_16_9,
            fps=24,
            quality=VideoQuality.HIGH
        )

        with pytest.raises(AttributeError):
            config.duration = 10

    def test_immutability_fps(self):
        """Should be immutable for fps."""
        config = VideoConfig(
            duration=5,
            aspect_ratio=AspectRatio.RATIO_16_9,
            fps=24,
            quality=VideoQuality.HIGH
        )

        with pytest.raises(AttributeError):
            config.fps = 30


class TestAspectRatio:
    """Tests for AspectRatio enum."""

    def test_all_ratios_exist(self):
        """Should have all expected aspect ratio values."""
        assert AspectRatio.RATIO_16_9.value == "16:9"
        assert AspectRatio.RATIO_9_16.value == "9:16"
        assert AspectRatio.RATIO_1_1.value == "1:1"
        assert AspectRatio.RATIO_4_3.value == "4:3"


class TestVideoQuality:
    """Tests for VideoQuality enum."""

    def test_all_qualities_exist(self):
        """Should have all expected quality values."""
        assert VideoQuality.STANDARD.value == "standard"
        assert VideoQuality.HIGH.value == "high"
