#!/bin/sh
set -e

if [ -n "${DD_API_KEY}" ]; then
  echo "DD_API_KEY set; starting with Datadog APM javaagent"
  exec java \
    -javaagent:/app/dd-java-agent.jar \
    -Ddd.logs.injection=true \
    -jar /app/app.jar "$@"
fi

echo "DD_API_KEY unset; starting without Datadog APM javaagent"
exec java -jar /app/app.jar "$@"
