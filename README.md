# Play4Change

Adaptive and gamified learning platform for civic education.

## Overview

Play4Change turns civic literacy into a daily habit through three pillars: **Engagement** (gamified daily challenges with streaks), **Personalisation** (an AI agent that adapts learning paths per citizen and reuses validated remediation paths via pgvector similarity search), and **Recognition** (micro-competency badges awarded on topic completion). The platform runs as a Kotlin Multiplatform mobile app, a React web portal, a stateless Spring Boot REST server, and a LangChain4j AI agent — all orchestrated with Docker Compose.

## Architecture

| Component | Description |
|---|---|
| **Mobile client** | Kotlin Multiplatform + Compose Multiplatform — Android and iOS from a single codebase; MVI with Decompose |
| **Web portal** | React — public landing page and restricted admin portal |
| **REST server** | Kotlin + Spring Boot — stateless, horizontally scalable; Clean Architecture with DDD |
| **AI agent** | LangChain4j + Mistral — generates learning content; pgvector for semantic retrieval and adaptive path reuse |

The server is split into five bounded contexts: **Identity**, **Topic**, **Enrollment**, **Struggle**, and **PeerReview**.

## Tech Stack

| | |
|---|---|
| Language | Kotlin 2.x |
| Mobile | Kotlin Multiplatform, Compose Multiplatform, Decompose |
| Web | React, Vite, TypeScript |
| Server | Spring Boot 3.2, Arrow Either |
| Database | PostgreSQL 16 + pgvector, Flyway |
| AI | LangChain4j 0.36 + Mistral (`mistral-small-latest`) |
| Cache | Redis 7 |
| Storage | MinIO (S3-compatible) |
| Auth | Magic link (SHA-256, Resend) → JWT |
| Observability | Micrometer, Prometheus, Grafana |
| Deploy | Docker Compose, GitHub Actions |

## Prerequisites

- Docker + Docker Compose
- JDK 21 (for local Gradle builds outside Docker)

## Quick Start

```bash
./scripts/setup.sh          # wipe, build, start all services, open tunnel
./scripts/check-health.sh   # wait for all services to report healthy
./scripts/minio-init.sh     # create the MinIO bucket (first run only)
```

Verify the server:

```bash
curl http://localhost:8080/actuator/health
# {"status":"UP"}
```

## Services

| Service | URL | Default credentials |
|---|---|---|
| REST API | http://localhost:8080 | — |
| Swagger UI | http://localhost:8080/swagger-ui.html | — |
| Grafana | http://localhost:3000 | admin / admin |
| Prometheus | http://localhost:9090 | — |
| MinIO console | http://localhost:9001 | minioadmin / minioadmin |

## Configuration

Copy `.env.example` to `.env` and fill in the required values.

| Variable | Description |
|---|---|
| `JWT_SECRET` | HS256 signing secret (min 32 chars) |
| `MISTRAL_API_KEY` | Mistral AI key for content generation |
| `RESEND_API_KEY` | Resend API key for magic link emails |
| `RESEND_FROM` | Sender address (e.g. `noreply@yourdomain.com`) |
| `MINIO_ROOT_USER` | MinIO root user (default: `minioadmin`) |
| `MINIO_ROOT_PASSWORD` | MinIO root password (default: `minioadmin`) |
| `DB_USER` | PostgreSQL user (default: `play4change`) |
| `DB_PASS` | PostgreSQL password (default: `play4change`) |
| `FRONTEND_ORIGIN` | Allowed CORS origin (e.g. `https://yourdomain.com`) |
| `SPRING_PROFILES_ACTIVE` | `dev`, `test`, or `prod` |
| `GRAFANA_ADMIN_PASSWORD` | Grafana admin password |

## Project Structure

```
play4change/
├── server/          # Spring Boot REST API (Clean Architecture, 5 bounded contexts)
├── composeApp/      # Kotlin Multiplatform mobile app (Android + iOS)
├── iosApp/          # iOS app entry point
├── ai-agent/        # LangChain4j + Mistral AI agent
├── common/          # Shared Kotlin models
├── client/          # Web portal build target (source lives in a separate repo)
├── infra/           # Docker configs — Nginx, Postgres init, Prometheus, Grafana
└── scripts/         # Operational scripts (setup, health, db, logs, admin, tests)
```

## API

Full interactive docs at **http://localhost:8080/swagger-ui.html** — JWT bearer pre-configured in the UI.

| Group | Prefix | Notes |
|---|---|---|
| Auth | `/auth` | Magic link request, verify, refresh, logout |
| Admin — Topics | `/admin/topics` | Create from URL or PDF, list, regenerate |
| Learning | `/topics`, `/tasks` | Enrol, get daily task, submit answer or photo |
| Struggle | `/struggle` | Adaptive remediation session and task submission |
| Peer review | `/reviews` | Submit verdict, get pending reviews |
| Admin metrics | `/admin/metrics` | Domain metrics (requires ADMIN role) |

## First Admin User

There is no seeded admin account. After any user signs in at least once, promote them:

```bash
./scripts/promote-admin.sh your@email.com
```

## Tests

210 unit tests, no Spring context required — all run as pure JVM.

```bash
./gradlew :server:test
./gradlew :ai-agent:langchain:test
./scripts/run-tests.sh all
```

Security scan (fails if CVSS ≥ 7.0):

```bash
./scripts/check-deps.sh
```

## License

See [LICENSE](LICENSE).
