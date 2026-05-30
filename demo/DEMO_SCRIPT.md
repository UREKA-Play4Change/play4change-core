# Play4Change — Live Demo Script

**Audience:** Anyone unfamiliar with the system.
**Time:** ~10–15 minutes on a clean database.
**Prerequisites:** Stack running (`docker compose up --build` completed). See `demo/HOW_TO_RUN.md`.

Variables you will collect as you go are shown in `UPPER_CASE`. Replace them in every subsequent command.

---

## Before You Start

Reset to a clean state and confirm the stack is healthy:

```bash
docker compose down -v && docker compose up -d
```

```bash
curl -s http://localhost:8080/actuator/health
# Expected: {"status":"UP"}
```

*(~30 seconds)*

---

## Step 1 — Identity: Request a magic link and sign in

**What this shows:** Passwordless auth, JWT pair, token rotation.

### 1a. Request a magic link

```bash
curl -s -X POST http://localhost:8080/auth/magic-link \
  -H "Content-Type: application/json" \
  -d '{"email":"demo@example.com"}'
```

Expected:
```json
{"message":"Magic link sent. Check your email."}
```

### 1b. Grab the token from the server log

```bash
docker logs play4change-server 2>&1 | grep -A3 "MAGIC LINK"
```

Expected (copy the 64-char hex from the `Link:` line):
```
INFO  ConsoleEmailAdapter - === MAGIC LINK (dev mode — no real email sent) ===
INFO  ConsoleEmailAdapter - To: demo@example.com
INFO  ConsoleEmailAdapter - Link: http://localhost:8080/auth/verify?token=<TOKEN>
```

### 1c. Verify and get JWT pair

```bash
MAGIC_TOKEN=<paste TOKEN here>

curl -s "http://localhost:8080/auth/verify?token=$MAGIC_TOKEN"
```

Expected:
```json
{"accessToken":"<ACCESS_TOKEN>","refreshToken":"<REFRESH_TOKEN>","expiresIn":900}
```

Save the access token:
```bash
USER_TOKEN=<paste accessToken here>
```

*(~60 seconds)*

---

## Step 2 — Promote to admin and create a topic from a URL

**What this shows:** Admin role management, URL ingestion, 4-phase AI generation pipeline.

### 2a. Promote the demo user to ADMIN

```bash
docker exec -it play4change-postgres psql -U play4change -d play4change -c \
  "UPDATE users SET role = 'ADMIN' WHERE email = 'demo@example.com';"
```

### 2b. Sign in again (new JWT with ADMIN role)

Repeat steps 1a–1c. Save the new token:
```bash
ADMIN_TOKEN=<new accessToken with role=ADMIN>
```

### 2c. Create a topic from a URL

```bash
curl -s -X POST http://localhost:8080/admin/topics \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -d '{
    "title": "Introduction to Sustainability",
    "sourceUrl": "https://en.wikipedia.org/wiki/Sustainability",
    "audienceLevel": "BEGINNER",
    "language": "en",
    "taskCount": 3
  }'
```

Expected — note the `id` field:
```json
{"id":"<TOPIC_ID>","title":"Introduction to Sustainability","status":"INGESTION",...}
```

```bash
TOPIC_ID=<paste id here>
```

### 2d. Poll the topic status until ACTIVE

```bash
watch -n 3 "curl -s http://localhost:8080/admin/topics/$TOPIC_ID \
  -H 'Authorization: Bearer $ADMIN_TOKEN' | python3 -m json.tool"
```

Expected progression (each phase takes ~5–30 s with a real Mistral key):
```
status: INGESTION → ANALYSIS → GENERATION → INDEXING → ACTIVE
```

Without `MISTRAL_API_KEY` the pipeline completes instantly with stub tasks.

*(~2 minutes with real AI; ~15 seconds without)*

---

## Step 3 — Enrollment: Enrol a user and get today's task

**What this shows:** Enrollment gating, per-user anti-cheat shuffle.

### 3a. Sign in as a learner (use a different email)

Repeat step 1 with `learner@example.com`. Save the token:
```bash
LEARNER_TOKEN=<learner accessToken>
```

### 3b. Enrol in the topic

```bash
curl -s -X POST http://localhost:8080/topics/$TOPIC_ID/enroll \
  -H "Authorization: Bearer $LEARNER_TOKEN"
```

Expected:
```json
{"enrollmentId":"<ENROLLMENT_ID>","topicId":"<TOPIC_ID>","status":"ACTIVE"}
```

```bash
ENROLLMENT_ID=<paste enrollmentId here>
```

### 3c. Get today's task

```bash
curl -s "http://localhost:8080/topics/$TOPIC_ID/tasks/today" \
  -H "Authorization: Bearer $LEARNER_TOKEN"
```

Expected — note that `optionOrder` is shuffled per user:
```json
{
  "assignmentId": "<ASSIGNMENT_ID>",
  "title": "...",
  "options": ["...", "...", "...", "..."],
  "optionOrder": [2, 0, 3, 1]
}
```

```bash
ASSIGNMENT_ID=<paste assignmentId here>
```

*(~30 seconds)*

---

## Step 4 — Enrollment: Submit a wrong answer (trigger struggle detection)

**What this shows:** Wrong-attempt counting, struggle trigger on 2nd wrong answer.

### 4a. Submit a wrong answer (first attempt)

```bash
curl -s -X POST http://localhost:8080/tasks/$ASSIGNMENT_ID/submit \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $LEARNER_TOKEN" \
  -d '{"selectedOption": 3}'
```

Expected (first wrong attempt — assignment stays PENDING):
```json
{"isCorrect": false, "struggleTriggered": false, "pointsAwarded": 0}
```

### 4b. Submit another wrong answer (second attempt — triggers struggle)

```bash
curl -s -X POST http://localhost:8080/tasks/$ASSIGNMENT_ID/submit \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $LEARNER_TOKEN" \
  -d '{"selectedOption": 2}'
```

Expected:
```json
{"isCorrect": false, "struggleTriggered": true, "pointsAwarded": 0}
```

*(~30 seconds)*

---

## Step 5 — Struggle path: Adaptive tasks and resolution

**What this shows:** Struggle detection, adaptive branch generation (AI or stub), resolution flow.

### 5a. Check for the struggle session

```bash
curl -s http://localhost:8080/struggle/enrollment/$ENROLLMENT_ID \
  -H "Authorization: Bearer $LEARNER_TOKEN"
```

Expected (may take up to 60 s for AI to generate adaptive tasks):
```json
{
  "struggleSessionId": "<SESSION_ID>",
  "status": "OPEN",
  "adaptiveTasks": [...]
}
```

```bash
SESSION_ID=<paste struggleSessionId here>
```

### 5b. Complete all adaptive tasks

For each `id` in `adaptiveTasks`:
```bash
ADAPTIVE_TASK_ID=<id from adaptiveTasks[0]>

curl -s -X POST http://localhost:8080/struggle/sessions/$SESSION_ID/tasks/$ADAPTIVE_TASK_ID/complete \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $LEARNER_TOKEN" \
  -d '{"selectedOption": 0}'
```

### 5c. Confirm struggle session is RESOLVED

```bash
curl -s http://localhost:8080/struggle/enrollment/$ENROLLMENT_ID \
  -H "Authorization: Bearer $LEARNER_TOKEN" | python3 -m json.tool
```

Expected:
```json
{"status": "RESOLVED", ...}
```

*(~90 seconds)*

---

## Step 6 — PeerReview: Photo task, three verdicts, majority decision

**What this shows:** Todo-action task type, peer review assignment, majority vote finalisation.

### 6a. Admin creates a photo/todo topic (or reuse the existing one)

The existing topic already has tasks. For peer review demonstration, use the
`POST /admin/topics/{id}/tasks` override endpoint to add a TODO_ACTION task if needed,
or skip to step 6b if a photo task was generated by the AI.

### 6b. Learner submits a photo answer

```bash
# First, get today's task (assuming the topic has a TODO_ACTION task)
curl -s "http://localhost:8080/topics/$TOPIC_ID/tasks/today" \
  -H "Authorization: Bearer $LEARNER_TOKEN"

TODO_ASSIGNMENT_ID=<assignmentId of the TODO_ACTION task>

# Upload a photo (use any small JPEG)
curl -s -X POST http://localhost:8080/tasks/$TODO_ASSIGNMENT_ID/photo \
  -H "Authorization: Bearer $LEARNER_TOKEN" \
  -F "photo=@/path/to/photo.jpg"
```

Expected:
```json
{"status": "PENDING_REVIEW", "photoUrl": "http://localhost:9000/play4change/..."}
```

### 6c. Three reviewers submit verdicts (use admin token for brevity)

```bash
# Reviewer 1 — get assigned a review
curl -s "http://localhost:8080/topics/$TOPIC_ID/reviews/next" \
  -H "Authorization: Bearer $ADMIN_TOKEN"

REVIEW_ID=<paste id from response>

curl -s -X POST http://localhost:8080/reviews/$REVIEW_ID/verdict \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -d '{"verdict": "CORRECT"}'
```

Repeat for two more reviewers. After the 3rd verdict the response includes:
```json
{"finalized": true, "submitterPointsAwarded": 20}
```

*(~2 minutes)*

---

## Step 7 — Badge: Complete all tasks and earn a badge

**What this shows:** Badge issuance after topic completion.

### 7a. Submit correct answers for all remaining tasks

Repeat steps 3c and 4a (with the correct `selectedOption`) for each remaining assignment until
the enrollment status transitions to `COMPLETED`.

### 7b. Check the badge in the user profile

```bash
curl -s http://localhost:8080/profile/badges \
  -H "Authorization: Bearer $LEARNER_TOKEN"
```

Expected:
```json
[{"badgeId": "...", "topicId": "<TOPIC_ID>", "name": "Introduction to Sustainability", "earnedAt": "..."}]
```

*(~60 seconds)*

---

## Step 8 — Learning Path: Prerequisites

**What this shows:** DAG prerequisite gating on enrollment.

### 8a. Admin creates a second topic and sets the first as a prerequisite

```bash
curl -s -X POST http://localhost:8080/admin/topics \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -d '{"title": "Advanced Sustainability","sourceUrl":"https://en.wikipedia.org/wiki/Sustainable_development","audienceLevel":"INTERMEDIATE","language":"en","taskCount":1}'

TOPIC_B_ID=<id from response>

curl -s -X POST http://localhost:8080/admin/topics/$TOPIC_B_ID/prerequisites \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -d '{"prerequisiteTopicId": "<TOPIC_ID>"}'
```

### 8b. A *new* learner tries to enrol in Topic B (blocked)

```bash
# Sign in a brand-new user (no completions yet) — repeat step 1 with new-learner@example.com
NEW_LEARNER_TOKEN=<new learner accessToken>

curl -s -X POST http://localhost:8080/topics/$TOPIC_B_ID/enroll \
  -H "Authorization: Bearer $NEW_LEARNER_TOKEN"
```

Expected:
```json
{"error": "PREREQUISITE_NOT_MET", "message": "..."}
```

*(~30 seconds)*

---

## Step 9 — Observability: Grafana dashboards

**What this shows:** AI latency and learner flow metrics are captured in real-time.

Open **http://localhost:3000** (credentials: `admin` / `admin`).

Navigate to **Dashboards**. You should see:

### "AI Generation Latency"
- P50, P95, P99 stat panels — values populated after topic creation in step 2.
- Time-series panel showing all three percentiles over the last 24 hours.

### "Learner Flow Metrics"
- **Task Submissions** panel — shows correct vs incorrect submissions (steps 4–7).
- **Struggle Sessions Created** panel — shows the struggle trigger from step 4.
- **Peer Review Verdicts** panel — shows the three verdicts from step 6.
- **Struggle Rate** stat — percentage of submissions that triggered a struggle.

If no data is visible, wait 30 seconds and click the refresh button (auto-refresh is on).

*(~30 seconds)*

---

## Appendix — Variable summary

| Variable | Where collected |
|----------|-----------------|
| `USER_TOKEN` | Step 1c |
| `ADMIN_TOKEN` | Step 2b |
| `TOPIC_ID` | Step 2c |
| `LEARNER_TOKEN` | Step 3a |
| `ENROLLMENT_ID` | Step 3b |
| `ASSIGNMENT_ID` | Step 3c |
| `SESSION_ID` | Step 5a |
| `TOPIC_B_ID` | Step 8a |
| `NEW_LEARNER_TOKEN` | Step 8b |
