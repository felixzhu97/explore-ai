#!/bin/bash

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo -e "${YELLOW}Stopping AI-Test services...${NC}\n"

echo "Stopping Node.js services..."
lsof -ti:4200 2>/dev/null | xargs kill -9 2>/dev/null || true
lsof -ti:3000 2>/dev/null | xargs kill -9 2>/dev/null || true

echo "Stopping Python services..."
lsof -ti:8000 2>/dev/null | xargs kill -9 2>/dev/null || true
lsof -ti:8003 2>/dev/null | xargs kill -9 2>/dev/null || true
lsof -ti:8006 2>/dev/null | xargs kill -9 2>/dev/null || true
lsof -ti:8010 2>/dev/null | xargs kill -9 2>/dev/null || true
lsof -ti:8013 2>/dev/null | xargs kill -9 2>/dev/null || true
lsof -ti:8015 2>/dev/null | xargs kill -9 2>/dev/null || true

echo "Stopping process patterns..."
pkill -f "ng serve.*ai-test" 2>/dev/null || true
pkill -f "tsx.*ai-test" 2>/dev/null || true
pkill -f "ai_agents.*main.py" 2>/dev/null || true
pkill -f "vision-service.*uvicorn" 2>/dev/null || true
pkill -f "rag.*uvicorn" 2>/dev/null || true
pkill -f "text-service.*uvicorn" 2>/dev/null || true
pkill -f "tts-service.*uvicorn" 2>/dev/null || true
pkill -f "media-gen.*app.py" 2>/dev/null || true

# Only stop Docker containers if Docker is running
if docker info >/dev/null 2>&1; then
    echo "Stopping Docker containers..."
    COMPOSE_CMD="docker compose"
    docker compose version >/dev/null 2>&1 || COMPOSE_CMD="docker-compose"
    SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
    ROOT_DIR="$(cd "$SCRIPT_DIR/../.." && pwd)"
    $COMPOSE_CMD -f "$ROOT_DIR/services/rag/docker-compose.yml" down 2>/dev/null || true
else
    echo -e "${YELLOW}Docker not running, skipping container cleanup${NC}"
fi

echo -e "${GREEN}All services stopped${NC}\n"
