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
| `StruggleDetectionTest` | ❌ Missing | Required by Task 3.2 spec |
| `StruggleResolutionTest` | ❌ Missing | Required by Task 3.2 spec |
| `StruggleControllerTest` | ❌ Missing | Required by Task 3.2 spec |

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

*(New entries are prepended above existing open items. Most recent first.)*
