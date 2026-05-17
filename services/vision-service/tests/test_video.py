import pytest
from httpx import AsyncClient, ASGITransport
from src.main import app


@pytest.fixture
async def client():
    transport = ASGITransport(app=app)
    async with AsyncClient(transport=transport, base_url="http://test") as ac:
        yield ac


@pytest.mark.asyncio
async def test_health(client):
    response = await client.get("/health")
    assert response.status_code == 200
    assert response.json() == {"status": "ok"}


@pytest.mark.asyncio
async def test_generate_video(client):
    response = await client.post(
        "/video/generate",
        json={
            "prompt": "A cat playing piano in a moonlit room",
            "duration": 5,
            "aspect_ratio": "16:9",
            "fps": 24,
            "quality": "high"
        }
    )
    assert response.status_code == 200
    data = response.json()
    assert "task_id" in data
    assert data["status"] == "pending"
    assert "created_at" in data


@pytest.mark.asyncio
async def test_generate_video_validation(client):
    response = await client.post(
        "/video/generate",
        json={"prompt": ""}
    )
    assert response.status_code == 422


@pytest.mark.asyncio
async def test_video_status(client):
    create_response = await client.post(
        "/video/generate",
        json={"prompt": "Sunset over ocean waves"}
    )
    task_id = create_response.json()["task_id"]

    status_response = await client.get(f"/video/status/{task_id}")
    assert status_response.status_code == 200
    data = status_response.json()
    assert data["task_id"] == task_id


@pytest.mark.asyncio
async def test_video_status_not_found(client):
    response = await client.get("/video/status/nonexistent_task_xyz")
    assert response.status_code == 404


@pytest.mark.asyncio
async def test_advanced_video_generation(client):
    response = await client.post(
        "/video/generate/advanced",
        json={
            "prompt": "A robot dancing in a futuristic city",
            "style": "cinematic",
            "cfg_scale": 7.5,
            "motion_intensity": 1.2
        }
    )
    assert response.status_code == 200
    data = response.json()
    assert "task_id" in data
