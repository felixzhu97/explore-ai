"""Video generation domain service.

This module defines the pure domain service for video generation validation.
The interface is a Protocol (duck-typed) - implementations must be provided
by the infrastructure layer.

Domain layer rules:
- No async I/O operations
- No direct provider references
- Only business logic validation
"""


class VideoGenerationService:
    """Pure domain service for video generation validation.

    This class contains only synchronous business logic validation.
    No async operations or infrastructure dependencies are allowed.

    For actual video generation, use VideoGenerationServiceImpl from
    the infrastructure layer.
    """

    @staticmethod
    def validate_prompt(prompt: str) -> None:
        """Validate video generation prompt.

        Raises:
            ValueError: If prompt is empty or invalid.
        """
        if not prompt or not prompt.strip():
            raise ValueError("Prompt cannot be empty")

    @staticmethod
    def validate_task_id(task_id: str) -> None:
        """Validate task ID.

        Raises:
            ValueError: If task_id is empty or invalid.
        """
        if not task_id or not task_id.strip():
            raise ValueError("Task ID cannot be empty")
