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

RUN apk add --no-cache wget && \
    wget -O /app/dd-java-agent.jar https://dtdg.co/latest-java-tracer && \
    chown appuser:appgroup /app/dd-java-agent.jar

COPY --from=build --chown=appuser:appgroup /app/build/libs/app.jar app.jar

# Pre-create H2 database directory with correct permissions
RUN mkdir -p /app/data && chown appuser:appgroup /app/data

USER appuser
ENV SPRING_PROFILES_ACTIVE=prod
EXPOSE 8080
ENTRYPOINT ["java", \
    "-javaagent:/app/dd-java-agent.jar", \
    "-Ddd.logs.injection=true", \
    "-jar", "app.jar"]
