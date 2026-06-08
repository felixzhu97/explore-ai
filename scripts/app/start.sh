#!/bin/bash
set -e

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

ENV=${1:-dev}
[[ "$ENV" == "dev" || "$ENV" == "prod" ]] || { echo -e "${RED}$0 [dev|prod]${NC}"; exit 1; }

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/../.." && pwd)"

# Load environment variables from .env files
if [ -f "$ROOT_DIR/.env" ]; then
  set -a
  source "$ROOT_DIR/.env"
  set +a
fi
if [ -f "$ROOT_DIR/services/ai_agents/.env" ]; then
  set -a
  source "$ROOT_DIR/services/ai_agents/.env"
  set +a
fi

cleanup() {
  echo -e "\n${YELLOW}Stopping services...${NC}"
  [ -n "$AI_AGENTS_PID" ] && kill $AI_AGENTS_PID 2>/dev/null || true
  [ -n "$VISION_PID" ] && kill $VISION_PID 2>/dev/null || true
  [ -n "$RAG_PID" ] && kill $RAG_PID 2>/dev/null || true
  [ -n "$TEXT_PID" ] && kill $TEXT_PID 2>/dev/null || true
  [ -n "$TTS_PID" ] && kill $TTS_PID 2>/dev/null || true
  [ -n "$MEDIA_PID" ] && kill $MEDIA_PID 2>/dev/null || true
  [ -n "$SERVER_PID" ] && kill $SERVER_PID 2>/dev/null || true
  [ -n "$WEB_PID" ] && kill $WEB_PID 2>/dev/null || true
  exit 0
}
trap cleanup SIGINT SIGTERM

echo -e "${GREEN}╔════════════════════════════════════════════════╗${NC}"
echo -e "${GREEN}║     AI-Test Platform (${ENV})                       ║${NC}"
echo -e "${GREEN}╚════════════════════════════════════════════════╝${NC}\n"

echo -e "${BLUE}[1/5] Stopping old processes...${NC}"
for port in 4200 3000 8000 8003 8006 8010 8013 8015 6333 11434; do
  lsof -ti:$port 2>/dev/null | xargs kill -9 2>/dev/null || true
done
pkill -f "ng serve.*ai-test" 2>/dev/null || true
pkill -f "tsx.*ai-test" 2>/dev/null || true
pkill -f "ai_agents.*main.py" 2>/dev/null || true
pkill -f "vision-service.*uvicorn" 2>/dev/null || true
pkill -f "rag.*uvicorn" 2>/dev/null || true
pkill -f "text-service.*uvicorn" 2>/dev/null || true
pkill -f "tts-service.*uvicorn" 2>/dev/null || true
pkill -f "media-gen.*app.py" 2>/dev/null || true
pkill -f "uvicorn.*8003" 2>/dev/null || true
pkill -f "uvicorn.*8006" 2>/dev/null || true
pkill -f "uvicorn.*8010" 2>/dev/null || true
pkill -f "uvicorn.*8013" 2>/dev/null || true
sleep 2

echo -e "${BLUE}[2/5] Docker + Qdrant...${NC}"
if docker info >/dev/null 2>&1; then
    COMPOSE_CMD="docker compose"
    docker compose version >/dev/null 2>&1 || COMPOSE_CMD="docker-compose"
    export COMPOSE_PROJECT_NAME=ai-test
    $COMPOSE_CMD -f "$ROOT_DIR/services/rag/docker-compose.yml" down 2>/dev/null || true
    $COMPOSE_CMD -f "$ROOT_DIR/services/rag/docker-compose.yml" up -d --wait qdrant 2>/dev/null || true

    for i in $(seq 1 30); do
      nc -z 127.0.0.1 6333 2>/dev/null && break
      sleep 1
    done
    echo -e "${GREEN}  ✓ Qdrant ready on http://localhost:6333${NC}"
else
    echo -e "${YELLOW}  ⚠ Docker not running, skipping Qdrant${NC}"
fi

echo -e "${BLUE}[3/5] Installing dependencies...${NC}"
[ ! -d "$ROOT_DIR/node_modules" ] && cd "$ROOT_DIR" && pnpm install

echo -e "${BLUE}[4/5] Starting Python services...${NC}"

start_ai_agents() {
  local dir="$ROOT_DIR/services/ai_agents"
  if [ -d "$dir" ]; then
    cd "$dir"
    [ ! -d ".venv" ] && python3 -m venv .venv
    .venv/bin/pip install -r requirements.txt -q 2>/dev/null || true
    PYTHONPATH="$ROOT_DIR" .venv/bin/python main.py &
    AI_AGENTS_PID=$!
    sleep 3
    if kill -0 $AI_AGENTS_PID 2>/dev/null; then
      echo -e "${GREEN}  ✓ AI Agents started on http://localhost:8003${NC}"
    else
      echo -e "${YELLOW}  ⚠ AI Agents failed to start${NC}"
    fi
  fi
}

start_uvicorn_service() {
  local name=$1
  local dir=$2
  local port=$3
  local pid_var=$4

  if [ -d "$dir" ]; then
    cd "$dir"
    [ ! -d ".venv" ] && python3 -m venv .venv
    if [ -f "requirements.txt" ]; then
      .venv/bin/pip install -r requirements.txt -q 2>/dev/null || true
    elif [ -f "pyproject.toml" ]; then
      .venv/bin/pip install -e . -q 2>/dev/null || true
    fi
    # Set PYTHONPATH for services that need root-level modules (e.g., services.shared)
    PYTHONPATH="$dir:$ROOT_DIR" .venv/bin/python -m uvicorn src.main:app --host 0.0.0.0 --port $port &
    eval "${pid_var}=$!"
    sleep 3
    if kill -0 $(eval echo \$${pid_var}) 2>/dev/null; then
      echo -e "${GREEN}  ✓ $name started on http://localhost:$port${NC}"
    else
      echo -e "${YELLOW}  ⚠ $name failed to start${NC}"
    fi
  fi
}

start_media_gen() {
  local dir="$ROOT_DIR/services/media-gen"
  if [ -d "$dir" ]; then
    cd "$dir"
    [ ! -d ".venv" ] && python3 -m venv .venv
    .venv/bin/pip install -r requirements.txt -q 2>/dev/null || true
    .venv/bin/python app.py &
    MEDIA_PID=$!
    sleep 3
    if kill -0 $MEDIA_PID 2>/dev/null; then
      echo -e "${GREEN}  ✓ Media Gen started on http://localhost:8015${NC}"
    else
      echo -e "${YELLOW}  ⚠ Media Gen failed to start${NC}"
    fi
  fi
}

start_ai_agents
start_uvicorn_service "Vision Service" "$ROOT_DIR/services/vision-service" "8000" "VISION_PID"
start_uvicorn_service "RAG Service" "$ROOT_DIR/services/rag" "8010" "RAG_PID"
start_uvicorn_service "Text Service" "$ROOT_DIR/services/text-service" "8006" "TEXT_PID"
start_uvicorn_service "TTS Service" "$ROOT_DIR/services/tts-service" "8013" "TTS_PID"
start_media_gen

echo -e "${BLUE}[5/5] Starting Node.js services...${NC}"

cd "$ROOT_DIR"
(pnpm --filter @ai-test/server dev) &
SERVER_PID=$!
sleep 3
if kill -0 $SERVER_PID 2>/dev/null; then
  echo -e "${GREEN}  ✓ Express Server started on http://localhost:3000${NC}"
else
  echo -e "${YELLOW}  ⚠ Express Server failed to start${NC}"
fi

(pnpm --filter @ai-test/web dev) &
WEB_PID=$!
sleep 5
if kill -0 $WEB_PID 2>/dev/null; then
  echo -e "${GREEN}  ✓ Web Frontend started on http://localhost:4200${NC}"
else
  echo -e "${YELLOW}  ⚠ Web Frontend failed to start${NC}"
fi

echo -e "\n${GREEN}╔════════════════════════════════════════════════╗${NC}"
echo -e "${GREEN}║              Services Ready!                      ║${NC}"
echo -e "${GREEN}╚════════════════════════════════════════════════╝${NC}"
echo ""
echo -e "  ${BLUE}Web Frontend${NC}   ${YELLOW}http://localhost:4200${NC}"
echo -e "  ${BLUE}Express Server${NC} ${YELLOW}http://localhost:3000${NC}"
echo -e "  ${BLUE}AI Agents${NC}     ${YELLOW}http://localhost:8003${NC}"
echo -e "  ${BLUE}Vision Service${NC}${YELLOW}http://localhost:8000${NC}"
echo -e "  ${BLUE}RAG Service${NC}   ${YELLOW}http://localhost:8010${NC}"
echo -e "  ${BLUE}Text Service${NC}  ${YELLOW}http://localhost:8006${NC}"
echo -e "  ${BLUE}TTS Service${NC}  ${YELLOW}http://localhost:8013${NC}"
echo -e "  ${BLUE}Media Gen${NC}     ${YELLOW}http://localhost:8015${NC}"
echo ""
echo -e "${YELLOW}Press Ctrl+C to stop all services${NC}\n"

wait
