# Phase 02 — Content Generation: Anti-Cheat, Multi-Language & Badges

**Status:** `IN PROGRESS`
**Goal:** Per-user anti-cheat task instances (shuffle seed = SHA-256(userId+taskId+enrollmentId)),
multilingual generation gated by subscriber language, dynamic micro-competence badges per topic,
4-phase generation pipeline (INGESTION→ANALYSIS→GENERATION→INDEXING) with phase state visible via API.

**Entry criteria:** Phase 01 DONE.

---

## Tasks

### Task 2.1 — STRIDE Threat Analysis (no code)
- [x] **What:** Before writing any implementation code for this phase, produce a STRIDE analysis
      of the content generation pipeline and write it to THREAT-LOG.md.
      This is a read-only planning task. No code changes.
- **Design constraints:**
  - STRIDE categories: Spoofing, Tampering, Repudiation, Information Disclosure,
    Denial of Service, Elevation of Privilege.
  - Analyse each of: URL ingestion endpoint, Mistral API call, AI output persistence,
    per-user task instance generation, badge issuance.
  - For each threat identified, record: STRIDE category, OWASP mapping, severity (High/Medium/Low),
    planned mitigation, and the task number where it will be addressed.
- **Tests required:** None — this is a threat modelling task.
- **Security log requirement:** This task IS the THREAT-LOG update. Add a PHASE-02 STRIDE section
      to THREAT-LOG.md.
- **ADR trigger:** No. STRIDE analysis does not require an ADR.
- **Exit criteria:** THREAT-LOG.md contains a Phase 02 STRIDE section with entries for all
      5 attack surfaces listed above.

---

### Task 2.2 — Anti-cheat: per-user task instances
- [x] **What:** Implement per-user option shuffling so each user receives a unique ordering
      of multiple-choice options. The shuffle is deterministic: given the same inputs,
      it always produces the same output for the same user.
      Seed = SHA-256(userId + taskId + enrollmentId).
- **Design constraints:**
  - The seed is computed from three inputs concatenated as UTF-8 strings, SHA-256 hashed,
    and the first 8 bytes used as a `Long` seed for `java.util.Random` (not `SecureRandom` —
    we want deterministic, not cryptographic randomness here).
  - `UserTaskDto` is the learner-facing DTO — it contains shuffled options in the user's
    unique order, no answer key.
  - `AdminTaskDto` is the admin-facing DTO — it contains options in canonical order with
    the correct answer marked.
  - The shuffle happens at read time in the application layer — options are stored in
    canonical order and shuffled on the way out.
  - No per-user copies of tasks are stored. Storage overhead is O(tasks), not O(users × tasks).
  - The `TaskAssignmentService` (or equivalent) is the single place where shuffling occurs.
- **Tests required:**
  - `AntiCheatShuffleTest`:
    - Same user + task + enrollment always produces the same order.
    - Two different users with the same task produce different orders (probabilistic — test
      with 3 users; at least 2 must differ).
    - Admin view is never shuffled.
    - Correct answer position in shuffled DTO is consistent with the stored canonical index.
- **Security log requirement:** Add entry to THREAT-LOG.md: "Anti-cheat shuffle implemented —
      deterministic per-user seed prevents option-sharing between users."
      OWASP A04 (Insecure Design — integrity of assessment).
- **ADR trigger:** No — the seed design is documented here and in THREAT-LOG.md.
- **Exit criteria:** `AntiCheatShuffleTest` passes. Two enrolled users calling `/tasks/today`
      on the same task receive differently-ordered options.

---

### Task 2.3 — Multi-language: subscriber language detection and gated generation trigger
- [x] **What:** When a user subscribes to a topic, their preferred language (BCP 47 tag stored
      on their user profile) is used to select which language variant of task instances to
      return. If a variant for that language does not exist yet, trigger generation.
- **Design constraints:**
  - User language preference is stored as a BCP 47 tag (e.g. `pt-PT`, `en-GB`, `fr-FR`).
  - Generation is gated: if a task instance in the requested language already exists,
    return it — no new generation call.
  - If generation for the requested language is not yet complete, return a `202 Accepted`
    with a `X-Generation-Status: PENDING` header. The client polls.
  - The gating logic lives in the application layer, not the controller.
  - Supported languages are configured in `application.yml` as a whitelist.
    An unsupported language request falls back to the topic's source language.
- **Tests required:**
  - `LanguageGatingTest`:
    - Request for existing language variant returns the cached instance.
    - Request for unsupported language falls back to source language.
    - Request for supported but not-yet-generated language triggers generation and returns 202.
- **Security log requirement:** None for this task specifically.
- **ADR trigger:** Yes — document the language gating strategy in DECISIONS.md
      (not a full ADR — tactical decision, single feature).
- **Exit criteria:** `LanguageGatingTest` passes. `application.yml` has a `supported-languages`
      list. Language preference on the user record is validated against this list.

---

### Task 2.4 — Multi-language: Mistral prompt + AI output sanitisation + SSRF mitigation
- [x] **What:** Rewrite the Mistral generation prompt to include the target language.
      Sanitise all AI-generated HTML/text output with jsoup before persistence.
      Validate AI output against a schema before accepting it.
      Mitigate SSRF on the URL ingestion endpoint.
- **Design constraints:**
  - **Prompt engineering:** The system prompt must include:
    `"Generate all content in {targetLanguage}. Do not mix languages."`
    The target language is passed as a BCP 47 tag. The prompt must not allow the AI to
    override this instruction.
  - **Output sanitisation:** All AI-generated string fields that will be stored and later
    displayed are passed through `Jsoup.clean(input, Safelist.none())` before persistence.
    This strips all HTML tags and prevents XSS if content is rendered in a web context.
  - **Schema validation:** The AI response is deserialized into a typed Kotlin data class
    (not a `Map<String, Any>`). Missing required fields cause the generation to fail and
    retry once with an explicit schema reminder appended to the prompt.
    After two failures, the generation is marked as FAILED with a structured error record.
  - **SSRF mitigation on URL ingestion:**
    - Parse the submitted URL with `java.net.URL`.
    - Reject schemes other than `https`.
    - Resolve the host to an IP address before fetching.
    - Reject if the resolved IP is in RFC 1918 ranges (10.0.0.0/8, 172.16.0.0/12,
      192.168.0.0/16) or loopback (127.0.0.0/8, ::1).
    - Reject if the resolved IP is a link-local address (169.254.0.0/16).
    - The fetch itself must have a connection timeout of 10s and a read timeout of 30s.
- **Tests required:**
  - `AiOutputSanitiserTest`:
    - HTML tags are stripped from AI output fields.
    - Script tags are stripped.
    - Plain text is preserved unchanged.
  - `UrlSsrfValidatorTest`:
    - Localhost URL is rejected.
    - 192.168.x.x URL is rejected.
    - 10.x.x.x URL is rejected.
    - `http://` (not https) is rejected.
    - Valid public HTTPS URL is accepted.
- **Security log requirement:**
  - Update THREAT-LOG.md: mark "AI output not validated before persistence" (OWASP A08) as
    FIXED in this task.
  - Update THREAT-LOG.md: mark "SSRF on URL ingestion" (OWASP A10) as PARTIALLY FIXED
    (validation implemented; ZAP scan to confirm in Phase 07).
- **ADR trigger:** Yes — write ADR-017 for the AI output sanitisation and schema validation
      strategy. The decision to use jsoup over a custom sanitiser and to fail-fast on schema
      mismatch is architectural.
- **Exit criteria:** Both test classes pass. THREAT-LOG.md rows updated.
      `./gradlew :server:detektMain` still passes.

---

### Task 2.5 — Anti-cheat: batch N-instance generation per task
- [x] **What:** Generate N shuffled instances of each task in a single Mistral call,
      where N is a configurable parameter (default: 5). This reduces API call overhead
      and ensures a pool of variants exists before any user requests a task.
- **Design constraints:**
  - The batch request asks Mistral to produce N variants of each question in a single call,
    with different distractors (wrong answers) in each variant.
  - The correct answer and core concept remain identical across variants.
  - Variants are stored as a one-to-many relationship: one task → many task instances.
  - The instance assigned to a user is selected by: `instances[seed % instances.size]`
    where `seed` is the same SHA-256 seed from Task 2.2.
  - N is configured in `application.yml` as `task-generation.instances-per-task`.
  - The batch generation must not exceed Mistral's context window — split into multiple
    calls if the topic has more than 10 tasks.
- **Tests required:**
  - `BatchInstanceGenerationTest`:
    - A topic with 3 tasks generates `3 × N` instances total.
    - Each instance has the same correct answer as its parent task.
    - Instance selection by seed is stable (same seed → same instance index).
- **Security log requirement:** None.
- **ADR trigger:** No — documented here.
- **Exit criteria:** `BatchInstanceGenerationTest` passes. The database contains
      `tasks × N` instance rows after generation completes.

---

### Task 2.6 — Badges: MicroCompetence domain model and Badge issuance
- [x] **What:** Add the `MicroCompetence` domain entity and the `Badge` earned record.
      Implement badge issuance: when a user completes all tasks in a topic, they earn
      the topic's micro-competence badge. Issuance is idempotent.
- **Design constraints:**
  - `MicroCompetence`: topic-scoped value object. Fields: `name`, `description`,
    `topicId`, `iconUrl` (optional, MinIO key).
  - `Badge`: earned record. Fields: `userId`, `microCompetenceId`, `earnedAt`.
    Unique constraint on `(userId, microCompetenceId)` — earnable only once per topic.
  - Issuance trigger: called by the enrollment service when the last task in the topic
    is marked as CORRECT. Must be idempotent: a second call has no effect and does not
    throw an error.
  - The badge domain lives in the `badge/` bounded context. The enrollment service calls
    a `BadgeIssuancePort` outbound interface — it does not import badge domain classes.
  - Flyway migration V11 adds: `micro_competences` table, `user_badges` table.
- **Tests required:**
  - `BadgeIssuanceServiceTest`:
    - Completing the last task triggers badge issuance.
    - A second issuance call for the same user+competence is a no-op (idempotent).
    - Completing a non-final task does not issue a badge.
  - `MicroCompetenceTest`:
    - Value object rejects blank name.
    - Value object rejects blank description.
- **Security log requirement:** None.
- **ADR trigger:** No — badge issuance design is documented here.
- **Exit criteria:** All tests pass. Flyway V11 migration runs cleanly. Badge appears
      in the user's badge list after completing all topic tasks.

---

### Task 2.7 — Badges: API endpoints
- [x] **What:** Add two new endpoints:
      `GET /profile/badges` — returns the authenticated user's earned badges.
      `GET /admin/topics/{topicId}/badges` — returns badge issuance stats for a topic
      (total earned, percentage of enrolled users who earned it).
- **Design constraints:**
  - `GET /profile/badges` requires authentication (any role).
    Response: list of `{ microCompetenceName, description, topicTitle, earnedAt }`.
  - `GET /admin/topics/{topicId}/badges` requires `ROLE_ADMIN`.
    Response: `{ totalIssued, enrolledCount, earnedPercentage, recentEarners: [userId, earnedAt] }`.
  - All responses use the existing `MessageResponse` error shape on failure.
  - Controller tests cover: 200 happy path, 401 without JWT, 403 with USER role on admin endpoint.
- **Tests required:**
  - `BadgeControllerTest` (`@WebMvcTest`):
    - `GET /profile/badges` with valid JWT returns user badges.
    - `GET /profile/badges` without JWT returns 401.
    - `GET /admin/topics/{id}/badges` with ADMIN JWT returns stats.
    - `GET /admin/topics/{id}/badges` with USER JWT returns 403.
- **Security log requirement:** Add note to THREAT-LOG.md confirming badge endpoints
      are access-controlled (OWASP A01).
- **ADR trigger:** No.
- **Exit criteria:** `BadgeControllerTest` passes. Both endpoints return the correct
      shape in a Docker Compose test.

---

### Task 2.8 — 4-phase pipeline: GenerationPhase enum and phase state in API
- [x] **What:** Make the 4-phase generation pipeline (INGESTION→ANALYSIS→GENERATION→INDEXING)
      visible in the API. The admin can poll `GET /admin/topics/{id}` to see the current phase.
- **Design constraints:**
  - Add `GenerationPhase` enum: `INGESTION`, `ANALYSIS`, `GENERATION`, `INDEXING`, `ACTIVE`, `FAILED`.
  - The `Topic` entity stores `currentPhase: GenerationPhase` and `phaseUpdatedAt: Instant`.
  - Phase transitions are logged: each transition writes a `TopicPhaseLog` record
    (topicId, fromPhase, toPhase, timestamp, durationMs).
  - `GET /admin/topics/{id}` response includes: `status`, `currentPhase`, `phaseUpdatedAt`,
    `generationLog` (list of phase transitions with durations).
  - Phase transitions happen in the application service — not in infrastructure adapters.
  - If GENERATION fails (Mistral error), the topic moves to FAILED. The admin can call
    `POST /admin/topics/{id}/regenerate` to reset to INGESTION and retry.
  - Flyway migration V12 adds: `topic_phase_log` table, `current_phase` column to `topics`.
- **Tests required:**
  - `GenerationPhaseTransitionTest`:
    - Topic starts in INGESTION on creation.
    - Successful ingestion transitions to ANALYSIS.
    - Successful analysis transitions to GENERATION.
    - Successful generation transitions to INDEXING.
    - Successful indexing transitions to ACTIVE.
    - Mistral failure in GENERATION transitions to FAILED.
    - Regenerate on FAILED topic resets to INGESTION.
  - `TopicControllerPhaseTest`:
    - `GET /admin/topics/{id}` returns `currentPhase` field.
    - `generationLog` contains timestamps and durations.
- **Security log requirement:** None.
- **ADR trigger:** No.
- **Exit criteria:** All tests pass. Flyway V12 runs cleanly. Admin polling shows phase
      advancement during generation.

---

### Task 2.9 — Manual test recipe for Phase 02
- [ ] **What:** Write the full end-to-end manual test recipe for Phase 02 in
      `agentic/manual-testing/phase-02-recipe.md`.
- **Design constraints:** The recipe must be executable by a non-developer using only
      curl commands and the admin web UI (or Swagger). No code knowledge required.
- **Tests required:** None — this is a documentation task.
- **Security log requirement:** None.
- **ADR trigger:** No.
- **Exit criteria:** File exists at `agentic/manual-testing/phase-02-recipe.md`.
      Every curl command in the recipe is accurate to the actual API.

---

## Exit Criteria (Phase Level)

All 9 tasks are checked off. The following is true:
- Two different users receive differently-ordered options for the same task.
- Topic badges are issued and retrievable.
- The pipeline phase is visible in the admin API.
- AI output is sanitised and schema-validated before persistence.
- SSRF mitigation is in place on URL ingestion.
- ADR-017 is written and committed.
- THREAT-LOG.md rows for A08 and A10 are updated.

---

## Human Checkpoint

Before marking Phase 02 DONE:

**1. Anti-cheat — two users, different ordering:**
```bash
# Enrol user A and get today's task
curl -H "Authorization: Bearer $TOKEN_A" http://localhost:8080/tasks/today?topicId=1
# Note the option order in the response

# Enrol user B and get the same task
curl -H "Authorization: Bearer $TOKEN_B" http://localhost:8080/tasks/today?topicId=1
# The option order must differ from user A's response
```
Expected: `options` array order is different between the two responses.

**2. Badge issuance:**
```bash
# Complete all tasks in a topic as user A, then:
curl -H "Authorization: Bearer $TOKEN_A" http://localhost:8080/profile/badges
```
Expected: Response contains the topic's micro-competence badge with `earnedAt` timestamp.

**3. Pipeline phase polling:**
```bash
# Create a topic
curl -X POST -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"url": "https://example.com/article"}' \
  http://localhost:8080/admin/topics

# Poll status
curl -H "Authorization: Bearer $ADMIN_TOKEN" http://localhost:8080/admin/topics/{id}
```
Expected: `currentPhase` advances from `INGESTION` → `ANALYSIS` → `GENERATION` → `INDEXING` → `ACTIVE`.
`generationLog` contains at least one entry per phase transition.

**4. AI output sanitisation (unit test evidence):**
```bash
./gradlew :server:test --tests "*.AiOutputSanitiserTest"
```
Expected: All tests PASSED.

If any check fails, Phase 02 is not done. Identify the failing task and fix it.
