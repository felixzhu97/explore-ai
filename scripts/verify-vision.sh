#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${VISION_BASE_URL:-http://localhost:9000}"
FIXTURES_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)/src/test/resources/vision/fixtures"
TASK="${1:-all}"

assert_json_field() {
  local body="$1"
  local field="$2"
  python3 - <<PY
import json, sys
body = json.loads('''${body}''')
field = "${field}"
parts = field.split('.')
cur = body
for part in parts:
    cur = cur[part]
if cur in (None, "", []):
    raise SystemExit(f"Expected non-empty field: {field}")
print(f"OK: {field}")
PY
}

post_image() {
  local endpoint="$1"
  local file="$2"
  curl -fsS -X POST "${BASE_URL}/api/vision/${endpoint}" -F "file=@${file}"
}

check_health() {
  local body
  body="$(curl -fsS "${BASE_URL}/api/vision/health")"
  echo "${body}" | python3 -c "import json,sys; data=json.load(sys.stdin); assert data.get('status') in {'UP','DEGRADED'}, data; print('health:', data)"
}

run_ocr() {
  local body
  body="$(post_image ocr "${FIXTURES_DIR}/ocr-hello.png")"
  assert_json_field "${body}" "fullText"
  echo "${body}" | python3 -c "import json,sys; t=json.load(sys.stdin)['fullText']; assert 'Hello' in t or len(t)>0"
  echo "OCR smoke passed"
}

run_detect() {
  local body
  body="$(post_image detect "${FIXTURES_DIR}/street-person.jpg")"
  printf '%s' "${body}" | python3 -c '
import json, sys
data = json.load(sys.stdin)
assert isinstance(data["detections"], list)
assert isinstance(data["processingTimeMs"], int)
for det in data["detections"]:
    assert len(det["bbox"]) == 4
    assert 0 <= det["confidence"] <= 1
print("detect:", len(data["detections"]), "objects")
'
  echo "Detect smoke passed"
}

run_caption() {
  local body
  body="$(post_image caption "${FIXTURES_DIR}/landscape.jpg")"
  assert_json_field "${body}" "caption"
  echo "${body}" | python3 -c "import json,sys; c=json.load(sys.stdin)['caption']; assert len(c)>=1"
  echo "Caption smoke passed"
}

check_health

case "${TASK}" in
  ocr) run_ocr ;;
  detect) run_detect ;;
  caption) run_caption ;;
  all)
    run_ocr
    run_detect
    run_caption
    ;;
  *)
    echo "Usage: $0 [ocr|detect|caption|all]"
    exit 1
    ;;
esac
