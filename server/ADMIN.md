# Admin API — Engineering Reference

All admin endpoints live under the `/admin/**` prefix and require a valid JWT with `role=ADMIN`. This document describes every operation available to an admin, how each one flows through the system, and what happens at each layer.

---

## Authentication & Authorization

Every request to `/admin/**` passes through two enforcement layers before reaching any controller:

### Layer 1 — JwtAuthFilter
`com.ureka.play4change.auth.adapter.inbound.security.JwtAuthFilter`

1. Checks if the request path matches a `PUBLIC_PREFIXES` list (`/auth/**`, `/actuator/**`, `/swagger-ui/**`, etc.). If it does, the filter is skipped entirely.
2. Reads the `Authorization: Bearer <token>` header.
3. Parses the JWT and extracts `userId` (sub) and `role` claims.
4. Registers a Spring Security authentication with the principal set to `userId` and the granted authority set to `ROLE_<role>` (e.g. `ROLE_ADMIN`).
5. Returns `401 Unauthorized` with a JSON body if the token is missing, malformed, or expired.

JWT claims structure:
```json
{
  "sub":   "<userId>",
  "email": "<email>",
  "role":  "ADMIN",
  "iat":   1234567890,
  "exp":   1234568790
}
```

Token TTLs are configured via:
- `jwt.accessTtlMinutes` (default 15 min)
- `jwt.refreshTtlDays` (default 7 days)

### Layer 2 — SecurityConfig
`com.ureka.play4change.infra.config.SecurityConfig`

Spring Security's route-level rules enforce:
```
/admin/**  →  hasRole("ADMIN")
```
Any authenticated non-admin user hitting `/admin/**` receives `403 Forbidden` with:
```json
{ "error": "Insufficient role" }
```

---

## Endpoints

### GET /admin/me — Get Admin Profile

**Controller**: `AdminProfileController.me()`
**Use case**: `GetAdminProfileUseCase` → `GetAdminProfileService`

#### Pipeline

```
Request
  └─ JwtAuthFilter          extracts userId from JWT
       └─ SecurityConfig     verifies ROLE_ADMIN
            └─ AdminProfileController.me()
                 └─ GetAdminProfileUseCase.execute(userId)
                      └─ UserRepository.findById(userId)
                           └─ UserEntity  (table: users)
                                └─ AdminProfileResponse
```

#### Response

```json
{
  "id":    "<uuid>",
  "email": "admin@example.com",
  "name":  "Admin Name"
}
```

`name` falls back to `email` when the `users.name` column is null.

---

### POST /admin/topics — Create Topic from URL

**Controller**: `TopicController.createFromUrl()`
**Use case**: `TopicManagementService.createFromUrl()`

#### Request body (JSON)

| Field | Type | Required | Notes |
|---|---|---|---|
| `title` | String | Yes | |
| `description` | String | Yes | |
| `category` | String | No | defaults to `""` |
| `urls` | List\<String\> | Yes | first element is used as the URL |
| `durationDays` | Int | Yes | controls task count and expiry defaults |
| `difficulty` | String | No | `"BEGINNER"` / `"INTERMEDIATE"` / `"ADVANCED"`, defaults to `"BEGINNER"` |
| `language` | String | No | defaults to `"en"` |
| `taskCount` | Int | No | defaults to `durationDays * 3` |
| `expiresAt` | OffsetDateTime | No | defaults to `now + durationDays + 30 days` |

#### Pipeline

```
POST /admin/topics
  └─ JwtAuthFilter + SecurityConfig
       └─ TopicController.createFromUrl()
            │  computes values directly from request fields:
            │    url       = request.urls.first()
            │    duration  = request.durationDays
            │    taskCount = request.taskCount ?: durationDays * 3
            │    expiresAt = request.expiresAt ?: now + duration + 30d
            │    audience  = AudienceLevel.valueOf(difficulty)
            └─ TopicManagementService.createFromUrl(command)
                 │
                 ├─ Validation
                 │    ensure(subscriptionWindowDays >= 3)   → 400 if not
                 │    ensure(taskCount > 0)                 → 400 if not
                 │
                 ├─ ContentExtractorPort.extractFromUrl(url)
                 │    └─ HTTP GET on URL, extracts readable plain text
                 │    ensure(rawText.isNotBlank())          → 400 if not
                 │
                 ├─ FileStoragePort.uploadFile(key, bytes, "text/plain")
                 │    └─ MinIO adapter: uploads extracted text
                 │    └─ Returns MinIO object key (contentSourceRef)
                 │
                 ├─ TopicRepository.save(topic)
                 │    └─ INSERT into topics (status = PENDING)
                 │
                 └─ TaskGenerationOrchestrator.generateAsync(topicId)  [async]
                      └─ see "Async Task Generation" section
```

#### Response — `201 Created`

```json
{
  "id":                    "<uuid>",
  "title":                 "...",
  "description":           "...",
  "category":              "...",
  "status":                "PENDING",
  "createdAt":             "2026-04-28T12:00:00Z",
  "durationDays":          7,
  "difficulty":            "BEGINNER",
  "contentSourceType":     "URL",
  "taskCount":             21,
  "subscriptionWindowDays":7,
  "audienceLevel":         "BEGINNER",
  "language":              "en",
  "expiresAt":             "2026-06-04T12:00:00Z",
  "createdBy":             "<adminId>",
  "stats":                 null,
  "contentTruncated":      false
}
```

---

### POST /admin/topics/pdf — Create Topic from PDF

**Controller**: `TopicController.createFromPdf()`
**Use case**: `TopicManagementService.createFromPdf()`

#### Request — multipart/form-data

| Field | Type | Required | Notes |
|---|---|---|---|
| `title` | String | Yes | |
| `description` | String | Yes | |
| `category` | String | No | defaults to `""` |
| `durationDays` | Int | Yes | controls task count and expiry defaults |
| `difficulty` | String | No | `"BEGINNER"` / `"INTERMEDIATE"` / `"ADVANCED"`, defaults to `"BEGINNER"` |
| `language` | String | No | defaults to `"en"` |
| `taskCount` | Int | No | defaults to `durationDays * 3` |
| `expiresAt` | OffsetDateTime | No | defaults to `now + durationDays + 30 days` |
| `file` | MultipartFile | Yes | the PDF binary |

#### Pipeline

```
POST /admin/topics/pdf  (multipart/form-data)
  └─ JwtAuthFilter + SecurityConfig
       └─ TopicController.createFromPdf()
            └─ TopicManagementService.createFromPdf(command)
                 │
                 ├─ Validation
                 │    ensure(subscriptionWindowDays >= 3)       → 400 if not
                 │    ensure(taskCount > 0)                     → 400 if not
                 │    ensure(pdfBytes.isNotEmpty())             → 400 if not
                 │
                 ├─ ContentExtractorPort.extractFromPdf(pdfBytes)
                 │    └─ Parses PDF binary into plain text
                 │    ensure(rawText.isNotBlank())              → 400 if not
                 │
                 ├─ FileStoragePort.uploadFile(key, pdfBytes, "application/pdf")
                 │    └─ MinIO adapter: stores original PDF
                 │    └─ Returns MinIO object key (contentSourceRef)
                 │
                 ├─ TopicRepository.save(topic)
                 │    └─ INSERT into topics (status = PENDING, contentSourceType = PDF)
                 │
                 └─ TaskGenerationOrchestrator.generateAsync(topicId)  [async]
                      └─ see "Async Task Generation" section
```

Response shape is identical to `POST /admin/topics`.

---

### GET /admin/topics — List All Topics

**Controller**: `TopicController.listAll()`
**Use case**: `TopicManagementService.listAll(statusFilter, page, size)`

#### Query parameters

| Param | Type | Required | Notes |
|---|---|---|---|
| `status` | String | No | filters by TopicStatus name (e.g. `ACTIVE`, `PENDING`, `FAILED`) |
| `page` | Int | No | zero-based page index, defaults to `0` |
| `size` | Int | No | page size, defaults to `20` |

#### Pipeline

```
GET /admin/topics?status=ACTIVE&page=0&size=20
  └─ JwtAuthFilter + SecurityConfig
       └─ TopicController.listAll(status?, page, size)
            └─ TopicManagementService.listAll(statusFilter, page, size)
                 └─ TopicRepository
                      ├─ findAll(page, size)           (no filter)
                      └─ findByStatus(status, page, size) (with filter)
                           └─ TopicJpaRepository.findAllByStatus(status, pageable)
                                └─ SELECT * FROM topics WHERE status = ? LIMIT ? OFFSET ?
```

#### Response — `200 OK`

```json
{
  "content": [
    { /* TopicResponse */ },
    { /* TopicResponse */ }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 5,
  "totalPages": 1
}
```

Returns `content: []` when no topics match.

---

### GET /admin/topics/{id} — Get Topic by ID

**Controller**: `TopicController.getById(id)`
**Use case**: `TopicManagementService.getById(id)`

#### Pipeline

```
GET /admin/topics/{id}
  └─ JwtAuthFilter + SecurityConfig
       └─ TopicController.getById(id)
            └─ TopicManagementService.getById(id)
                 └─ TopicRepository.findById(id)
                      └─ SELECT * FROM topics WHERE id = ?
                           ├─ Found    → TopicResponse (200)
                           └─ Not found→ 404 NotFound.ResourceNotFound
```

---

### POST /admin/topics/{id}/regenerate — Regenerate Topic Tasks

**Controller**: `TopicController.regenerate(id)`
**Use case**: `TopicManagementService.regenerate(id)`

Discards the existing AI-generated task templates and re-runs the generation pipeline from the stored content. Useful when the AI output was poor or the model/prompt was updated.

#### Pipeline

```
POST /admin/topics/{id}/regenerate
  └─ JwtAuthFilter + SecurityConfig
       └─ TopicController.regenerate(id)
            └─ TopicManagementService.regenerate(id)
                 │
                 ├─ TopicRepository.findById(id)
                 │    └─ 404 if not found
                 │
                 ├─ ensure(topic.status != GENERATING)
                 │    └─ 409 Conflict.ConcurrentModification if already running
                 │
                 └─ TaskGenerationOrchestrator.generateAsync(topicId)  [async]
                      └─ see "Async Task Generation" section
```

Response is the updated `TopicResponse` with status transitioning to `GENERATING`.

---

## Async Task Generation

`TaskGenerationOrchestrator` is launched in a background thread after topic creation or regeneration. The calling HTTP request returns immediately after the topic is persisted with `status=PENDING`; the orchestrator runs independently.

### Orchestrator Flow

```
TaskGenerationOrchestrator.generateAsync(topicId)
  │
  ├─ TopicRepository.updateStatus(id, GENERATING)
  │    └─ UPDATE topics SET status = 'GENERATING', status_updated_at = NOW() WHERE id = ?
  │
  ├─ Fetch content
  │    ├─ If rawExtractedText is cached in entity → use it directly
  │    └─ Else FileStoragePort.downloadFile(contentSourceRef)
  │         └─ MinIO GET object using key directly
  │
  ├─ (on regenerate) Delete existing task templates
  │    └─ TaskTemplateRepository.deleteByTopicId(topicId)
  │
  ├─ Create TopicModule for this topic
  │
  ├─ Log truncation warnings if content > 8000 chars or description > 500 chars
  │
  ├─ TaskGenerationPort.generateTasks(
  │      topicId      = topicId,
  │      subject      = rawExtractedText.take(8000),
  │      audienceLevel,
  │      language,
  │      taskCount
  │   )
  │    └─ HTTP call to AI service (Mistral)
  │         timeout: ai.mistral.timeout-seconds (default 60s)
  │
  ├─ Parse AI response → List<TaskTemplate>
  │    └─ TaskTemplateRepository.saveAll(templates)
  │         └─ INSERT INTO task_templates ...
  │
  ├─ TopicRepository.updateStatus(id, ACTIVE)   [success path]
  │
  └─ TopicRepository.updateStatus(id, FAILED)   [on any error]
```

### Topic Status State Machine

```
             createFromUrl / createFromPdf
                         │
                         ▼
                       PENDING
                         │
          TaskGenerationOrchestrator starts
                         │
                         ▼
                    GENERATING ──────────────────────────────┐
                    /         \                              │
                success       error / timeout               │
                  /               \                         │
                 ▼                 ▼                         │
              ACTIVE             FAILED                      │
                │                  │                         │
                │         POST .../regenerate               │
                │                  │                         │
    expiresAt < now                └─────────────────────────┘
                │
                ▼
             EXPIRED
```

> `EXPIRED` is managed by `TopicExpirationJob` (scheduled). `FAILED` topics stuck in
> `GENERATING` for too long are reset to `FAILED` by `StuckGenerationWatchdogJob`.

---

## Scheduled Jobs

### TopicExpirationJob

Runs every 60 seconds (configurable via `scheduler.expiration.rate-ms`). Finds all `ACTIVE` topics whose `expiresAt < now` and transitions them to `EXPIRED`.

Each transition calls `TopicRepository.updateStatus()`, which also sets `statusUpdatedAt = NOW()`.

### StuckGenerationWatchdogJob

Runs every 120 seconds (configurable via `scheduler.watchdog.rate-ms`). Finds all `GENERATING` topics whose `statusUpdatedAt` is older than `scheduler.watchdog.stuck-threshold-minutes` (default 5 minutes) and transitions them to `FAILED`.

Protects against AI timeouts or thread-pool starvation leaving topics stuck forever in the `GENERATING` state.

---

## Domain Models

### Topic

```kotlin
data class Topic(
    val id:                    String,
    val title:                 String,
    val description:           String,
    val category:              String,
    val contentSourceType:     ContentSourceType,   // URL | PDF
    val contentSourceRef:      String,               // MinIO object key
    val rawExtractedText:      String?,
    val taskCount:             Int,
    val subscriptionWindowDays:Int,
    val expiresAt:             OffsetDateTime,
    val audienceLevel:         AudienceLevel,        // BEGINNER | INTERMEDIATE | ADVANCED
    val language:              String,
    val status:                TopicStatus,          // PENDING | GENERATING | ACTIVE | EXPIRED | FAILED
    val createdBy:             String,               // Admin userId
    val createdAt:             OffsetDateTime,
    val version:               Long,                 // JPA optimistic lock version
    val statusUpdatedAt:       OffsetDateTime        // tracks last status transition time
)
```

### User (identity)

```kotlin
data class User(
    val id:                String,
    val email:             String,
    val name:              String?,
    val avatarUrl:         String?,
    val role:              UserRole,         // USER | ADMIN
    val provider:          AuthProvider,
    val providerId:        String?,
    val preferredLanguage: String,
    val audienceLevel:     String,
    val createdAt:         OffsetDateTime
)
```

---

## TopicResponse Fields

| Field | Type | Notes |
|---|---|---|
| `id` | String | UUID |
| `title` | String | |
| `description` | String | |
| `category` | String | |
| `status` | String | `PENDING` \| `GENERATING` \| `ACTIVE` \| `EXPIRED` \| `FAILED` |
| `createdAt` | OffsetDateTime | |
| `durationDays` | Int | alias for `subscriptionWindowDays` |
| `difficulty` | String | alias for `audienceLevel` |
| `contentSourceType` | String | `URL` \| `PDF` |
| `taskCount` | Int | |
| `subscriptionWindowDays` | Int | |
| `audienceLevel` | String | |
| `language` | String | |
| `expiresAt` | OffsetDateTime | |
| `createdBy` | String | admin userId |
| `stats` | TopicStats? | always `null` in current responses — see note below |
| `contentTruncated` | Boolean | `true` when extracted content exceeded 8000 characters and was silently truncated before being sent to the AI |

> **TopicStats note**: The `stats` object is `null` in all current responses. It will be wired to real enrollment data (enrolled users, completion rate, average score, active users) in Phase 4 when the enrollment domain is integrated.

---

## Error Responses

| Scenario | HTTP | Body |
|---|---|---|
| Missing / invalid JWT | 401 | `{ "error": "..." }` |
| Valid JWT but not ADMIN | 403 | `{ "error": "Insufficient role" }` |
| Validation failure | 400 | `{ "field": "...", "reason": "..." }` |
| Topic not found | 404 | `{ "resourceType": "Topic", "id": "..." }` |
| Topic already generating | 409 | `{ "error": "ConcurrentModification" }` |
| Concurrent regenerate (optimistic lock) | 409 | `{ "error": "ConcurrentModification" }` |
| Storage / AI failure | 500 | `{ "error": "UnexpectedException" }` |

---

## Infrastructure Ports (Abstractions)

| Port | Implementation | Purpose |
|---|---|---|
| `ContentExtractorPort` | HTTP scraper / PDF parser | Converts URL or PDF bytes → plain text |
| `FileStoragePort` | MinIO adapter | Stores and retrieves content blobs (returns object key, not full URL) |
| `TaskGenerationPort` | Mistral AI HTTP adapter (`TaskGenerationPort.kt`) | Generates task templates from content |
| `TopicRepository` | JPA / PostgreSQL adapter | Persists `Topic` entities |
| `UserRepository` | JPA / PostgreSQL adapter | Persists `User` / `Admin` entities |

---

## Quick Reference

| Operation | Method | Path | Side Effects |
|---|---|---|---|
| Get own profile | GET | `/admin/me` | none |
| Create topic from URL | POST | `/admin/topics` | persist topic, upload to MinIO, trigger AI generation |
| Create topic from PDF | POST | `/admin/topics/pdf` | persist topic, upload PDF to MinIO, trigger AI generation |
| List all topics (paginated) | GET | `/admin/topics` | none |
| List topics by status | GET | `/admin/topics?status=ACTIVE` | none |
| Get topic by ID | GET | `/admin/topics/{id}` | none |
| Regenerate topic tasks | POST | `/admin/topics/{id}/regenerate` | delete old tasks, trigger AI re-generation |
