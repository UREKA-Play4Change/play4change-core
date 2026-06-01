# Phase 03 Manual Test Recipe
## Learner Logic: Rate Control, Struggle Path & Reporting

This recipe verifies all Phase 03 human-checkpoint criteria.
Execute the sections in order. No programming knowledge is required —
only `curl`, a terminal, and a running Docker Compose stack.

---

## Prerequisites

1. Start the full stack from the project root:
   ```bash
   docker compose up --build -d
   ```
2. Confirm the server is healthy:
   ```bash
   curl -s http://localhost:8080/actuator/health
   ```
   Expected: `{"status":"UP"}`

3. `curl` and `python3` must be installed (python3 is used only for pretty-printing JSON).

4. A topic must already be `ACTIVE` with at least 1 task. If you have not run the
   Phase 02 recipe, do so now to produce an active topic before continuing.

5. **Dev-mode must be enabled** for Sections 1 and 2. Edit
   `server/src/main/resources/application.yml` and set:
   ```yaml
   task-delivery:
     task-rate-minutes: 1440
     dev-mode: true
   ```
   Rebuild and restart:
   ```bash
   docker compose up --build -d server
   ```
   > **Important:** `dev-mode: true` must never appear in the `prod` Spring profile.
   > The server will refuse to start if it does (guarded by `@PostConstruct`).

---

## Shell Variables

Declare these now. Fill in each value as you reach the step that produces it.

```bash
ADMIN_TOKEN=""
USER_A_TOKEN=""
TOPIC_ID=""
ENROLLMENT_ID=""
ASSIGNMENT_ID=""          # the assignment you will answer wrong twice
SESSION_ID=""             # struggle session ID
ADAPTIVE_TASK_1=""
ADAPTIVE_TASK_2=""
ADAPTIVE_TASK_3=""
REPORT_ID=""
```

---

## Section 1 — Obtain Tokens

### 1.1 Admin token

If you already have an admin token from the Phase 02 recipe, reuse it.
Otherwise, follow the Phase 02 recipe Sections 1.1–1.2 to obtain `ADMIN_TOKEN`.

```bash
ADMIN_TOKEN="<paste accessToken here>"
```

### 1.2 User A token

```bash
curl -s -X POST http://localhost:8080/auth/magic-link \
  -H "Content-Type: application/json" \
  -d '{"email": "user-a@example.com"}'
```

Read the token from the server logs if you are using console email:
```bash
docker compose logs server --no-log-prefix | grep -i "magic\|token" | tail -5
```

Verify:
```bash
curl -s -X POST http://localhost:8080/auth/magic-link/verify \
  -H "Content-Type: application/json" \
  -d '{"token": "<paste-token-here>"}' | python3 -m json.tool
```

```bash
USER_A_TOKEN="<paste accessToken here>"
```

---

## Section 2 — Dev-Mode Task Delivery Rate

> **Goal:** Verify that in dev mode a new task becomes available 2 minutes after
> the previous submission, not after waiting 24 hours.

### 2.1 Enroll User A in the topic (skip if already enrolled)

```bash
curl -s -X POST http://localhost:8080/topics/$TOPIC_ID/enroll \
  -H "Authorization: Bearer $USER_A_TOKEN" \
  | python3 -m json.tool
```

Note the enrollment `id` field:
```bash
ENROLLMENT_ID="<paste enrollment id here>"
```

### 2.2 Get today's task

```bash
curl -s "http://localhost:8080/tasks/today?topicId=$TOPIC_ID" \
  -H "Authorization: Bearer $USER_A_TOKEN" \
  | python3 -m json.tool
```

Expected:
```json
{
  "assignmentId": "...",
  "title": "...",
  "options": ["...", "...", "..."],
  "pointsReward": 20,
  "wrongAttemptCount": 0
}
```

```bash
ASSIGNMENT_ID="<paste assignmentId here>"
```

### 2.3 Submit a correct answer

Try option indices 0, 1, 2, … until you receive `"isCorrect": true`:

```bash
curl -s -X POST http://localhost:8080/tasks/$ASSIGNMENT_ID/submit \
  -H "Authorization: Bearer $USER_A_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"selectedOption": 0}' | python3 -m json.tool
```

Expected:
```json
{
  "isCorrect": true,
  "pointsAwarded": 20,
  "totalPoints": 20,
  "streakDays": 1,
  "struggleTriggered": false
}
```

### 2.4 Immediately request the next task — expect "not available yet"

```bash
curl -si "http://localhost:8080/tasks/today?topicId=$TOPIC_ID" \
  -H "Authorization: Bearer $USER_A_TOKEN"
```

Expected: `HTTP 404` with response header `X-Task-Available-At: <timestamp>`.

✅ **PASS:** Status is 404 and `X-Task-Available-At` is present.
❌ **FAIL:** Status is 200 — dev-mode rate check is not enforcing the window.

### 2.5 Wait 2 minutes, then request again

```bash
sleep 120

curl -s "http://localhost:8080/tasks/today?topicId=$TOPIC_ID" \
  -H "Authorization: Bearer $USER_A_TOKEN" \
  | python3 -m json.tool
```

Expected: `HTTP 200` with a new `assignmentId`.

✅ **PASS:** A new task is returned with a different `assignmentId`.
❌ **FAIL:** Still 404 — check that `task-delivery.dev-mode: true` is set and the
server was restarted.

---

## Section 3 — Struggle Path: Trigger

> **Goal:** Verify that answering the same task incorrectly twice in a row
> creates an active struggle session with 3 adaptive tasks.

### 3.1 Get today's task

```bash
curl -s "http://localhost:8080/tasks/today?topicId=$TOPIC_ID" \
  -H "Authorization: Bearer $USER_A_TOKEN" \
  | python3 -m json.tool
```

```bash
ASSIGNMENT_ID="<paste assignmentId here>"
```

### 3.2 Submit a wrong answer (first attempt)

Look at the options returned in step 3.1. Submit any index that is NOT the correct
answer. If you don't know the correct answer, submit index 0 and iterate.

```bash
curl -s -X POST http://localhost:8080/tasks/$ASSIGNMENT_ID/submit \
  -H "Authorization: Bearer $USER_A_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"selectedOption": 0}' | python3 -m json.tool
```

Expected:
```json
{
  "isCorrect": false,
  "pointsAwarded": 0,
  "totalPoints": 20,
  "streakDays": 1,
  "struggleTriggered": false
}
```

`struggleTriggered` must be `false` after the first wrong answer.

✅ **PASS:** `isCorrect: false`, `struggleTriggered: false`.

### 3.3 Submit a wrong answer again (second attempt)

Submit the same wrong option again:

```bash
curl -s -X POST http://localhost:8080/tasks/$ASSIGNMENT_ID/submit \
  -H "Authorization: Bearer $USER_A_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"selectedOption": 0}' | python3 -m json.tool
```

Expected:
```json
{
  "isCorrect": false,
  "pointsAwarded": 0,
  "totalPoints": 20,
  "streakDays": 1,
  "struggleTriggered": true
}
```

✅ **PASS:** `struggleTriggered: true` on the second consecutive wrong answer.
❌ **FAIL:** `struggleTriggered: false` — the threshold or `HandleStruggleService` wiring is broken.

### 3.4 Fetch the struggle session

The struggle session is generated asynchronously. Wait 5–10 seconds for Mistral to
generate the 3 adaptive tasks, then:

```bash
curl -s http://localhost:8080/struggle/enrollment/$ENROLLMENT_ID \
  -H "Authorization: Bearer $USER_A_TOKEN" \
  | python3 -m json.tool
```

Expected:
```json
{
  "sessionId": "...",
  "errorPattern": "WRONG_CONCEPT",
  "status": "OPEN",
  "adaptiveTasks": [
    { "taskId": "...", "title": "...", "options": ["..."], "pointsReward": 10 },
    { "taskId": "...", "title": "...", "options": ["..."], "pointsReward": 10 },
    { "taskId": "...", "title": "...", "options": ["..."], "pointsReward": 10 }
  ]
}
```

```bash
SESSION_ID="<paste sessionId here>"
ADAPTIVE_TASK_1="<paste adaptiveTasks[0].taskId here>"
ADAPTIVE_TASK_2="<paste adaptiveTasks[1].taskId here>"
ADAPTIVE_TASK_3="<paste adaptiveTasks[2].taskId here>"
```

✅ **PASS:** Status is `OPEN`, exactly 3 adaptive tasks are present.
❌ **FAIL:** 404 — the session has not been created yet; wait a few more seconds and retry.

---

## Section 4 — Struggle Path: Resolution

> **Goal:** Verify that completing all 3 adaptive tasks resolves the session
> and makes the original task retryable.

### 4.1 Submit adaptive task 1

Find the correct answer by trying option 0 first, then 1, etc.
The submit endpoint is `POST /struggle/{sessionId}/tasks/{taskId}/submit`.

```bash
curl -s -X POST http://localhost:8080/struggle/$SESSION_ID/tasks/$ADAPTIVE_TASK_1/submit \
  -H "Authorization: Bearer $USER_A_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"selectedOption": 0}' | python3 -m json.tool
```

Repeat with `selectedOption: 1`, `2`, … until you see a response with `"taskId"` and `"title"`.
The response is the updated task DTO; any 200 means the submission was accepted.

### 4.2 Submit adaptive task 2

```bash
curl -s -X POST http://localhost:8080/struggle/$SESSION_ID/tasks/$ADAPTIVE_TASK_2/submit \
  -H "Authorization: Bearer $USER_A_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"selectedOption": 0}' | python3 -m json.tool
```

### 4.3 Submit adaptive task 3

```bash
curl -s -X POST http://localhost:8080/struggle/$SESSION_ID/tasks/$ADAPTIVE_TASK_3/submit \
  -H "Authorization: Bearer $USER_A_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"selectedOption": 0}' | python3 -m json.tool
```

After this call, the session is resolved automatically.

### 4.4 Verify the session is resolved

```bash
curl -s http://localhost:8080/struggle/enrollment/$ENROLLMENT_ID \
  -H "Authorization: Bearer $USER_A_TOKEN" \
  | python3 -m json.tool
```

Expected:
```json
{
  "sessionId": "...",
  "status": "RESOLVED",
  "adaptiveTasks": [...]
}
```

✅ **PASS:** `status` is `RESOLVED`.
❌ **FAIL:** `status` is still `OPEN` — check that all 3 adaptive tasks were submitted.

### 4.5 Verify the original task is retryable

After resolution the original failing assignment is reset to `PENDING`.
Fetch today's task:

```bash
curl -s "http://localhost:8080/tasks/today?topicId=$TOPIC_ID" \
  -H "Authorization: Bearer $USER_A_TOKEN" \
  | python3 -m json.tool
```

Expected: `HTTP 200` with the **same task** that caused the struggle (same title, same template),
and `"wrongAttemptCount": 0`.

✅ **PASS:** The task is retried with `wrongAttemptCount: 0`.
❌ **FAIL:** 404 or a different task — check `AdaptiveTaskService` assignment reset logic.

---

## Section 5 — Bad Question Reporting

> **Goal:** Verify a learner can report a task, an admin can see the report,
> and the admin can dismiss or correct it.

### 5.1 Report the current task as a bad question

Use the `assignmentId` from step 4.5 as the `taskId` path variable
(report endpoint takes the task template ID — the `assignmentId` here is the
task assignment; note: in practice `taskId` in the report URL refers to
the `taskTemplateId` returned inside the assignment. Check the task response
for a `taskTemplateId` field, or use the `assignmentId` directly if the endpoint
accepts it — see step 5.1a):

#### 5.1a If the task response includes a `taskTemplateId` field

```bash
curl -s "http://localhost:8080/tasks/today?topicId=$TOPIC_ID" \
  -H "Authorization: Bearer $USER_A_TOKEN" \
  | python3 -m json.tool
```

Note the `taskTemplateId` from the response.

#### 5.1b Submit the report

```bash
TASK_TEMPLATE_ID="<paste taskTemplateId here>"

curl -s -X POST http://localhost:8080/tasks/$TASK_TEMPLATE_ID/report \
  -H "Authorization: Bearer $USER_A_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"reason": "The correct answer is wrong — option B is clearly right."}' \
  | python3 -m json.tool
```

Expected `HTTP 201`:
```json
{
  "reportId": "..."
}
```

```bash
REPORT_ID="<paste reportId here>"
```

✅ **PASS:** Status 201 with a `reportId`.
❌ **FAIL:** 404 — the task template ID is incorrect. Use the `taskTemplateId`
from the task response, not the `assignmentId`.

### 5.2 Second report from same user returns 409

```bash
curl -si -X POST http://localhost:8080/tasks/$TASK_TEMPLATE_ID/report \
  -H "Authorization: Bearer $USER_A_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"reason": "Duplicate report."}'
```

Expected: `HTTP 409 Conflict`.

✅ **PASS:** Duplicate report is rejected.

### 5.3 Admin lists pending reports

```bash
curl -s "http://localhost:8080/admin/task-reports?status=PENDING" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  | python3 -m json.tool
```

Expected:
```json
{
  "content": [
    {
      "reportId": "...",
      "taskTemplateId": "...",
      "userId": "...",
      "reason": "The correct answer is wrong — option B is clearly right.",
      "status": "PENDING",
      "reportedAt": "..."
    }
  ],
  "totalElements": 1,
  "totalPages": 1
}
```

✅ **PASS:** The report appears in the list with `status: "PENDING"`.

### 5.4 Admin fetches report detail

```bash
curl -s http://localhost:8080/admin/task-reports/$REPORT_ID \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  | python3 -m json.tool
```

Expected: the full report object with `status: "PENDING"`.

### 5.5 Admin dismisses the report

```bash
curl -s -X POST http://localhost:8080/admin/task-reports/$REPORT_ID/dismiss \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  | python3 -m json.tool
```

Expected:
```json
{
  "reportId": "...",
  "status": "DISMISSED",
  "resolvedAt": "..."
}
```

✅ **PASS:** `status` is `DISMISSED`.

---

## Section 6 — Admin Question Correction and Instance Regeneration

> **Goal:** Verify an admin can correct a task and trigger instance regeneration.
> This uses a **fresh report** because the one from Section 5 is already DISMISSED.

### 6.1 Create a second report from a different user

Obtain a token for User B (follow steps 1.2 with a different email), then:

```bash
USER_B_TOKEN="<paste accessToken here>"

curl -s -X POST http://localhost:8080/tasks/$TASK_TEMPLATE_ID/report \
  -H "Authorization: Bearer $USER_B_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"reason": "Answer C is more accurate than the listed correct answer."}' \
  | python3 -m json.tool
```

```bash
REPORT_ID_2="<paste reportId here>"
```

### 6.2 Admin corrects the task

```bash
curl -s -X POST http://localhost:8080/admin/task-reports/$REPORT_ID_2/correct \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "correctedTitle": "Updated question title",
    "correctedOptions": ["Option A", "Option B", "Option C (correct)", "Option D"],
    "correctAnswerIndex": 2
  }' | python3 -m json.tool
```

Expected:
```json
{
  "reportId": "...",
  "status": "RESOLVED",
  "resolvedAt": "..."
}
```

✅ **PASS:** `status` is `RESOLVED`. The template and its instances were updated.

### 6.3 Verify task instances were regenerated (server logs)

```bash
docker compose logs server --no-log-prefix | grep -i "regenerated\|corrected\|instance" | tail -10
```

Expected log output containing something like:
```
Task report <id> resolved: template <id> corrected, instances regenerated
```

✅ **PASS:** Log line confirms regeneration.

---

## Section 7 — Language Preference Update

> **Goal:** Verify `PUT /profile/preferences` updates language and timezone,
> and that subsequent task requests use the updated language.

### 7.1 View current preferences

```bash
curl -s http://localhost:8080/profile/preferences \
  -H "Authorization: Bearer $USER_A_TOKEN" \
  | python3 -m json.tool
```

Expected:
```json
{
  "language": "en",
  "timezone": null
}
```

### 7.2 Update language and timezone

```bash
curl -s -X PUT http://localhost:8080/profile/preferences \
  -H "Authorization: Bearer $USER_A_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"language": "pt-PT", "timezone": "Europe/Lisbon"}' \
  | python3 -m json.tool
```

Expected:
```json
{
  "language": "pt-PT",
  "timezone": "Europe/Lisbon"
}
```

✅ **PASS:** Both fields updated.

### 7.3 Verify persisted preferences

```bash
curl -s http://localhost:8080/profile/preferences \
  -H "Authorization: Bearer $USER_A_TOKEN" \
  | python3 -m json.tool
```

Expected:
```json
{
  "language": "pt-PT",
  "timezone": "Europe/Lisbon"
}
```

✅ **PASS:** Preferences are persisted across requests.

### 7.4 Invalid BCP 47 tag is rejected (400)

```bash
curl -si -X PUT http://localhost:8080/profile/preferences \
  -H "Authorization: Bearer $USER_A_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"language": "not-valid!!"}'
```

Expected: `HTTP 400 Bad Request`.

✅ **PASS:** Invalid syntax is rejected.

### 7.5 Unsupported language tag is rejected (422)

`fr-FR` is valid BCP 47 but is not in the `supported-languages` whitelist:

```bash
curl -si -X PUT http://localhost:8080/profile/preferences \
  -H "Authorization: Bearer $USER_A_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"language": "fr-FR"}'
```

Expected: `HTTP 422 Unprocessable Entity`.

✅ **PASS:** Valid-but-unsupported language is rejected with 422.

### 7.6 Invalid timezone is rejected (400)

```bash
curl -si -X PUT http://localhost:8080/profile/preferences \
  -H "Authorization: Bearer $USER_A_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"timezone": "Not/AZone"}'
```

Expected: `HTTP 400 Bad Request`.

✅ **PASS:** Invalid timezone is rejected.

### 7.7 Partial update preserves unchanged field

Update language only:

```bash
curl -s -X PUT http://localhost:8080/profile/preferences \
  -H "Authorization: Bearer $USER_A_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"language": "en"}' \
  | python3 -m json.tool
```

Expected:
```json
{
  "language": "en",
  "timezone": "Europe/Lisbon"
}
```

✅ **PASS:** `timezone` is preserved even though it was not included in the request.

---

## Section 8 — Full Test Suite

Restore `dev-mode: false` before running the full suite to avoid the prod-guard startup failure:

```yaml
task-delivery:
  dev-mode: false
```

Then run:

```bash
./gradlew :server:test
```

Expected:
```
BUILD SUCCESSFUL
```

All server tests must be green. No failures.

---

## Phase 03 Exit Checklist

Mark each item confirmed before declaring Phase 03 complete:

- [ ] **Dev-mode rate:** A task submitted at T is not available again before T+2 min; after 2 min a new task is returned (Sections 2.4–2.5).
- [ ] **Struggle trigger:** First wrong answer does not trigger struggle; second consecutive wrong answer returns `struggleTriggered: true` (Sections 3.2–3.3).
- [ ] **Struggle session:** `GET /struggle/enrollment/{id}` returns an OPEN session with exactly 3 adaptive tasks (Section 3.4).
- [ ] **Struggle resolution:** Completing all 3 adaptive tasks sets session `status: RESOLVED` (Section 4.4).
- [ ] **Original task retry:** After resolution, `GET /tasks/today` returns the original task with `wrongAttemptCount: 0` (Section 4.5).
- [ ] **Report creation:** `POST /tasks/{taskId}/report` returns 201 with a `reportId` (Section 5.1).
- [ ] **Duplicate report rejected:** Second report from same user returns 409 (Section 5.2).
- [ ] **Admin report list:** Pending report visible in `GET /admin/task-reports?status=PENDING` (Section 5.3).
- [ ] **Admin dismiss:** Report marked DISMISSED (Section 5.5).
- [ ] **Admin correct:** Report marked RESOLVED, server logs confirm instance regeneration (Sections 6.2–6.3).
- [ ] **Language update:** `PUT /profile/preferences` updates language and timezone; changes persist (Sections 7.2–7.3).
- [ ] **Validation:** Invalid BCP 47 → 400, unsupported language → 422, invalid timezone → 400 (Sections 7.4–7.6).
- [ ] **Partial update:** Omitting a field preserves the existing value (Section 7.7).
- [ ] **Full test suite:** `./gradlew :server:test` is green (Section 8).
