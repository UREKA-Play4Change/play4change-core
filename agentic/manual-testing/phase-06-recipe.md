# Phase 06 Manual Test Recipe
## Admin Web: Full Feature Walkthrough

This recipe verifies all Phase 06 human-checkpoint criteria.
Execute the sections in order against the local Docker Compose stack with the
admin web app running.

---

## Prerequisites

1. Start the full stack:
   ```bash
   docker compose up --build -d
   ```
2. Confirm server health:
   ```bash
   curl -s http://localhost:8080/actuator/health
   ```
   Expected: `{"status":"UP"}`

3. Start the admin web in dev mode:
   ```bash
   cd play4change-web
   npm run dev
   ```
   Vite will serve at `http://localhost:5173`.

4. Ensure at least one registered user exists (use the mobile app or create
   one via magic link before running this recipe).

---

## Section 1 — Admin Login via Magic Link

**Goal:** Admin can log in and land on the dashboard.

1. Open `http://localhost:5173/admin/login`.
2. Enter the admin user's email. Click **Send magic link**.
   - Expected: "Check your email" view appears.
3. Check the server log for a line like:
   ```
   Magic-link token for <email>: http://localhost:5173/auth/verify?token=...
   ```
4. Open that URL in the browser.
   - Expected: Redirect to `/admin/dashboard`.
5. Verify the dashboard shows stat cards (topics, enrolled, etc.).

---

## Section 2 — Topic Creation from URL

**Goal:** Admin can create a topic using a URL source and observe live phase
polling.

1. Navigate to `/admin/topics/new`.
2. Enter:
   - **Title:** `Test Topic URL`
   - **Description:** any text
   - **Category:** `Test`
   - **Source:** URL tab
   - **URL:** any valid public HTTPS URL (e.g. Wikipedia article)
3. Click **Create Topic**.
   - Expected: Phase stepper appears — `INGESTION` is highlighted.
4. Wait 30–120 seconds (depends on local Mistral speed).
   - Expected: Stepper advances through `GENERATION` and eventually reaches `ACTIVE`.
5. Navigate to `/admin/topics`.
   - Expected: The new topic appears with status badge **ACTIVE**.

---

## Section 3 — Topic Creation from PDF

**Goal:** Admin can create a topic by uploading a PDF.

1. Navigate to `/admin/topics/new`.
2. Enter title, description, and category.
3. Click the **PDF Upload** tab.
4. Upload any PDF file under 100 MB.
5. Click **Create Topic**.
   - Expected: Phase stepper appears and topic eventually reaches `ACTIVE`.

---

## Section 4 — Question Report Review and Correction

**Goal:** Admin can view, correct, and dismiss question reports.

**Setup:** Use the mobile app (or curl) to report a task question:
```bash
curl -X POST http://localhost:8080/user/tasks/{taskId}/report \
  -H "Authorization: Bearer <user_token>" \
  -H "Content-Type: application/json" \
  -d '{"reason": "The answer is wrong"}'
```

1. Navigate to `/admin/reports`.
   - Expected: The report appears in the list with reason and date.
2. Click the report row.
   - Expected: Report detail page shows reason, task ID, and action buttons.
3. Click **Correct**.
   - Expected: Inline form appears with title, 4 option fields, and correct-answer radio buttons.
4. Fill in corrected title, all 4 options, select the correct answer. Click **Submit correction**.
   - Expected: Success toast. Report disappears from the pending list.
5. Repeat steps 1–2 for a second report. Click **Dismiss**.
   - Expected: Dismiss confirmation dialog appears.
6. Click **Confirm** in the dialog.
   - Expected: Success toast. Report disappears from the pending list.

---

## Section 5 — User Promotion

**Goal:** Admin can promote a USER-role user to ADMIN.

1. Navigate to `/admin/users`.
   - Expected: Table shows all registered users with role badges (USER / ADMIN).
2. Find a user with role **USER**. Click **Promote**.
   - Expected: Confirmation dialog appears: "This grants full admin access…".
3. Click **Promote** in the dialog.
   - Expected: Success toast. The user's role badge updates to **ADMIN**.
4. Refresh the page.
   - Expected: The promoted user still shows the **ADMIN** badge.

---

## Section 6 — Badge Stats

**Goal:** Admin can see per-topic badge issuance statistics.

1. Navigate to `/admin/badges`.
   - Expected: Table lists all topics. Topics with no earners show "No earners yet" in the Earned column. Topics with earners show a percentage (e.g. `30.0%`).
2. Click a topic row that has earners.
   - Expected: A side panel opens showing the 10 most recent earners (userId + date).
3. Click the same row again.
   - Expected: The side panel closes.

---

## Pass / Fail Criteria

| Check | Expected |
|---|---|
| Magic link login | Redirects to dashboard |
| URL topic creation | Reaches ACTIVE via phase stepper |
| PDF topic creation | Reaches ACTIVE via phase stepper |
| Report correction | Report leaves PENDING list |
| Report dismissal | Report leaves PENDING list |
| User promotion | Role badge changes to ADMIN |
| Badge stats | % shown; side panel with recent earners |

All checks must pass for Phase 06 to be considered complete.
