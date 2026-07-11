#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
FIXTURES_DIR="${ROOT_DIR}/src/test/resources/vision/fixtures"
mkdir -p "${FIXTURES_DIR}"

python3 - <<'PY'
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont

fixtures = Path("src/test/resources/vision/fixtures")
fixtures.mkdir(parents=True, exist_ok=True)

def save(name, width, height, draw_fn):
    image = Image.new("RGB", (width, height), "white")
    draw = ImageDraw.Draw(image)
    draw_fn(draw, width, height)
    image.save(fixtures / name)

save("ocr-hello.png", 240, 80, lambda d, w, h: d.text((20, 20), "Hello", fill="black"))
save("ocr-chinese.png", 240, 80, lambda d, w, h: d.text((20, 20), "测试", fill="black"))
save("empty-white.png", 64, 64, lambda d, w, h: None)

street = Image.new("RGB", (320, 240), "#87CEEB")
draw = ImageDraw.Draw(street)
draw.rectangle((40, 120, 120, 220), fill="#333333")
draw.ellipse((60, 80, 140, 160), fill="#ffcc99")
street.save(fixtures / "street-person.jpg", quality=85)

landscape = Image.new("RGB", (320, 240), "#4a90e2")
draw = ImageDraw.Draw(landscape)
draw.polygon([(40, 200), (160, 60), (280, 200)], fill="#2d6a4f")
draw.rectangle((0, 180, 320, 240), fill="#6b8e23")
landscape.save(fixtures / "landscape.jpg", quality=85)
PY

echo "Vision fixtures generated in ${FIXTURES_DIR}"
