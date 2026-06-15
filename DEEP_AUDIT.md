# Deep Code Audit — play4change

> Strict engineering critique: repetitive code, design flaws, SOLID/DDD/RFC violations, anti-patterns.  
> Issues are ordered by severity. Each entry has a verdict and a concrete fix.

---

## 🔴 BUGS (will fail at runtime or compile time)

---

### B1 — `UserJpaAdapter.toEntity()` references deleted type `User`

**File:** `server/.../auth/adapter/outbound/persistence/UserJpaAdapter.kt:35`

```kotlin
private fun User.toEntity() = UserEntity(...)   // ← User no longer exists; renamed to AuthUser
```

The `User` class was renamed to `AuthUser` in the previous session. The `toDomain()` extension was updated, but `toEntity()` was missed. This is a compile error.

**Fix:**
```kotlin
private fun AuthUser.toEntity() = UserEntity(
    id = id, email = email, name = name,
    provider = provider.name, providerId = providerId,
    createdAt = createdAt, role = role
)
```

---

### B2 — `toUiError` declared as a higher-order function by accident

**Files:**  
- `composeApp/.../core/network/NetworkErrorMapper.kt:45`  
- `composeApp/.../core/component/stateful/SafeLaunch.kt:5,23,25`

```kotlin
// Declaration — returns () -> UiError, not UiError
fun NetworkError.toUiError()(): UiError = when (this) { ... }

// Import is also malformed
import com.ureka.play4change.core.network.toUiError()

// Call sites double-invoke the returned lambda
e.error.toUiError()()
e.toNetworkError().toUiError()()
```

The `()` after the function name is a leftover from the `toAppError()→toUiError()` rename — the parens got baked into the identifier. This compiles as "a function named `toUiError` that returns `() -> UiError`", forcing every call site to double-invoke. It may also break incremental compilation.

**Fix:**
```kotlin
// NetworkErrorMapper.kt
fun NetworkError.toUiError(): UiError = when (this) { ... }

// SafeLaunch.kt — remove the extra ()
import com.ureka.play4change.core.network.toUiError
...
error = e.error.toUiError()
error = e.toNetworkError().toUiError()
```

---

### B3 — `TaskService.submitAnswer` and `EnrollmentService.enroll` are non-atomic

**Files:**  
- `server/.../application/enrollment/TaskService.kt:192`  
- `server/.../application/enrollment/EnrollmentService.kt:46`

`submitAnswer` performs at minimum **five** separate DB writes (saveAssignment, save enrollment, potentially save completion + badge + counter) with no `@Transactional` boundary. If any write after the first succeeds but a later one fails:

- Points are awarded with no streak update, or
- Assignment is marked submitted but enrollment stays ACTIVE forever, or
- A badge fires without the assignment being persisted.

`enroll` similarly does `enrollmentRepository.save(enrollment)` then `saveAssignment(...)` — if the assignment save fails the enrollment row is orphaned with no first task.

**Fix:** Add `@Transactional` to both methods. For `submitAnswer` the badge issuance port (`badgeIssuancePort.issueBadge`) is an external side-effect that must happen _after_ the transaction commits; move it to a `@TransactionalEventListener(AFTER_COMMIT)`.

```kotlin
@Transactional
override fun submitAnswer(command: SubmitAnswerCommand): Either<AppError, SubmitResult> = either { ... }

@Transactional
override fun enroll(command: EnrollCommand): Either<AppError, Enrollment> = either { ... }
```

---

## 🟠 DESIGN FLAWS (correct today, but will hurt you)

---

### D1 — `sha256` + `generateSecureToken` copied verbatim into two application services

**Files:**  
- `server/.../auth/application/TokenService.kt:141-149`  
- `server/.../auth/application/MagicLinkService.kt:68-78`

Both implement identical private `sha256(input: String): String` and token-generation helpers. Any divergence (e.g. switching from hex-encoded to base64url, adding a pepper) must be made in two places. The implementations already diverge slightly in function name (`generateToken` vs `generateSecureToken`).

**Fix:** Extract to `server/.../auth/domain/crypto/AuthCrypto.kt`:
```kotlin
object AuthCrypto {
    private val secureRandom = SecureRandom()

    fun generateOpaqueToken(): String {
        val bytes = ByteArray(32)
        secureRandom.nextBytes(bytes)
        return String(Hex.encode(bytes))
    }

    fun sha256Hex(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        return String(Hex.encode(digest.digest(input.toByteArray(Charsets.UTF_8))))
    }
}
```
Both services inject or reference this object. `SecureRandom` is expensive to construct; sharing one instance is also a small performance win.

---

### D2 — `parseOptionsJson` duplicated four times

**Files:**  
- `server/.../application/topic/TaskGenerationOrchestrator.kt:213`  
- `server/.../application/struggle/HandleStruggleService.kt:232`  
- `server/.../infrastructure/language/LanguageGenerationAdapter.kt:111`  
- `server/.../infrastructure/ai/BatchInstanceAdapter.kt:94` (inlined)

All four are identical:
```kotlin
private fun parseOptionsJson(json: String): List<String>? = runCatching {
    Json.parseToJsonElement(json).jsonArray.map { it.jsonPrimitive.content }
}.getOrNull()
```

**Fix:** Extract to `ai-agent/.../model/OptionsJsonParser.kt` (shared utility visible to both AI adapters and orchestration services):
```kotlin
object OptionsJsonParser {
    fun parse(json: String): List<String>? = runCatching {
        Json.parseToJsonElement(json).jsonArray.map { it.jsonPrimitive.content }
    }.getOrNull()
}
```

---

### D3 — `runBlocking` inside `@Async` Spring services (5 occurrences)

**Files:**  
- `TaskGenerationOrchestrator.kt:114`  
- `HandleStruggleService.kt:148`  
- `ExplanationService.kt:112, 205`  
- `LanguageGenerationAdapter.kt:49`  
- `BatchInstanceAdapter.kt:56`

`runBlocking` inside an `@Async` method creates a new coroutine event loop pinned to the Spring-managed `generationExecutor` thread. While it works, it:

1. Blocks the thread entirely for the duration of the AI call — defeats `@Async`.
2. Prevents structured concurrency — a cancel/timeout in the outer coroutine cannot propagate.
3. Nests event loops, which is forbidden in some coroutine environments and causes deadlocks if `withTimeoutOrNull` is nested.

These services already receive a coroutine-based `TaskGenerationPort`. The right fix is to make them suspend-native:

```kotlin
// Replace @Async + runBlocking with a CoroutineScope bound to the Spring context
@Component
class TaskGenerationOrchestrator(
    ...
    private val generationScope: CoroutineScope,  // ApplicationContext-scoped CoroutineScope
) {
    fun generateAsync(topicId: String) {
        generationScope.launch {
            withTimeout(timeoutSeconds * 1_000L) { doGenerate(topicId) }
        }
    }

    private suspend fun doGenerate(topicId: String) { ... }
}
```
Provide the `CoroutineScope` as a Spring bean backed by the same `generationExecutor` thread pool:
```kotlin
@Bean fun generationScope(executor: Executor): CoroutineScope =
    CoroutineScope(executor.asCoroutineDispatcher() + SupervisorJob())
```

---

### D4 — `TopicManagementService.createFromUrl` and `createFromPdf` are ~85% duplicated

**File:** `server/.../application/topic/TopicManagementService.kt`

Both methods: validate `taskCount`, extract raw text, generate a UUID, upload to MinIO, build a `Topic`, save it, and call `orchestrator.generateAsync`. The only differences are source type, extraction call, and storage key.

**Fix:** Extract the shared body:
```kotlin
private fun createTopic(
    rawText: String,
    storageKey: String,
    bytes: ByteArray,
    contentType: String,
    sourceType: ContentSourceType,
    command: CreateTopicCommand,
    adminId: String,
): Either<AppError, Topic> = either {
    val topicId = UUID.randomUUID().toString()
    val contentRef = try {
        fileStoragePort.uploadFile(storageKey, bytes, contentType)
    } catch (ex: Exception) {
        log.error("MinIO upload failed for topic {}: {}", topicId, ex.message)
        raise(InternalServerError.UnexpectedException)
    }
    val now = OffsetDateTime.now()
    val topic = topicRepository.save(Topic(
        id = topicId, ..., contentSourceType = sourceType,
        rawExtractedText = rawText, status = TopicStatus.PENDING,
        createdBy = adminId, createdAt = now,
        currentPhase = GenerationPhase.INGESTION, phaseUpdatedAt = now
    ))
    orchestrator.generateAsync(topicId)
    topic
}
```
Both public methods call `createTopic` after their source-specific extraction.

---

### D5 — `TokenService.issue()` and `refresh()` duplicate access-token building

**File:** `server/.../auth/application/TokenService.kt`

The `Jwts.builder()...compact()` block appears identically in `issue()` (lines ~44-52) and `refresh()` (lines ~86-93). Any change to the JWT structure (add a claim, change expiry) must be made in both.

**Fix:**
```kotlin
private fun buildAccessToken(userId: String, email: String, role: String): String {
    val expiryMs = System.currentTimeMillis() + jwtProperties.accessTtlMinutes * 60 * 1000L
    return Jwts.builder()
        .subject(userId)
        .claim("email", email)
        .claim("role", role)
        .issuedAt(Date())
        .expiration(Date(expiryMs))
        .signWith(signingKey)
        .compact()
}
```

---

### D6 — `RateLimitService` `ConcurrentHashMap` grows without bound — memory leak

**File:** `server/.../auth/adapter/inbound/security/RateLimitService.kt`

```kotlin
private val buckets = ConcurrentHashMap<String, Bucket>()
```

Each unique `"$clientIp:$path"` key creates a permanent entry. An attacker cycling source IPs fills the heap indefinitely. The server never evicts stale entries.

**Fix:** Use Caffeine (already a transitive Spring Boot dependency) with a time-based eviction:
```kotlin
private val buckets: Cache<String, Bucket> = Caffeine.newBuilder()
    .expireAfterAccess(10, TimeUnit.MINUTES)
    .maximumSize(50_000)
    .build()

fun tryConsume(clientIp: String, path: String): Boolean {
    ...
    val bucket = buckets.get(key) { buildBucket(capacity) }
    return bucket.tryConsume(1)
}
```

---

### D7 — `isProd` computed per request in `JwtAuthFilter` and `SecurityConfig`

**Files:**  
- `server/.../auth/adapter/inbound/security/JwtAuthFilter.kt:31`  
- `server/.../infrastructure/config/SecurityConfig.kt` (computed property `get()`)

```kotlin
val isProd = environment.activeProfiles.contains("prod")  // inside doFilterInternal()
```

`activeProfiles` returns an array; `.contains()` does a linear scan. Called on every HTTP request. In `SecurityConfig`, `isProd` is a `get()` property, so it re-evaluates every time it's referenced during bean wiring too.

**Fix:** Compute once at construction:
```kotlin
class JwtAuthFilter(
    private val tokenService: TokenService,
    environment: Environment,        // constructor param, not field
) : OncePerRequestFilter() {
    private val isProd = environment.activeProfiles.contains("prod")  // val, computed once
    ...
}
```

---

### D8 — `AuthController`: `DELETE /logout` violates REST semantics; two identical methods

**File:** `server/.../auth/adapter/inbound/web/AuthController.kt`

```kotlin
@DeleteMapping("/logout")
fun logout(@Valid @RequestBody request: RefreshRequest): ResponseEntity<Void> {
    tokenUseCase.revoke(request.refreshToken)
    return ResponseEntity.noContent().build()
}
@PostMapping("/logout")
fun logoutPost(@Valid @RequestBody request: RefreshRequest): ResponseEntity<Void> {
    tokenUseCase.revoke(request.refreshToken)
    return ResponseEntity.noContent().build()
}
```

Two problems:

1. **Pure duplication** — identical body, two annotations. Remove one; keep only `POST /logout`.
2. **`DELETE /logout` is semantically wrong** — `DELETE` deletes a resource by URI. "Logging out" is a state transition, not a resource deletion. RFC 7231 §4.3.5 defines `DELETE` for resource removal. `POST` is correct here. The client (`HttpAuthRepository`) already calls `client.post("auth/logout")` — the `DELETE` mapping is never called by the client and can only cause confusion or CORS preflight issues with some proxies.

**Fix:** Keep only `POST /logout`, delete the `@DeleteMapping` method.

---

### D9 — `GET /verify` puts the token in the URL query parameter — RFC 6749 §10.3 violation

**File:** `server/.../auth/adapter/inbound/web/AuthController.kt`

```kotlin
@GetMapping("/verify")
fun verify(@RequestParam token: String): ResponseEntity<TokenResponse> { ... }
```

RFC 6749 §10.3 explicitly warns: "The authorization server MUST NOT issue access tokens ... in the URI query component." Tokens in URLs are logged in:
- Server access logs
- Browser history
- `Referer` headers on any subsequent request
- CDN/proxy logs

**Fix:** Change to `POST /magic-link/verify` with the token in the request body (which `HttpAuthRepository.verifyMagicLink` already does — it POSTs to `auth/magic-link/verify` with a JSON body). The `GET /verify` endpoint appears to be a legacy path — confirm it's unused and remove it.

---

### D10 — `BaseComponent.Effect` is untyped — sealed class hierarchy is defeated

**File:** `composeApp/.../core/component/base/BaseComponent.kt`

```kotlin
interface Effect  // bare marker inside BaseComponent

val effects: Flow<Effect>  // all effects, all features, same type
```

Every feature declares `sealed class LoginEffect : BaseComponent.Effect` — but `effects: Flow<Effect>` erases the concrete type. The UI must cast:
```kotlin
effects.collect { effect ->
    when (effect as LoginEffect) { ... }  // unsafe cast
}
```

**Fix:** Add `Eff` as a type parameter to `BaseComponent`:
```kotlin
abstract class BaseComponent<S : ComponentState, E : ComponentEvents, Eff : Any>(
    componentContext: ComponentContext,
    initialState: S
) : ComponentContext by componentContext, StatefulComponent<S> {

    private val _effects = Channel<Eff>(Channel.BUFFERED)
    val effects: Flow<Eff> = _effects.receiveAsFlow()

    protected fun emitEffect(effect: Eff) { scope.launch { _effects.send(effect) } }
}
```
Now `DefaultLoginComponent : BaseComponent<LoginState, LoginEvents, LoginEffect>` and `effects` is `Flow<LoginEffect>` — no cast needed.

---

### D11 — `StatefulComponent.copyBase` as an abstract extension in an interface

**File:** `composeApp/.../core/component/stateful/StatefulComponent.kt`

```kotlin
interface StatefulComponent<S : ComponentState> {
    fun S.copyBase(isLoading: Boolean = this.isLoading, error: UiError? = this.error): S
}
```

An abstract extension function on `S` in an interface:
1. Implementations must be `public` (interface contract) — can't be private helpers.
2. The receiver `S.copyBase(...)` looks like a method on the state, but it's actually on the component, creating conceptual confusion.
3. Every concrete `Default*Component` must implement it even though the body is always `copy(isLoading = isLoading, error = error)`.

**Fix:** Move to `BaseComponent` as a concrete protected function operating on `_state`:
```kotlin
protected fun setLoading(loading: Boolean, error: UiError? = null) {
    updateState { copyBase(isLoading = loading, error = error) }
}
```
`copyBase` stays as an abstract method on `ComponentState` — the data class implementing it is trivial.

---

### D12 — `AiOutputSanitiser` is in `application.topic` — infrastructure concern in the application layer

**File:** `server/.../application/topic/AiOutputSanitiser.kt`

`AiOutputSanitiser` wraps `Jsoup`, which is a third-party HTML-parsing library — an infrastructure dependency. The application layer should have no knowledge of libraries. The hexagonal rule: the application ring must not import outer-ring libraries.

**Fix:** Move to `server/.../infrastructure/ai/AiOutputSanitiser.kt`. Pass sanitized strings _into_ the application layer from the adapter.

---

## 🟡 DRY / MINOR ISSUES

---

### M1 — AI context-window truncation limits are magic numbers scattered across two services

**Files:**  
- `TaskGenerationOrchestrator.kt`: `rawText.take(8000)`, `description.take(500)`  
- `HandleStruggleService.kt`: `template.description.take(2000)`, `module.objective.take(500)`, `template.description.take(500)`

**Fix:**
```kotlin
// server/.../infrastructure/ai/AiContextLimits.kt
object AiContextLimits {
    const val CONTENT_CHARS    = 8_000
    const val DESCRIPTION_CHARS = 500
    const val STRUGGLE_CONTEXT_CHARS = 2_000
    const val OBJECTIVE_CHARS  = 500
}
```

---

### M2 — `HttpAuthRepository.verifyMagicLink` manually maps status codes instead of using `networkErrorFromStatus()`

**File:** `composeApp/.../features/auth/data/http/HttpAuthRepository.kt:75-84`

```kotlin
response.status == HttpStatusCode.Unauthorized -> throw NetworkException(NetworkError.Unauthorized)
response.status == HttpStatusCode.Forbidden -> throw NetworkException(NetworkError.Forbidden)
response.status.value in 500..599 -> throw NetworkException(NetworkError.ServerError(...))
else -> throw NetworkException(NetworkError.Unknown(...))
```

`networkErrorFromStatus(code: Int)` already exists in `NetworkErrorMapper.kt` for exactly this purpose.

**Fix:**
```kotlin
else -> throw NetworkException(networkErrorFromStatus(response.status.value))
```

---

### M3 — Dead import `delete` in `HttpAuthRepository`

**File:** `composeApp/.../features/auth/data/http/HttpAuthRepository.kt:13`

```kotlin
import io.ktor.client.request.delete  // never used; logout now calls client.post
```

Remove it.

---

### M4 — Mock/prod switch inconsistent across DI feature modules

**Files:** `composeApp/.../di/features/`

`AuthModule`, `TaskModule`, `StruggleModule`, `ExplanationModule`, `ProfileModule` check `config.useMocks` and switch between `Mock*Repository` and `Http*Repository`. `HomeModule`, `AboutModule`, `ExploreModule` hardcode `Http*Repository` regardless.

Setting `USE_MOCKS=true` for testing partially mocks the app — auth and tasks are mocked, but home feed and explore use live endpoints. This makes `USE_MOCKS` unreliable as a testing flag.

**Fix:** Either implement mock repositories for all features, or remove the mock switch entirely and use Ktor's `MockEngine` in tests. If mocks exist for some features, they must exist for all.

---

### M5 — `TaskShuffleSeed` uses `java.util.Random` (LCG) — weaker than the SHA-256 seed implies

**File:** `server/.../domain/enrollment/TaskShuffleSeed.kt`

```kotlin
val random = Random(seed)  // java.util.Random — linear congruential generator, 48-bit internal state
```

The seed is derived from SHA-256 (64 bits extracted). `java.util.Random` has only 48 bits of internal state and uses a linear congruential generator — given enough observed shuffles a determined attacker can reconstruct future shuffles. For an anti-cheat shuffle, this is worth hardening.

**Fix:** Use a cryptographic PRNG seeded from the full SHA-256 output, or use `Collections.shuffle` with a seeded `java.security.SecureRandom` (deterministic seed path).

---

### M6 — `ErrorPattern.PARTIAL_UNDERSTANDING` is a dead enum value

**Files:**  
- `server/.../domain/struggle/ErrorPattern.kt` (enum definition)  
- `server/.../application/struggle/ErrorPatternClassifier.kt` (classify method)

`ErrorPatternClassifier.classify()` never returns `PARTIAL_UNDERSTANDING` — the KDoc comment explicitly says it's absent. The classifier also never returns the value in `mapErrorPattern()`. The enum value exists, is mapped in `HandleStruggleService.mapErrorPattern`, and then...maps to `PROCEDURAL_ERROR` in the AI model. Dead code creating confusion.

If it genuinely can't be classified yet, remove the enum value and add a `// TODO` at the `WRONG_CONCEPT` default instead. Dead enum values become invisible lies when the code is read later.

---

### M7 — `OffsetDateTime.now()` called 77 times — untestable clock

**All services**

Every time-dependent operation (`expiresAt`, `enrolledAt`, `createdAt`, `now.isAfter(...)`) calls `OffsetDateTime.now()` directly. This makes unit-testing any time-dependent logic impossible without mocking the JVM clock.

**Fix:** Inject a `Clock` (from `java.time.Clock`) into services that use time:
```kotlin
@Service
class TokenService(
    ...,
    private val clock: Clock = Clock.systemUTC()
) {
    private fun now() = OffsetDateTime.now(clock)
}
```
Test code passes `Clock.fixed(...)`. This is the standard approach from *Working Effectively with Legacy Code* and the Java Time documentation.

---

### M8 — `AiOutputSanitiser.sanitise` strips HTML at write time only — no cleanup of pre-existing data

If injection payloads were stored before `AiOutputSanitiser` was introduced, they remain in the DB permanently. The sanitizer provides no defence for old data.

**Fix:** Add a Flyway data migration that runs `jsoup.parse(title).text()` (or equivalent SQL regexp) on all existing `task_templates` and `task_instances`. Alternatively, sanitize at read time in the JPA adapter rather than write time.

---

## Summary table

| ID  | Severity | Category           | File(s)                                        |
|-----|----------|--------------------|------------------------------------------------|
| B1  | 🔴 Bug    | Compile error      | `UserJpaAdapter.kt`                            |
| B2  | 🔴 Bug    | Compile/runtime    | `NetworkErrorMapper.kt`, `SafeLaunch.kt`       |
| B3  | 🔴 Bug    | Data integrity     | `TaskService.kt`, `EnrollmentService.kt`       |
| D1  | 🟠 Design | DRY / SRP          | `TokenService.kt`, `MagicLinkService.kt`       |
| D2  | 🟠 Design | DRY                | 4 files across server                          |
| D3  | 🟠 Design | Concurrency        | 5 `@Async` services                            |
| D4  | 🟠 Design | DRY / SRP          | `TopicManagementService.kt`                    |
| D5  | 🟠 Design | DRY                | `TokenService.kt`                              |
| D6  | 🟠 Design | Memory safety      | `RateLimitService.kt`                          |
| D7  | 🟠 Design | Performance        | `JwtAuthFilter.kt`, `SecurityConfig.kt`        |
| D8  | 🟠 Design | REST semantics     | `AuthController.kt`                            |
| D9  | 🟠 Design | Security / RFC     | `AuthController.kt`                            |
| D10 | 🟠 Design | Type safety        | `BaseComponent.kt`                             |
| D11 | 🟠 Design | Abstraction        | `StatefulComponent.kt`                         |
| D12 | 🟠 Design | Hexagonal arch     | `AiOutputSanitiser.kt`                         |
| M1  | 🟡 Minor  | Magic numbers      | `TaskGenerationOrchestrator`, `HandleStruggle` |
| M2  | 🟡 Minor  | DRY                | `HttpAuthRepository.kt`                        |
| M3  | 🟡 Minor  | Dead import        | `HttpAuthRepository.kt`                        |
| M4  | 🟡 Minor  | Consistency        | `di/features/*.kt`                             |
| M5  | 🟡 Minor  | Security hardening | `TaskShuffleSeed.kt`                           |
| M6  | 🟡 Minor  | Dead code          | `ErrorPattern.kt`, `ErrorPatternClassifier.kt` |
| M7  | 🟡 Minor  | Testability        | All services with `OffsetDateTime.now()`       |
| M8  | 🟡 Minor  | Data safety        | `AiOutputSanitiser` / Flyway                   |
