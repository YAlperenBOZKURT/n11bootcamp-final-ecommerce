# ── Stage 1: Dependencies (cached layer) ──────────────────────────────────────
FROM maven:3.9.6-eclipse-temurin-21 AS deps

WORKDIR /app
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2/repository mvn dependency:go-offline -DexcludeScope=test -q

# ── Stage 2: Build ────────────────────────────────────────────────────────────
FROM deps AS builder

COPY src ./src
RUN --mount=type=cache,target=/root/.m2/repository mvn package -Dmaven.test.skip=true -q

# ── Stage 3: Runtime ──────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

RUN apk add --no-cache curl

RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Create logs directory with correct permissions before switching user
RUN mkdir -p /app/logs && chown -R appuser:appgroup /app/logs

USER appuser

COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8769

ENTRYPOINT ["java", "-jar", "app.jar"]
