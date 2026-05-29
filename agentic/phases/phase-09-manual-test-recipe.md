# Phase 09 — Manual Test Recipe

**Branch under test:** `feat/phase-09-learning-path-dag`

## Prerequisites

- Docker Compose stack running (`./gradlew :server:bootRun` or `docker-compose up`)
- Admin web running (`npm run dev` inside `play4change-web/`, `VITE_USE_MOCK=false`)
- Android emulator or physical device with the latest debug APK installed
- At least two ACTIVE topics exist (create via admin web if needed)
- A regular user account (not ADMIN) able to log in on mobile

---

## Scenario 1 — Admin sets a prerequisite via admin web

1. Open admin web → `/admin/topics` → click topic **B**.
2. Open the **Prerequisites** tab (5th tab).
3. The tab shows a checkbox list of other ACTIVE topics.
4. Check topic **A** → click **Save**.
5. **Expected:** save succeeds, the checkbox list reflects the saved state (A is checked).
6. Open `/admin/learning-paths`.
7. **Expected:** the stats row shows at least 1 topic with a prerequisite; the graph view shows "Topic B requires [A]".

---

## Scenario 2 — Cycle detection blocks setting a reverse prerequisite

1. While B's prerequisite is still A (from Scenario 1), open topic **A** → Prerequisites tab.
2. Check topic **B** → click **Save**.
3. **Expected:** the server returns HTTP 400; the admin web displays an error toast (or the save button stays enabled). Topic A's prerequisite list remains unchanged.

---

## Scenario 3 — Mobile shows lock icon for locked topic

1. On mobile, log in as a regular user who has **not** completed topic A.
2. Navigate to the Explore screen.
3. **Expected:** Topic B shows a **lock icon** badge in the title row and a disabled grey **"Locked"** button instead of the normal enroll button.
4. Topic A shows the normal **enroll** button (no lock icon).

---

## Scenario 4 — Server blocks enrollment with unmet prerequisites

```bash
# Replace <TOKEN> and <SERVER> with real values
curl -X POST https://<SERVER>/topics/topic-b/enroll \
  -H "Authorization: Bearer <TOKEN>" \
  -H "Content-Type: application/json"
```

**Expected:** HTTP 400 with body containing `"prerequisites not completed"`.

---

## Scenario 5 — Lock is lifted after completing the prerequisite topic

1. On mobile, enroll in topic **A** and submit all of its daily tasks until the enrollment status transitions to **COMPLETED**.
   (In dev mode, task rate is 2 min — use dev server for speed.)
2. Return to the Explore screen and refresh.
3. **Expected:** Topic B now shows the normal **enroll** button (no lock icon).

---

## Scenario 6 — User can now enroll in topic B

1. Tap **Enroll** on topic B → confirm dialog appears.
2. Tap **Confirm**.
3. **Expected:** Enrollment succeeds; topic B moves to the home screen active topics list.

---

## Scenario 7 — Clearing prerequisites via admin web

1. Open admin web → topic B → Prerequisites tab.
2. Uncheck topic A → click **Save**.
3. **Expected:** save succeeds; Prerequisites tab shows empty list.
4. Open `/admin/learning-paths`.
5. **Expected:** the A→B edge no longer appears in the graph.

---

## Scenario 8 — API smoke tests (curl)

```bash
BASE=https://<SERVER>
ADMIN_TOKEN=<ADMIN_JWT>
TOPIC_B=<topic-b-uuid>
TOPIC_A=<topic-a-uuid>

# GET prerequisites — should return empty after Scenario 7
curl -s -H "Authorization: Bearer $ADMIN_TOKEN" \
  $BASE/admin/topics/$TOPIC_B/prerequisites | jq .

# POST set prerequisite
curl -s -X POST \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"prerequisiteIds\":[\"$TOPIC_A\"]}" \
  $BASE/admin/topics/$TOPIC_B/prerequisites | jq .

# GET full learning graph
curl -s -H "Authorization: Bearer $ADMIN_TOKEN" \
  $BASE/admin/learning-graph | jq .

# Unauthenticated request — should return 401
curl -s -o /dev/null -w "%{http_code}" \
  $BASE/admin/learning-graph
```

**Expected responses:** prerequisites returns `[{id, title, status, category}]`; learning-graph returns `{edges:[{topicId, prerequisiteTopicId}]}`; unauthenticated returns `401`.

---

## Pass criteria

| # | Check | Result |
|---|-------|--------|
| 1 | Prerequisite saved and visible in admin web | ☐ |
| 2 | Cycle detection returns 400 | ☐ |
| 3 | Lock icon shown on mobile for locked topic | ☐ |
| 4 | Server returns 400 for enrollment with unmet prereqs | ☐ |
| 5 | Lock removed after completing prerequisite topic | ☐ |
| 6 | Enrollment in B succeeds after completing A | ☐ |
| 7 | Clearing prerequisites removes edge from graph | ☐ |
| 8 | API smoke tests return expected shapes and status codes | ☐ |

All 8 boxes must be checked before merging the PR.
