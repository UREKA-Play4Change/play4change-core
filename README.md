# Play4Change

**Author:** Radesh Ilesh Gamanbhai Govind (A51620) — ISEL Engenharia Informática, Projecto e Seminário 2025/26
**Supervisors:** Prof. Nuno Datia · Prof. António Serrador · Prof. Michel Vorenhout

---

## 1. What is Play4Change

Play4Change is an adaptive learning platform for civic literacy and urban sustainability. It is built around three pillars:

- **Engagement** — gamified daily challenges delivered through a mobile app, with streaks and progress tracking to sustain participation
- **Personalisation** — AI-driven adaptive paths that detect learning difficulties and reuse previously validated remediation paths across users with similar profiles
- **Recognition** — microcompetencies certified by badges, awarded on topic completion, building a progressive competency portfolio for each citizen

Users enrol in AI-generated topics, complete multiple-choice and photo tasks, and earn points through a peer-review majority-vote system. The platform comprises a KMP mobile client (Android/iOS), a React web portal, a stateless REST server, and an AI agent.

---

## 2. Architecture

The system is composed of four components:

| Component | Description |
|---|---|
| **Mobile client** | Kotlin Multiplatform + Compose Multiplatform; shared logic for Android and iOS; MVI pattern with Decompose |
| **Web portal** | React; public landing page and restricted admin portal (topic management, AI content review, metrics) |
| **REST server** | Kotlin + Spring Boot; stateless, horizontally scalable; coordinates all business logic |
| **AI agent** | LangChain4j + Mistral; generates and adapts learning content; pgvector for semantic retrieval and adaptive path reuse |

The server follows Clean Architecture with Domain-Driven Design. All business rules live in pure Kotlin domain and application layers with no framework dependencies. Infrastructure adapters implement outbound ports — JPA repositories, Redis cache, MinIO storage, and the Mistral AI client are all swappable without touching domain code. The five bounded contexts are **Identity** (auth, tokens), **Topic** (AI content pipeline), **Enrollment** (daily task progression), **Struggle** (adaptive remediation), and **PeerReview** (collective assessment).

| Layer | What lives here |
|---|---|
| `domain/` | Pure Kotlin — entities, value objects, domain repository interfaces |
| `application/` | Use cases, inbound/outbound ports (interfaces), orchestrators |
| `infrastructure/` | JPA adapters, Redis, MinIO, Mistral/LangChain4j, Resend (magic link) |
| `web/` | Spring MVC controllers, DTOs, security filter chain |

---

## 3. Tech Stack

| Component | Technology |
|---|---|
| Language | Kotlin 2.x |
| Framework | Spring Boot 3.2 |
| Database | PostgreSQL 16 + pgvector |
| AI | LangChain4j 0.36 + Mistral AI (`mistral-small-latest`) |
| Cache | Redis 7 (Spring Data / Lettuce) |
| File Storage | MinIO (S3-compatible, AWS SDK) |
| Auth | Magic link (SHA-256, Resend API) |
| Observability | Micrometer + Prometheus + Grafana |
| Migrations | Flyway (V1–V10) |
| Containerisation | Docker Compose (6 services) |

---

## 4. Running Locally

Full setup instructions — prerequisites, health checks, environment variables, service verification, and reset commands — are in **[demo/HOW_TO_RUN.md](demo/HOW_TO_RUN.md)**.

All operational scripts live in [`scripts/`](scripts/README.md). Start everything:

```bash
./scripts/setup.sh         # wipe + build + start everything + tunnel
./scripts/check-health.sh      # wait for all services to report healthy
./scripts/minio-init.sh        # create required MinIO bucket (first run only)
```

Verify the server is up:

```bash
curl http://localhost:8080/actuator/health
# {"status":"UP"}
```

Swagger UI: **http://localhost:8080/swagger-ui.html**

---

## 5. API

All endpoints are documented interactively at **http://localhost:8080/swagger-ui.html** (no auth required to browse). The JWT bearer scheme is pre-configured in the UI.

### Auth

| Method | Path | Description |
|---|---|---|
| `POST` | `/auth/magic-link` | Request a magic link email |
| `GET` | `/auth/verify?token=` | Verify token → returns JWT pair |
| `POST` | `/auth/refresh` | Rotate refresh token → new JWT pair |
| `DELETE` | `/auth/logout` | Revoke refresh token (entire family) |

### Admin — Topics

| Method | Path | Description |
|---|---|---|
| `POST` | `/admin/topics` | Create topic from URL (async AI generation) |
| `POST` | `/admin/topics/pdf` | Create topic from PDF upload (multipart) |
| `GET` | `/admin/topics` | List all topics (optional `?status=` filter) |
| `GET` | `/admin/topics/{id}` | Get topic by ID (poll for `ACTIVE` status) |
| `POST` | `/admin/topics/{id}/regenerate` | Re-trigger AI generation from stored content |

### User — Learning

| Method | Path | Description |
|---|---|---|
| `POST` | `/topics/{topicId}/enroll` | Enrol in a topic |
| `GET` | `/topics/{topicId}/enrollment` | Get enrollment status and points |
| `GET` | `/tasks/today?topicId=` | Get today's task (optional `X-Timezone` header) |
| `POST` | `/tasks/{assignmentId}/submit` | Submit multiple-choice answer |
| `POST` | `/tasks/{assignmentId}/submit-photo` | Submit photo for TODO_ACTION task |
| `GET` | `/topics/{topicId}/roadmap` | Full day-by-day task status roadmap |

### User — Struggle (Adaptive Remediation)

| Method | Path | Description |
|---|---|---|
| `GET` | `/struggle/enrollment/{enrollmentId}` | Get active struggle session |
| `POST` | `/struggle/{sessionId}/tasks/{taskId}/submit` | Submit adaptive task answer |

### User — Peer Review

| Method | Path | Description |
|---|---|---|
| `POST` | `/reviews/{reviewId}/verdict` | Submit verdict (`CORRECT`/`INCORRECT`) |
| `GET` | `/reviews/pending?topicId=` | Get pending reviews assigned to current user |

---

## 6. Key Architectural Decisions

All decisions are documented in [`docs/adr/`](docs/adr/) (ADR-001 through ADR-016).

| ADR | Decision |
|---|---|
| ADR-001 | Result Type: Custom Implementation → Arrow Either |
| ADR-002 | Communication Architecture: Hybrid REST + gRPC *(superseded by ADR-006)* |
| ADR-003 | AI Content Generation: Batch vs On-Demand |
| ADR-004 | Internationalisation Strategy: Hybrid Server + Client |
| ADR-005 | Adaptive Learning Path Architecture |
| ADR-006 | AI Agent: gRPC Service → Gradle Module Boundary |
| ADR-007 | AI Content Generation Pipeline |
| ADR-008 | Client Architecture: Decompose + MVI + Domain-Driven Feature Slices |
| ADR-009 | Deployment Strategy: Docker Compose over Kubernetes + AWS |
| ADR-010 | BaseView Scaffold Upgrade: Edge-to-Edge Insets, Navigation Drawer, and Log-Out Placement |
| ADR-011 | Passwordless Authentication: Magic Link |
| ADR-012 | Topic Content Pipeline: Async Generation, File Storage, and Content Extraction |
| ADR-013 | Learning Flow: Day Progression, Struggle Detection, and Caching Strategy |
| ADR-014 | Peer Review: Cost-Free Assessment Through Collective Correction |
| ADR-015 | Observability Strategy: Custom Metrics, Prometheus, and Grafana |
| ADR-016 | Authentication Hardening: Security Audit and Remediation |

---

## 7. Demo

**[demo/DEMO_SCRIPT.md](demo/DEMO_SCRIPT.md)** — self-contained walkthrough covering all 5 bounded contexts in ~15 minutes. Follows auth → topic creation → enrollment → struggle → peer review → badge → observability.

The [`demo/`](demo/) directory also contains runnable JetBrains HTTP Client files:

- **[demo/auth.http](demo/auth.http)** — passwordless magic link authentication flow
- **[demo/admin_topic.http](demo/admin_topic.http)** — admin PDF upload and async AI generation
- **[demo/peer_review.http](demo/peer_review.http)** — collective assessment 3-verdict majority flow

---

## 8. First admin user

There is no seeded admin account. Promote a user to `ADMIN` manually after they have
signed in at least once (so a `users` row exists):

```bash
./scripts/promote-admin.sh your@email.com
```

The change takes effect on the user's next login.

---

## 9. Observability

Two Grafana dashboards are provisioned automatically on `docker compose up`. Access at **http://localhost:3000** (admin / admin).

| Dashboard | What it shows |
|---|---|
| AI Generation Latency | P50 / P95 / P99 of Mistral API call duration, per generation phase |
| Learner Flow Metrics | Task submission rate (correct vs incorrect), struggle trigger rate, peer review verdict throughput |

Prometheus scrapes the server on port 9090. Raw metrics: **http://localhost:9090**.

---

## 10. Tests

210 unit tests across 39 test classes. No Spring context required — all tests run as pure JVM.

Selected highlights:

| Class | Area |
|---|---|
| `AiGenerationMetricsTest` | AI timer with `generation_phase` and `topic_id` tags |
| `LearnerFlowMetricsTest` | tasks.submitted / struggle.sessions.created / reviews.verdicts counters |
| `AuthControllerValidationTest` | Bean validation on auth endpoints |
| `ErrorPatternClassifierTest` | Struggle pattern classification rules |
| `DayIndexCalculatorTest` | Day progression, timezone boundary cases |
| `MagicLinkServiceTest` | Hash storage, atomic claim, concurrency path |
| `EnrollmentPrerequisiteGateTest` | DAG prerequisite enforcement |

```bash
./gradlew :server:test
./gradlew :ai-agent:langchain:test
```
