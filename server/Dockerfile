# ── Build stage ───────────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app

# Copy gradle files first — layer caching means dependencies only
# re-download when build files change, not on every code change
COPY gradlew .
COPY gradle gradle
COPY build.gradle.kts .
COPY settings.gradle.kts .
COPY gradle.properties .

# Pre-download the Gradle distribution in its own layer so source changes
# don't trigger a re-download on every rebuild
RUN chmod +x gradlew && ./gradlew --version --no-daemon

# Copy all module sources
COPY common common
COPY ai-agent ai-agent
COPY server server

# Build
RUN ./gradlew :server:bootJar --no-daemon -x test

# ── Runtime stage ──────────────────────────────────────────────────────────────
# Separate stage so the final image doesn't contain JDK, Gradle, or source code
FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app

# Install curl for Docker healthcheck
RUN apk add --no-cache curl

# Non-root user — never run your app as root
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

COPY --from=build /app/server/build/libs/*.jar app.jar

EXPOSE 8080

# JVM flags:
# -XX:+UseContainerSupport — respect Docker memory limits (default in JDK 17+)
# -XX:MaxRAMPercentage=75  — use 75% of container memory for heap
ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-jar", "app.jar"]