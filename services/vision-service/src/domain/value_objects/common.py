"""Consolidated value objects for domain models."""

from dataclasses import dataclass
from enum import Enum
from typing import Optional, Tuple


class AspectRatio(str, Enum):
    RATIO_16_9 = "16:9"
    RATIO_9_16 = "9:16"
    RATIO_1_1 = "1:1"
    RATIO_4_3 = "4:3"


class VideoQuality(str, Enum):
    STANDARD = "standard"
    HIGH = "high"


@dataclass(frozen=True)
class Dimensions:
    """Immutable image dimensions.

    Enforces that dimensions are positive and divisible by 8 (for diffusion models).
    """

    width: int
    height: int

    MIN_DIMENSION = 256
    MAX_DIMENSION = 2048

    def __post_init__(self):
        if self.width < self.MIN_DIMENSION or self.height < self.MIN_DIMENSION:
            raise ValueError(f"Dimensions must be at least {self.MIN_DIMENSION}px")
        if self.width > self.MAX_DIMENSION or self.height > self.MAX_DIMENSION:
            raise ValueError(f"Dimensions cannot exceed {self.MAX_DIMENSION}px")
        if self.width % 8 != 0 or self.height % 8 != 0:
            raise ValueError("Dimensions must be divisible by 8")

    @property
    def aspect_ratio(self) -> float:
        return self.width / self.height

    def scale(self, factor: int) -> "Dimensions":
        """Return new dimensions scaled by factor."""
        return Dimensions(self.width * factor, self.height * factor)

    def to_tuple(self) -> Tuple[int, int]:
        return (self.width, self.height)


@dataclass(frozen=True)
class VideoConfig:
    """Immutable video generation configuration."""

    duration: int
    aspect_ratio: AspectRatio
    fps: int
    quality: VideoQuality
    negative_prompt: Optional[str] = None
    seed: Optional[int] = None

    def __post_init__(self):
        if not 5 <= self.duration <= 10:
            raise ValueError("Duration must be between 5 and 10 seconds")
        if not 24 <= self.fps <= 60:
            raise ValueError("FPS must be between 24 and 60")
