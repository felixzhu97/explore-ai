# Build stage
FROM eclipse-temurin:25-jdk-alpine AS build
WORKDIR /app
COPY gradle gradle
COPY gradlew build.gradle.kts settings.gradle.kts ./
RUN chmod +x gradlew
RUN ./gradlew dependencies --no-daemon

COPY src src
RUN ./gradlew bootJar --no-daemon

# Runtime stage
FROM eclipse-temurin:25-jre-alpine

# Create non-root user for security
RUN addgroup -g 1001 -S appgroup && \
    adduser -u 1001 -S appuser -G appgroup

WORKDIR /app

ARG DD_AGENT_VERSION=1.45.0
RUN apk add --no-cache wget && \
    wget -O /app/dd-java-agent.jar "https://github.com/DataDog/dd-trace-java/releases/download/v${DD_AGENT_VERSION}/dd-java-agent.jar" && \
    chown appuser:appgroup /app/dd-java-agent.jar

COPY --from=build --chown=appuser:appgroup /app/build/libs/app.jar app.jar
COPY --chown=appuser:appgroup scripts/docker-entrypoint.sh /app/docker-entrypoint.sh
RUN chmod +x /app/docker-entrypoint.sh

# Pre-create H2 database directory with correct permissions
RUN mkdir -p /app/data && chown appuser:appgroup /app/data

USER appuser
ENV SPRING_PROFILES_ACTIVE=prod
EXPOSE 8080
ENTRYPOINT ["/app/docker-entrypoint.sh"]
