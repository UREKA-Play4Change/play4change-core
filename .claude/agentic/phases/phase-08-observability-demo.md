# Phase 08 — Observability & Demo Polish

**Status:** `PENDING`
**Goal:** Grafana dashboards for AI latency / task submission rate / struggle trigger rate /
peer review throughput, demo script, HOW_TO_RUN.md updated, README updated.

**Entry criteria:** Phase 07 DONE.

---

## Tasks

### Task 8.1 — Grafana dashboard: AI generation latency (P50/P95/P99)
- [ ] **What:** Add a Grafana dashboard that shows AI generation latency as a histogram
      with P50, P95, and P99 percentiles over the last 24 hours.
- **Design constraints:**
  - **Server instrumentation:** The `TaskGenerationPort` implementation (LangChain4j adapter)
    must record generation latency as a Micrometer `Timer` named
    `ai.generation.duration` with tags: `topic_id`, `generation_phase` (ANALYSIS, GENERATION).
    This timer records from when the Mistral API call starts to when the response is parsed.
  - **Histogram configuration:**
    ```yaml
    management:
      metrics:
        distribution:
          percentiles-histogram:
            ai.generation.duration: true
          percentiles:
            ai.generation.duration: 0.5, 0.95, 0.99
    ```
  - **Grafana dashboard JSON:** Provision the dashboard via the Grafana provisioning
    directory (`infra/grafana/provisioning/dashboards/ai-latency.json`).
    The dashboard has 3 stat panels (P50, P95, P99 as single values) and one time-series
    panel showing all three percentiles over time.
  - **PromQL queries:**
    - P50: `histogram_quantile(0.50, rate(ai_generation_duration_seconds_bucket[5m]))`
    - P95: `histogram_quantile(0.95, rate(ai_generation_duration_seconds_bucket[5m]))`
    - P99: `histogram_quantile(0.99, rate(ai_generation_duration_seconds_bucket[5m]))`
  - The dashboard is provisioned automatically on `docker compose up` — no manual
    Grafana configuration required by the operator.
- **Tests required:**
  - `AiGenerationMetricsTest` (integration test with `@SpringBootTest` + `SimpleMeterRegistry`):
    - A generation call records a timer observation.
    - The timer tag `generation_phase=GENERATION` is present.
    - The timer value is within a plausible range (> 0ms, < 120s).
- **Security log requirement:** None.
- **ADR trigger:** No.
- **Exit criteria:** `AiGenerationMetricsTest` passes. Grafana shows a non-empty AI latency
      dashboard after at least one topic creation.

---

### Task 8.2 — Grafana dashboard: task submission rate, struggle trigger rate, peer review throughput
- [ ] **What:** Add a second Grafana dashboard covering the three core learner flow metrics.
- **Design constraints:**
  - **Server instrumentation — three new counters:**
    - `tasks.submitted.total` — incremented on every `POST /tasks/{id}/submit` (any answer).
      Tags: `result` (`correct` / `incorrect`), `topic_id`.
    - `struggle.sessions.created.total` — incremented when a struggle session is created.
      Tags: `topic_id`.
    - `reviews.verdicts.submitted.total` — incremented on every `POST /reviews/{id}/verdict`.
      Tags: `verdict` (`correct` / `incorrect`), `topic_id`.
  - **PromQL queries:**
    - Task submission rate: `rate(tasks_submitted_total[5m])`
    - Correct task rate: `rate(tasks_submitted_total{result="correct"}[5m])`
    - Struggle trigger rate: `rate(struggle_sessions_created_total[5m])`
    - Peer review throughput: `rate(reviews_verdicts_submitted_total[5m])`
  - **Dashboard JSON:** `infra/grafana/provisioning/dashboards/learner-flow.json`.
    Four time-series panels in a 2×2 grid. Title: "Learner Flow Metrics".
  - The dashboard is provisioned automatically with `docker compose up`.
- **Tests required:**
  - `LearnerFlowMetricsTest` (integration test with `SimpleMeterRegistry`):
    - Task submission increments `tasks.submitted.total` with `result=correct` on correct answer.
    - Task submission increments `tasks.submitted.total` with `result=incorrect` on wrong answer.
    - Struggle session creation increments `struggle.sessions.created.total`.
    - Review verdict submission increments `reviews.verdicts.submitted.total`.
- **Security log requirement:** None.
- **ADR trigger:** No.
- **Exit criteria:** `LearnerFlowMetricsTest` passes. Grafana learner flow dashboard shows
      data after a simulated end-to-end flow.

---

### Task 8.3 — Demo script: end-to-end walkthrough
- [ ] **What:** Write a comprehensive demo script that a presenter can follow to demonstrate
      all 5 bounded contexts in a 10–15 minute live demo.
- **Design constraints:**
  - The script file is `demo/DEMO_SCRIPT.md`.
  - The script must be self-contained: all required API calls, credentials, and expected
    outputs are included. A presenter with no prior knowledge of the system can follow it.
  - **Sequence to demonstrate:**
    1. **Identity:** Request a magic link. Verify it. Show the JWT pair returned.
    2. **Topic:** Admin creates a topic from a URL. Show phase polling to ACTIVE.
    3. **Enrollment:** User enrolls in the topic. Show enrollment status.
    4. **Enrollment:** User calls `/tasks/today`. Show today's task with shuffled options.
    5. **Enrollment:** User submits a wrong answer (trigger struggle detection).
    6. **Struggle:** Show the struggle session appearing. Submit adaptive task answers.
       Show struggle resolution. Show the original task becoming retryable.
    7. **PeerReview:** Admin uploads a photo task. User submits a photo answer.
       Three users submit verdicts. Show the majority verdict resolving.
    8. **Badge:** User completes all topic tasks. Show the badge appearing in `/profile/badges`.
    9. **Observability:** Open Grafana. Show the AI latency and learner flow dashboards
       with data from this demo run.
  - Each step has: a heading, the curl command (or UI action), expected output snippet,
    and a "(30 seconds)" time estimate.
  - The script assumes the Docker Compose stack is running and the database has been
    reset to a clean state using the reset command in `HOW_TO_RUN.md`.
- **Tests required:** None — this is a documentation task.
- **Security log requirement:** None.
- **ADR trigger:** No.
- **Exit criteria:** `demo/DEMO_SCRIPT.md` exists with all 9 steps. A dry run by a team
      member who did not write the script completes in under 15 minutes.

---

### Task 8.4 — Update HOW_TO_RUN.md and README.md
- [ ] **What:** Update both documents to reflect the final state of the system after all 8 phases.
- **Design constraints:**
  - **`demo/HOW_TO_RUN.md`** must include:
    - Prerequisites (Docker, JDK 21, Node 20+, Android Studio or Xcode).
    - Environment variable table: all required env vars, their purpose, and example values.
    - Step-by-step startup: `docker compose up --build`, health check, first admin user setup.
    - Dev-mode flag instructions for accelerated testing.
    - Database reset command.
    - How to access: server API (`localhost:8080`), Swagger (`localhost:8080/swagger-ui.html`),
      Grafana (`localhost:3000`), admin web (`localhost:5173`).
    - FCM and APNs configuration notes (optional — required for push notifications).
    - OWASP dependency check command.
    - Troubleshooting: common startup failures and their fixes.
  - **`README.md`** must be updated to:
    - Add Phase 08 to the ADR table if any new ADRs were written in phases 07–08.
    - Update the test count (currently "24 unit tests across 4 test classes" — update to
      reflect the actual count after all phases).
    - Add the Grafana dashboard section (brief — point to HOW_TO_RUN.md).
    - Remove or update any section that no longer reflects reality.
  - Do not rewrite sections that are still accurate — minimal, surgical edits only.
- **Tests required:** None — documentation task.
- **Security log requirement:** None.
- **ADR trigger:** No.
- **Exit criteria:** A team member who has never run the project can follow HOW_TO_RUN.md
      from scratch and reach a running system in under 20 minutes.

---

## Exit Criteria (Phase Level)

All 4 tasks are checked off. The following is true:
- Two Grafana dashboards are provisioned automatically on `docker compose up`.
- Both dashboards show data after the demo script is run.
- The demo script completes in under 15 minutes on a clean database.
- HOW_TO_RUN.md reflects the final system state.
- README.md is accurate.

---

## Human Checkpoint

Before marking Phase 08 DONE:

**1. Grafana dashboards:**
```bash
docker compose up --build
```
Navigate to `http://localhost:3000` (admin/admin default credentials).
Open "Dashboards". Confirm:
- "AI Generation Latency" dashboard exists and shows P50/P95/P99 panels.
- "Learner Flow Metrics" dashboard exists and shows 4 panels.

**2. Data in dashboards:**
Create a topic (triggers AI generation). Submit a task answer.
Return to Grafana. Refresh dashboards.
Expected: AI latency dashboard shows at least one data point.
Learner flow dashboard shows at least one task submission event.

**3. Demo script dry run:**
Ask a team member (not the author of the script) to follow `demo/DEMO_SCRIPT.md`.
Time the run. Expected: completed in under 15 minutes with no steps that
require knowledge not in the script.

**4. HOW_TO_RUN.md from scratch:**
On a machine without the project configured, follow `demo/HOW_TO_RUN.md`.
Expected: running system in under 20 minutes.

If any check fails, Phase 08 is not done.
