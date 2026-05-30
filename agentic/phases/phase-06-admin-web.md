# Phase 06 — Admin Web: Full Feature Parity

**Status:** `IN PROGRESS`
**Goal:** React admin web covers all server admin endpoints — topic creation (URL+PDF),
4-phase status polling, question report review and correction, user management, badge overview.

**Entry criteria:** Phase 01 DONE (stable server API to wire against).

---

## Tasks

### Task 6.0 — Topic content visibility: question list, struggle tasks, per-question stats, inline edit
- [x] **What:** Expose generated topic content to admins post-creation. Admin can see all
      generated questions (grouped by day), per-question attempt/success stats, adaptive/struggle
      tasks logged for a topic, and can inline-edit any question (title, description, hint,
      MCQ options, correct answer).
- **Delivered:**
  - **Server:** `GET /admin/topics/{topicId}/tasks` → `List<TaskTemplateAdminResponse>` (with nested stats).
    `GET /admin/topics/{topicId}/struggle-tasks` → `List<AdaptiveTaskAdminResponse>`.
    `PUT /admin/tasks/{templateId}` → `TaskTemplateAdminResponse` (increments version, regenerates instances).
    Stats aggregated from `task_assignments` via `JdbcAdminTaskStatsRepository` — no new schema.
  - **Web:** `TopicDetailPage` redesigned with 4 tabs: Overview, Questions, Struggle Questions,
    Generation Log. `TaskQuestionsPanel`, `StruggleTasksPanel`, `GenerationLogPanel`, `EditTaskModal` components.
- **Tests:** `AdminTaskServiceTest` (8 unit), `AdminTaskControllerTest` (7 `@WebMvcTest`).
  Frontend: 30 existing tests stay green; new hooks/adapters covered by mock adapter.
- **Branch:** `feat/phase-06-topic-question-management` (both repos)
- **Exit criteria:** ✅ Server tests pass. ✅ Frontend typecheck + Vitest pass. PRs open.

---

### Task 6.1 — Auth: JWT login page, token storage, logout
- [x] **What:** Implement the admin web login flow. The admin enters their email, receives
      a magic link, clicks it, and the admin web stores the JWT and uses it for all subsequent
      requests.
- **Design constraints:**
  - **Login page:** Single email field. Submit calls `POST /auth/magic-link`.
    Shows "Check your email" message after submission.
  - **Verify page:** Route `/verify?token={token}`. On mount, calls `GET /auth/verify?token={token}`.
    On success, stores the JWT and redirects to the dashboard.
    On failure (expired, already used), shows an error and a "Request a new link" button.
  - **Token storage:** Access token stored in JavaScript memory (module-level variable,
    not localStorage or sessionStorage — XSS cannot exfiltrate it).
    Refresh token stored in an `httpOnly` cookie set by the server (requires a server-side
    change: add `Set-Cookie: refreshToken=...; HttpOnly; Secure; SameSite=Strict` on the
    `/auth/verify` and `/auth/oauth` responses, and a `POST /auth/refresh` that reads
    the cookie). If the server-side cookie approach is not feasible in this phase,
    use sessionStorage as a fallback and document the XSS risk in THREAT-LOG.md.
  - **Automatic refresh:** On any 401 response, call `POST /auth/refresh`.
    If refresh succeeds, retry the request. If refresh fails, redirect to login.
  - **Logout:** Calls `DELETE /auth/logout`, clears the in-memory access token
    and the refresh cookie, redirects to login.
  - **Route guard:** All routes except `/login` and `/verify` require an access token.
    If none is present, redirect to `/login`.
- **Tests required:**
  - `LoginPage.test.tsx`:
    - Submitting an email calls `POST /auth/magic-link`.
    - Shows "Check your email" after successful submission.
  - `VerifyPage.test.tsx`:
    - Calls `GET /auth/verify?token=` on mount.
    - Redirects to dashboard on success.
    - Shows error message on 400 response.
  - `AuthGuard.test.tsx`:
    - Unauthenticated user accessing `/dashboard` is redirected to `/login`.
- **Security log requirement:** Add to THREAT-LOG.md: "Admin web access token held in
      JS memory only, not localStorage. Refresh token in httpOnly cookie (or sessionStorage
      with documented risk if cookie approach is deferred)." OWASP A02 + A05.
- **ADR trigger:** Yes — write DECISIONS.md entry for the access token memory strategy
      vs localStorage trade-off.
- **Exit criteria:** All 3 test files pass. Admin can log in, navigate to dashboard,
      and log out on the local stack.

---

### Task 6.2 — Topic creation: URL form, PDF upload, 4-phase status polling UI
- [x] **What:** Build the topic creation page with two input modes (URL and PDF upload)
      and a live status polling UI that shows the current generation phase.
- **Design constraints:**
  - **URL form:**
    - Single URL field with `https://` validation.
    - Submit calls `POST /admin/topics` with body `{ url }`.
    - On success (202 Accepted), navigate to the status polling view for the new topic ID.
  - **PDF upload form:**
    - File input accepting `.pdf` only (mime type `application/pdf`).
    - Max file size: 50MB (validate client-side before uploading).
    - Submit calls `POST /admin/topics/pdf` as `multipart/form-data`.
    - On success (202 Accepted), navigate to the status polling view.
  - **Status polling view:**
    - Displays topic title (if available), current phase, phase duration.
    - Polls `GET /admin/topics/{id}` every 5 seconds.
    - Shows a step indicator: `INGESTION → ANALYSIS → GENERATION → INDEXING → ACTIVE`.
    - Current phase is highlighted. Completed phases show a checkmark.
    - On `ACTIVE`: stop polling, show success with a "View topic" link.
    - On `FAILED`: stop polling, show error with a "Retry generation" button
      that calls `POST /admin/topics/{id}/regenerate`.
    - Polling stops when the component unmounts (clean up the interval on unmount).
  - Topics list page (`/admin/topics`) shows all topics with their current phase.
- **Tests required:**
  - `TopicCreationForm.test.tsx`:
    - Submitting a valid URL calls `POST /admin/topics`.
    - Submitting without a URL shows a validation error.
    - Non-HTTPS URL shows "URL must start with https://".
  - `PdfUploadForm.test.tsx`:
    - Selecting a valid PDF enables the submit button.
    - File over 50MB shows "File too large" error before uploading.
  - `GenerationStatusPoller.test.tsx`:
    - Polling calls `GET /admin/topics/{id}` at 5-second intervals.
    - Status advances from `GENERATION` to `ACTIVE` — polling stops.
    - On `FAILED`, retry button appears.
    - Interval is cleared on component unmount.
- **Delivered:**
  - **Web:** `CreateTopicPage` (379 lines, monolithic). URL mode calls `createFromUrl.mutate`;
    PDF mode calls `createFromPdf.mutate(formData)`. Progress shown via `TopicProgressStepper`
    with SSE (`useTopicProgress`), not polling. URL validation uses parseable URL check (not
    HTTPS-only). PDF limit is 100MB (not 50MB as spec). Navigation to topic detail fires when
    progress `done` or `failed`.
  - **Tests:** `CreateTopicPage.test.tsx` — 7 tests: URL submission, missing URL error,
    invalid URL error, PDF mode missing-file error, valid PDF submission, oversized PDF error,
    progress stepper visible after success. All tests mock `useTopicProgress` (SSE suppressed).
- **Notes:** Generation status uses SSE not polling. URL validation does not enforce `https://`
  (any parseable URL accepted). PDF max is 100MB. All spec tests mapped to single file since
  `CreateTopicPage` is monolithic (no sub-components).
- **Branch:** `feat/phase-06-topic-creation-tests` (web repo)
- **Security log requirement:** None.
- **ADR trigger:** No.
- **Exit criteria:** ✅ 7 Vitest tests pass. PR open.

---

### Task 6.3 — Question report review: list, show, correct, dismiss
- [x] **What:** Build the question report review page. Admin sees pending reports, views
      the question and user reason, and either corrects or dismisses the report.
- **Design constraints:**
  - **Report list page** (`/admin/reports`):
    - Calls `GET /admin/task-reports?status=PENDING` (paginated, 20 per page).
    - Each row shows: task question (truncated to 80 chars), user reason (truncated),
      reported date, topic title.
    - Clicking a row navigates to the report detail page.
  - **Report detail page** (`/admin/reports/{reportId}`):
    - Shows: full task question, all options with correct answer marked, user's reason.
    - Two actions: "Correct" and "Dismiss".
    - "Correct" opens an inline form to edit: question text, each option, correct answer index.
      Submit calls `POST /admin/task-reports/{reportId}/correct`.
    - "Dismiss" calls `POST /admin/task-reports/{reportId}/dismiss` with a confirmation dialog.
    - On success, navigates back to the list with a success toast.
- **Tests required:**
  - `ReportList.test.tsx`:
    - Renders list of reports from API response.
    - Shows pagination controls.
    - Empty state when no pending reports.
  - `ReportDetail.test.tsx`:
    - Dismiss button opens confirmation dialog.
    - Confirming dismiss calls the dismiss endpoint.
    - Correct form validates that all fields are non-empty before submitting.
    - Correct form submission calls the correct endpoint.
- **Delivered:**
  - **Web:** `domain/models/Report.ts`, `domain/ports/ReportPort.ts`, `ReportAdapter`,
    `MockReportAdapter`, `useReports.ts` hooks, `ReportListPage.tsx`, `ReportDetailPage.tsx`.
    Routes: `ADMIN_REPORTS=/admin/reports`, `ADMIN_REPORT_DETAIL=/admin/reports/:reportId`.
    `container.ts` wired with `reportService`.
  - **Note:** `TaskReportResponse` does not include task question/options (only `taskTemplateId`).
    Correction form starts empty — admin enters corrected values. This is a server API limitation
    (no `GET /admin/tasks/{id}` endpoint and `TaskReportResponse` omits question details).
  - **Tests:** `ReportList.test.tsx` (5 tests), `ReportDetail.test.tsx` (6 tests). All 11 pass.
- **Branch:** `feat/phase-06-topic-creation-tests` (web repo, same branch as Task 6.2)
- **Security log requirement:** None.
- **ADR trigger:** No.
- **Exit criteria:** ✅ 11 tests pass. PR #21 open.

---

### Task 6.4 — User management: list, promote to ADMIN, view enrollment status
- [x] **What:** Build the user management page. Admin can see all users, promote a user
      to ADMIN, and view their enrollment status.
- **Design constraints:**
  - This requires a new server endpoint if it does not already exist:
    - `GET /admin/users` — paginated list of users. Response fields:
      `{ id, email, name, role, createdAt, enrollmentCount }`.
    - `POST /admin/users/{userId}/promote` — promotes user to ADMIN role.
      Requires ADMIN role. Cannot demote — promotion is one-way in this phase.
  - **User list page** (`/admin/users`):
    - Paginated table: email, name, role (USER/ADMIN badge), joined date, topics enrolled.
    - Promote button visible for USER-role users. Not visible for ADMIN-role users.
    - Clicking Promote shows a confirmation dialog: "This grants full admin access.
      This cannot be undone. Promote {name}?"
  - **Enrollment view:**
    - Clicking a user shows a modal with their enrolled topics and current day progress.
    - Calls `GET /admin/users/{userId}/enrollments` (add this endpoint if missing).
- **Tests required:**
  - `UserList.test.tsx`:
    - Renders users with correct role badges.
    - Promote button is visible for USER role, hidden for ADMIN.
    - Promote action shows confirmation dialog.
  - `PromoteUser.test.tsx`:
    - Confirming promotion calls `POST /admin/users/{userId}/promote`.
    - On success, the user's role badge updates to ADMIN.
- **Security log requirement:** Add to THREAT-LOG.md: "User promotion endpoint restricted
      to ADMIN role. Demotion not possible via API — requires direct DB intervention,
      which is intentional." OWASP A01.
- **ADR trigger:** No.
- **Exit criteria:** All test files pass. An admin can promote another user via the UI.
- **Delivered:**
  - Server: `AdminUserController` — `GET /admin/users`, `POST /admin/users/{userId}/promote`
  - Server: `UserRepository.findAll`, `EnrollmentRepository.countByUserId` added
  - Web: `AdminUserFull`/`AdminUserPage` models, `IUserService` port, `UserAdapter`, `MockUserAdapter`
  - Web: `useUsers`/`usePromoteUser` hooks, `UserListPage`, route at `/admin/users`
  - Tests: `UserList.test.tsx` (6), `PromoteUser.test.tsx` (4) — all pass

---

### Task 6.5 — Badge overview: per-topic badge issuance stats
- [x] **What:** Build the badge overview page showing how many users have earned each
      topic's badge and recent earners.
- **Design constraints:**
  - **Badge stats page** (`/admin/badges`):
    - Lists all topics with badges.
    - For each topic: `{ topicTitle, totalIssued, enrolledCount, earnedPercentage }`.
    - Calls `GET /admin/topics/{topicId}/badges` for each topic (added in Task 2.7).
    - Uses a single summary call if a batch endpoint is available; otherwise,
      fetches per-topic in parallel (`Promise.all`).
  - **Badge detail panel:**
    - Clicking a topic shows a side panel with recent earners (userId, earnedAt).
    - Sorted by earnedAt descending. Shows the 10 most recent earners.
- **Tests required:**
  - `BadgeOverview.test.tsx`:
    - Renders topic list with earned percentage.
    - "0%" renders as "No earners yet" not "0%".
    - Clicking a topic shows the detail panel.
    - Detail panel shows recent earners in descending date order.
- **Security log requirement:** None.
- **ADR trigger:** No.
- **Exit criteria:** `BadgeOverview.test.tsx` passes. Stats page loads on the local stack.
- **Delivered:**
  - Models: `TopicBadgeStats`, `RecentEarner` added to `Topic.ts`
  - Port: `getTopicBadgeStats(topicId)` added to `ITopicService`
  - Adapters: `TopicAdapter` and `MockTopicAdapter` implement `getTopicBadgeStats`
  - Hook: `useAllTopicBadgeStats(topicIds)` in `useTopics.ts` — parallel `Promise.all`
  - Web: `BadgeOverviewPage`, route `/admin/badges`; "0%" shows "No earners yet"
  - Tests: `BadgeOverview.test.tsx` (5) — all pass

---

### Task 6.6 — Manual test recipe for Phase 06
- [x] **What:** Write the full end-to-end manual test recipe for Phase 06 in
      `agentic/manual-testing/phase-06-recipe.md`.
- **Design constraints:** The recipe must cover: login, topic creation via URL and PDF,
      status polling to ACTIVE, report review and correction, user promotion, badge stats.
- **Tests required:** None.
- **Security log requirement:** None.
- **ADR trigger:** No.
- **Exit criteria:** File exists and is accurate.
- **Delivered:** `agentic/manual-testing/phase-06-recipe.md` — 6 sections covering login,
  URL creation, PDF creation, report correction, user promotion, badge stats.

---

## Exit Criteria (Phase Level)

All 6 tasks are checked off. The following is true:
- Admin can log in via magic link on the admin web.
- Topics can be created from URL and PDF with live phase polling.
- Question reports can be reviewed, corrected, and dismissed.
- Users can be promoted to ADMIN role.
- Badge stats are visible.

---

## Human Checkpoint

Before marking Phase 06 DONE:

**1. Login:**
- Navigate to `http://localhost:5173/login`.
- Enter admin email. Click "Send link". Check server log for the magic link URL.
- Open the verify URL. Expected: redirect to dashboard.

**2. Topic creation with phase polling:**
- Navigate to `/admin/topics/new`.
- Enter a valid HTTPS URL. Submit.
- Expected: status polling UI appears, showing `INGESTION` highlighted.
  Within 30–60 seconds (local Mistral), status advances to `ACTIVE`.

**3. Question report correction:**
- Ensure a question report exists (use the mobile app or curl to report a task).
- Navigate to `/admin/reports`.
- Click the report. Click "Correct". Edit the question. Submit.
- Expected: report disappears from the PENDING list.

**4. User promotion:**
- Navigate to `/admin/users`.
- Find a USER-role user. Click "Promote".
- Confirm in the dialog.
- Expected: role badge changes to ADMIN.

If any check fails, Phase 06 is not done.
