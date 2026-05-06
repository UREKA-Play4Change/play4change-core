# Phase 03 — Learner Logic: Rate Control, Struggle Path & Reporting

**Status:** `IN PROGRESS`
**Goal:** Configurable task delivery rate (default 1/day, dev mode: 2min), fully wired struggle
path (detection→adaptive branch→resolution→return), bad-question reporting with admin correction
flow, user language/timezone preferences.

**Entry criteria:** Phase 02 DONE.

---

## Tasks

### Task 3.1 — Configurable task delivery rate
- [x] **What:** Replace any hardcoded "one task per day" logic with a configurable
      `taskRateMinutes` setting. Add a dev-mode flag that reduces the rate to 2 minutes
      so that the full task progression can be tested without waiting 24 hours.
- **Design constraints:**
  - `taskRateMinutes` is an `application.yml` property with default value `1440` (24 hours).
  - Dev mode is enabled by `task-delivery.dev-mode: true` in `application.yml`.
    When dev mode is enabled, `taskRateMinutes` is overridden to `2`.
  - The rate is enforced in the enrollment service when checking if the next task is
    unlocked: `lastTaskCompletedAt + taskRateMinutes <= now`.
  - The `X-Timezone` header (already accepted by the task controller) is used to resolve
    "today" in the user's local timezone. The rate check must be timezone-aware:
    in production mode (1440 min), the next task unlocks at midnight local time,
    not 24 hours after the last submission.
  - Dev mode must never be `true` in the `prod` Spring profile.
    Add a `@PostConstruct` guard that throws `IllegalStateException` if dev mode is
    enabled in prod.
  - Validate that `taskRateMinutes` is a positive integer ≥1.
- **Tests required:**
  - `TaskDeliveryRateTest`:
    - With dev mode off, a task submitted at T is not available again until T+1440 minutes.
    - With dev mode on, a task submitted at T is available again at T+2 minutes.
    - Dev mode enabled in prod profile throws `IllegalStateException` on startup.
    - `taskRateMinutes=0` is rejected at startup with a descriptive error.
- **Security log requirement:** None.
- **ADR trigger:** No — documented here.
- **Exit criteria:** `TaskDeliveryRateTest` passes. Dev mode is verifiable by submitting
      a task and calling `/tasks/today` two minutes later.

---

### Task 3.2 — Struggle path full wire-up
- [x] **What:** Audit the existing struggle path code and fully wire it end-to-end.
      Before writing any code, document what is present, what is missing, and what is broken
      in `agentic/ISSUES.md`. Then implement the missing pieces.
- **Design constraints:**
  - **Audit first:** Read every file in the `struggle/` package. For each class, determine
    if it is: (a) fully implemented, (b) stubbed, (c) wired to the wrong adapter,
    (d) missing entirely. Write findings in ISSUES.md before coding.
  - **Struggle detection:** Triggered when a user answers the same task incorrectly
    `failureThreshold` times in a row (configurable, default: 2).
    The `ErrorPatternClassifier` (already tested) must be wired to the submission flow.
  - **Adaptive branch:** On struggle detection, the struggle service generates an adaptive
    task branch via Mistral (using the existing `TaskGenerationPort`).
    The branch consists of 3 simpler tasks targeting the identified misconception.
  - **Resolution:** A struggle session is resolved when the user answers all 3 adaptive
    tasks correctly. Resolution updates `StruggleSession.resolvedAt`.
  - **Return to main path:** After resolution, the user's regular task progression resumes
    from the task they were struggling with. The struggling task does not count as a failure
    after the struggle session resolves — it is retried once.
  - **API completeness:** `GET /struggle/enrollment/{enrollmentId}` and
    `POST /struggle/{sessionId}/tasks/{taskId}/submit` must both be functional.
- **Tests required:**
  - `StruggleDetectionTest`:
    - Second consecutive failure triggers struggle session creation.
    - First failure does not trigger a struggle session.
    - Failure on a different task resets the consecutive count.
  - `StruggleResolutionTest`:
    - Completing all 3 adaptive tasks resolves the struggle session.
    - Completing 2 of 3 does not resolve.
    - After resolution, the original task becomes retryable.
  - `StruggleControllerTest` (`@WebMvcTest`):
    - `GET /struggle/enrollment/{id}` returns 200 with active session.
    - `GET /struggle/enrollment/{id}` returns 404 when no active session.
    - `POST /struggle/{sessionId}/tasks/{taskId}/submit` returns 200.
    - Both endpoints return 401 without JWT.
- **Security log requirement:** None.
- **ADR trigger:** No — the struggle architecture is already documented in ADR-005 and ADR-013.
      Add a DECISIONS.md entry if any design choice deviates from those ADRs.
- **Exit criteria:** All tests pass. An end-to-end test (can be manual) triggers a struggle
      session by answering wrong twice, completes the adaptive tasks, and resumes normal progression.

---

### Task 3.3 — Bad question reporting
- [x] **What:** Add a reporting mechanism for learners to flag bad or incorrect questions.
      Admin can review reports, correct the question, and trigger instance regeneration.
- **Design constraints:**
  - **Learner endpoint:** `POST /tasks/{taskId}/report`
    Request body: `{ reason: String (max 500 chars) }`.
    The report is created with status `PENDING`.
    A user can report the same task once (unique constraint: `userId + taskId`).
    A second report from the same user on the same task returns 409.
  - **Admin review endpoints:**
    - `GET /admin/task-reports?status=PENDING` — list pending reports (paginated).
    - `GET /admin/task-reports/{reportId}` — report detail with task content and user reason.
    - `POST /admin/task-reports/{reportId}/correct` — body: `{ correctedQuestion, correctedOptions, correctAnswerIndex }`.
      Updates the task, marks the report as RESOLVED, triggers regeneration of all instances
      for this task (replaces existing instances with newly generated ones using the corrected content).
    - `POST /admin/task-reports/{reportId}/dismiss` — marks report as DISMISSED with no task change.
  - Regeneration after correction uses the same `TaskGenerationPort` as the main pipeline.
    It generates `N` new instances to replace the old ones.
  - Flyway migration V13 adds: `task_reports` table.
- **Tests required:**
  - `TaskReportServiceTest`:
    - Reporting a valid task creates a PENDING report.
    - Second report from same user on same task throws conflict.
    - Correcting a report triggers instance regeneration and marks report RESOLVED.
    - Dismissing a report marks it DISMISSED without changing the task.
  - `TaskReportControllerTest` (`@WebMvcTest`):
    - `POST /tasks/{id}/report` with valid JWT and body returns 201.
    - `POST /tasks/{id}/report` without JWT returns 401.
    - `GET /admin/task-reports` with USER JWT returns 403.
    - `POST /admin/task-reports/{id}/correct` with ADMIN JWT returns 200.
- **Security log requirement:** Add DECISIONS.md entry: "Report reason is stored as plain text
      and sanitised with jsoup on read to prevent stored XSS" (OWASP A03).
- **ADR trigger:** No.
- **Exit criteria:** All tests pass. Flyway V13 runs cleanly. An admin can review and correct
      a reported question via the API.

---

### Task 3.4 — User preferences: language and timezone
- [x] **What:** Add language and timezone preferences to the user profile.
      Language must be a valid BCP 47 tag. Timezone must be a valid `java.time.ZoneId`.
- **Design constraints:**
  - **Language preference:** BCP 47 tag (e.g. `en-GB`, `pt-PT`, `fr-FR`).
    Validated against the `supported-languages` whitelist from `application.yml` (added in Task 2.3).
    If the submitted tag is syntactically invalid (cannot be parsed by `java.util.Locale.forLanguageTag`),
    reject with 400.
    If the submitted tag is valid but not in the whitelist, reject with 422 with a message
    listing the supported options.
  - **Timezone preference:** Validated as a valid `java.time.ZoneId` string.
    If invalid, reject with 400.
  - **Endpoints:**
    - `PUT /profile/preferences` — body: `{ language?: String, timezone?: String }`.
      Partial update — only provided fields are updated.
    - `GET /profile/preferences` — returns `{ language, timezone }`.
  - Both endpoints require authentication (any role).
  - Flyway migration V14 adds `language` and `timezone` columns to `users` (nullable, default null).
- **Tests required:**
  - `UserPreferencesServiceTest`:
    - Valid BCP 47 + valid timezone updates both preferences.
    - Invalid BCP 47 tag returns validation error.
    - Valid tag but unsupported language returns 422.
    - Invalid timezone string returns validation error.
    - Partial update (language only) does not overwrite timezone.
  - `UserPreferencesControllerTest`:
    - `PUT /profile/preferences` with valid JWT and valid body returns 200.
    - `PUT /profile/preferences` with invalid timezone returns 400.
    - `GET /profile/preferences` returns current preferences.
    - Both endpoints return 401 without JWT.
- **Security log requirement:** None.
- **ADR trigger:** No — preference validation rules are documented here.
- **Exit criteria:** All tests pass. Flyway V14 runs cleanly. Setting a language preference
      causes subsequent task requests to return content in that language (integration with Task 2.3).

---

### Task 3.5 — Manual test recipe for Phase 03
- [x] **What:** Write the full end-to-end manual test recipe for Phase 03 in
      `agentic/manual-testing/phase-03-recipe.md`.
- **Design constraints:** The recipe must cover: dev-mode task rate, struggle trigger,
      struggle resolution, bad question report, admin correction, language preference update.
- **Tests required:** None — this is a documentation task.
- **Security log requirement:** None.
- **ADR trigger:** No.
- **Exit criteria:** File exists at `agentic/manual-testing/phase-03-recipe.md`.
      All curl commands are accurate.

---

## Exit Criteria (Phase Level)

All 5 tasks are checked off. The following is true:
- Dev mode allows completing a full task cycle in 2 minutes.
- Answering wrong twice triggers a visible struggle session.
- Completing struggle tasks resumes normal progression.
- Reporting a question creates a record visible to the admin.
- Admin correcting a question triggers instance regeneration.
- Language and timezone preferences are validated and stored.

---

## Human Checkpoint

Before marking Phase 03 DONE:

**1. Dev mode task rate:**
```bash
# Enable dev mode in application.yml: task-delivery.dev-mode: true
# Submit today's task as user A
curl -X POST -H "Authorization: Bearer $TOKEN_A" \
  -H "Content-Type: application/json" \
  -d '{"assignmentId":"abc","answer":0}' \
  http://localhost:8080/tasks/abc/submit

# Wait 2 minutes, then request today's task again
curl -H "Authorization: Bearer $TOKEN_A" http://localhost:8080/tasks/today?topicId=1
```
Expected after 2 min: a new task is available. Before 2 min: 404 or "no task available today".

**2. Struggle path:**
```bash
# Submit a wrong answer twice on the same task
curl -X POST -H "Authorization: Bearer $TOKEN_A" \
  -H "Content-Type: application/json" \
  -d '{"answer":1}' http://localhost:8080/tasks/abc/submit
# repeat

# Check for struggle session
curl -H "Authorization: Bearer $TOKEN_A" \
  http://localhost:8080/struggle/enrollment/{enrollmentId}
```
Expected: 200 with an active struggle session containing 3 adaptive tasks.

**3. Question reporting:**
```bash
curl -X POST -H "Authorization: Bearer $TOKEN_A" \
  -H "Content-Type: application/json" \
  -d '{"reason":"The correct answer is wrong — option B is clearly right."}' \
  http://localhost:8080/tasks/{taskId}/report
# Expected: 201

curl -H "Authorization: Bearer $ADMIN_TOKEN" \
  http://localhost:8080/admin/task-reports?status=PENDING
# Expected: report appears in the list
```

If any check fails, Phase 03 is not done.
