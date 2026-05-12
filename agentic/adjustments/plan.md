# P001 Implementation Plan
**Source:** `docs/adr/ADR-021.md` + `docs/plan/P001.md`
**Generated:** 2026-05-12
**Branch:** `feat/phase-05-mobile-notifications`
**Next Flyway migration:** V22

---

## Pre-flight: Already Fixed (verify only, no code change)

| Finding | Why it is already fixed |
|---------|------------------------|
| F-11 option shuffle end-to-end | `TaskResponse.from()` line 19–21: `assignment.optionOrder.mapNotNull { originalIdx -> template.options?.getOrNull(originalIdx) }` — options ARE re-mapped through shuffledOrder before serialisation. |
| F-18 adaptive task bounds check | `AdaptiveTaskService.kt` lines 67–74: `ensure(command.selectedOption >= 0)` + `task.optionOrder.getOrNull(command.selectedOption).ensure(not null)` — both guards already in place. |
| F-24 ORDER BY on findByTaskTemplateId | `TaskInstanceRepositoryAdapter.findByTaskTemplateId` calls `jpa.findByTaskTemplateIdOrderByInstanceIndex(...)` — already ordered stably by `instanceIndex`. |

---

## Deferred / Accepted (docs only, no code)

| Finding | Action | File |
|---------|--------|------|
| F-02 post-auth 401 always fires | Add entry to `agentic/DECISIONS.md`: "Ktor bearer plugin caches `loadTokens` result; first post-login request always costs one extra 401+refresh round-trip. Accepted per ADR-021 D1. Fix would require custom BearerAuthProvider." | `agentic/DECISIONS.md` |
| F-06 logout clears local storage before server call | Add entry to `agentic/security/THREAT-LOG.md` KNOWN RISKS table: R18, A07, Low. "Tokens cleared locally before server call on logout. Residual risk: stolen device within 7-day refresh window can re-auth." | `agentic/security/THREAT-LOG.md` |
| F-16 adaptive task shuffle unseeded | Add entry to `agentic/DECISIONS.md`: "Adaptive task option shuffle uses `java.util.Random()` with no seed. Accepted: shuffle is stored at generation time (stable per session), adaptive tasks are remediation not anti-cheat, each session is unique to one user." | `agentic/DECISIONS.md` |
| F-20 shared generationExecutor pool | Add entry to `agentic/DECISIONS.md`: "Struggle generation shares `generationExecutor` pool (size 3, queue 25) with topic generation. Accepted per ADR-021 D2. Revisit in Phase 06 if concurrent admin topic creation is observed to starve struggle." | `agentic/DECISIONS.md` |
| F-23 no @Valid on POST /auth/magic-link | No action — already tracked as R16 in THREAT-LOG, scheduled for Phase 07. |  |

---

## Commit Group 1 — Mobile Auth Fixes (Critical, implement first)
**Scope:** `composeApp`
**Commit:** `fix(mobile): prevent concurrent 401 race and spurious cold-start session expiry`

### 1-A — F-01: Mutex in `refreshTokens` to prevent token family revocation

**File:** `composeApp/src/commonMain/kotlin/com/ureka/play4change/core/network/HttpClientFactory.kt`

**Root cause:** When access token expires on home-screen load, 2–3 requests fire simultaneously. All get 401. All call `refreshTokens`. Second call presents the already-rotated (used) refresh token → server revokes family → force-logout.

**Exact change:**

Add imports at top of file (after existing imports):
```kotlin
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
```

In `create()` (currently line 47), before `if (engine != null)`:
```kotlin
val refreshMutex = Mutex()
```

In `applyConfig()`, replace the entire `refreshTokens { ... }` block (currently lines 81–110) with:
```kotlin
refreshTokens {
    refreshMutex.withLock {
        // If another coroutine already refreshed while we waited for the lock,
        // its new access token is now in storage — return it without a server call.
        val storedAccess = tokenStorage.getAccessToken()
        if (storedAccess != null && storedAccess != oldTokens?.accessToken) {
            val storedRefresh = tokenStorage.getRefreshToken() ?: return@withLock null
            return@withLock BearerTokens(storedAccess, storedRefresh)
        }

        val refreshToken = oldTokens?.refreshToken ?: tokenStorage.getRefreshToken()
        if (refreshToken == null) {
            // Only signal session expiry if the user had tokens before.
            // Cold-start (no tokens ever stored): return null without navigating to login.
            if (oldTokens != null) {
                tokenStorage.clear()
                onSessionExpired()
            }
            return@withLock null
        }

        try {
            val response = client.post("${networkConfig.baseUrl}/auth/refresh") {
                markAsRefreshTokenRequest()
                contentType(ContentType.Application.Json)
                setBody(RefreshBody(refreshToken))
            }

            if (response.status.isSuccess()) {
                val tokens = Json.decodeFromString<TokensBody>(response.bodyAsText())
                tokenStorage.store(tokens.accessToken, tokens.refreshToken)
                BearerTokens(tokens.accessToken, tokens.refreshToken)
            } else {
                tokenStorage.clear()
                onSessionExpired()
                null
            }
        } catch (_: Exception) {
            tokenStorage.clear()
            onSessionExpired()
            null
        }
    }
}
```

**Note on `create()` signature:** The `refreshMutex` is created in `create()`, not in `applyConfig()`. `applyConfig()` is a private extension on `HttpClientConfig`. To close over `refreshMutex`, you need to pass it into `applyConfig()` OR create it as a local val in `create()` and reference it in the lambda passed to `applyConfig()`. The cleanest approach is to move the `Mutex` creation into `create()` and thread it through:

```kotlin
fun create(
    tokenStorage: TokenStorage,
    networkConfig: NetworkConfig,
    onSessionExpired: () -> Unit,
    engine: HttpClientEngine? = null,
): HttpClient {
    val refreshMutex = Mutex()
    return if (engine != null) {
        HttpClient(engine) { applyConfig(tokenStorage, networkConfig, onSessionExpired, refreshMutex) }
    } else {
        platformHttpClient { applyConfig(tokenStorage, networkConfig, onSessionExpired, refreshMutex) }
    }
}

private fun HttpClientConfig<*>.applyConfig(
    tokenStorage: TokenStorage,
    networkConfig: NetworkConfig,
    onSessionExpired: () -> Unit,
    refreshMutex: Mutex,
) { ... }
```

**Tests to write:**
File: `composeApp/src/commonTest/kotlin/com/ureka/play4change/core/network/HttpClientFactoryTest.kt`
- `given two concurrent 401 responses when refreshTokens then only one refresh call is made`
- `given null refresh token after cold start when 401 received then session expired not called`
- Use `MockEngine` + coroutine test with `async` to simulate concurrent 401s.

---

### 1-B — F-04: Add `X-Timezone` to every authenticated request

**File:** `composeApp/src/commonMain/kotlin/com/ureka/play4change/core/network/HttpClientFactory.kt`

**Exact change:** In `defaultRequest { }` block (currently lines 115–118), add one line after the `X-Request-ID` header:
```kotlin
defaultRequest {
    url(networkConfig.baseUrl)
    header("X-Request-ID", Uuid.random().toString())
    header("X-Timezone", TimeZone.currentSystemDefault().id)
}
```

Add import: `import kotlinx.datetime.TimeZone`
(Already available: `libs.kotlinx.datetime` is in `commonMain.dependencies` in `composeApp/build.gradle.kts`.)

**Tests:** No dedicated unit test. Covered by F-14 integration verification.

---

## Commit Group 2 — Server Config Fixes
**Scope:** `server`
**Commit:** `fix(server): env-var config for DB password, APP_BASE_URL, HTTP Basic, Mistral model`

### 2-A — F-07: `APP_BASE_URL` must be explicitly set (no default)

**File:** `server/src/main/resources/application.yml`

**Current (line 80):**
```yaml
app:
  base-url: ${APP_BASE_URL:http://localhost:5173}
```

**Change to:**
```yaml
app:
  base-url: ${APP_BASE_URL}
```

**Side effect:** Server will fail to start if `APP_BASE_URL` is not set. Update `docs/plan/` or `demo/HOW_TO_RUN.md` (when it exists) to document this required env var.

**Docker Compose check:** `docker-compose.yml` line 76 already sets `APP_BASE_URL: ${FRONTEND_ORIGIN}`. Add `APP_BASE_URL` to the local `.env` example with value `http://localhost:8080` (for bare-server use) or `https://radesh-govind.com` (production equivalent).

No code test needed — startup failure is the guard.

---

### 2-B — F-08: DB credentials via env vars

**File:** `server/src/main/resources/application.yml`

**Current (lines 9–11):**
```yaml
datasource:
  url: ${SPRING_DATASOURCE_URL:jdbc:postgresql://play4change-postgres:5432/play4change}
  username: play4change
  password: play4change
```

**Change to:**
```yaml
datasource:
  url: ${SPRING_DATASOURCE_URL:jdbc:postgresql://play4change-postgres:5432/play4change}
  username: ${DB_USERNAME}
  password: ${DB_PASSWORD}
```

**Docker Compose update:** Add to the `server` service environment block in `docker-compose.yml`:
```yaml
DB_USERNAME: play4change
DB_PASSWORD: play4change
```
(Matches the existing Postgres container credentials.)

---

### 2-C — F-09: Disable Spring HTTP Basic auth

**File:** `server/src/main/kotlin/com/ureka/play4change/infra/config/SecurityConfig.kt`

**Current `securityFilterChain` (lines 43–69):**
```kotlin
http
    .cors { ... }
    .csrf { it.disable() }
    .sessionManagement { ... }
    .authorizeHttpRequests { ... }
    .exceptionHandling { ... }
    .addFilterBefore(...)
```

**Add after `.csrf { it.disable() }`:**
```kotlin
.httpBasic { it.disable() }
.formLogin { it.disable() }
```

**File:** `server/src/main/resources/application.yml`

Add under `spring:` section:
```yaml
spring:
  security:
    user:
      password: "{noop}disabled-not-used"
      name: disabled
```

This suppresses the "Using generated security password" startup log and disables the default user entirely.

**Test:** `SecurityConfigTest` (`@WebMvcTest`) — `GET /profile` with HTTP Basic credentials returns 401 (or the request is rejected before reaching the controller).

---

### 2-D — F-10: Mistral model env-var configurable

**File:** `server/src/main/resources/application.yml`

**Current (line 35):**
```yaml
ai:
  mistral:
    model: mistral-small-latest
```

**Change to:**
```yaml
ai:
  mistral:
    model: ${MISTRAL_MODEL:mistral-small-latest}
```

No tests needed — purely config.

---

## Commit Group 3 — Server Bug Fixes: Task Lifecycle & API
**Scope:** `server`
**Commit:** `fix(enrollment): dueAt midnight, badge count, report by assignmentId, duplicate enrollment, empty error bodies`

### 3-A — F-05-A: `GET /profile` returns 400 instead of 401 when unauthenticated

**Root cause:** Spring Security calls `ExceptionTranslationFilter` when an unauthenticated request hits a protected endpoint. However, when the method parameter `@AuthenticationPrincipal userId: String` (non-nullable Kotlin `String`) fails to resolve because the SecurityContext has no authentication, Spring MVC throws `IllegalArgumentException` → 400 Bad Request BEFORE Spring Security's `AccessDeniedException` path fires. Spring Security's authorization check and the controller's parameter binding happen in sequence; parameter binding can short-circuit with a 400 for non-nullable principals.

**File:** `server/src/main/kotlin/com/ureka/play4change/web/user/UserProfileController.kt`

**Current (line 16):**
```kotlin
fun getProfile(@AuthenticationPrincipal userId: String): ResponseEntity<UserProfileResponse>
```

**Change to:**
```kotlin
fun getProfile(@AuthenticationPrincipal userId: String?): ResponseEntity<UserProfileResponse> {
    if (userId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
    return getUserProfileUseCase.execute(userId).fold(
        ifLeft = { ResponseEntity.notFound().build() },
        ifRight = { profile -> ResponseEntity.ok(UserProfileResponse.from(profile)) }
    )
}
```

Add import: `import org.springframework.http.HttpStatus`

**Test:** `UserProfileControllerTest` (`@WebMvcTest`) — `GET /profile` without `Authorization` header returns 401.

---

### 3-B — F-12: Badge check uses actual template count, not requested taskCount

**Root cause:** `BadgeIssuanceService.issueBadge()` compares `correctCount >= topic.taskCount`, where `topic.taskCount` is the number requested at topic creation. If Mistral rate-limits and generates fewer tasks, the threshold is never reached and no badge is ever issued.

**Fix A+C (both required):**

**Fix C — Update `topic.taskCount` to actual count after generation:**

Find where topic transitions to ACTIVE and task templates are persisted. This is in `TopicManagementService` (or the inline pipeline service). After templates are saved, update `topic.taskCount` to `templates.size`.

File: `server/src/main/kotlin/com/ureka/play4change/application/topic/TopicManagementService.kt`

Search for the ACTIVE transition code. After the `taskTemplates` list is built and saved, add:
```kotlin
val actualCount = savedTemplates.size
topicRepository.save(topic.copy(taskCount = actualCount))
```

**Fix A — Compare against actual template count in BadgeIssuanceService:**

File: `server/src/main/kotlin/com/ureka/play4change/application/badge/BadgeIssuanceService.kt`

Add dependency injection of `TaskTemplateRepository` and `TopicModuleRepository` in constructor:
```kotlin
class BadgeIssuanceService(
    private val badgeRepository: BadgeRepository,
    private val enrollmentRepository: EnrollmentRepository,
    private val topicRepository: TopicRepository,
    private val taskTemplateRepository: TaskTemplateRepository,
    private val topicModuleRepository: TopicModuleRepository
) : BadgeIssuancePort {
```

Change `issueBadge()` (currently lines 23–38):
```kotlin
override fun issueBadge(userId: String, topicId: String, enrollmentId: String) {
    topicRepository.findById(topicId) ?: run {
        log.warn("Badge issuance skipped — topic {} not found", topicId)
        return
    }
    val module = topicModuleRepository.findByTopicId(topicId).firstOrNull() ?: run {
        log.warn("Badge issuance skipped — no module for topic {}", topicId)
        return
    }
    val actualTemplateCount = taskTemplateRepository.findCurrentByModuleId(module.id).size
    if (actualTemplateCount == 0) {
        log.warn("Badge issuance skipped — no templates in topic {}", topicId)
        return
    }
    val assignments = enrollmentRepository.findAssignmentsByEnrollmentId(enrollmentId)
    val correctCount = assignments.count { it.isCorrect == true && it.status != AssignmentStatus.PENDING }
    if (correctCount >= actualTemplateCount) {
        issueIfNotYetEarned(userId, topicId)
    } else {
        log.debug(
            "Badge not yet earned for user {} in topic {} — {}/{} tasks correct",
            userId, topicId, correctCount, actualTemplateCount
        )
    }
}
```

**Tests:**
File: `server/src/test/kotlin/com/ureka/play4change/application/badge/BadgeIssuanceServiceTest.kt`
- `given topic with 2 requested but 1 actual template when user completes 1 task then badge is issued`
- `given topic with 2 requested and 2 actual templates when user completes 1 task then badge not issued`

---

### 3-C — F-14: First assignment `dueAt` uses midnight instead of `now + 24h`

**Depends on:** F-04 (X-Timezone header must be sent by the mobile client)

**File:** `server/src/main/kotlin/com/ureka/play4change/application/port/EnrollCommand.kt`

Check current `EnrollCommand` definition — add `timezone: String?` field if not present.

**File:** `server/src/main/kotlin/com/ureka/play4change/web/user/EnrollmentController.kt`

Current `enroll()` (line 17–24):
```kotlin
@PostMapping("/{topicId}/enroll")
fun enroll(
    @PathVariable topicId: String,
    @AuthenticationPrincipal userId: String
): ResponseEntity<EnrollmentResponse>
```

Change to:
```kotlin
@PostMapping("/{topicId}/enroll")
fun enroll(
    @PathVariable topicId: String,
    @AuthenticationPrincipal userId: String,
    @RequestHeader(value = "X-Timezone", required = false) timezone: String?
): ResponseEntity<EnrollmentResponse> =
    enrollmentUseCase.enroll(EnrollCommand(userId, topicId, timezone)).fold(...)
```

**File:** `server/src/main/kotlin/com/ureka/play4change/application/enrollment/EnrollmentService.kt`

Current line 99:
```kotlin
dueAt = now.plusHours(24),
```

Change to:
```kotlin
dueAt = DayIndexCalculator.startOfTomorrow(command.timezone),
```

Add import if needed: `import com.ureka.play4change.application.enrollment.DayIndexCalculator`

**Tests:**
File: `server/src/test/kotlin/com/ureka/play4change/application/enrollment/EnrollmentServiceTest.kt`
- `given enrollment at 10 PM UTC when timezone is UTC+1 then first assignment dueAt is midnight UTC next day`
- `given enrollment at 10 PM UTC when no timezone header then first assignment dueAt is midnight UTC`

---

### 3-D — F-15: Report endpoint accepts `assignmentId` instead of `taskTemplateId`

**Root cause:** `GET /tasks/today` response does not include `taskTemplateId`. The report endpoint `POST /tasks/{taskId}/report` currently uses `taskId` as `taskTemplateId`. The client only has `assignmentId`.

**File:** `server/src/main/kotlin/com/ureka/play4change/web/user/TaskReportController.kt`

Current (line 20): `@PostMapping("/{taskId}/report")`

Change path variable semantic: rename `taskId` to `assignmentId` and resolve the `taskTemplateId` from it.

```kotlin
@PostMapping("/{assignmentId}/report")
fun reportTask(
    @PathVariable assignmentId: String,
    @RequestBody request: ReportTaskRequest,
    @AuthenticationPrincipal userId: String
): ResponseEntity<Map<String, String>> {
    require(request.reason.isNotBlank()) { "reason must not be blank" }
    require(request.reason.length <= MAX_REASON_LENGTH) { "reason must be at most $MAX_REASON_LENGTH characters" }

    return taskReportUseCase.reportTask(
        ReportTaskCommand(
            userId = userId,
            assignmentId = assignmentId,   // ← changed from taskTemplateId
            reason = request.reason
        )
    ).fold(...)
}
```

**File:** `server/src/main/kotlin/com/ureka/play4change/application/port/ReportTaskCommand.kt`

Change `taskTemplateId: String` to `assignmentId: String` (or add `assignmentId` and keep both temporarily — prefer clean rename).

**File:** `server/src/main/kotlin/com/ureka/play4change/application/report/TaskReportService.kt`

Current line 42: `ensureNotNull(taskTemplateRepository.findById(command.taskTemplateId)) { ... }`

Add `EnrollmentRepository` to constructor. In `reportTask()`:
```kotlin
val assignment = ensureNotNull(enrollmentRepository.findAssignmentById(command.assignmentId)) {
    NotFound.ResourceNotFound("TaskAssignment", command.assignmentId)
}
val taskTemplateId = assignment.taskTemplateId
ensureNotNull(taskTemplateRepository.findById(taskTemplateId)) {
    NotFound.ResourceNotFound("TaskTemplate", taskTemplateId)
}
// ... rest of the method uses taskTemplateId
```

**Tests:**
File: `server/src/test/kotlin/com/ureka/play4change/application/report/TaskReportServiceTest.kt`
- `given valid assignmentId when reportTask then report created with correct templateId`
- `given invalid assignmentId when reportTask then returns NotFound`

---

### 3-E — F-21: 400 responses include error body (TopicController)

**File:** `server/src/main/kotlin/com/ureka/play4change/web/admin/TopicController.kt`

Current (lines 114–116):
```kotlin
@Suppress("UNCHECKED_CAST")
private fun <T> AppError.toErrorResponse(): ResponseEntity<T> =
    ResponseEntity.status(httpStatus).build()
```

Change to:
```kotlin
@Suppress("UNCHECKED_CAST")
private fun <T> AppError.toErrorResponse(): ResponseEntity<T> =
    ResponseEntity.status(httpStatus)
        .body(mapOf("message" to messageKey, "params" to params) as T)
```

**Note:** `AppError` has `messageKey: String` and `params: List<Any>` — these are i18n keys (e.g., `"error.bad_request.invalid_field"`). For now, surfacing the key is sufficient; Phase 07 can add i18n resolution if needed.

**Tests:**
File: `server/src/test/kotlin/com/ureka/play4change/web/admin/TopicControllerTest.kt`
- `given subscriptionWindowDays less than 3 when POST admin/topics then returns 400 with message body`
- `given taskCount 0 when POST admin/topics then returns 400 with message body`

---

### 3-F — F-22: Use `DuplicateResource` instead of `ConcurrentModification` for double enrollment

**File:** `server/src/main/kotlin/com/ureka/play4change/application/enrollment/EnrollmentService.kt`

Current (line 52):
```kotlin
ensure(enrollmentRepository.findByUserIdAndTopicId(command.userId, command.topicId) == null) {
    Conflict.ConcurrentModification
}
```

Change to:
```kotlin
ensure(enrollmentRepository.findByUserIdAndTopicId(command.userId, command.topicId) == null) {
    Conflict.DuplicateResource("Enrollment", "userId+topicId")
}
```

`Conflict.DuplicateResource` already exists in `common/src/commonMain/kotlin/com/ureka/play4change/error/client/Conflict.kt` (line 11) and maps to 409 — correct HTTP semantics.

**Tests:**
File: `server/src/test/kotlin/com/ureka/play4change/application/enrollment/EnrollmentServiceTest.kt`
- `given already enrolled user when enroll then returns DuplicateResource 409`

---

### 3-G — F-25: Remove dead `DELETE /auth/logout`

**Pre-check:** Search tests for `DELETE /auth/logout` usage before removing.
```
grep -r "DELETE.*logout\|delete.*logout" server/src/test --include="*.kt"
```

**File:** `server/src/main/kotlin/com/ureka/play4change/auth/adapter/inbound/web/AuthController.kt`

Remove lines 51–55 (the `@DeleteMapping("/logout")` method):
```kotlin
@DeleteMapping("/logout")
fun logout(@RequestBody request: RefreshRequest): ResponseEntity<Void> {
    tokenUseCase.revoke(request.refreshToken)
    return ResponseEntity.noContent().build()
}
```

Remove unused import `import io.ktor.client.request.delete` if it exists in `HttpAuthRepository.kt` (it does exist at line 16 — remove it).

**Tests:** Verify no existing test uses `DELETE /auth/logout`. If any does, update it to `POST /auth/logout`.

---

## Commit Group 4 — Server Bug Fixes: Struggle Flow
**Scope:** `server`
**Commit:** `fix(struggle): TIME_PRESSURE mapping, single-session enforcement, GET returns resolved session, restore pre-struggle streak`

### 4-A — F-17: `TIME_PRESSURE` maps to `UNKNOWN` in AI context

**File:** `ai-agent/api/src/main/kotlin/com/ureka/play4change/model/StruggleContext.kt`

Current `ErrorPattern` enum (lines 19–25):
```kotlin
enum class ErrorPattern {
    CONCEPTUAL_MISUNDERSTANDING,
    PROCEDURAL_ERROR,
    KNOWLEDGE_GAP,
    UNCLEAR_INSTRUCTIONS,
    UNKNOWN
}
```

Add `TIME_PRESSURE`:
```kotlin
enum class ErrorPattern {
    CONCEPTUAL_MISUNDERSTANDING,
    PROCEDURAL_ERROR,
    KNOWLEDGE_GAP,
    UNCLEAR_INSTRUCTIONS,
    TIME_PRESSURE,         // ← new
    UNKNOWN
}
```

**File:** `server/src/main/kotlin/com/ureka/play4change/application/struggle/HandleStruggleService.kt`

Current mapping (line 160):
```kotlin
ErrorPattern.TIME_PRESSURE -> com.ureka.play4change.model.ErrorPattern.UNKNOWN
```

Change to:
```kotlin
ErrorPattern.TIME_PRESSURE -> com.ureka.play4change.model.ErrorPattern.TIME_PRESSURE
```

**Tests:**
File: `server/src/test/kotlin/com/ureka/play4change/application/struggle/HandleStruggleServiceTest.kt`
- `given TIME_PRESSURE error pattern when mapErrorPattern then returns TIME_PRESSURE not UNKNOWN`

---

### 4-B — F-19: Enforce single OPEN struggle session per enrollment

**Root cause:** If `triggerAsync` is called twice for the same enrollment (e.g., two rapid wrong submissions), two OPEN sessions can exist. `findOpenByEnrollmentId` returns non-deterministically.

**File:** `server/src/main/kotlin/com/ureka/play4change/application/struggle/HandleStruggleService.kt`

In `triggerAsync()`, BEFORE creating the new session (currently line 64: `val session = struggleRepository.save(StruggleSession(...))`), add:

```kotlin
// Ensure only one OPEN session per enrollment — abandon any existing one.
val existingOpen = struggleRepository.findOpenByEnrollmentId(enrollmentId)
if (existingOpen != null) {
    log.warn("Abandoning existing open struggle session {} for enrollment {} before creating new one",
        existingOpen.id, enrollmentId)
    struggleRepository.save(existingOpen.abandon())
}
```

**Tests:**
File: `server/src/test/kotlin/com/ureka/play4change/application/struggle/HandleStruggleServiceTest.kt`
- `given existing open session when new struggle triggered then existing session is abandoned`

---

### 4-C — F-13: `GET /struggle/enrollment/{id}` returns session regardless of status

**Root cause:** `AdaptiveTaskService.getSession()` calls `struggleRepository.findOpenByEnrollmentId(enrollmentId)` — only finds OPEN sessions. After resolution, endpoint returns 404.

**Step 1 — Extend `StruggleRepository` domain interface:**

File: `server/src/main/kotlin/com/ureka/play4change/domain/struggle/StruggleRepository.kt`

Add:
```kotlin
fun findLatestByEnrollmentId(enrollmentId: String): StruggleSession?
```

**Step 2 — Extend `StruggleSessionJpaRepository`:**

File: `server/src/main/kotlin/com/ureka/play4change/infrastructure/persistence/repository/StruggleSessionJpaRepository.kt`

Add:
```kotlin
fun findTopByEnrollmentIdOrderByDetectedAtDesc(enrollmentId: String): StruggleSessionEntity?
```

**Step 3 — Implement in `StruggleRepositoryAdapter`:**

File: `server/src/main/kotlin/com/ureka/play4change/infrastructure/persistence/adapter/StruggleRepositoryAdapter.kt`

Add after `findOpenByEnrollmentId`:
```kotlin
override fun findLatestByEnrollmentId(enrollmentId: String): StruggleSession? =
    jpa.findTopByEnrollmentIdOrderByDetectedAtDesc(enrollmentId)?.toDomain()
```

**Step 4 — Update `AdaptiveTaskService.getSession()`:**

File: `server/src/main/kotlin/com/ureka/play4change/application/struggle/AdaptiveTaskService.kt`

Current (line 39–41):
```kotlin
ensureNotNull(struggleRepository.findOpenByEnrollmentId(enrollmentId)) {
    NotFound.ResourceNotFound("StruggleSession", enrollmentId)
}
```

Change to:
```kotlin
ensureNotNull(struggleRepository.findLatestByEnrollmentId(enrollmentId)) {
    NotFound.ResourceNotFound("StruggleSession", enrollmentId)
}
```

**Step 5 — Ensure `StruggleSessionResponse` includes status field:**

File: `server/src/main/kotlin/com/ureka/play4change/web/user/dto/StruggleSessionResponse.kt`

Check if `status` is already in the DTO. If not, add it:
```kotlin
data class StruggleSessionResponse(
    ...
    val status: String,   // OPEN | RESOLVED | ABANDONED
    ...
)
```

And in `StruggleSessionResponse.from(session)`:
```kotlin
status = session.status.name,
```

**Tests:**
File: `server/src/test/kotlin/com/ureka/play4change/application/struggle/AdaptiveTaskServiceTest.kt`
- `given resolved struggle session when getSession called then returns session with RESOLVED status`
- `given no session for enrollment when getSession called then returns NotFound`

---

### 4-D — F-27: Restore pre-struggle streak on successful struggle resolution

**Root cause:** When the 2nd wrong answer is submitted, `enrollment.resetStreak()` is called. After struggle resolution + correct retry, streak is 1 not (pre-struggle-streak + 1).

**Fix requires Flyway migration V22.**

**Step 1 — Flyway migration:**

File: `server/src/main/resources/db/migration/V22__add_pre_struggle_streak_to_sessions.sql`

```sql
ALTER TABLE struggle_sessions
    ADD COLUMN pre_struggle_streak_days INT NOT NULL DEFAULT 0;
```

**Step 2 — Domain model:**

File: `server/src/main/kotlin/com/ureka/play4change/domain/struggle/StruggleSession.kt`

Add field:
```kotlin
data class StruggleSession(
    val id: String,
    val enrollmentId: String,
    val originalTaskAssignmentId: String,
    val errorPattern: ErrorPattern,
    val attemptCount: Int,
    val detectedAt: OffsetDateTime,
    val resolvedAt: OffsetDateTime?,
    val status: StruggleStatus,
    val adaptiveTasks: List<AdaptiveTask>,
    val preStruggleStreakDays: Int = 0     // ← new field
)
```

**Step 3 — JPA entity:**

File: `server/src/main/kotlin/com/ureka/play4change/infrastructure/persistence/entity/StruggleSessionEntity.kt`

Add field:
```kotlin
@Column(name = "pre_struggle_streak_days", nullable = false)
var preStruggleStreakDays: Int = 0
```

**Step 4 — `StruggleRepositoryAdapter` — persist and restore the new field:**

File: `server/src/main/kotlin/com/ureka/play4change/infrastructure/persistence/adapter/StruggleRepositoryAdapter.kt`

In `save()` entity construction, set: `preStruggleStreakDays = session.preStruggleStreakDays`

In `StruggleSessionEntity.toDomain()`, read: `preStruggleStreakDays = preStruggleStreakDays`

**Step 5 — `TaskService.submitAnswer()` — pass streak to `triggerAsync`:**

File: `server/src/main/kotlin/com/ureka/play4change/application/enrollment/TaskService.kt`

The enrollment's streak is computed at line 234–244 (`updatedEnrollment`). The `triggerAsync` is called at line 205 — BEFORE the streak is reset. At that point, `enrollment.streakDays` still holds the pre-reset value.

Change `triggerAsync` call (lines 205–211):
```kotlin
handleStruggleService.triggerAsync(
    enrollmentId = assignment.enrollmentId,
    assignmentId = assignment.id,
    errorPattern = pattern,
    template = template,
    userId = command.userId,
    preStruggleStreakDays = enrollment.streakDays    // ← pass current streak before reset
)
```

**Step 6 — `HandleStruggleService.triggerAsync()` — accept and persist:**

File: `server/src/main/kotlin/com/ureka/play4change/application/struggle/HandleStruggleService.kt`

Add `preStruggleStreakDays: Int = 0` parameter to `triggerAsync()`:
```kotlin
@Async("generationExecutor")
fun triggerAsync(
    enrollmentId: String,
    assignmentId: String,
    errorPattern: ErrorPattern,
    template: TaskTemplate,
    userId: String,
    preStruggleStreakDays: Int = 0    // ← new parameter
) {
```

In the session creation, pass the streak:
```kotlin
val session = struggleRepository.save(
    StruggleSession(
        ...
        preStruggleStreakDays = preStruggleStreakDays
    )
)
```

**Step 7 — `AdaptiveTaskService.submitAdaptiveTask()` — restore streak on resolution:**

File: `server/src/main/kotlin/com/ureka/play4change/application/struggle/AdaptiveTaskService.kt`

After the original assignment reset block (lines 100–114), add streak restoration:
```kotlin
if (allComplete) {
    // Reset original assignment (already done above)

    // Restore pre-struggle streak + 1 (correct retry adds 1 via TaskService.submitAnswer)
    // We restore to pre-struggle value; the +1 for the correct retry
    // is added by TaskService.submitAnswer when the user retries and gets it right.
    val restoredEnrollment = enrollment.copy(streakDays = resolvedSession.preStruggleStreakDays)
    enrollmentRepository.save(restoredEnrollment)
    log.info(
        "Streak restored to {} for enrollment {} after struggle resolution",
        resolvedSession.preStruggleStreakDays, session.enrollmentId
    )
}
```

**Note on semantics:** `preStruggleStreakDays` stores the streak value AT the moment struggle was triggered (BEFORE `TaskService` resets it). After resolution, `enrollment.streakDays` is restored to this value. When the user correctly retries the original task, `TaskService.submitAnswer()` calls `enrollment.addPoints(...).incrementStreak()` — so the final streak = `preStruggleStreakDays + 1`.

**Tests:**
File: `server/src/test/kotlin/com/ureka/play4change/application/struggle/AdaptiveTaskServiceTest.kt`
- `given user had streak 5 before struggle when all adaptive tasks complete then enrollment streak restored to 5`
- `given user had streak 0 before struggle when all adaptive tasks complete then enrollment streak stays 0`

---

## Commit Group 5 — iOS Base URL Configurable
**Scope:** `composeApp` (iosMain + Xcode project)
**Commit:** `fix(mobile): make iOS base URL configurable via xcconfig`

### 5-A — F-03: iOS base URL configurable (xcconfig approach)

**Note:** `buildkonfig` plugin is NOT in the project. The xcconfig approach achieves the same result without adding a new plugin dependency.

**Step 1 — Create xcconfig files:**

File: `composeApp/iosApp/Debug.xcconfig` (new file):
```
BASE_URL = http://localhost:8080
```

File: `composeApp/iosApp/Release.xcconfig` (new file):
```
BASE_URL = https://api.play4change.app
```

**Step 2 — Update `Info.plist`:**

File: `composeApp/iosApp/iosApp/Info.plist`

Add inside `<dict>`:
```xml
<key>BASE_URL</key>
<string>$(BASE_URL)</string>
```

**Step 3 — Assign xcconfig to Xcode build configurations:**

In `composeApp/iosApp/iosApp.xcodeproj/project.pbxproj` (or via Xcode UI):
- Set `Debug` configuration → `Debug.xcconfig`
- Set `Release` configuration → `Release.xcconfig`

**Step 4 — Read from bundle in `PlatformModule.ios.kt`:**

File: `composeApp/src/iosMain/kotlin/com/ureka/play4change/di/PlatformModule.ios.kt`

Current (line 11):
```kotlin
single { NetworkConfig("http://localhost:8080") }
```

Change to:
```kotlin
single {
    val baseUrl = NSBundle.mainBundle.objectForInfoDictionaryKey("BASE_URL") as? String
        ?: "http://localhost:8080"  // fallback for unit tests that don't use bundle
    NetworkConfig(baseUrl)
}
```

Add import: `import platform.Foundation.NSBundle`

**Test:** iOS build produces a debug binary with `http://localhost:8080` and a release binary with the production URL. Verify with `xcrun simctl launch` and inspect the URL used in network requests.

---

## Commit Group 6 — Broken Topic Admin Endpoint
**Scope:** `server`
**Commit:** `feat(admin): add PATCH /admin/topics/{id}/status to mark broken topics FAILED`

### 6-A — F-26: Admin can mark a topic as FAILED

**Root cause:** If Mistral generates 0 valid tasks, topic reaches ACTIVE with 0 templates. Enrollment returns `"topic has no tasks yet"` (sounds temporary, is permanent). No admin endpoint to fix it.

**Step 1 — Add `FAILED` status if not already in `TopicStatus` enum:**

File: `server/src/main/kotlin/com/ureka/play4change/domain/topic/TopicStatus.kt` (or wherever defined)

Add `FAILED` to the enum. Verify against Flyway migration that stores topic status.

**Step 2 — Add use case method:**

File: `server/src/main/kotlin/com/ureka/play4change/application/port/TopicUseCase.kt`

Add:
```kotlin
fun markFailed(topicId: String): Either<AppError, Topic>
```

**Step 3 — Implement in `TopicManagementService`:**

File: `server/src/main/kotlin/com/ureka/play4change/application/topic/TopicManagementService.kt`

```kotlin
override fun markFailed(topicId: String): Either<AppError, Topic> = either {
    val topic = ensureNotNull(topicRepository.findById(topicId)) {
        NotFound.ResourceNotFound("Topic", topicId)
    }
    ensure(topic.status != TopicStatus.FAILED) {
        Conflict.ConcurrentModification
    }
    topicRepository.save(topic.copy(status = TopicStatus.FAILED))
}
```

**Step 4 — Add endpoint in `TopicController`:**

File: `server/src/main/kotlin/com/ureka/play4change/web/admin/TopicController.kt`

Add after `regenerate()`:
```kotlin
@PatchMapping("/{id}/status/failed")
fun markFailed(
    @PathVariable id: String,
    @AuthenticationPrincipal adminId: String
): ResponseEntity<TopicResponse> =
    topicUseCase.markFailed(id).toResponse(HttpStatus.OK)
```

**Tests:**
File: `server/src/test/kotlin/com/ureka/play4change/web/admin/TopicControllerTest.kt`
- `given active topic with no tasks when PATCH admin/topics/{id}/status/failed then status is FAILED`
- `given already failed topic when PATCH admin/topics/{id}/status/failed then returns 409`

---

## Commit Group 7 — Verification Test (F-11)
**Scope:** `server`
**Commit:** `test(enrollment): verify option shuffle is end-to-end correct`

### 7-A — F-11: Option shuffle end-to-end regression test

**Finding:** Code is correct (`TaskResponse.from()` maps through `optionOrder`). Write a test to prevent regression.

**File:** `server/src/test/kotlin/com/ureka/play4change/web/user/TaskControllerTest.kt`

Test scenario: Given a `TaskTemplate` with options `["A","B","C","D"]` and `correctAnswer = 0` (index of "A"), and a `TaskAssignment` with `optionOrder = [2, 0, 3, 1]` (shuffle: C,A,D,B), when `GET /tasks/today` is called, then:
- `response.options[0]` = "C"
- `response.options[1]` = "A"
- `response.options[2]` = "D"
- `response.options[3]` = "B"

And when `POST /tasks/{id}/submit` is called with `selectedOption = 1` (index of "A" in shuffled order), then `isCorrect = true`.

Test name: `` `given shuffled task when client submits index of shuffled correct option then isCorrect is true` ``

---

## Commit Group 8 — Documentation (no code changes)
**Commit:** `docs(agentic): document accepted trade-offs from P001 audit`

### 8-A — F-02, F-06, F-16, F-20: Accepted trade-offs

**File:** `agentic/DECISIONS.md`

Add entries:
```
[2026-05-12] [mobile/auth] — Ktor BearerAuthProvider cache not invalidated after login
  Post-login, the first request always costs one extra 401+refresh round-trip (B.3 in ADR-021).
  Accepted: UX is invisible (~31ms), the round-trip is functionally correct, and fixing it
  requires replacing the Ktor bearer plugin with a custom interceptor (high risk). See ADR-021 D1.

[2026-05-12] [mobile/struggle] — Adaptive task option shuffle uses unseed java.util.Random
  AdaptiveTask shuffle is stored at generation time; each session is unique to one user;
  adaptive tasks are remediation not anti-cheat. Risk of answer-sharing between users is
  negligible. If regenerated after ABANDONED, the user gets a different ordering, which is
  acceptable. See P001 F-16.

[2026-05-12] [server/struggle] — generationExecutor shared by topic gen and struggle gen
  Both @Async("generationExecutor") calls share pool-size: 3, queue: 25. Acceptable for
  a POC with ≤5 concurrent admins. Revisit in Phase 06 if heavy admin usage starves
  struggle generation. See ADR-021 D2.
```

**File:** `agentic/security/THREAT-LOG.md`

Add to KNOWN RISKS table:
```
| R18 | A07 Auth Failures | Logout clears local tokens before server call. Server token remains valid for up to 7 days if network fails during logout. | Low | ACCEPTED | N/A | Accepted in P001 F-06. Keychain is protected by iOS Data Protection; extraction requires device bypass. Residual risk documented. |
```

---

## Implementation Order & Dependency Map

```
Group 1 (mobile auth) ──── independent, do first
Group 2 (server config) ── independent, parallel with Group 1
Group 3 (server lifecycle) depends on Group 2 (DB password must work)
  └── 3-C (F-14 dueAt) depends on Group 1 (F-04 X-Timezone header)
Group 4 (struggle) ──────── F-27 depends on Flyway V22 being the next available number
Group 5 (iOS base URL) ──── independent, requires Xcode access
Group 6 (admin endpoint) ── independent
Group 7 (test) ──────────── independent
Group 8 (docs) ─────────── do last
```

---

## Checklist of All Findings

| Finding | Status | Commit Group |
|---------|--------|-------------|
| F-01 Concurrent 401 Mutex | Code change | Group 1 |
| F-02 Post-auth 401 always fires | Accepted | Group 8 |
| F-03 iOS base URL hardcoded | Code change | Group 5 |
| F-04 X-Timezone never sent | Code change | Group 1 |
| F-05 Keychain cleared on cold start (client) | Code change | Group 1 |
| F-05 GET /profile returns 400 not 401 (server) | Code change | Group 3 |
| F-06 Logout local-first | Accepted | Group 8 |
| F-07 APP_BASE_URL default wrong | Code change | Group 2 |
| F-08 DB password plaintext | Code change | Group 2 |
| F-09 HTTP Basic enabled | Code change | Group 2 |
| F-10 Mistral model not configurable | Code change | Group 2 |
| F-11 Option shuffle verification | Test only | Group 7 |
| F-12 Badge vs actual template count | Code change | Group 3 |
| F-13 Struggle GET empty on RESOLVED | Code change | Group 4 |
| F-14 First assignment dueAt wrong | Code change | Group 3 |
| F-15 taskTemplateId missing from task response | Code change | Group 3 |
| F-16 Adaptive shuffle unseeded | Accepted | Group 8 |
| F-17 TIME_PRESSURE maps to UNKNOWN | Code change | Group 4 |
| F-18 Adaptive bounds check | Already fixed | — |
| F-19 Multiple OPEN sessions | Code change | Group 4 |
| F-20 Shared thread pool | Accepted | Group 8 |
| F-21 400 responses empty body | Code change | Group 3 |
| F-22 Wrong error type for duplicate enrollment | Code change | Group 3 |
| F-23 No @Valid on magic-link | Deferred Phase 07 | — |
| F-24 findByTaskTemplateId no ORDER BY | Already fixed | — |
| F-25 DELETE /auth/logout dead code | Code change | Group 3 |
| F-26 Broken topic misleading state | Code change | Group 6 |
| F-27 Streak permanently reduced by struggle | Code change | Group 4 |

---

## Files Modified Summary

| File | Findings addressed |
|------|--------------------|
| `composeApp/.../HttpClientFactory.kt` | F-01, F-04, F-05-client |
| `composeApp/iosMain/.../PlatformModule.ios.kt` | F-03 |
| `composeApp/iosApp/Debug.xcconfig` (new) | F-03 |
| `composeApp/iosApp/Release.xcconfig` (new) | F-03 |
| `composeApp/iosApp/iosApp/Info.plist` | F-03 |
| `server/src/main/resources/application.yml` | F-07, F-08, F-09, F-10 |
| `server/.../SecurityConfig.kt` | F-09 |
| `server/.../UserProfileController.kt` | F-05-server |
| `server/.../BadgeIssuanceService.kt` | F-12 |
| `server/.../TopicManagementService.kt` | F-12-C |
| `server/.../EnrollmentController.kt` | F-14 |
| `server/.../EnrollmentService.kt` | F-14, F-22 |
| `server/.../TaskReportController.kt` | F-15 |
| `server/.../TaskReportService.kt` | F-15 |
| `server/.../TopicController.kt` | F-21, F-26 |
| `server/.../AuthController.kt` | F-25 |
| `server/.../HandleStruggleService.kt` | F-17, F-19, F-27 |
| `server/.../AdaptiveTaskService.kt` | F-13, F-27 |
| `server/.../StruggleRepository.kt` | F-13 |
| `server/.../StruggleSessionJpaRepository.kt` | F-13 |
| `server/.../StruggleRepositoryAdapter.kt` | F-13, F-27 |
| `server/.../StruggleSession.kt` | F-27 |
| `server/.../StruggleSessionEntity.kt` | F-27 |
| `ai-agent/api/.../StruggleContext.kt` | F-17 |
| `server/src/main/resources/db/migration/V22__...sql` (new) | F-27 |
| `docker-compose.yml` | F-08 |
| `agentic/DECISIONS.md` | F-02, F-16, F-20 |
| `agentic/security/THREAT-LOG.md` | F-06 |
