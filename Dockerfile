# Marry-It MCP-only HTTP server image.
#
# PlayMCP builds the default Dockerfile unless a custom Dockerfile path is
# explicitly supported. Keep this branch's Dockerfile MCP-only so container_port
# 8000 maps to the actual MCP server.

# ---- Build stage ----
FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace

COPY gradlew settings.gradle build.gradle ./
COPY gradle gradle
RUN chmod +x gradlew && ./gradlew --version

COPY src src
RUN ./gradlew bootJar --no-daemon -x test

# ---- Runtime stage ----
FROM eclipse-temurin:21-jre AS runtime
WORKDIR /app

LABEL org.opencontainers.image.title="marry-it-mcp"
LABEL org.opencontainers.image.description="Marry-It backend with MCP-style AI Agent tools"

ENV SPRING_PROFILES_ACTIVE=mcp
ENV SPRING_MAIN_WEB_APPLICATION_TYPE=servlet
ENV MCP_PORT=8000
ENV JAVA_OPTS=""

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*
RUN addgroup --system app && adduser --system --ingroup app app
COPY --from=build /workspace/build/libs/*.jar app.jar
USER app

EXPOSE 8000
HEALTHCHECK --interval=30s --timeout=3s --start-period=20s --retries=3 \
    CMD curl -fsS http://127.0.0.1:8000/health || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar --spring.profiles.active=mcp"]
