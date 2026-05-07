# Phase 04 Manual Test Recipe
## Mobile Client: HTTP Wiring & Auth

This recipe verifies all Phase 04 human-checkpoint criteria.
Execute the sections in order on a physical Android device or emulator connected
to the local Docker Compose stack.

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

3. Build and install the debug APK on the target device or emulator:
   ```bash
   ./gradlew :composeApp:installDebug
   ```

4. An active topic with at least 1 task must exist. If not, run the Phase 02
   recipe to create one.

5. The emulator loopback address `10.0.2.2` resolves to the host machine.
   On a physical device on the same network, use the host's LAN IP instead of
   `10.0.2.2` and update `gradle.properties` > `BASE_URL` accordingly before
   building.

---

## Section 1 — Magic Link Flow

**Goal:** Verify that a user can authenticate via magic link and land on the
home screen showing live data.

### Steps

1. Open the app. Enter your email address in the magic-link field and tap **Send**.

2. Watch the server console for the generated link:
   ```bash
   docker compose logs server --follow | grep "magic link"
   ```
   The log line contains a URL such as:
   ```
   Magic link: http://localhost:8080/auth/verify?token=<TOKEN>
   ```

3. Copy `<TOKEN>` from the log. Simulate the deep-link click by running on the
   host machine:
   ```bash
   curl -v "http://localhost:8080/auth/verify?token=<TOKEN>"
   ```
   Expected response: `200 OK` with JSON body containing `accessToken` and `refreshToken`.

4. The app should detect the incoming token (via the deep link, or by polling
   the verification endpoint) and transition to the **Home screen**.

   Expected: the home screen displays real enrolled topics loaded from the server.

---

## Section 2 — Token Persistence Across App Restarts

**Goal:** Verify that tokens are stored in EncryptedSharedPreferences and survive
a full app kill/reopen without requiring re-authentication.

### Steps

1. While on the home screen, completely kill the app:
   - On Android: swipe the app away from the Recents screen.
   - On the emulator: `adb shell am force-stop com.ureka.play4change`

2. Reopen the app.

   Expected: the home screen loads immediately without a login prompt.
   Tokens are read from EncryptedSharedPreferences and the Bearer header is
   injected automatically.

---

## Section 3 — Network Error UI (No Connection)

**Goal:** Verify that disabling the network shows a meaningful error UI instead
of a crash.

### Steps

1. While on the home screen, put the device/emulator in airplane mode:
   - Device: Settings → Airplane mode ON.
   - Emulator: three-dot menu → Cellular → Network type → None.

2. Kill and reopen the app (or trigger a reload with the Retry button if visible).

   Expected on the **Home screen**:
   - `isLoading = false`
   - `networkError = NetworkError.NoConnection` in state
   - The screen shows an error message and a **Retry** button (not a blank screen
     or a crash).

3. Navigate to the **Task screen** while still offline.

   Expected: same error UI with a **Retry** button.

4. Re-enable the network.

5. Tap the **Retry** button.

   Expected: the screen reloads successfully and shows live data.

---

## Section 4 — Unauthorized Recovery (401 → Refresh → Retry)

**Goal:** Verify that the Ktor Auth plugin automatically refreshes the access
token on a 401 response without user intervention.

### Steps

**Quick test (simulate expired token):**

1. Clear only the access token from storage while keeping the refresh token.
   The easiest way on the emulator:
   ```bash
   # Open the app's encrypted prefs; use the adb shell to clear just the access key
   adb shell am force-stop com.ureka.play4change
   # Or use the "clear token" debug option if available in the build
   ```
   Alternatively, set a very short `ACCESS_TOKEN_TTL` (e.g. 5 seconds) in
   `server/src/main/resources/application.yml` and wait for it to expire:
   ```yaml
   auth:
     access-token-ttl: 5s
   ```

2. Perform any authenticated action (e.g. reload the home screen).

   Expected:
   - The Ktor Auth plugin detects the 401.
   - `POST /auth/refresh` is called automatically with the stored refresh token.
   - The new access token is stored and the original request is retried.
   - The home screen loads successfully — the user sees **no error**.

**Failed refresh test (simulate expired refresh token):**

3. Manually clear both the access and refresh tokens from storage (or wait for
   both to expire if using very short TTLs).

4. Perform any authenticated action.

   Expected:
   - Refresh fails (server returns 401 or 403 on `/auth/refresh`).
   - `SessionEventBus.sessionExpired()` is called.
   - The app navigates to the **login screen**.

---

## Section 5 — Profile Screen Shows Real Data

**Goal:** Verify that the profile screen fetches from `GET /profile` and
`GET /profile/badges`.

### Steps

1. Navigate to the profile screen.

   Expected:
   - `userId`, `name`, and `email` match the authenticated user.
   - `streakDays` and `totalPoints` reflect the active enrollment.
   - The badge list is non-empty if any badges have been earned.

---

## Checklist

| # | Check | Pass |
|---|-------|------|
| 1 | Magic link flow completes and home screen shows real data | ☐ |
| 2 | App survives full kill/reopen without re-login | ☐ |
| 3 | Home screen shows error UI (not crash) when offline | ☐ |
| 4 | Task screen shows error UI (not crash) when offline | ☐ |
| 5 | Retry button reloads successfully after network restored | ☐ |
| 6 | 401 triggers silent refresh + retry — user sees no error | ☐ |
| 7 | Expired refresh token → redirect to login screen | ☐ |
| 8 | Profile screen shows correct name, email, and points | ☐ |

All 8 checks must pass before marking Phase 04 DONE.
