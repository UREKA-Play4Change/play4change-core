# Phase 4.1 — Full-Stack Integration Test
## Phases 01–04 End-to-End: Two iOS Simulators + Real Server

This recipe validates the complete Phase 04 system with two simultaneous iOS simulators,
a live server, two AI-generated topics, three real accounts, task completion, struggle
detection, and logout. Run it after every significant merge into `feat/phase-04-*` or
`feat/phase-05-*` before opening a PR against `main`.

---

## Devices, Simulators & Accounts

| Role | Device / Simulator | iOS Version | Email |
|------|--------------------|-------------|-------|
| Admin (curl, no simulator) | Mac terminal | — | radesh.govind@gmail.com |
| Simulator A | iPhone 17e | iOS 26.4 | radesh.govind@yahoo.com |
| Simulator B | iPhone 16e | iOS 26.2 | viriato.security@gmail.com |

---

## Shell Variables — declare now, fill in as you go

Open **two terminal windows**: one for admin curl calls, one for log tailing.

```bash
# Terminal 1 — admin shell
ADMIN_TOKEN=""
USER_A_TOKEN=""      # radesh.govind@yahoo.com
USER_B_TOKEN=""      # viriato.security@gmail.com
TOPIC_1_ID=""        # SDG Goal 11 targets & indicators
TOPIC_2_ID=""        # SDG 2025 Report Goal 11
ENROLLMENT_A1=""     # yahoo user, topic 1
ENROLLMENT_A2=""     # yahoo user, topic 2
ENROLLMENT_B1=""     # viriato user, topic 1
ENROLLMENT_B2=""     # viriato user, topic 2
ASSIGNMENT_A=""      # the assignment yahoo user will struggle on
SESSION_A=""         # struggle session id for yahoo user
```

---

## Section 0 — Environment Setup

### 0.1 Enable 3-minute dev-mode task delivery

Edit `server/src/main/resources/application.yml`:

```yaml
task-delivery:
  task-rate-minutes: 3
  dev-mode: true
```

> **Important:** `dev-mode: true` is protected by a `@PostConstruct` guard — the server
> refuses to start if the active Spring profile is `prod` and dev-mode is `true`.
> Local Docker Compose uses the default profile, so this is safe.

### 0.2 Start the server stack and Cloudflare tunnel

```bash
# From the project root — this builds all containers and starts cloudflared
./start-dev.sh
```

Wait until the server is healthy:

```bash
curl -s http://localhost:8080/actuator/health
```

Expected: `{"status":"UP"}`

If the Cloudflare tunnel is not needed (no email delivery):

```bash
docker compose up --build -d
```

### 0.3 Tail server logs in Terminal 2

```bash
# Terminal 2 — keep this running throughout the session
docker compose logs server --follow --no-log-prefix | grep -E "magic|token|ERROR|WARN|struggle|notification"
```

### 0.4 Open both simulators

```bash
# Boot both simulators (they may already be booted from Xcode)
xcrun simctl boot "iPhone 17e"    # Simulator A — iOS 26.4
xcrun simctl boot "iPhone 16e"    # Simulator B — iOS 26.2

# Open the Simulator.app so both appear on screen
open -a Simulator
```

> If the device names above do not match exactly, list available simulators:
> ```bash
> xcrun simctl list devices available | grep -E "iPhone 17e|iPhone 16e"
> ```

### 0.5 Build the KMP framework and install the debug app on both simulators

```bash
# Step 1 — build the iOS debug framework (arm64 simulator)
./gradlew :composeApp:embedAndSignAppleFrameworkForXcode \
  CONFIGURATION=Debug PLATFORM_NAME=iphonesimulator

# Step 2 — build the Xcode project for simulator
xcodebuild -project iosApp/iosApp.xcodeproj \
  -scheme iosApp \
  -configuration Debug \
  -destination "platform=iOS Simulator,name=iPhone 17e" \
  build | tail -5

# Step 3 — install on Simulator A (iPhone 17e)
APP_PATH=$(find build/ios -name "Play4Change.app" -path "*Debug-iphonesimulator*" | head -1)
xcrun simctl install "iPhone 17e" "$APP_PATH"

# Step 4 — install on Simulator B (iPhone 16e — same build, same binary)
xcrun simctl install "iPhone 16e" "$APP_PATH"
```

> Both simulators receive the same debug binary. The debug build shows the
> **"Paste your verification token"** field on the magic link screen — this is
> required for the auth flow below (eliminates dependency on real email delivery).

### 0.6 Launch the app on both simulators

```bash
BUNDLE_ID="com.ureka.play4change"
xcrun simctl launch "iPhone 17e" "$BUNDLE_ID"
xcrun simctl launch "iPhone 16e" "$BUNDLE_ID"
```

Both simulators should show the **Login / magic link request screen**.

---

## Section 1 — Account Creation and Admin Promotion

> Each account is created by requesting a magic link and verifying the token.
> The token appears in the server log (Terminal 2) or in the real inbox if Resend is active.
> Use the **in-app token paste field** to verify without needing inbox access.

### 1.1 Create radesh.govind@gmail.com (future admin)

```bash
curl -s -X POST http://localhost:8080/auth/magic-link \
  -H "Content-Type: application/json" \
  -d '{"email": "radesh.govind@gmail.com"}'
```

Read the token from Terminal 2 (the log line looks like):
```
Magic link token for radesh.govind@gmail.com: <TOKEN>
```

Verify the token (this creates the user record):
```bash
curl -s -X POST http://localhost:8080/auth/magic-link/verify \
  -H "Content-Type: application/json" \
  -d '{"token": "<TOKEN>"}' | python3 -m json.tool
```

You do not need the token from this step — do not save it yet.

### 1.2 Promote radesh.govind@gmail.com to ADMIN

```bash
docker compose exec postgres psql -U play4change -d play4change \
  -c "UPDATE users SET role = 'ADMIN' WHERE email = 'radesh.govind@gmail.com';"
```

Expected: `UPDATE 1`

### 1.3 Obtain admin token

```bash
curl -s -X POST http://localhost:8080/auth/magic-link \
  -H "Content-Type: application/json" \
  -d '{"email": "radesh.govind@gmail.com"}'
```

Read the token from Terminal 2, then:
```bash
curl -s -X POST http://localhost:8080/auth/magic-link/verify \
  -H "Content-Type: application/json" \
  -d '{"token": "<ADMIN-TOKEN>"}' | python3 -m json.tool
```

```bash
ADMIN_TOKEN="<paste accessToken from response>"
```

Confirm admin role:
```bash
curl -s http://localhost:8080/admin/task-reports?status=PENDING \
  -H "Authorization: Bearer $ADMIN_TOKEN" | python3 -m json.tool
```

Expected: `HTTP 200` (any admin endpoint returns 200, not 403).

### 1.4 Create radesh.govind@yahoo.com on Simulator A (iPhone 17e — iOS 26.4)

On **Simulator A**:

1. Tap the email field → type `radesh.govind@yahoo.com` → tap **Send**.
2. The "Check your inbox" screen appears with the **"Paste your verification token"** field below.
3. Read the token from Terminal 2:
   ```
   Magic link token for radesh.govind@yahoo.com: <TOKEN>
   ```
4. Paste `<TOKEN>` into the field → tap **Verify token**.

Expected: app transitions to the **Home screen** and shows live data.

Also obtain the token via curl for server-side steps later:
```bash
curl -s -X POST http://localhost:8080/auth/magic-link \
  -H "Content-Type: application/json" \
  -d '{"email": "radesh.govind@yahoo.com"}'
# (this request will just queue another magic link — ignore it; use the one from the app flow)
```

After the simulator shows the home screen, get the access token for curl:
```bash
curl -s -X POST http://localhost:8080/auth/magic-link \
  -H "Content-Type: application/json" \
  -d '{"email": "radesh.govind@yahoo.com"}'
# Read token from Terminal 2
curl -s -X POST http://localhost:8080/auth/magic-link/verify \
  -H "Content-Type: application/json" \
  -d '{"token": "<TOKEN>"}' | python3 -m json.tool
```

```bash
USER_A_TOKEN="<paste accessToken>"
```

### 1.5 Create viriato.security@gmail.com on Simulator B (iPhone 16e — iOS 26.2)

On **Simulator B** — repeat the same flow as 1.4 with email `viriato.security@gmail.com`.

Then obtain the token for curl:
```bash
curl -s -X POST http://localhost:8080/auth/magic-link \
  -H "Content-Type: application/json" \
  -d '{"email": "viriato.security@gmail.com"}'
# Read token from Terminal 2
curl -s -X POST http://localhost:8080/auth/magic-link/verify \
  -H "Content-Type: application/json" \
  -d '{"token": "<TOKEN>"}' | python3 -m json.tool
```

```bash
USER_B_TOKEN="<paste accessToken>"
```

---

## Section 2 — Topic Creation (Admin, via curl)

The admin creates two topics from UN SDG sources. Mistral needs a few minutes to complete
the 4-phase pipeline: INGESTION → ANALYSIS → GENERATION → INDEXING.

### 2.1 Create Topic 1 — SDG Goal 11 Targets & Indicators

```bash
curl -s -X POST http://localhost:8080/admin/topics \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "SDG Goal 11 — Targets and Indicators",
    "sourceUrl": "https://sdgs.un.org/goals/goal11#targets_and_indicators",
    "language": "en"
  }' | python3 -m json.tool
```

Expected HTTP 201:
```json
{
  "topicId": "...",
  "status": "PENDING"
}
```

```bash
TOPIC_1_ID="<paste topicId>"
```

### 2.2 Create Topic 2 — SDG 2025 Report Goal 11

```bash
curl -s -X POST http://localhost:8080/admin/topics \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "SDG 2025 Progress Report — Goal 11",
    "sourceUrl": "https://unstats.un.org/sdgs/report/2025/Goal-11/",
    "language": "en"
  }' | python3 -m json.tool
```

```bash
TOPIC_2_ID="<paste topicId>"
```

### 2.3 Poll topic pipeline status until both topics are ACTIVE

Run the following every 30 seconds until both return `"status": "ACTIVE"`:

```bash
# Topic 1
curl -s "http://localhost:8080/admin/topics/$TOPIC_1_ID" \
  -H "Authorization: Bearer $ADMIN_TOKEN" | python3 -m json.tool

# Topic 2
curl -s "http://localhost:8080/admin/topics/$TOPIC_2_ID" \
  -H "Authorization: Bearer $ADMIN_TOKEN" | python3 -m json.tool
```

Pipeline states in order: `PENDING` → `INGESTION` → `ANALYSIS` → `GENERATION` → `INDEXING` → `ACTIVE`

> Full pipeline typically takes 2–5 minutes per topic when Mistral is responding normally.
> Watch Terminal 2 for AI generation log lines.

✅ **PASS:** Both topics show `"status": "ACTIVE"` with at least 3 task templates each.
❌ **FAIL:** Status stuck at `INGESTION` for > 5 min — check `MISTRAL_API_KEY` in `.env` and
   circuit-breaker state in the server logs.

---

## Section 3 — Enrollment

Both users enroll in both topics. Use curl for the server side to confirm, and verify that
the topic cards appear on the home screen in each simulator.

### 3.1 Simulator A (radesh.govind@yahoo.com) — enroll in both topics

Via the app: navigate to the **Explore / Topics** screen and tap **Enrol** on each topic.

Confirm via curl:
```bash
# Topic 1
curl -s -X POST "http://localhost:8080/topics/$TOPIC_1_ID/enroll" \
  -H "Authorization: Bearer $USER_A_TOKEN" | python3 -m json.tool

ENROLLMENT_A1="<paste enrollment id>"

# Topic 2
curl -s -X POST "http://localhost:8080/topics/$TOPIC_2_ID/enroll" \
  -H "Authorization: Bearer $USER_A_TOKEN" | python3 -m json.tool

ENROLLMENT_A2="<paste enrollment id>"
```

> If the app already triggered enrollment via the UI, the curl call may return 409 Conflict.
> That is acceptable — the enrollment already exists.

### 3.2 Simulator B (viriato.security@gmail.com) — enroll in both topics

Same flow on **Simulator B** via the app UI, then confirm:

```bash
curl -s -X POST "http://localhost:8080/topics/$TOPIC_1_ID/enroll" \
  -H "Authorization: Bearer $USER_B_TOKEN" | python3 -m json.tool

ENROLLMENT_B1="<paste enrollment id>"

curl -s -X POST "http://localhost:8080/topics/$TOPIC_2_ID/enroll" \
  -H "Authorization: Bearer $USER_B_TOKEN" | python3 -m json.tool

ENROLLMENT_B2="<paste enrollment id>"
```

### 3.3 Verify home screens update

On both simulators, the **Home screen** should now show:
- The enrolled topic cards.
- A **"Start Challenge →"** button for today's available task.
- Streak: 0 days, Points: 0.

✅ **PASS:** Both simulators show topic cards with a task button.
❌ **FAIL:** Home screen still blank or loading — pull-to-refresh or kill/reopen the app.

---

## Section 4 — Task Completion Flow

Complete the first task on each simulator, then wait 3 minutes for the next task to unlock.

### 4.1 Simulator A — complete Task 1 on Topic 1

On **Simulator A**, tap **Start Challenge** for Topic 1.

The task screen shows a question with multiple-choice options. Select an answer and tap **Submit**.

Check the result via curl (to see server-side state):
```bash
curl -s "http://localhost:8080/tasks/today?topicId=$TOPIC_1_ID" \
  -H "Authorization: Bearer $USER_A_TOKEN" | python3 -m json.tool
```

Note the `assignmentId` — you will need it in Section 5 to trigger struggle:
```bash
ASSIGNMENT_A="<paste assignmentId — the one you will deliberately fail>"
```

After submitting a **correct** answer, the server returns:
```json
{
  "isCorrect": true,
  "pointsAwarded": 20,
  "totalPoints": 20,
  "streakDays": 1,
  "struggleTriggered": false
}
```

On the app: the task card should change to a completed state and streak/points update.

✅ **PASS:** Home screen reflects `streakDays: 1` and `totalPoints: 20` after correct answer.
❌ **FAIL:** Home screen still shows "Start Challenge" — check bug B5 fix (`TaskService` PENDING guard).

### 4.2 Simulator B — complete Task 1 on Topic 1

Same steps on **Simulator B** for `viriato.security@gmail.com`.

Confirm the option ordering is **different** from Simulator A's — this is the anti-cheat shuffle:
```bash
# Compare the options in each response
curl -s "http://localhost:8080/tasks/today?topicId=$TOPIC_1_ID" \
  -H "Authorization: Bearer $USER_A_TOKEN" | python3 -m json.tool

curl -s "http://localhost:8080/tasks/today?topicId=$TOPIC_1_ID" \
  -H "Authorization: Bearer $USER_B_TOKEN" | python3 -m json.tool
```

✅ **PASS:** The `options` arrays are in a different order for the two users.
❌ **FAIL:** Identical ordering — anti-cheat shuffle seed (Phase 02 Task 2.2) is broken.

### 4.3 Verify "next task not yet available" response

Immediately after completing Task 1, try to load the next task on Simulator A:

```bash
curl -si "http://localhost:8080/tasks/today?topicId=$TOPIC_1_ID" \
  -H "Authorization: Bearer $USER_A_TOKEN"
```

Expected: `HTTP 404` with header `X-Task-Available-At: <timestamp ~3 min from now>`.

On the app: the task card should show "Next task available in ~3 min" or similar, not "Start Challenge".

### 4.4 Wait 3 minutes, then complete Task 2 on both simulators

```bash
sleep 180
```

On each simulator, pull-to-refresh the Home screen. The next task should now appear.
Complete it on each simulator.

Repeat for as many tasks as are available (minimum 2 rounds to exercise the progression).

---

## Section 5 — Struggle Detection

Deliberately trigger struggle detection on Simulator A for the **next available task on Topic 1**.
Get the current task first:

```bash
curl -s "http://localhost:8080/tasks/today?topicId=$TOPIC_1_ID" \
  -H "Authorization: Bearer $USER_A_TOKEN" | python3 -m json.tool
```

```bash
ASSIGNMENT_A="<paste assignmentId>"
```

### 5.1 Submit a wrong answer (first attempt)

On **Simulator A**, tap **Start Challenge** and deliberately select the wrong option.

Confirm via curl:
```bash
curl -s -X POST "http://localhost:8080/tasks/$ASSIGNMENT_A/submit" \
  -H "Authorization: Bearer $USER_A_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"selectedOption": 0}' | python3 -m json.tool
```

Expected: `"isCorrect": false`, `"struggleTriggered": false`.

On the app: the answer is marked wrong; the question should still be available for a retry.

✅ **PASS:** Wrong answer accepted, no struggle session yet.

### 5.2 Submit a wrong answer again (second attempt — triggers struggle)

On **Simulator A**, submit the same wrong option again:

```bash
curl -s -X POST "http://localhost:8080/tasks/$ASSIGNMENT_A/submit" \
  -H "Authorization: Bearer $USER_A_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"selectedOption": 0}' | python3 -m json.tool
```

Expected: `"isCorrect": false`, `"struggleTriggered": true`.

On the app: the UI should transition to a **Struggle mode** screen showing that adaptive
tasks are being prepared. Watch Terminal 2 for Mistral generation logs.

✅ **PASS:** `struggleTriggered: true` on the second consecutive wrong answer.
❌ **FAIL:** `struggleTriggered: false` — `HandleStruggleService` or threshold logic is broken.

### 5.3 Wait for adaptive tasks (Mistral generation, ~30–90 seconds)

```bash
# Poll every 10 seconds
curl -s "http://localhost:8080/struggle/enrollment/$ENROLLMENT_A1" \
  -H "Authorization: Bearer $USER_A_TOKEN" | python3 -m json.tool
```

Expected once ready:
```json
{
  "sessionId": "...",
  "errorPattern": "WRONG_CONCEPT",
  "status": "OPEN",
  "adaptiveTasks": [
    { "taskId": "...", "title": "...", "options": [...], "pointsReward": 10 },
    { "taskId": "...", "title": "...", "options": [...], "pointsReward": 10 },
    { "taskId": "...", "title": "...", "options": [...], "pointsReward": 10 }
  ]
}
```

```bash
SESSION_A="<paste sessionId>"
ADAPTIVE_1="<paste adaptiveTasks[0].taskId>"
ADAPTIVE_2="<paste adaptiveTasks[1].taskId>"
ADAPTIVE_3="<paste adaptiveTasks[2].taskId>"
```

On the app: the Struggle screen should display 3 adaptive tasks once ready.

✅ **PASS:** 3 adaptive tasks present, `status: OPEN`.
❌ **FAIL:** 404 after > 2 minutes — check Mistral circuit-breaker and `AsyncConfig` bean.

### 5.4 Complete all 3 adaptive tasks

On **Simulator A**, complete each adaptive task via the app UI. For each, try option 0, then 1,
etc. until you find the correct answer. Or use curl to submit and observe `isCorrect`:

```bash
curl -s -X POST "http://localhost:8080/struggle/$SESSION_A/tasks/$ADAPTIVE_1/submit" \
  -H "Authorization: Bearer $USER_A_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"selectedOption": 0}' | python3 -m json.tool

curl -s -X POST "http://localhost:8080/struggle/$SESSION_A/tasks/$ADAPTIVE_2/submit" \
  -H "Authorization: Bearer $USER_A_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"selectedOption": 0}' | python3 -m json.tool

curl -s -X POST "http://localhost:8080/struggle/$SESSION_A/tasks/$ADAPTIVE_3/submit" \
  -H "Authorization: Bearer $USER_A_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"selectedOption": 0}' | python3 -m json.tool
```

### 5.5 Verify session is resolved

```bash
curl -s "http://localhost:8080/struggle/enrollment/$ENROLLMENT_A1" \
  -H "Authorization: Bearer $USER_A_TOKEN" | python3 -m json.tool
```

Expected: `"status": "RESOLVED"`.

✅ **PASS:** Session resolved.

### 5.6 Verify original task is retryable

```bash
curl -s "http://localhost:8080/tasks/today?topicId=$TOPIC_1_ID" \
  -H "Authorization: Bearer $USER_A_TOKEN" | python3 -m json.tool
```

Expected: HTTP 200 with the **same task**, `"wrongAttemptCount": 0`.

On the app: the task card reappears in the home screen, ready for a fresh attempt.

✅ **PASS:** Original task reset to PENDING with `wrongAttemptCount: 0`.
❌ **FAIL:** 404 or a different task — check `AdaptiveTaskService` assignment reset logic (Issue I02).

---

## Section 6 — UX Inconsistency Observation Log

Observe and record any anomalies in the table below. Check both simulators for each item.
Items marked **SERVER** can be verified via curl or DB query. Items marked **CLIENT** are
observed on-screen.

| # | Where | What to check | Simulator A | Simulator B | Notes |
|---|-------|---------------|-------------|-------------|-------|
| U01 | CLIENT | Home screen greeting shows first name only, not full email | ☐ | ☐ | Bug B3: email used if no display name — extract local part before `@` |
| U02 | CLIENT | Home screen updates streak + points immediately after task submit (no stale state) | ☐ | ☐ | Pull-to-refresh should not be required |
| U03 | CLIENT | "Start Challenge →" disappears after completing today's task | ☐ | ☐ | Bug B5: submitting twice no longer possible |
| U04 | CLIENT | "Next task in Xm" or similar message shown after first submission, not blank | ☐ | ☐ | |
| U05 | CLIENT | Struggle screen appears within 10 seconds of second wrong answer (not a crash or blank) | ☐ | ☐ | |
| U06 | CLIENT | Struggle adaptive tasks are legible — no backslash apostrophes (Bug B4) | ☐ | ☐ | `Today's` not `Today\'s` |
| U07 | CLIENT | After struggle resolved, task card reappears on Home — no manual refresh needed | ☐ | ☐ | |
| U08 | CLIENT | Roadmap screen: completed days show a different visual state from pending days | ☐ | ☐ | Bug B-ROADMAP: SUBMITTED → COMPLETED status |
| U09 | CLIENT | No double-login prompt after app kill/reopen while tokens are valid | ☐ | ☐ | Bug B9 / Keychain Data Protection |
| U10 | CLIENT | iOS 26.4 (Sim A) and iOS 26.2 (Sim B) both retain session after app kill | ☐ | ☐ | Bug B7: Keychain kSecUseDataProtectionKeychain fix |
| U11 | CLIENT | No duplicate network calls trigger two simultaneous refreshes (no sudden logout during load) | ☐ | ☐ | Bug B11: Mutex in refreshTokens |
| U12 | CLIENT | Profile screen shows correct name, email, streak, points for each user | ☐ | ☐ | |
| U13 | CLIENT | Explore screen lists both created topics with ACTIVE status indicator | ☐ | ☐ | |
| U14 | CLIENT | Network error shown as a user-readable message (not a raw exception or blank screen) | ☐ | ☐ | Toggle airplane mode to test |
| U15 | SERVER | Both users get different option orderings for the same question | ☐ | — | Anti-cheat shuffle — verify with curl diff |
| U16 | SERVER | Roadmap endpoint returns COMPLETED for submitted day (not PENDING) | ☐ | ☐ | Bug B-ROADMAP fix |
| U17 | SERVER | `/tasks/today` returns 404 with `X-Task-Available-At` header when task is done | ☐ | ☐ | Bug B5 fix |
| U18 | SERVER | Token refresh does not cause logout when multiple requests fire simultaneously | ☐ | ☐ | Bug B11 fix |
| U19 | SERVER | `device_tokens` table is empty before Phase 05 — confirm no leftover schema issues | ☐ | — | Run: `SELECT * FROM device_tokens;` |

#### DB spot-checks

```bash
# Verify device_tokens table exists (added by Flyway V15 in Phase 05 — should NOT exist yet)
docker compose exec postgres psql -U play4change -d play4change \
  -c "\d device_tokens" 2>&1

# Check enrollments for both users
docker compose exec postgres psql -U play4change -d play4change \
  -c "SELECT u.email, e.id, e.status, e.enrolled_at FROM enrollments e JOIN users u ON e.user_id = u.id;"

# Check today's task assignments
docker compose exec postgres psql -U play4change -d play4change \
  -c "SELECT u.email, ta.status, ta.is_correct, ta.wrong_attempt_count, ta.points_awarded FROM task_assignments ta JOIN enrollments e ON ta.enrollment_id = e.id JOIN users u ON e.user_id = u.id ORDER BY ta.created_at DESC LIMIT 10;"

# Check struggle sessions
docker compose exec postgres psql -U play4change -d play4change \
  -c "SELECT ss.id, u.email, ss.status, ss.error_pattern, ss.resolved_at FROM struggle_sessions ss JOIN enrollments e ON ss.enrollment_id = e.id JOIN users u ON e.user_id = u.id;"
```

---

## Section 7 — Logout

### 7.1 Simulator A — logout radesh.govind@yahoo.com

On **Simulator A**: navigate to the **Profile screen** → tap **Log out**.

Expected:
- App navigates to the **Login screen**.
- Home screen data is no longer visible without re-authenticating.
- On iOS 26.4: the Keychain entry for `play4change-access-token` is deleted.

Confirm server-side token revocation:
```bash
# The access token should now be rejected
curl -si http://localhost:8080/profile \
  -H "Authorization: Bearer $USER_A_TOKEN"
```

Expected: `HTTP 401` (token has been revoked via family invalidation on logout).

✅ **PASS:** 401 after logout. App shows login screen.
❌ **FAIL:** 200 — `TokenService.revokeFamily()` or `AuthService.logout()` is broken.

### 7.2 Simulator B — logout viriato.security@gmail.com

Same flow on **Simulator B**. Confirm via curl:

```bash
curl -si http://localhost:8080/profile \
  -H "Authorization: Bearer $USER_B_TOKEN"
```

Expected: `HTTP 401`.

On iOS 26.2: Keychain entries cleared. App shows login screen.

### 7.3 Revoke admin token

```bash
curl -s -X POST http://localhost:8080/auth/logout \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

Expected: `HTTP 204 No Content`.

Confirm:
```bash
curl -si "http://localhost:8080/admin/task-reports?status=PENDING" \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

Expected: `HTTP 401`.

---

## Section 8 — Restore Production Settings

After the session, revert dev-mode in `application.yml`:

```yaml
task-delivery:
  task-rate-minutes: 1440
  dev-mode: false
```

Rebuild the server container:

```bash
docker compose up --build -d server
```

Confirm dev-mode is off:

```bash
curl -s http://localhost:8080/actuator/health
# No error about dev-mode in prod — if dev-mode was accidentally left on,
# the prod profile guard would print a fatal error in the server log.
```

---

## Final Pass/Fail Checklist

| # | Check | Status |
|---|-------|--------|
| C01 | Simulator A (iOS 26.4) completes magic link auth without raw JSON in browser | ☐ |
| C02 | Simulator B (iOS 26.2) completes magic link auth without raw JSON in browser | ☐ |
| C03 | Both simulators retain session after full kill/reopen | ☐ |
| C04 | Admin can create both UN SDG topics via curl | ☐ |
| C05 | Both topics reach `ACTIVE` status (4-phase pipeline completes) | ☐ |
| C06 | Both users can enroll in both topics via the app | ☐ |
| C07 | Task options are in different order for User A vs User B (anti-cheat shuffle) | ☐ |
| C08 | Completing today's task hides the "Start Challenge" button | ☐ |
| C09 | `GET /tasks/today` returns 404 + `X-Task-Available-At` immediately after submission | ☐ |
| C10 | 3-minute wait unlocks the next task | ☐ |
| C11 | Second consecutive wrong answer returns `struggleTriggered: true` | ☐ |
| C12 | Struggle screen appears in the app after second wrong answer | ☐ |
| C13 | 3 adaptive tasks appear within 90 seconds (Mistral generation completes) | ☐ |
| C14 | Completing all 3 adaptive tasks resolves the session (`status: RESOLVED`) | ☐ |
| C15 | Original task resets to PENDING with `wrongAttemptCount: 0` and reappears in app | ☐ |
| C16 | All UX observation items in Section 6 passed or have a logged note | ☐ |
| C17 | After logout, User A token returns 401 | ☐ |
| C18 | After logout, User B token returns 401 | ☐ |
| C19 | After logout, admin token returns 401 | ☐ |
| C20 | Login screen shown on both simulators after logout (no cached data visible) | ☐ |
| C21 | `dev-mode: false` restored and server restarted cleanly | ☐ |

**All 21 checks must pass before marking Phase 04 fully validated.**

---

## Known Issues to Watch During This Session

| Issue | Description | Expected behaviour |
|-------|-------------|-------------------|
| B11 | Concurrent 401s on app load trigger multiple refresh calls | No unexpected logout when home screen loads (Mutex fix) |
| B9 | Ktor `sendWithoutRequest` does not proactively add token | First request gets 401; refresh+retry is transparent |
| B7 | iOS 26.2 keychain requires `kSecUseDataProtectionKeychain` | Tokens persist across kill/reopen on Sim B |
| H04 | dep-check Gradle plugin broken — use CI CLI action | Not relevant to this manual test |
