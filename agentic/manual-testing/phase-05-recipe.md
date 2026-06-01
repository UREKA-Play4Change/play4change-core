# Phase 05 — Manual Test Recipe: Notifications & Smart Fetching

## Prerequisites

| Item | Requirement |
|------|-------------|
| Server | Docker Compose stack running (`docker compose up --build`) |
| Android device | Physical device or emulator with API 26+ |
| Firebase project | `google-services.json` placed in `composeApp/androidApp/` (dev Firebase project) |
| Server env vars | `FIREBASE_SERVICE_ACCOUNT_JSON` set to the service-account JSON for the dev Firebase project |
| APNs (iOS, optional) | `APNS_KEY_ID`, `APNS_TEAM_ID`, `APNS_PRIVATE_KEY` set if testing iOS path |

---

## Section 1 — Push notification received at 8 PM local time

### 1.1 Enrol in a topic and do NOT complete today's task

1. Install the app on the test device.
2. Log in via magic link.
3. On the Explore screen, enrol in at least one topic.
4. Do **not** submit today's task.

### 1.2 Temporarily accelerate the scheduler (optional — for same-day testing)

On the server, change the cron expression to fire in 1 minute:

```bash
# Edit DailyReminderScheduler.kt — change the cron to trigger once per minute:
@Scheduled(cron = "0 * * * * *")
```

Rebuild and restart the server.

### 1.3 Verify notification arrives

**Expected:** Within one minute of the scheduled hour, a push notification appears in the device's notification tray with:
- Title: "Your daily challenge is waiting 🎯"
- Body: "Complete today's task in `<topic name>` to keep your streak."

### 1.4 Tap the notification

**Expected:** The app opens to the task screen for that topic.

---

## Section 2 — Notification is not sent twice (idempotency)

1. After the first notification was received (Section 1), wait for the next full hour.
2. **Expected:** No second notification arrives.
3. Verify via server logs: `Daily reminder sent to user` should appear exactly once per user per day.

To confirm via DB:
```bash
docker compose exec postgres psql -U play4change -d play4change -c \
  "SELECT id, user_id, platform, last_notified_at FROM device_tokens;"
```
`last_notified_at` should be set to today's date.

---

## Section 3 — WorkManager periodic work request is enqueued

### 3.1 Confirm work is scheduled via adb

```bash
adb shell dumpsys jobscheduler | grep "background-task-fetch"
```

**Expected:** The periodic work request appears in the scheduled jobs list.

### 3.2 Confirm work tag via WorkManager API (debug builds)

```kotlin
// In a debug screen or logcat filter
WorkManager.getInstance(context)
    .getWorkInfosByTag("background-task-fetch")
    .get()
    .forEach { println(it.state) }
```

**Expected:** At least one `WorkInfo` with state `ENQUEUED` or `RUNNING`.

---

## Section 4 — WiFi-only constraint respected

1. Connect the test device to **mobile data** (disable WiFi).
2. Force-stop the app and relaunch to re-trigger `WorkManagerSetup.scheduleBackgroundFetch`.
3. In the JobScheduler dump, confirm the work is in `WAITING` state (network constraint not met):

```bash
adb shell dumpsys jobscheduler | grep -A5 "background-task-fetch"
```

**Expected:** The work is not immediately executed — it waits for an unmetered (WiFi) connection.

4. Reconnect to WiFi.
5. **Expected:** WorkManager executes the worker within the next flex window (up to 1 hour).

---

## Section 5 — Cache warm-up: task data loads without a spinner

1. Ensure the device is on WiFi and battery is not low.
2. Wait for `BackgroundFetchWorker` to execute (can accelerate by reducing the flex window in debug).
3. Kill the app via the device's app switcher.
4. Reopen the app.
5. **Expected:** The home screen shows today's task cards without a visible loading spinner (data served from `TaskCache`).

Server logs should show **no** new `GET /tasks/today` calls during the first foreground resume after a successful worker run.

---

## Section 6 — Device token registered on fresh install

1. Fresh install the app (or clear app data).
2. Log in via magic link.
3. Verify the device token in the DB:

```bash
docker compose exec postgres psql -U play4change -d play4change -c \
  "SELECT user_id, platform, LEFT(token,20) AS token_prefix, created_at FROM device_tokens;"
```

**Expected:** A row with `platform = 'ANDROID'` (or `'IOS'`) and a fresh `created_at`.

---

## Section 7 — Device token deleted on logout

1. Log out from the app.
2. Verify the token is removed from the DB:

```bash
docker compose exec postgres psql -U play4change -d play4change -c \
  "SELECT COUNT(*) FROM device_tokens WHERE user_id = '<your-user-id>';"
```

**Expected:** `COUNT = 0`.

---

## Phase 05 Checklist

| Check | Expected | Pass? |
|-------|----------|-------|
| Push notification arrives at 8 PM local time when task is incomplete | ✓ Notification appears in tray | |
| No duplicate notification in the same day | ✓ `last_notified_at` gates re-sends | |
| Tap notification opens task screen | ✓ Deep-link to task | |
| WorkManager work request is enqueued | ✓ `adb dumpsys` shows entry | |
| WiFi-only constraint: work waits on mobile data | ✓ `WAITING` state on cellular | |
| Cache warm-up: no spinner after background fetch | ✓ Data served from cache | |
| Device token registered on first launch | ✓ Row in `device_tokens` | |
| Device token deleted on logout | ✓ Row removed | |
