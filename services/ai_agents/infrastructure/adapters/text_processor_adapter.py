"""Text processing adapter - Infrastructure layer.

This adapter handles regex-based text processing operations,
separating technical details from domain logic.
"""

import re
from typing import List

from services.ai_agents.domain.ports import TextProcessorPort


class TextProcessorAdapter:
    """Adapter implementing regex-based text processing.

    This adapter belongs to the infrastructure layer and is responsible
    for all regex operations, keeping the domain layer pure.

    Implements the TextProcessorPort protocol defined in the domain layer.
    """

    _SENTENCE_DELIMITER = r"(?<=[.!?])\s+"
    _HEADING_PATTERN = r"^(#{1,6})\s+(.+)$"

    def __init__(self):
        self._sentence_pattern = re.compile(self._SENTENCE_DELIMITER)
        self._heading_pattern = re.compile(self._HEADING_PATTERN)

    def split_sentences(self, content: str) -> List[str]:
        """Split content into sentences.

        Args:
            content: Text to split.

        Returns:
            List of sentences.
        """
        return self._sentence_pattern.split(content)

    def match_headings(self, content: str) -> List[tuple[str, str]]:
        """Match markdown headings in content.

        Args:
            content: Text containing markdown headings.

        Returns:
            List of tuples (heading_level, heading_text).
        """
        results: List[tuple[str, str]] = []
        for line in content.split("\n"):
            line_stripped = line.strip()
            match = self._heading_pattern.match(line_stripped)
            if match:
                results.append((match.group(1), match.group(2)))
        return results


# Type alias for domain layer compatibility
TextProcessorProtocol = TextProcessorPort


class NoOpTextProcessor:
    """No-operation text processor for testing.

    Used when regex is not needed, splits by newlines only.
    """

    def split_sentences(self, content: str) -> List[str]:
        """Split by newline for testing."""
        return content.split("\n")

    def match_headings(self, content: str) -> List[tuple[str, str]]:
        """Return empty list for testing."""
        return []
