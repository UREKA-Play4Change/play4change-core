# Phase 04 — Mobile Client: HTTP Wiring & Auth

**Status:** `IN PROGRESS`
**Goal:** Replace all mock repositories with real Ktor HTTP calls, JWT secure storage
(EncryptedSharedPreferences on Android, Keychain on iOS), automatic token refresh on 401,
all existing screens connected to live data.

**Entry criteria:** Phase 01 DONE (CI stable, server API stable enough to wire against).

---

## Tasks

### Task 4.1 — Ktor HttpClient setup
- [x] **What:** Set up the Ktor `HttpClient` in the `composeApp` KMP module with base URL
      configuration, JSON content negotiation, Bearer token injection, 401→refresh→retry
      logic, and a `X-Request-ID` header on every request. Define the `TokenStorage` interface
      and platform-specific implementations.
- **Design constraints:**
  - **HttpClient configuration:**
    - `ContentNegotiation` plugin with `kotlinx.serialization` JSON.
    - `Auth` plugin with `BearerTokens` — reads access token from `TokenStorage`,
      sends as `Authorization: Bearer {token}`.
    - On 401 response: call `AuthRepository.refresh(refreshToken)`. If successful,
      store new tokens and retry the original request. If refresh fails (refresh token
      expired or revoked), clear tokens and emit `SessionExpired` effect to redirect to login.
    - `X-Request-ID` header: `UUID.randomUUID().toString()` injected on every request.
      This allows server-side log correlation.
  - **Base URL:** Configurable via `BuildConfig.BASE_URL` (set in `gradle.properties`
    for local dev, overridden in CI and release builds). Default: `http://10.0.2.2:8080`
    (Android emulator loopback to host machine).
  - **Timeouts:** Connection timeout: 10s. Request timeout: 30s. Socket timeout: 30s.
  - **TokenStorage interface:**
    ```kotlin
    interface TokenStorage {
        suspend fun getAccessToken(): String?
        suspend fun getRefreshToken(): String?
        suspend fun store(accessToken: String, refreshToken: String)
        suspend fun clear()
    }
    ```
  - **Android implementation:** `EncryptedSharedPreferencesTokenStorage` using
    `androidx.security.crypto.EncryptedSharedPreferences` with `AES256_SIV` key encryption
    and `AES256_GCM` value encryption.
  - **iOS implementation:** `KeychainTokenStorage` using `Security` framework keychain APIs
    with `kSecAttrAccessibleAfterFirstUnlock` accessibility.
  - The `HttpClient` instance is created once and injected via the DI graph (use Koin
    if it is already present; do not add a new DI framework).
- **Tests required:**
  - `TokenStorageTest` (Android instrumented test):
    - `store()` then `getAccessToken()` returns the stored value.
    - `clear()` causes `getAccessToken()` to return null.
  - `HttpClientRefreshTest` (unit test with mock server using `MockEngine`):
    - 401 response triggers a refresh call.
    - After successful refresh, the original request is retried with the new token.
    - After failed refresh, `SessionExpired` effect is emitted.
- **Security log requirement:** Add to THREAT-LOG.md: "Mobile tokens stored in
      EncryptedSharedPreferences (Android) and Keychain (iOS) — not in plain SharedPreferences
      or UserDefaults." OWASP A02 (Cryptographic Failures).
- **ADR trigger:** Yes — write ADR-018 documenting the token storage strategy on mobile:
      why EncryptedSharedPreferences over plain SharedPreferences, why Keychain over UserDefaults,
      the 401→refresh→retry flow, and the X-Request-ID correlation header.
- **Exit criteria:** `TokenStorageTest` and `HttpClientRefreshTest` pass.
      ADR-018 is written and committed.

---

### Task 4.2 — Replace mock: Auth repositories
- [x] **What:** Replace `MockAuthRepository` with `HttpAuthRepository` that calls the real
      server endpoints: magic link request, magic link verify, OAuth login, refresh, logout.
- **Design constraints:**
  - `HttpAuthRepository` implements the same `AuthRepository` interface as the mock.
  - Endpoints to wire:
    - `POST /auth/magic-link` — `requestMagicLink(email: String)`
    - `GET /auth/verify?token={token}` — `verifyMagicLink(token: String): AuthResult?`
    - `POST /auth/oauth` — `loginWithOAuth(provider: AuthProvider, idToken: String): AuthResult?`
    - `POST /auth/refresh` — `refresh(refreshToken: String): AuthResult?`
    - `DELETE /auth/logout` — `logout()`
  - On successful verify/oauth/refresh: call `tokenStorage.store(accessToken, refreshToken)`.
  - On logout: call `tokenStorage.clear()`.
  - `NetworkError` sealed class (see Task 4.6) is used for all error paths.
  - The DI graph switches from `MockAuthRepository` to `HttpAuthRepository` when
    `BuildConfig.USE_MOCKS = false` (default for non-test builds).
- **Tests required:**
  - `HttpAuthRepositoryTest` (using `MockEngine`):
    - `requestMagicLink` with valid email calls `POST /auth/magic-link` and returns success.
    - `verifyMagicLink` with valid token returns `AuthResult` with access and refresh tokens.
    - `verifyMagicLink` with expired token returns `NetworkError.Unauthorized`.
    - `logout` calls `DELETE /auth/logout` and clears token storage.
- **Security log requirement:** None beyond Task 4.1 entry.
- **ADR trigger:** No.
- **Exit criteria:** `HttpAuthRepositoryTest` passes. Magic link flow works end-to-end on
      Android emulator connected to local Docker Compose stack.

---

### Task 4.3 — Replace mock: Topic and Enrollment repositories
- [x] **What:** Replace `MockTopicRepository` and `MockEnrollmentRepository` with HTTP
      implementations that call the real server endpoints.
- **Design constraints:**
  - Topic endpoints to wire:
    - `GET /topics/{topicId}` — get topic detail
    - `GET /topics` — list available topics (if this endpoint exists; add it if not)
  - Enrollment endpoints to wire:
    - `POST /topics/{topicId}/enroll` — enrol in a topic
    - `GET /topics/{topicId}/enrollment` — get enrollment status and points
    - `GET /topics/{topicId}/roadmap` — full day-by-day task status
    - `GET /tasks/today?topicId={id}` — get today's task (with `X-Timezone` header)
    - `POST /tasks/{assignmentId}/submit` — submit multiple-choice answer
    - `POST /tasks/{assignmentId}/submit-photo` — submit photo task
  - All requests include the Bearer token from `TokenStorage` (via the Ktor `Auth` plugin).
  - The `X-Timezone` header is populated from the user's timezone preference
    (from `TokenStorage` or a separate `PreferencesStore`).
- **Tests required:**
  - `HttpTopicRepositoryTest` and `HttpEnrollmentRepositoryTest` (using `MockEngine`):
    - Each method maps to the correct HTTP method and path.
    - Error responses (404, 403) map to the correct `NetworkError` subtype.
    - Task submission with correct answer returns success.
    - Task submission with wrong answer returns the correct feedback structure.
- **Security log requirement:** None.
- **ADR trigger:** No.
- **Exit criteria:** Both test classes pass. Home screen shows real enrolled topics on
      a physical device connected to the local server.

---

### Task 4.4 — Replace mock: Struggle and PeerReview repositories
- [x] **What:** Replace `MockStruggleRepository` and `MockPeerReviewRepository` with HTTP
      implementations.
- **Design constraints:**
  - Struggle endpoints to wire:
    - `GET /struggle/enrollment/{enrollmentId}` — get active struggle session
    - `POST /struggle/{sessionId}/tasks/{taskId}/submit` — submit adaptive task answer
  - PeerReview endpoints to wire:
    - `GET /reviews/pending?topicId={id}` — get pending reviews
    - `POST /reviews/{reviewId}/verdict` — submit verdict
  - Error handling: if no active struggle session, the repository returns null
    (not a `NetworkError`) — a 404 on this endpoint is expected behaviour.
- **Tests required:**
  - `HttpStruggleRepositoryTest` and `HttpPeerReviewRepositoryTest` (using `MockEngine`):
    - Struggle session 404 maps to null return (not an error).
    - Verdict submission with `CORRECT` sends the correct request body.
    - Pending reviews list deserialises correctly.
- **Security log requirement:** None.
- **ADR trigger:** No.
- **Exit criteria:** Both test classes pass.

---

### Task 4.5 — Replace mock: Profile and Badge repositories
- [x] **What:** Replace `MockProfileRepository` and `MockBadgeRepository` with HTTP
      implementations.
- **Design constraints:**
  - Profile endpoints to wire:
    - `GET /profile` — get user profile (name, email, points, joined date)
    - `PUT /profile/preferences` — update language and timezone
    - `GET /profile/preferences` — get current preferences
  - Badge endpoints to wire:
    - `GET /profile/badges` — get earned badges
- **Tests required:**
  - `HttpProfileRepositoryTest` and `HttpBadgeRepositoryTest` (using `MockEngine`):
    - Each method maps to the correct endpoint.
    - Badge list deserialises with correct field mapping.
    - Preferences update sends the correct partial-update body.
- **Security log requirement:** None.
- **ADR trigger:** No.
- **Exit criteria:** Both test classes pass. Profile screen shows real data on device.

---

### Task 4.6 — NetworkError sealed class and error state UI
- [x] **What:** Define a `NetworkError` sealed class covering all error conditions.
      Wire error states to every screen so the user sees a meaningful error message
      instead of a crash or blank screen when network requests fail.
- **Design constraints:**
  - `NetworkError` sealed class:
    ```kotlin
    sealed class NetworkError {
        object Unauthorized : NetworkError()     // 401 — session expired, redirect to login
        object Forbidden : NetworkError()         // 403 — insufficient role
        data class NotFound(val id: String) : NetworkError()  // 404
        data class ServerError(val code: Int) : NetworkError() // 5xx
        object NoConnection : NetworkError()      // IOException / network unavailable
        object Timeout : NetworkError()           // Timeout exception
        data class Unknown(val message: String) : NetworkError() // catch-all
    }
    ```
  - Every screen that makes network calls must handle the `NetworkError` state:
    - `Unauthorized`: trigger logout and navigate to login screen.
    - `NoConnection` / `Timeout`: show a "Check your connection" snackbar with a retry button.
    - `ServerError`: show "Something went wrong. Try again later."
    - `NotFound`: show context-appropriate message (e.g. "Topic not found").
    - `Forbidden`: show "You do not have permission to do this."
  - Error state is part of the screen's MVI state — not a side effect — so it survives
    configuration changes.
- **Tests required:**
  - `NetworkErrorMappingTest`:
    - HTTP 401 maps to `Unauthorized`.
    - HTTP 403 maps to `Forbidden`.
    - HTTP 404 maps to `NotFound`.
    - HTTP 500 maps to `ServerError(500)`.
    - `IOException` maps to `NoConnection`.
    - Timeout exception maps to `Timeout`.
  - UI state tests for affected screens (at least HomeScreen and TaskScreen):
    - When state is `NetworkError.NoConnection`, a retry button is visible.
- **Security log requirement:** None.
- **ADR trigger:** No.
- **Exit criteria:** All tests pass. Disabling network on the emulator shows error UI
      (not a crash) on every screen.

---

### Task 4.7 — Manual test recipe for Phase 04
- [ ] **What:** Write the full end-to-end manual test recipe for Phase 04 in
      `agentic/manual-testing/phase-04-recipe.md`.
- **Design constraints:** The recipe must cover: magic link flow on device, token persistence
      across app restarts, 401 refresh flow (set a short token TTL for testing), error UI
      on network loss.
- **Tests required:** None — this is a documentation task.
- **Security log requirement:** None.
- **ADR trigger:** No.
- **Exit criteria:** File exists and is accurate.

---

## Exit Criteria (Phase Level)

All 7 tasks are checked off. The following is true:
- All repository interfaces have HTTP implementations.
- `BuildConfig.USE_MOCKS = false` is the default for non-test builds.
- Tokens are stored in EncryptedSharedPreferences (not plain SharedPreferences).
- 401 responses trigger a token refresh and retry.
- All screens show error UI (not crashes) on network failure.
- ADR-018 is written and committed.

---

## Human Checkpoint

Before marking Phase 04 DONE, on a physical Android device connected to the local server:

**1. Magic link flow:**
- Open the app. Enter your email. Submit.
- Check the server console log for the magic link URL (dev mode — console email adapter).
- Copy the token from the log. The app should handle the deep link if configured,
  or manually call:
  ```bash
  # Simulate what the deep link click does:
  curl "http://10.0.2.2:8080/auth/verify?token={token}"
  ```
- The app should show the home screen with enrolled topics.

**2. Token persistence:**
- Kill the app completely (swipe away from recents).
- Reopen the app.
- Expected: home screen loads without re-login prompt. No token re-entry required.

**3. Network error UI:**
- Put the device in airplane mode.
- Navigate to the task screen.
- Expected: "Check your connection" error message and a Retry button — not a crash.

**4. Unauthorized recovery:**
- Wait for the access token to expire (15 minutes), or manually clear the token storage.
- Perform any authenticated action.
- Expected: refresh is called automatically. If refresh succeeds, the action completes.
  If refresh fails, the user is redirected to the login screen.

If any check fails, Phase 04 is not done.
