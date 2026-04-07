# Play4Change

**Author:** Radesh Ilesh Gamanbhai Govind (A51620) — ISEL Engenharia Informática, Projecto e Seminário 2025/26
**Supervisors:** Prof. Nuno Datia · Prof. António Serrador · Prof. Michel Vorenhout

---

## 1. What is Play4Change

Play4Change is an adaptive learning platform that delivers gamified daily challenges on sustainability and digital literacy. Users enrol in AI-generated topics, complete multiple-choice and photo tasks, and earn points through a peer-review majority-vote system. The backend is complete; a KMP (Android/iOS/Desktop) frontend is planned.

---

## 2. Architecture

The backend follows Clean Architecture with Domain-Driven Design. All business rules live in pure Kotlin domain and application layers with no framework dependencies. Infrastructure adapters implement outbound ports — JPA repositories, Redis cache, MinIO storage, Mistral AI, and OAuth verifiers are all swappable without touching domain code. The five bounded contexts are **Identity** (auth, tokens), **Topic** (AI content pipeline), **Enrollment** (daily task progression), **Struggle** (adaptive remediation), and **PeerReview** (collective assessment).

| Layer | What lives here |
|---|---|
| `domain/` | Pure Kotlin — entities, value objects, domain repository interfaces |
| `application/` | Use cases, inbound/outbound ports (interfaces), orchestrators |
| `infrastructure/` | JPA adapters, Redis, MinIO, Mistral/LangChain4j, OAuth |
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
| Auth | Magic link (Resend) + Google OAuth (JWKS) + Facebook OAuth |
| Observability | Micrometer + Prometheus + Grafana |
| Migrations | Flyway (V1–V9) |
| Containerisation | Docker Compose (6 services) |

---

## 4. Running Locally

Full setup instructions — prerequisites, health checks, environment variables, service verification, and reset commands — are in **[demo/HOW_TO_RUN.md](demo/HOW_TO_RUN.md)**.

Start everything:

```bash
docker compose up --build
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
| `POST` | `/auth/oauth` | Login/register via Google or Facebook |
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

All decisions are documented in [`docs/adr/`](docs/adr/) (ADR-001 through ADR-015).

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
| ADR-011 | Passwordless Authentication: Magic Link + OAuth 2.0 (Google & Facebook) |
| ADR-012 | Topic Content Pipeline: Async Generation, File Storage, and Content Extraction |
| ADR-013 | Learning Flow: Day Progression, Struggle Detection, and Caching Strategy |
| ADR-014 | Peer Review: Cost-Free Assessment Through Collective Correction |
| ADR-015 | Observability Strategy: Custom Metrics, Prometheus, and Grafana |

---

## 7. Demo

The [`demo/`](demo/) directory contains runnable JetBrains HTTP Client files covering the three core flows:

- **[demo/auth.http](demo/auth.http)** — passwordless magic link and OAuth login flow
- **[demo/admin_topic.http](demo/admin_topic.http)** — admin PDF upload and async AI generation
- **[demo/peer_review.http](demo/peer_review.http)** — collective assessment 3-verdict majority flow

---

## 8. Tests

22 unit tests across 3 test classes. No Spring context required — all tests run as pure JVM.

| Class | Tests | Coverage |
|---|---|---|
| `ErrorPatternClassifierTest` | 8 | Struggle pattern classification rules and priority ordering |
| `DayIndexCalculatorTest` | 7 | Day progression, timezone boundary cases, clock-skew guard |
| `MagicLinkServiceTest` | 7 | Auth service with MockK — happy path, expired/used/missing tokens |

```bash
./gradlew :server:test
```
