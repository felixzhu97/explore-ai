#!/bin/sh
set -e

if [ -n "${DD_API_KEY}" ] || [ -n "${DD_AGENT_HOST}" ]; then
  agent_host="${DD_AGENT_HOST:-localhost}"
  agent_port="${DD_TRACE_AGENT_PORT:-8126}"
  echo "Starting with Datadog APM javaagent (agent=${agent_host}:${agent_port})"
  exec java \
    -javaagent:/app/dd-java-agent.jar \
    -Ddd.logs.injection=true \
    -Ddd.agent.host="${agent_host}" \
    -Ddd.trace.agent.port="${agent_port}" \
    -jar /app/app.jar "$@"
fi

echo "DD_API_KEY and DD_AGENT_HOST unset; starting without Datadog APM javaagent"
exec java -jar /app/app.jar "$@"
