"""Pytest configuration and fixtures."""

import pytest
from fastapi.testclient import TestClient
import sys
from pathlib import Path

# Add src to path
sys.path.insert(0, str(Path(__file__).parent.parent / "src"))

from main import app


@pytest.fixture
def client():
    """Create a test client."""
    return TestClient(app)


@pytest.fixture
def sample_messages():
    """Sample chat messages for testing."""
    return [
        {"role": "user", "content": "Hello, how are you?"},
    ]


@pytest.fixture
def sample_system_prompt():
    """Sample system prompt for testing."""
    return "You are a helpful assistant that speaks in a friendly tone."
