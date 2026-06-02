# Phase 02 Manual Test Recipe
## Content Generation: Anti-Cheat, Multi-Language & Badges

This recipe verifies all Phase 02 human-checkpoint criteria.
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

4. You need access to three distinct email inboxes **or** you can read tokens directly
   from the server logs:
   ```bash
   docker compose logs server --follow | grep -i "magic"
   ```

---

## Shell Variables

Declare these now. Fill in each value as you reach the step that produces it.

```bash
ADMIN_TOKEN=""
USER_A_TOKEN=""
USER_B_TOKEN=""
TOPIC_ID=""
ASSIGNMENT_A=""
ASSIGNMENT_A2=""   # second task for User A (if taskCount > 1)
ASSIGNMENT_B=""
```

---

## Section 1 — Obtain Tokens

### 1.1 Grant admin role to an account

The server has no built-in admin seeding. After the first magic-link login, promote
that account manually:

```bash
docker compose exec postgres psql -U play4change -d play4change \
  -c "UPDATE users SET role = 'ADMIN' WHERE email = 'admin@example.com';"
```

### 1.2 Request a magic link for the admin account

```bash
curl -s -X POST http://localhost:8080/auth/magic-link \
  -H "Content-Type: application/json" \
  -d '{"email": "admin@example.com"}'
```

Expected:
```json
{"message": "Magic link sent. Check your email."}
```

Check your inbox for a link containing `?token=<TOKEN>`, or read it from logs:
```bash
docker compose logs server --no-log-prefix | grep -i "magic\|token" | tail -5
```

Verify the token:
```bash
curl -s -X POST http://localhost:8080/auth/magic-link/verify \
  -H "Content-Type: application/json" \
  -d '{"token": "<paste-token-here>"}'
```

Expected:
```json
{"accessToken": "eyJ...", "refreshToken": "...", "expiresIn": 900}
```

```bash
ADMIN_TOKEN="<paste accessToken here>"
```

Run the `UPDATE` from step 1.1 if you haven't already, then verify again to get a
fresh token that carries the ADMIN role.

### 1.3 User A token

```bash
curl -s -X POST http://localhost:8080/auth/magic-link \
  -H "Content-Type: application/json" \
  -d '{"email": "user-a@example.com"}'
```

Verify:
```bash
curl -s -X POST http://localhost:8080/auth/magic-link/verify \
  -H "Content-Type: application/json" \
  -d '{"token": "<paste-token-here>"}'
```

```bash
USER_A_TOKEN="<paste accessToken here>"
```

### 1.4 User B token

```bash
curl -s -X POST http://localhost:8080/auth/magic-link \
  -H "Content-Type: application/json" \
  -d '{"email": "user-b@example.com"}'
```

Verify:
```bash
curl -s -X POST http://localhost:8080/auth/magic-link/verify \
  -H "Content-Type: application/json" \
  -d '{"token": "<paste-token-here>"}'
```

```bash
USER_B_TOKEN="<paste accessToken here>"
```

---

## Section 2 — Create a Topic and Poll the Pipeline Phase

*This section covers Human Checkpoint 3: pipeline phase polling.*

### 2.1 Create a topic from a URL

```bash
curl -s -X POST http://localhost:8080/admin/topics \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Introduction to Kubernetes",
    "description": "Learn the basics of container orchestration with Kubernetes.",
    "category": "DevOps",
    "urls": ["https://kubernetes.io/docs/concepts/overview/what-is-kubernetes/"],
    "durationDays": 1,
    "difficulty": "BEGINNER",
    "language": "en",
    "taskCount": 2
  }' | python3 -m json.tool
```

Expected response — note the `id` and `currentPhase`:
```json
{
  "id": "abc123",
  "title": "Introduction to Kubernetes",
  "currentPhase": "INGESTION",
  "phaseUpdatedAt": "2026-05-02T13:00:00Z",
  "generationLog": [],
  ...
}
```

```bash
TOPIC_ID="<paste id here>"
```

### 2.2 Poll the pipeline phase

Run this every 10–30 seconds until `currentPhase` is `ACTIVE`:

```bash
curl -s http://localhost:8080/admin/topics/$TOPIC_ID \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  | python3 -m json.tool | grep -E '"currentPhase"|"phaseUpdatedAt"'
```

**Expected progression (in order):**

| Poll | `currentPhase` |
|------|---------------|
| 1    | `INGESTION`   |
| 2    | `ANALYSIS`    |
| 3    | `GENERATION`  |
| 4    | `INDEXING`    |
| 5    | `ACTIVE`      |

✅ **PASS:** `currentPhase` reaches `ACTIVE`.
❌ **FAIL:** `currentPhase` becomes `FAILED` — see step 2.3.

### 2.3 Inspect the generation log

Once the topic is `ACTIVE`, fetch the full response and check `generationLog`:

```bash
curl -s http://localhost:8080/admin/topics/$TOPIC_ID \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  | python3 -m json.tool
```

Look for `"generationLog"` — it must contain one entry per phase transition:
```json
"generationLog": [
  {
    "fromPhase": "INGESTION",
    "toPhase": "ANALYSIS",
    "transitionedAt": "2026-05-02T13:00:05Z",
    "durationMs": 4823
  },
  {
    "fromPhase": "ANALYSIS",
    "toPhase": "GENERATION",
    "transitionedAt": "2026-05-02T13:00:12Z",
    "durationMs": 7102
  }
  ...
]
```

✅ **PASS:** At least 4 entries, one per transition (INGESTION→ANALYSIS, ANALYSIS→GENERATION,
GENERATION→INDEXING, INDEXING→ACTIVE).

### 2.4 Recovery: restart a failed topic

If step 2.2 ended in `FAILED`:

```bash
curl -s -X POST http://localhost:8080/admin/topics/$TOPIC_ID/regenerate \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  | python3 -m json.tool | grep '"currentPhase"'
```

Expected: `"currentPhase": "INGESTION"`. Then resume polling from step 2.2.

> **Do not continue to Section 3 until `currentPhase` is `ACTIVE`.**

---

## Section 3 — Anti-cheat Shuffle Verification

*This section covers Human Checkpoint 1: two users receive differently-ordered options.*

### 3.1 Enroll User A

```bash
curl -s -X POST http://localhost:8080/topics/$TOPIC_ID/enroll \
  -H "Authorization: Bearer $USER_A_TOKEN" \
  | python3 -m json.tool
```

Expected:
```json
{"id": "enroll-a-id", "topicId": "...", "status": "ACTIVE", "currentDayIndex": 0, ...}
```

### 3.2 Enroll User B

```bash
curl -s -X POST http://localhost:8080/topics/$TOPIC_ID/enroll \
  -H "Authorization: Bearer $USER_B_TOKEN" \
  | python3 -m json.tool
```

### 3.3 Get today's task for User A

```bash
curl -s "http://localhost:8080/tasks/today?topicId=$TOPIC_ID" \
  -H "Authorization: Bearer $USER_A_TOKEN" \
  | python3 -m json.tool
```

Example response:
```json
{
  "assignmentId": "assign-a-001",
  "title": "What is Kubernetes primarily used for?",
  "description": "...",
  "options": ["A database system", "Container orchestration", "A programming language", "A web server"],
  "pointsReward": 10,
  "dueAt": "2026-05-02T23:59:59Z",
  "wrongAttemptCount": 0
}
```

```bash
ASSIGNMENT_A="<paste assignmentId here>"
```

**Write down the full `options` array** — you will compare it with User B's in step 3.4.

### 3.4 Get today's task for User B

```bash
curl -s "http://localhost:8080/tasks/today?topicId=$TOPIC_ID" \
  -H "Authorization: Bearer $USER_B_TOKEN" \
  | python3 -m json.tool
```

```bash
ASSIGNMENT_B="<paste assignmentId here>"
```

**Write down User B's `options` array.**

### 3.5 Compare the option orderings

The two responses contain the **same option strings** but in a **different order**.

Example comparison:

| Index | User A options             | User B options             |
|-------|---------------------------|---------------------------|
| 0     | "A database system"        | "Container orchestration"  |
| 1     | "Container orchestration"  | "A web server"             |
| 2     | "A programming language"   | "A database system"        |
| 3     | "A web server"             | "A programming language"   |

✅ **PASS:** The order differs between User A and User B.
❌ **FAIL:** The order is identical — open a bug against `TaskShuffleSeed.kt`.

> The shuffle is deterministic: the same user always receives the same ordering
> for the same task. The seed is SHA-256(userId + taskId + enrollmentId).

---

## Section 4 — Badge Issuance

*This section covers Human Checkpoint 2: badge appears after completing all tasks.*

Complete all tasks in the topic as **User A**.

### 4.1 Submit User A's first answer

The submit endpoint takes the **index** (0-based) of the option the user selects.
Try each index until `"isCorrect": true`:

```bash
# Try index 0
curl -s -X POST http://localhost:8080/tasks/$ASSIGNMENT_A/submit \
  -H "Authorization: Bearer $USER_A_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"selectedOption": 0}' | python3 -m json.tool
```

If `"isCorrect": false`, try the next index:
```bash
curl -s -X POST http://localhost:8080/tasks/$ASSIGNMENT_A/submit \
  -H "Authorization: Bearer $USER_A_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"selectedOption": 1}' | python3 -m json.tool
```

Repeat with `2`, `3`, etc. until you receive:
```json
{
  "isCorrect": true,
  "pointsAwarded": 10,
  "totalPoints": 10,
  "streakDays": 1,
  "struggleTriggered": false
}
```

### 4.2 Check for a second task

Because the topic was created with `taskCount: 2`, there may be a second task:

```bash
curl -s "http://localhost:8080/tasks/today?topicId=$TOPIC_ID" \
  -H "Authorization: Bearer $USER_A_TOKEN" \
  | python3 -m json.tool
```

- If a second task is returned, record its `assignmentId` and repeat step 4.1:
  ```bash
  ASSIGNMENT_A2="<paste second assignmentId here>"
  ```
- If the server returns an error (no more tasks), proceed to step 4.3 — the
  enrollment is complete.

### 4.3 Verify badge issuance for User A

```bash
curl -s http://localhost:8080/profile/badges \
  -H "Authorization: Bearer $USER_A_TOKEN" \
  | python3 -m json.tool
```

Expected:
```json
[
  {
    "microCompetenceName": "Introduction to Kubernetes",
    "description": "...",
    "topicTitle": "Introduction to Kubernetes",
    "earnedAt": "2026-05-02T13:10:00Z"
  }
]
```

✅ **PASS:** The array contains one entry for this topic with a non-null `earnedAt`.
❌ **FAIL:** Empty array — check that all tasks were submitted with `isCorrect: true`.

### 4.4 Verify admin badge statistics

```bash
curl -s http://localhost:8080/admin/topics/$TOPIC_ID/badges \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  | python3 -m json.tool
```

Expected (User A earned it; User B has not yet):
```json
{
  "totalIssued": 1,
  "enrolledCount": 2,
  "earnedPercentage": 50.0,
  "recentEarners": [
    {"userId": "<user-a-id>", "earnedAt": "2026-05-02T13:10:00Z"}
  ]
}
```

✅ **PASS:** `totalIssued: 1`, `enrolledCount: 2`, `earnedPercentage: 50.0`.

---

## Section 5 — Multi-language Gating (informational)

Task 2.3 implements server-side language gating. The full manual verification requires
updating a user's `preferred_language` directly in the database, because no
`PATCH /profile` endpoint exists yet:

```bash
docker compose exec postgres psql -U play4change -d play4change \
  -c "UPDATE users SET preferred_language = 'pt-PT' WHERE email = 'user-b@example.com';"
```

With `pt-PT` set and assuming `pt-PT` is in the server's `supported-languages` whitelist
(`application.yml`), the next call to `GET /tasks/today?topicId=<new-topic-id>` should
return:

```
HTTP/1.1 202 Accepted
X-Generation-Status: PENDING
X-Generation-Language: pt-PT
```

Once generation completes for the `pt-PT` variant, the same call returns `200 OK`
with content in Portuguese.

> **This check is covered by `LanguageGatingTest` unit tests, which are part of the
> full test suite run in Section 7.** Manual verification requires an additional
> `pt-PT`-supporting topic; skip if time does not permit.

---

## Section 6 — AI Output Sanitisation and SSRF Validation (unit test evidence)

*This section covers Human Checkpoint 4.*

From the project root:

```bash
./gradlew :server:test --tests "*.AiOutputSanitiserTest"
```

Expected:
```
BUILD SUCCESSFUL
```

The following cases must all pass:
- HTML tags are stripped from AI-generated string fields.
- `<script>` tags are stripped (XSS prevention).
- Plain text is preserved unchanged after sanitisation.

```bash
./gradlew :server:test --tests "*.UrlSsrfValidatorTest"
```

Expected:
```
BUILD SUCCESSFUL
```

The following cases must all pass:
- `http://` (non-HTTPS) URL is rejected.
- `http://localhost` is rejected.
- `http://127.0.0.1` is rejected.
- `http://192.168.1.1` (RFC 1918) is rejected.
- `http://10.0.0.1` (RFC 1918) is rejected.
- A valid public HTTPS URL is accepted.

---

## Section 7 — Full Test Suite

```bash
./gradlew :server:test
```

Expected:
```
BUILD SUCCESSFUL
```

All server tests must be green. No failures, no skips.

---

## Phase 02 Exit Checklist

Mark each item confirmed before declaring Phase 02 complete:

- [ ] **Pipeline phases:** `currentPhase` advanced through all 5 phases to `ACTIVE` (Section 2.2).
- [ ] **Generation log:** `generationLog` contains at least 4 transition entries with `durationMs` values (Section 2.3).
- [ ] **Anti-cheat:** User A and User B received differently-ordered `options` for the same task (Section 3.5).
- [ ] **Badge issuance:** User A's badge appears in `GET /profile/badges` after completing all tasks (Section 4.3).
- [ ] **Badge stats:** `GET /admin/topics/{id}/badges` returns `totalIssued: 1`, `enrolledCount: 2`, `earnedPercentage: 50.0` (Section 4.4).
- [ ] **AI sanitisation:** `AiOutputSanitiserTest` all pass (Section 6).
- [ ] **SSRF validation:** `UrlSsrfValidatorTest` all pass (Section 6).
- [ ] **Full test suite:** `./gradlew :server:test` is green (Section 7).
