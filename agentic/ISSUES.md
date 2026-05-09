# Play4Change — Open Issues

## Format

An issue is a discovered problem that has not yet been fixed. Issues differ from TODO items
(which are deferred features) and HACKS (which are known code shortcuts).
Issues are bugs, inconsistencies, broken behaviour, or infrastructure problems.

When an issue is fixed, mark it FIXED with the phase, task, and commit. Do not delete entries.

**Entry format:**
```
## [ID] [OPEN|FIXED] Severity:[Critical|High|Medium|Low] — [Short title]

**Discovered:** [Date or "Phase X, Task Y"]
**Description:** [What the problem is — be specific. Not "it doesn't work" but what
  exactly happens, what is expected, and what is observed.]
**Impact:** [Who is affected and how]
**Workaround:** [Any workaround available until the fix is in, or "none"]
**Fix plan:** [Which phase and task will fix this, or "unscheduled"]
**Fixed:** [If FIXED: Phase X, Task Y, commit hash]
```

---

## B10 [FIXED] Severity:High — Magic link email opens JSON in browser instead of launching the mobile app

**Discovered:** Phase 04, fix/session-fixes-05, 2026-05-09.

**Description:** The email magic link pointed to `https://radesh-govind.com/auth/verify?token=...`.
When clicked from a mobile email client, iOS opened Safari, which called the server's
`GET /auth/verify` endpoint. That endpoint returned `200 OK` with JSON
`{ accessToken, refreshToken, expiresIn }` — raw JWTs displayed in the browser. The Play4Change
app was never opened. The custom URL scheme `play4change://` registered in `Info.plist` was
never triggered.

Additionally, the app called `GET /auth/verify?token=...` internally (via `HttpAuthRepository`),
meaning it would follow any redirect returned by the server and fail when trying to parse a
`302 Location: play4change://...` response as JSON.

**Impact:** Every user clicking the magic link from their email saw raw JSON in the browser.
Authentication on iOS was impossible via the email link path. The debug paste field (B8)
was the only workaround.

**Workaround:** Use the in-app debug paste field (debug builds only).

**Fix plan:** Change `GET /auth/verify` to respond with `302 Found` to `play4change://auth/verify?token={token}`.
Change the app's `verifyMagicLink` to call `POST /auth/magic-link/verify` (JSON endpoint)
instead of `GET /auth/verify` (now redirect-only). The token is NOT consumed in the redirect;
it is consumed when the app calls `POST /auth/magic-link/verify`.

**Fixed:** 2026-05-09 — `AuthController.kt` `GET /auth/verify` returns 302 redirect;
`HttpAuthRepository.verifyMagicLink` uses `POST /auth/magic-link/verify`. Confirmed:
token consumed (`used=t`), refresh tokens created in DB within the test window.
Branch: fix/session-fixes-05, commit a783959.

---

## B9 [FIXED] Severity:Critical — First authenticated request after login fires session-expired due to Ktor 3.0.0 `sendWithoutRequest` behaviour

**Discovered:** Phase 04, fix/session-fixes-05, 2026-05-09.

**Description:** In Ktor 3.0.0, `sendWithoutRequest { true }` does NOT cause `addRequestHeaders` /
`loadTokens` to be called proactively before the request. The auth plugin only adds the `Authorization`
header after the server returns a 401 challenge. Consequently, the first authenticated request
after login (e.g. `GET /profile`) goes out without an `Authorization` header, receives 401, and
`refreshTokens` is invoked with `oldTokens = null`. The original `refreshTokens` block read
`val refreshToken = oldTokens?.refreshToken`, which evaluates to `null`, immediately calling
`tokenStorage.clear()` and `onSessionExpired()`. The app bounced back to the login screen within 1
second of a successful magic link auth — even though tokens were correctly stored in the Keychain.

**Impact:** Every iOS user was logged out immediately after a successful magic link authentication.
The entire Phase 04 iOS auth flow was broken end-to-end (Android was unaffected because
`EncryptedSharedPreferences` shares the same token storage path but the race condition masked it).

**Workaround:** None — every post-login navigation to Home failed.

**Fix plan:** In `refreshTokens`, fall back to `tokenStorage.getRefreshToken()` when `oldTokens`
is null: `val refreshToken = oldTokens?.refreshToken ?: tokenStorage.getRefreshToken()`. This
ensures that on the first 401 challenge after login, the stored refresh token is used to silently
refresh rather than immediately expiring the session.

**Fixed:** 2026-05-09 — `HttpClientFactory.kt` line 82, `refreshTokens` fallback to
`tokenStorage.getRefreshToken()` when `oldTokens` is null. Branch: fix/session-fixes-05.

---

## B5 [FIXED] Severity:Medium — `GET /tasks/today` returns submitted task instead of 404

**Discovered:** Phase 04, manual testing session 2026-05-09.

**Description:** In prod mode, `TaskService.getTodayTask()` searches existing assignments for
one whose template `dayIndex` matches the current calendar day index. It returned
`TodayTaskResult.Available` for any matching assignment regardless of `AssignmentStatus`.
After a user submitted a task correctly (`status = SUBMITTED`), calling `GET /tasks/today`
again returned HTTP 200 with the already-submitted assignment still showing
`wrongAttemptCount` and "Start Challenge". The `X-Task-Available-At` 404 path was never
reached. `submitAnswer()` would then reject a re-submission with `Conflict.ConcurrentModification`.

**Impact:** The home screen task card shows "Start Challenge →" permanently after completion.
Tapping it leads to a conflict error. Users have no feedback that today's task is done.

**Workaround:** None visible to the user.

**Fix plan:** `TaskService.getTodayTask()` prod-mode block — add `status == PENDING` guard.
If the matching assignment is not PENDING, return `NotAvailableYet(enrollment.enrolledAt
.plusDays(dayIndex + 1))` so the controller renders HTTP 404 with `X-Task-Available-At`.

**Fixed:** 2026-05-09 — `TaskService.kt` lines 92-100, `TaskDeliveryRateTest.kt` test
corrected, commit acc0295.

---

## B6 [FIXED] Severity:Low — `GET /admin/topics/{id}/badges` returns `enrolledCount: 0`

**Discovered:** Phase 04, manual testing session 2026-05-09.

**Description:** `BadgeQueryService.getTopicBadgeStats()` fetched the `MicroCompetence` for
the topic first, then early-returned `TopicBadgeStatsDto(0, 0, 0.0, emptyList())` when none
was found — hardcoding `enrolledCount = 0` without ever querying
`enrollmentRepository.countByTopicId()`. Topics that have not yet generated a
`MicroCompetence` (e.g. topics still in progress or newly created) always reported zero
enrolled users regardless of actual enrollment state. DB confirmed 2 active enrollments
while the API returned 0.

**Impact:** Admin badge stats page shows 0 enrolled users for any topic without a
MicroCompetence record. `earnedPercentage` is therefore always 0.0 in those cases even
if users are enrolled.

**Workaround:** None — the count is always wrong until a MicroCompetence is created for
the topic.

**Fix plan:** Move `enrolledCount = enrollmentRepository.countByTopicId(topicId).toInt()`
before the null-check so it is always computed. Pass the real count into the early-return
`TopicBadgeStatsDto`.

**Fixed:** 2026-05-09 — `BadgeQueryService.kt` lines 35-38, commit acc0295.

---

## I02 [FIXED] Severity:High — Struggle resolution does not reset original task assignment for retry

**Discovered:** Phase 03, Task 3.2 — audit of the struggle/ packages before implementation.

**Description:** `AdaptiveTaskService.submitAdaptiveTask()` correctly resolves the
`StruggleSession` when all adaptive tasks are completed (sets `status = RESOLVED`,
`resolvedAt = now()`). However, it does NOT reset the original `TaskAssignment` that
triggered the struggle. That assignment remains in `AssignmentStatus.SUBMITTED` with
`wrongAttemptCount = 1` and `isCorrect = false`. When `TaskService.getTodayTask()` is
called after resolution, it finds the existing SUBMITTED assignment (matching `dayIndex`)
and returns it as-is — but `TaskService.submitAnswer()` rejects submissions with
`status != PENDING` (`Conflict.ConcurrentModification`). The user is permanently blocked
from retrying the task they struggled on.

**Impact:** After completing a struggle session, learners cannot retry the original task.
The main learning path is permanently blocked for that day index. ADR-013 Decision 5 states
the original task must be retried once after struggle resolution — this is violated.

**Workaround:** None — the user is stuck. Admin must manually delete or patch the assignment.

**Fix plan:** Phase 03, Task 3.2 — when `AdaptiveTaskService.submitAdaptiveTask()` resolves
the session (all adaptive tasks done), fetch the original `TaskAssignment` via
`enrollmentRepository.findAssignmentById(session.originalTaskAssignmentId)` and reset it:
`status = PENDING, wrongAttemptCount = 0, submittedAt = null, selectedOption = null,
isCorrect = null, pointsAwarded = 0`.

**Fixed:** Phase 03, Task 3.2 — `AdaptiveTaskService` fetches and resets original assignment to PENDING on session resolution.

---

## Phase 03, Task 3.2 — Struggle Path Audit Findings

**Conducted:** 2026-05-06

**Scope:** All files under `application/struggle/`, `domain/struggle/`, `infrastructure/persistence/`
(struggle entities), `web/user/StruggleController.kt`, Flyway migrations V5 and V8, and ADR-005/ADR-013.

**Status by component:**

| Component | Status | Notes |
|-----------|--------|-------|
| `StruggleSession` domain entity | ✅ Fully implemented | `resolve()` and `abandon()` transitions correct |
| `AdaptiveTask` domain entity | ✅ Fully implemented | All MC fields present |
| `StruggleRepository` interface | ✅ Fully implemented | `findById`, `findOpenByEnrollmentId`, `save` |
| `ErrorPattern` / `StruggleStatus` enums | ✅ Fully implemented | |
| `HandleStruggleService` | ✅ Fully implemented | Async on `generationExecutor` (configured); timeout + abandon logic |
| `ErrorPatternClassifier` | ✅ Fully implemented + tested | 4-rule priority chain; unit tests pass |
| `AdaptiveTaskService` | ⚠️ Bug — see I02 above | Resolution logic correct; missing assignment reset |
| `StruggleUseCase` / port | ✅ Fully implemented | |
| `StruggleRepositoryAdapter` | ✅ Fully implemented | JSONB serialisation correct |
| JPA entities (session + task) | ✅ Fully implemented | |
| JPA repositories | ✅ Fully implemented | |
| `StruggleController` | ✅ Fully implemented | Both endpoints wired correctly |
| DTOs (response/request) | ✅ Fully implemented | No `correctAnswer` leakage |
| Flyway V5 + V8 migrations | ✅ Fully implemented | All columns present |
| `TaskService` integration | ✅ Fully implemented | Triggers on `wrongAttemptCount == 1` |
| `generationExecutor` bean | ✅ Configured | `AsyncConfig.kt` — present |
| `StruggleDetectionTest` | ✅ Written & passing | Phase 04, session 2026-05-09 |
| `StruggleResolutionTest` | ✅ Written & passing | Phase 04, session 2026-05-09 |
| `StruggleControllerTest` | ✅ Written & passing | Phase 04, session 2026-05-09 |

---

## I01 [FIXED] Severity:Medium — CI JDK version mismatch (17 vs 21)

**Discovered:** Phase 01

**Description:** The GitHub Actions CI workflow (`.github/workflows/ci.yml`) specifies
`java-version: '17'` in the `setup-java` step. The server Gradle build specifies
`jvmToolchain(21)`. This means CI compiles and runs tests using JDK 17 despite the
codebase targeting JDK 21. The Gradle JVM toolchain will attempt to resolve JDK 21
automatically when running locally, but in CI the resolution may fall back to the
available JDK 17, causing a potential discrepancy between local and CI compilation.

**Impact:** Tests may pass locally on JDK 21 and fail in CI (or vice versa) if any
code uses JDK 21-specific APIs or behaviour. The discrepancy silently erodes CI trust.
Severity is Medium (not High) because no active test failures are known at this time —
but the gap is real.

**Workaround:** Run `./gradlew :server:test` locally on JDK 21 before trusting CI results.

**Fix plan:** Phase 01, Task 1.2 — update CI YAML to `java-version: '21'` (Temurin distribution).

**Fixed:** Phase 01, Task 1.2 — updated `.github/workflows/ci.yml` `java-version` to `'21'` (temurin).

---

## B3 [FIXED] Severity:Low — Profile name falls back to full email address in greeting

**Discovered:** Phase 04, manual testing session 2026-05-09.

**Description:** The server returns `"name": "radesh.govind@gmail.com"` (identical to the
email) when the user has no display name set. The home screen greeting computed
`userName.split(" ").first()`, which for an email with no spaces returns the entire
email address. Result: "Good morning, radesh.govind@gmail.com!".

**Impact:** Every new user who has not set a display name sees their email address in the
greeting. Aesthetically broken; not a data leak.

**Workaround:** Set a display name on the account.

**Fix plan:** UI layer only — extract the local part (before `@`) and capitalise it.

**Fixed:** 2026-05-09 — `HomeScreen.kt` `greetingName()` helper: if `userName` contains
`@`, use `substringBefore('@').replaceFirstChar { it.uppercaseChar() }`.

---

## B4 [FIXED] Severity:Low — Apostrophe rendered as literal backslash in KMP Compose strings

**Discovered:** Phase 04, manual testing session 2026-05-09.

**Description:** KMP Compose multiplatform string resources do not process Android-style `\'`
escape sequences. Strings such as `Today\'s Challenge` and French contractions (e.g.
`d\'apprentissage`) were rendered with a literal backslash on screen.

**Impact:** Cosmetic — punctuation is wrong in the UI.

**Workaround:** None.

**Fix plan:** Replace all `\'` occurrences in `values/strings.xml` and `values-fr/strings.xml`
with unescaped `'`.

**Fixed:** 2026-05-09 — replaced all `\'` with `'` in `values/strings.xml` (2 occurrences)
and `values-fr/strings.xml` (12 occurrences).

---

## Phase 03, Task 3.2 — Struggle tests status update

The three test classes previously marked `❌ Missing` now exist and all pass:
- `StruggleDetectionTest` — FIXED 2026-05-09
- `StruggleResolutionTest` — FIXED 2026-05-09
- `StruggleControllerTest` — FIXED 2026-05-09

`./gradlew :server:test` BUILD SUCCESSFUL.

---

## B7 [FIXED] Severity:Critical — KeychainTokenStorage.saveItem silently discards tokens on iOS 26.2 simulator

**Discovered:** Phase 04, fix/session-fixes-05, 2026-05-09.

**Description:** `saveItem()` called `SecItemAdd(query, null)` without checking the returned
`OSStatus`. On the iOS 26.2 simulator SecItemAdd was returning a non-success status (no entry
written to the keychain DB, confirmed via `sqlite3` query on `keychain-2-debug.db`). Because
the failure was silent, `store(accessToken, refreshToken)` appeared to succeed. The app
navigated to the Home screen, `GET /profile` was called with no `Authorization` header,
received 401, refresh token was also null (also not stored), `SessionEventBus.sessionExpired()`
fired, and the app bounced back to the login screen in under 1 second.

**Impact:** iOS users could not complete authentication at all. The entire Phase 04 iOS flow
was blocked. Android was unaffected (EncryptedSharedPreferences has its own write path).

**Workaround:** None — every iOS auth attempt failed silently.

**Fix plan:** Check OSStatus from SecItemAdd. If errSecDuplicateItem, call SecItemUpdate
instead. If any other non-success status, throw IllegalStateException so the caller can
surface a meaningful error. Pre-delete call removed; replaced with add-or-update pattern.

**Fixed:** 2026-05-09 — Two-stage fix on `KeychainTokenStorage.kt`. Stage 1: check OSStatus
from SecItemAdd, throw on failure. Stage 2 (root cause): iOS 26.2 simulator has the System
Keychain disabled (`System Keychain Always Supported set via feature flag to disabled`).
Added `kSecUseDataProtectionKeychain = kCFBooleanTrue` to all Keychain queries to route
operations to the Data Protection Keychain (required on iOS 13+, mandatory on iOS 26.2 sim).
`SecItemUpdate` + `errSecDuplicateItem` imports added for the duplicate-item fallback path.
Unit tests not feasible in KMP without XCTest host (documented in class KDoc); verified via
Phase 04 manual test recipe Section 1 (Keychain DB query) and Section 2 (persistence across
restart). See DECISIONS.md [2026-05-09] [iosMain] — KeychainTokenStorage. Branch: fix/session-fixes-05.

---

## B8 [FIXED] Severity:High — isDebugBuild hardcoded to false on iOS; debug paste field never shows

**Discovered:** Phase 04, fix/session-fixes-05, 2026-05-09.

**Description:** `BuildInfo.ios.kt` declared `actual val isDebugBuild: Boolean = false`.
The in-app "Paste your verification token" field — added in Phase 04 and guarded by
`if (isDebugBuild)` — was therefore permanently hidden on iOS, including debug/simulator
builds. The Phase 04 recipe §1 primary testing path depends on this field to inject the
magic link token without accessing the real inbox.

**Impact:** iOS testing of the magic link flow was impossible via the recipe's primary path.
Combined with B7 (Keychain silent failure), iOS authentication was completely blocked.

**Workaround:** None on iOS.

**Fix plan:** Replace hardcoded `false` with `Platform.isDebugBinary` from the Kotlin/Native
standard library. `Platform.isDebugBinary` is set by the linker at build time: debug
framework → true, release framework → false.

**Fixed:** 2026-05-09 — `BuildInfo.ios.kt` updated to `actual val isDebugBuild: Boolean =
Platform.isDebugBinary`. DECISIONS.md entry added. Branch: fix/session-fixes-05.

---

## B-ROADMAP [FIXED] Severity:Medium — Roadmap returns PENDING for today's completed task

**Discovered:** Phase 04, fix/session-fixes-05, 2026-05-09.

**Description:** `RoadmapService.getRoadmap()` handled `template.dayIndex == dayIndex`
(today's node) with only two cases: `PENDING_REVIEW` and an `else → PENDING` fallback.
When an assignment existed with `status = SUBMITTED` (task correctly answered, `is_correct
= true`, `points_awarded = 10` in DB), it fell to the `else` branch and returned
`RoadmapNodeStatus.PENDING`. Learners saw no visual distinction between "task done" and
"task not yet started" on today's roadmap node.

**Impact:** Home screen roadmap node shows same state before and after completing today's
task. Users cannot tell from the roadmap whether they have completed a day.

**Workaround:** Check `GET /tasks/today` which returns 404 when task is submitted.

**Fix plan:** Add `AssignmentStatus.SUBMITTED -> RoadmapNodeStatus.COMPLETED` as an
explicit case before the `else` in the `dayIndex == dayIndex` branch.

**Fixed:** 2026-05-09 — `RoadmapService.kt` line 64 updated; `RoadmapServiceTest.kt`
added with 3 tests (SUBMITTED→COMPLETED, PENDING→PENDING, no-assignment→PENDING).
All tests green. Branch: fix/session-fixes-05.

---

*(New entries are prepended above existing open items. Most recent first.)*
