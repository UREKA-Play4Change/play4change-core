# Play4Change — Implementation Roadmap

## Status Legend

| Symbol | Meaning |
|--------|---------|
| `PENDING` | Not started. Entry criteria not yet met. |
| `IN PROGRESS` | Active work. A branch exists. |
| `BLOCKED` | Cannot proceed — see ISSUES.md for the blocker. |
| `DONE` | All tasks checked off. Final PR merged. Human checkpoint passed. |

## Phase Sequencing Rules

1. A phase moves to `IN PROGRESS` only when its entry criteria are met.
2. Phases 01–03 are server-side and can overlap with Phase 06 (admin web) but not with each other.
3. Phase 04 depends on Phase 01 being DONE (CI stable, value objects valid).
4. Phase 07 (security hardening) runs after Phases 01–06 are DONE.
5. Phase 08 (observability) runs after Phase 07 is DONE.
6. Any deviation from this sequence requires a ROADMAP_CHANGES.md entry and human approval.

---

## Phase 01 — Scaffold & Operating System

**Status:** `DONE`
**Phase file:** [phases/phase-01-scaffold.md](phases/phase-01-scaffold.md)

**Goal:** Install the agentic/ operating system, fix CI JDK mismatch, add Detekt,
fix Name/Password value object stubs, document initial security posture, add OWASP
dependency check.

**Entry criteria:** None — this is the first phase.

**Human checkpoint:** CI passes on a clean branch. `./gradlew :server:test` is green.
Name.kt and Password.kt reject invalid input in unit tests.

---

## Phase 02 — Content Generation: Anti-Cheat, Multi-Language & Badges

**Status:** `DONE`
**Phase file:** [phases/phase-02-content-generation.md](phases/phase-02-content-generation.md)

**Goal:** Per-user anti-cheat task instances (shuffle seed = SHA-256(userId+taskId+enrollmentId)),
multilingual generation gated by subscriber language, dynamic micro-competence badges per topic,
4-phase generation pipeline (INGESTION→ANALYSIS→GENERATION→INDEXING) with phase state visible via API.

**Entry criteria:** Phase 01 DONE.

**Human checkpoint:** Admin creates a topic. Two different users enrol and call `/tasks/today`.
The option ordering is different for each user. The topic badge appears in the user's badge list
after completing all tasks. The topic status endpoint shows the correct pipeline phase during generation.

---

## Phase 03 — Learner Logic: Rate Control, Struggle Path & Reporting

**Status:** `DONE`
**Phase file:** [phases/phase-03-learner-logic.md](phases/phase-03-learner-logic.md)

**Goal:** Configurable task delivery rate (default 1/day, dev mode: 2min), fully wired struggle
path (detection→adaptive branch→resolution→return), bad-question reporting with admin correction
flow, user language/timezone preferences.

**Entry criteria:** Phase 02 DONE.

**Human checkpoint:** In dev mode, submit a wrong answer twice on the same topic. A struggle session
appears at `/struggle/enrollment/{id}`. Complete the struggle tasks. The next day's regular task
becomes available. Report a question; admin sees it in the pending reports list.

---

## Phase 04 — Mobile Client: HTTP Wiring & Auth

**Status:** `DONE`
**Phase file:** [phases/phase-04-mobile-http.md](phases/phase-04-mobile-http.md)

**Goal:** Replace all mock repositories with real Ktor HTTP calls, JWT secure storage
(EncryptedSharedPreferences on Android, Keychain on iOS), automatic token refresh on 401,
all existing screens connected to live data.

**Entry criteria:** Phase 01 DONE (CI stable, server API stable enough to wire against).

**Human checkpoint:** Install the APK on a physical Android device. Request a magic link.
Click the link. The home screen shows real enrolled topics. Submit a task answer. The
submission persists after app restart. Kill and reopen the app — no re-login required.

---

## Phase 05 — Mobile Client: Notifications & Smart Fetching

**Status:** `DONE`
**Phase file:** [phases/phase-05-mobile-notifications.md](phases/phase-05-mobile-notifications.md)

**Goal:** Push notifications for daily task reminders at 8 PM user local time if task not done,
WorkManager background fetch with battery/connectivity constraints, cache warm-up on
charging + good WiFi.

**Entry criteria:** Phase 04 DONE.

**Human checkpoint:** Enrol in a topic. Do not complete today's task. At 8 PM local time a
push notification arrives. Tap it — the app opens to the task screen. Background fetch log
shows cache warm-up when device is plugged in and on WiFi.

---

## Phase 06 — Admin Web: Full Feature Parity

**Status:** `DONE`
**Phase file:** [phases/phase-06-admin-web.md](phases/phase-06-admin-web.md)

**Goal:** React admin web covers all server admin endpoints — topic creation (URL+PDF),
4-phase status polling, question report review and correction, user management, badge overview.

**Entry criteria:** Phase 01 DONE (stable server API to wire against).

**Human checkpoint:** Log in to the admin web. Create a topic from a URL. Watch the status
polling UI advance through INGESTION→ANALYSIS→GENERATION→INDEXING→ACTIVE. Open pending
question reports. Correct one question. The corrected task regenerates its instances.

---

## Phase 09 — Learning Path DAG

**Status:** `DONE`
**Phase file:** [phases/phase-09-learning-path-dag.md](phases/phase-09-learning-path-dag.md)

**Goal:** Prerequisite graph (DAG) for topics. Admin sets prerequisites via
`POST /admin/topics/{id}/prerequisites`. Server gates enrollment (all prereqs must be COMPLETED).
Mobile shows a lock icon on locked topics. Admin web shows a Prerequisites tab on topic detail
and a new Learning Paths page with the full graph overview.

**Entry criteria:** Phase 02 DONE (topics + badges exist).

**Human checkpoint:** Create two topics A and B. Set B's prerequisite to A.
Attempt to enroll in B on mobile — enrollment is blocked. Complete topic A.
Attempt to enroll in B again — succeeds. Admin web Learning Paths page shows the A→B edge.

---

## Phase 07 — Security Hardening & DAST

**Status:** `DONE`
**Phase file:** [phases/phase-07-security-hardening.md](phases/phase-07-security-hardening.md)

**Goal:** Systematic OWASP Top 10 pass, OWASP ZAP DAST run against Docker Compose stack,
all Critical/High findings remediated or risk-accepted with written justification,
rate limiting on `/auth/**`, Swagger gated in prod, actuator prometheus internal-only.

**Entry criteria:** Phases 01–06 all DONE.

**Human checkpoint:** Run the ZAP baseline scan command from DAST-REPORT.md. No Critical or
High findings remain unaddressed. Hit `/auth/magic-link` 20 times rapidly — the 11th request
returns 429. Swagger UI is not accessible without ADMIN JWT in prod profile.

---

## Phase 08 — Observability & Demo Polish

**Status:** `DONE`
**Phase file:** [phases/phase-08-observability-demo.md](phases/phase-08-observability-demo.md)

**Goal:** Grafana dashboards for AI latency / task submission rate / struggle trigger rate /
peer review throughput, demo script, HOW_TO_RUN.md updated, README updated.

**Entry criteria:** Phase 07 DONE.

**Human checkpoint:** Open Grafana at `http://localhost:3000`. The AI Latency dashboard shows
P50/P95/P99 percentiles. Submit a task answer — the task submission counter increments within
30 seconds. The demo script runs end-to-end without any manual intervention beyond copy-pasting
the curl commands.
