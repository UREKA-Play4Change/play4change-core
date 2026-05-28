# Phase 05 — Mobile Client: Notifications & Smart Fetching

**Status:** `IN PROGRESS`
**Goal:** Push notifications for daily task reminders at 8 PM user local time if task not done,
WorkManager background fetch with battery/connectivity constraints, cache warm-up on
charging + good WiFi.

**Entry criteria:** Phase 04 DONE.

---

## Tasks

### Task 5.1 — Push notification setup (FCM + APNs + device token registration)
- [x] **What:** Set up Firebase Cloud Messaging (FCM) for Android and Apple Push Notification
      service (APNs) for iOS. Add a server endpoint to register device tokens.
- **Design constraints:**
  - **Android (FCM):**
    - Add `google-services.json` to `composeApp/androidApp/` (obtained from Firebase console —
      do not commit production credentials; use a dev project credential in the repo).
    - Add the `com.google.firebase:firebase-messaging` dependency.
    - Implement `FirebaseMessagingService` subclass that handles token updates and
      foreground message display.
    - On token update: call `POST /notifications/device-token` with the new FCM token.
  - **iOS (APNs):**
    - Enable Push Notifications capability in Xcode project settings.
    - Register for remote notifications in `AppDelegate.application(_:didFinishLaunchingWithOptions:)`.
    - On token receipt: call `POST /notifications/device-token` with the APNs token.
  - **Server endpoint:** `POST /notifications/device-token`
    - Requires authentication (any role).
    - Body: `{ token: String, platform: "ANDROID" | "IOS" }`.
    - Stores the token associated with the authenticated user.
    - If the user already has a token for the platform, replace it (upsert on `userId + platform`).
    - Flyway migration V15 adds: `device_tokens` table.
  - **Security:** Device tokens are not secrets in the cryptographic sense but are sensitive
    operational data. Store them in the database with `userId` association so they are
    revoked when the user logs out (delete all device tokens for the user on logout).
- **Tests required:**
  - `DeviceTokenServiceTest`:
    - Registering a token for a new user creates a record.
    - Registering a second token for the same user+platform replaces the first.
    - Logout deletes all device tokens for the user.
  - `DeviceTokenControllerTest` (`@WebMvcTest`):
    - `POST /notifications/device-token` with valid JWT returns 204.
    - `POST /notifications/device-token` without JWT returns 401.
    - Invalid platform value returns 400.
- **Security log requirement:** Add to THREAT-LOG.md: "Device tokens associated with userId;
      all tokens revoked on logout — prevents notifications to ex-sessions."
- **ADR trigger:** No.
- **Exit criteria:** All server tests pass. Flyway V15 runs cleanly. A fresh Android install
      registers a device token visible in the `device_tokens` table.

---

### Task 5.2 — Daily reminder scheduler: server-side job at 8 PM user timezone
- [ ] **What:** Add a server-side scheduled job that runs every hour, checks which users
      have not completed today's task, and sends a push notification to their registered
      device if the current time is 8 PM in the user's local timezone.
- **Design constraints:**
  - The scheduler runs every hour on the hour (`@Scheduled(cron = "0 0 * * * *")`).
  - For each enrolled user with a registered device token:
    1. Load the user's timezone preference (from Task 3.4). Default: `UTC`.
    2. Compute "now" in the user's timezone.
    3. If the hour is 20 (8 PM) ± 0 (exact hour only — do not send twice):
    4. Check if the user has submitted today's task in their timezone.
    5. If not submitted: send push notification via FCM (Android) or APNs (iOS).
  - Notification title: "Your daily challenge is waiting 🎯"
  - Notification body: "Complete today's task in {topicName} to keep your streak."
  - Push delivery is fire-and-forget: log failures but do not retry in this phase.
  - The scheduler must not send a notification if the user has already received one
    today (track last-sent date in `device_tokens.last_notified_at`).
  - FCM HTTP v1 API is used for Android push delivery (not the legacy HTTP API).
  - APNs HTTP/2 provider API is used for iOS push delivery.
  - **Service accounts and certificates:** these are environment variables, not committed files.
    Document required env vars in THREAT-LOG.md and in `demo/HOW_TO_RUN.md`.
- **Tests required:**
  - `DailyReminderSchedulerTest`:
    - User who has not completed today's task and whose local time is 20:00 receives a notification.
    - User who has already completed today's task does not receive a notification.
    - User with no device token is skipped without error.
    - User whose local time is 19:00 or 21:00 does not receive a notification.
    - User who already received a notification today (same `last_notified_at` date) is not notified again.
- **Security log requirement:** None.
- **ADR trigger:** No.
- **Exit criteria:** `DailyReminderSchedulerTest` passes. In a manual test at 8 PM local time
      with today's task incomplete, a push notification arrives on the test device.

---

### Task 5.3 — WorkManager background fetch: constraints
- [ ] **What:** Add a WorkManager periodic work request that fetches the next day's task
      data in the background, subject to battery and connectivity constraints.
- **Design constraints:**
  - **Work request configuration:**
    - Periodic interval: 24 hours.
    - Flex interval: 1 hour (WorkManager may run it up to 1 hour early).
    - Constraints:
      - `NetworkType.UNMETERED` (WiFi only) — OR `NetworkType.CONNECTED` if the user is
        on a known-good connection (implement a simple heuristic: if battery > 50% and
        not on cellular, treat as UNMETERED).
      - `requiresBatteryNotLow()` — does not run when battery is critically low.
    - The work is non-expedited and does not require charging.
  - **What the worker fetches:**
    - `GET /tasks/today?topicId={id}` for each enrolled topic.
    - The result is saved to an in-memory or room-based cache.
    - The worker does not display any UI.
  - **Failure handling:** If the network request fails, the worker returns `Result.retry()`.
    WorkManager handles backoff automatically with exponential delay.
  - **Work tag:** `"background-task-fetch"` — used to cancel and re-enqueue when the user
    changes enrolled topics.
  - **DI:** The `HttpClient` from Task 4.1 is injected into the worker via the KMP-compatible
    DI approach already in use.
- **Tests required:**
  - `BackgroundFetchWorkerTest` (using `TestWorkerBuilder` from WorkManager test library):
    - Worker with network available fetches tasks and returns `Result.success()`.
    - Worker with no network returns `Result.retry()`.
    - Worker is constrained: does not enqueue when battery is low (test via `TestListenableWorkerBuilder`
      with low battery constraint set).
- **Security log requirement:** None.
- **ADR trigger:** No — document constraint rationale in DECISIONS.md.
- **Exit criteria:** `BackgroundFetchWorkerTest` passes. WorkManager observer in the Android
      system settings shows the periodic work request is enqueued.

---

### Task 5.4 — Cache warm-up strategy on WorkManager execution
- [ ] **What:** When the background worker runs, pre-fetch and cache not just today's task
      but also the next 3 days' worth of roadmap data so the app feels fast when the user
      opens it.
- **Design constraints:**
  - During the WorkManager execution (Task 5.3), after fetching today's task, also fetch:
    - `GET /topics/{topicId}/roadmap` — the full roadmap (day-by-day status).
    - Roadmap data is cached with a TTL of 6 hours.
  - The cache layer is a simple in-memory `Map<String, CachedEntry>` where
    `CachedEntry(data, expiresAt)`. The `expiredAt` is `now + 6h`.
  - On app foreground: check cache first. If fresh (not expired), return cached data.
    If stale, fetch from network and update cache.
  - The cache is not persisted to disk (in-memory only). It is lost on app kill.
    This is acceptable — the WorkManager re-warms it on the next background execution.
  - The cache is a singleton provided via DI. It is thread-safe (`ConcurrentHashMap`).
- **Tests required:**
  - `TaskCacheTest`:
    - Fresh cache entry is returned without a network call.
    - Expired cache entry triggers a network fetch.
    - Cache returns null for a key that was never fetched.
    - Two concurrent reads for the same key do not trigger two concurrent network calls
      (only one fetch in flight at a time — use `Mutex` or `async` deduplication).
- **Security log requirement:** None.
- **ADR trigger:** No — in-memory cache TTL strategy is tactical, documented here.
- **Exit criteria:** `TaskCacheTest` passes. Opening the app immediately after WorkManager
      executes shows task data without a visible loading spinner.

---

### Task 5.5 — Manual test recipe for Phase 05
- [ ] **What:** Write the full end-to-end manual test recipe for Phase 05 in
      `agentic/manual-testing/phase-05-recipe.md`.
- **Design constraints:** The recipe must cover: push notification receipt at 8 PM,
      WorkManager enqueue verification, cache warm-up on WiFi.
- **Tests required:** None — this is a documentation task.
- **Security log requirement:** None.
- **ADR trigger:** No.
- **Exit criteria:** File exists and is accurate.

---

## Exit Criteria (Phase Level)

All 5 tasks are checked off. The following is true:
- Device token is registered on first app launch and updated on token rotation.
- Tokens are deleted from `device_tokens` table on logout.
- Push notification is received at 8 PM local time when today's task is incomplete.
- WorkManager periodic work request is enqueued with correct constraints.
- Opening the app after background fetch shows data without a network call.

---

## Human Checkpoint

Before marking Phase 05 DONE:

**1. Push notification:**
- Install the app. Complete onboarding. Do not complete today's task.
- Wait until 8 PM local time (or temporarily change the scheduler cron for testing to run
  in 1 minute on the next full minute).
- Expected: a push notification appears in the notification tray.
- Tap the notification: the app opens to the task screen.

**2. Notification is not sent twice:**
- If the notification was received at 8 PM, no second notification arrives at 9 PM
  (because `last_notified_at` is set).

**3. WorkManager status:**
```bash
# On Android device via adb:
adb shell dumpsys jobscheduler | grep "background-task-fetch"
```
Expected: the periodic work request appears in the scheduled jobs list.

**4. WiFi-only constraint:**
- Switch from WiFi to mobile data.
- Check that WorkManager does not immediately enqueue a fetch run
  (it waits for the next unmetered connection).

If any check fails, Phase 05 is not done.
