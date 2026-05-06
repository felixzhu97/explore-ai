import pytest
from fastapi.testclient import TestClient
from httpx import AsyncClient, ASGITransport
from PIL import Image
import io


@pytest.fixture
def sample_image_bytes():
    img = Image.new("RGB", (100, 100), color="red")
    buf = io.BytesIO()
    img.save(buf, format="JPEG")
    buf.seek(0)
    return buf.getvalue()


@pytest.fixture
def sample_image_file(sample_image_bytes):
    return ("test.jpg", io.BytesIO(sample_image_bytes), "image/jpeg")


@pytest.fixture
def sample_image(sample_image_bytes):
    return Image.open(io.BytesIO(sample_image_bytes))
