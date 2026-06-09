FROM python:3.12-slim

WORKDIR /app

# Install dependencies
RUN pip install --no-cache-dir \
    fastapi \
    uvicorn[standard] \
    pydantic \
    pydantic-settings \
    loguru \
    langchain-core \
    langchain-openai \
    langgraph \
    sse-starlette

# Copy application
COPY . /app

# Expose port
EXPOSE 8003

# Run the service
CMD ["python", "-m", "uvicorn", "services.ai_agents.main:app", "--host", "0.0.0.0", "--port", "8003"]
