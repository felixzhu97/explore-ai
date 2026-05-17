"""Tests for Text-to-Text API endpoints."""

import pytest
from fastapi.testclient import TestClient


class TestHealthEndpoint:
    """Tests for health check endpoint."""

    def test_health_check(self, client: TestClient):
        """Test health check returns OK status."""
        response = client.get("/api/text/health")
        assert response.status_code == 200
        data = response.json()
        assert data["status"] == "ok"
        assert "provider" in data
        assert "model" in data
        assert "version" in data


class TestProvidersEndpoint:
    """Tests for providers listing endpoint."""

    def test_list_providers(self, client: TestClient):
        """Test listing all available providers."""
        response = client.get("/api/text/providers")
        assert response.status_code == 200
        data = response.json()
        assert isinstance(data, list)
        assert len(data) >= 3  # openai, anthropic, ollama

        provider_names = [p["name"] for p in data]
        assert "openai" in provider_names
        assert "anthropic" in provider_names
        assert "ollama" in provider_names

    def test_provider_info_structure(self, client: TestClient):
        """Test provider info has correct structure."""
        response = client.get("/api/text/providers")
        data = response.json()
        
        for provider in data:
            assert "name" in provider
            assert "display_name" in provider
            assert "models" in provider
            assert "status" in provider
            assert isinstance(provider["models"], list)


class TestModelsEndpoint:
    """Tests for models listing endpoint."""

    def test_list_all_models(self, client: TestClient):
        """Test listing all models from all providers."""
        response = client.get("/api/text/models")
        assert response.status_code == 200
        data = response.json()
        assert isinstance(data, list)
        assert len(data) > 0

    def test_filter_by_provider(self, client: TestClient):
        """Test filtering models by provider."""
        response = client.get("/api/text/models?provider=openai")
        assert response.status_code == 200
        data = response.json()
        
        for model in data:
            assert model["provider"] == "openai"

    def test_model_info_structure(self, client: TestClient):
        """Test model info has correct structure."""
        response = client.get("/api/text/models")
        data = response.json()
        
        for model in data:
            assert "name" in model
            assert "provider" in model
            assert "description" in model


class TestCompletionEndpoint:
    """Tests for text completion endpoint."""

    def test_complete_missing_prompt(self, client: TestClient):
        """Test completion fails without prompt."""
        response = client.post("/api/text/complete", json={})
        assert response.status_code == 422  # Validation error

    def test_complete_invalid_temperature(self, client: TestClient):
        """Test completion fails with invalid temperature."""
        response = client.post("/api/text/complete", json={
            "prompt": "Hello",
            "temperature": 3.0  # Invalid: > 2.0
        })
        assert response.status_code == 422


class TestChatEndpoint:
    """Tests for chat endpoint."""

    def test_chat_missing_messages(self, client: TestClient):
        """Test chat fails without messages."""
        response = client.post("/api/text/chat", json={})
        assert response.status_code == 422

    def test_chat_empty_messages(self, client: TestClient):
        """Test chat fails with empty messages list."""
        response = client.post("/api/text/chat", json={"messages": []})
        assert response.status_code == 422

    def test_chat_invalid_role(self, client: TestClient):
        """Test chat with invalid message role."""
        response = client.post("/api/text/chat", json={
            "messages": [{"role": "invalid", "content": "Hello"}]
        })
        # Should accept but might not work with invalid role
        assert response.status_code in [200, 500]


class TestSessionEndpoint:
    """Tests for session management endpoints."""

    def test_get_nonexistent_session(self, client: TestClient):
        """Test getting a session that doesn't exist."""
        response = client.get("/api/text/session/nonexistent-id")
        assert response.status_code == 200
        data = response.json()
        assert data["session_id"] == "nonexistent-id"
        assert data["messages"] == []

    def test_clear_nonexistent_session(self, client: TestClient):
        """Test clearing a session that doesn't exist."""
        response = client.delete("/api/text/session/nonexistent-id")
        assert response.status_code == 200
        data = response.json()
        assert data["status"] == "not_found"


class TestResetEndpoint:
    """Tests for LLM reset endpoint."""

    def test_reset_llm(self, client: TestClient):
        """Test resetting LLM cache."""
        response = client.post("/api/text/reset")
        assert response.status_code == 200
        data = response.json()
        assert data["status"] == "success"
