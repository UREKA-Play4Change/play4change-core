# Play4Change — AI Operating Rules

**Every Claude Code session reads this file first, before doing anything else.**
This is not optional. This is the operating contract.

---

## §0 Project at a Glance

### Components

| Module | Language / Framework | Purpose |
|--------|---------------------|---------|
| `server` | Kotlin 2.x + Spring Boot 3.2 | Core API, all bounded contexts |
| `ai-agent` | Kotlin + LangChain4j 0.36 + Mistral | AI content generation pipeline |
| `composeApp` | KMP (Android + iOS) | Mobile learner client |
| `common` | KMP (Kotlin Multiplatform) | Shared serialisable types |
| `infra` | Docker Compose + Nginx | Local orchestration |
| `play4change-admin` | React + Vite (separate repo) | Admin web interface |

### Bounded Contexts

| Context | Package root | Responsibility |
|---------|-------------|----------------|
| Identity | `auth/` | Magic link, OAuth, JWT dual-token, user entity |
| Topic | `topic/` | AI content pipeline, task instances, badges |
| Enrollment | `enrollment/` | Daily progression, task assignment, rate control |
| Struggle | `struggle/` | Error pattern detection, adaptive remediation branch |
| PeerReview | `peerreview/` | Photo task collective assessment, majority verdict |
| Badge | `badge/` | MicroCompetence issuance, idempotency, user badges |

### Tech Stack

| Concern | Choice |
|---------|--------|
| Database | PostgreSQL 16 + pgvector extension |
| Cache | Redis 7 (Lettuce, Spring Data) |
| File storage | MinIO (AWS SDK v2, S3-compatible) |
| AI | Mistral AI `mistral-small-latest` via LangChain4j |
| Auth | Magic link (Resend) + Google JWKS + Facebook Graph |
| Migrations | Flyway (V1–V10 at baseline; continue from V11) |
| Observability | Micrometer + Prometheus + Grafana |
| Build | Gradle 8 (Kotlin DSL), multi-module |
| CI | GitHub Actions (JDK 21, Detekt, OWASP dep-check) |

---

## §1 The Golden Loop

**Every task follows these 10 steps in order. No exceptions.**

```
Orient → Branch → Red test → Implement → Refactor → Verify → Log → Commit → PR → STOP
```

### Step 1 — Orient
Run the checklist in §10. Read the current phase file. Read ROADMAP.md. Read HACKS.md.
Read ISSUES.md. Do not write a single line of code until orient is complete.

### Step 2 — Branch
```bash
git checkout -b feat/phase-XX-<short-description>
# Example: git checkout -b feat/phase-02-multilang
```
Never work on `main`. If already on a branch, verify it is the right one.

### Step 3 — Red test
Write a failing test that proves the feature does not exist yet.
Run it. Confirm it is red. Do not write implementation code first.

### Step 4 — Implement
Write the minimum code to make the test pass. No more.
No unrelated refactoring. No "while I'm here" changes.

### Step 5 — Refactor
Clean up implementation code without changing behaviour. Keep tests green.

### Step 6 — Verify
```bash
./gradlew :server:test          # all server tests
./gradlew :server:detektMain    # linting
```
All tests must be green. All Detekt checks must pass. No exceptions.
If a test is flaky: stop. Document it in ISSUES.md. Do not merge a flaky test.

### Step 7 — Log
Update the relevant agentic/ files:
- **HACKS.md** — if the implementation required a shortcut or workaround
- **THREAT-LOG.md** — if a security-relevant decision was made (see §5 for when)
- **DECISIONS.md** — if a non-obvious design choice was made that is not worth a full ADR
- **Phase file checkbox** — check the completed task

### Step 8 — Commit
Follow the commit format in §6. Commit all changes in a single logical commit per task.
Do not commit agentic/ files in the same commit as code — use a separate commit:
```
feat(topic): add per-user shuffle seed for anti-cheat instances

Phase: 02  Task: 2.2
```

### Step 9 — PR
```bash
gh pr create --title "<type>(<scope>): <subject>" --body "..."
```
The PR body must include: what changed, why, phase+task reference, test evidence.
Do not merge. Do not approve.

### Step 10 — STOP
Report to the human: PR URL, what was done, any blockers or concerns.
Wait for the human to review and merge before starting the next task.

---

## §2 Architecture Rules

### The Dependency Rule
```
web/ → application/ → domain/
infrastructure/ → application/  (implements outbound ports)
web/ → domain/  (for DTOs that wrap value objects — allowed)
```

**Never:**
- `domain/` imports Spring, JPA, Jakarta, or Jackson
- `application/` imports adapter classes directly
- `web/` imports JPA entities or repository implementations
- One bounded context imports another's domain classes directly
  (use inbound ports or shared `common` types)

### Package Structure (per bounded context)
```
<context>/
├── domain/
│   ├── model/          # Entities, value objects, domain events
│   └── repository/     # Outbound port interfaces (no Spring annotations)
├── application/
│   ├── port/
│   │   ├── inbound/    # Use case interfaces
│   │   └── outbound/   # Port interfaces (email, AI, storage, etc.)
│   └── service/        # Implements inbound ports, uses outbound ports
└── adapter/
    ├── inbound/
    │   └── web/        # Controllers, DTOs, exception handlers
    └── outbound/
        └── persistence/ # JPA entities, Spring repos, adapter impls
```

### Value Objects
All value objects must validate at construction. No setters. Immutable.
A value object that accepts invalid state is a domain bug.

### Flyway Migrations
New migrations are `V11__description.sql` and up. Never modify an existing migration.
Migration filenames are permanent. Breaking a migration breaks every existing deployment.

---

## §3 TDD Discipline

### The Rhythm
1. Write a failing test that names the behaviour clearly
2. Write the minimum implementation to make it pass
3. Refactor — structure, naming, duplication — with tests staying green

### Test Location Conventions

| Test type | Location | Annotation |
|-----------|----------|------------|
| Domain unit test | `server/src/test/kotlin/<context>/domain/` | Plain JUnit 5 |
| Application unit test | `server/src/test/kotlin/<context>/application/` | `@ExtendWith(MockKExtension::class)` |
| Integration test | `server/src/test/kotlin/<context>/adapter/` | `@SpringBootTest` |
| Controller test | `server/src/test/kotlin/<context>/adapter/inbound/` | `@WebMvcTest` |

### Coverage Targets

| Layer | Minimum coverage |
|-------|-----------------|
| `domain/` | 90% line + branch |
| `application/` | 80% line |
| `adapter/inbound/web/` | Key paths (happy + principal error) |
| `adapter/outbound/` | Integration tests, not unit coverage |

### Test Naming
```kotlin
@Test
fun `given expired magic link token when verify called then returns InvalidOrExpiredLink`()
```
Test names are plain English sentences describing the scenario. No abbreviations.

### What Not to Test
- Spring framework wiring (trust `@SpringBootTest`)
- JPA queries (test with real DB in integration tests, not mocks)
- Third-party library behaviour

---

## §4 Security Standard

### OWASP Top 10 — Where Each Risk Lives in This Project

| OWASP | Risk | Where it applies | Mandatory mitigation |
|-------|------|-----------------|---------------------|
| A01 | Broken Access Control | Every controller endpoint | `@PreAuthorize` or SecurityConfig matchers; test each protected path returns 403 without ADMIN role |
| A02 | Cryptographic Failures | Token storage, magic link hash | SHA-256 for magic-link tokens; refresh tokens never stored raw; `JWT_SECRET` ≥256 bits |
| A03 | Injection | All `@RequestBody`, AI output inserted to DB | Bean Validation on every DTO; parameterised queries only (JPA); sanitise AI output with jsoup before persistence |
| A04 | Insecure Design | Auth flow, CSRF, session management | Stateless JWT; CSRF disabled intentionally (stateless, documented); no session cookies |
| A05 | Security Misconfiguration | Swagger, actuator, CORS | Swagger gated in prod; `/actuator/prometheus` internal only; CORS from env var |
| A06 | Vulnerable Components | All Gradle dependencies | OWASP dependency-check plugin; weekly CI run; CVE ≥7.0 is a blocker |
| A07 | Auth Failures | `/auth/**` endpoints | Rate limiting (phase 07); magic link single-use; refresh token rotation with theft detection |
| A08 | Software + Data Integrity | AI-generated content | Validate AI output against schema before persistence; never inject unvalidated AI text into SQL |
| A09 | Logging Failures | All services | Log security events (login, logout, token reuse); never log PII (email in log = bug); use structured logging |
| A10 | SSRF | URL ingestion in `/admin/topics` | Validate URL scheme (https only); block RFC 1918 / localhost before HTTP fetch |

### When to Write to THREAT-LOG.md
Write a THREAT-LOG entry when:
- A new security-relevant decision is made (algorithm choice, trust boundary added)
- A known risk is accepted (document the acceptance reason)
- A vulnerability is found, even if outside the current phase scope
- A Phase 07 security fix is implemented (mark the corresponding KNOWN RISKS row as FIXED)

### Security Non-Negotiables
- Never log email addresses, tokens, or any PII
- Never hardcode secrets — all via environment variables
- Never trust AI output without schema validation + sanitisation
- Never store raw tokens — always hash before persistence
- Never make HTTP requests to user-supplied URLs without SSRF mitigation
- Never disable auth on a protected endpoint "temporarily"

---

## §5 The Logging System

Every agentic/ file has a specific purpose and specific trigger conditions.

| File | Purpose | Write when |
|------|---------|-----------|
| `ROADMAP.md` | Phase status and sequence | Phase status changes (PENDING→IN PROGRESS→DONE) |
| `phases/phase-XX-*.md` | Task checklist + design detail | Task completed (check the box); task state changes |
| `HACKS.md` | Known shortcuts and workarounds | Any time code is written that is not the "right" solution |
| `ISSUES.md` | Open bugs and problems | Any discovered problem not fixed in the current session |
| `DECISIONS.md` | Non-obvious design choices | A choice was made that a future session might question |
| `THREAT-LOG.md` | Security posture log | Any security-relevant decision or finding (see §4) |
| `DAST-REPORT.md` | ZAP scan results | Phase 07 only — when ZAP is run |
| `ROADMAP_CHANGES.md` | Proposed deviations from plan | Before deviating from the roadmap |
| `adr/` | Full architectural decisions | See §7 — ADR trigger conditions |

**Writing to these files is not optional.** A session that implements code without updating
the logs is incomplete. The logs are the institutional memory.

---

## §6 Commit Format

All commits follow [Conventional Commits](https://www.conventionalcommits.org/).

### Format
```
<type>(<scope>): <subject>

<body — why, not what. What is in the diff.>

Phase: <number>  Task: <number>
```

### Types
| Type | Use for |
|------|---------|
| `feat` | New feature or capability |
| `fix` | Bug fix |
| `test` | Test-only changes |
| `refactor` | Code restructuring without behaviour change |
| `chore` | Build, CI, dependency, tooling changes |
| `docs` | Documentation only (ADRs, agentic/ files) |
| `security` | Security fix (use in addition to feat/fix when security-relevant) |

### Scopes (match bounded context)
`identity`, `topic`, `enrollment`, `struggle`, `peerreview`, `badge`, `ci`, `agentic`, `mobile`, `admin`

### Rules
- Subject line: imperative mood, lowercase after colon, no period, ≤72 chars
- Body: explain why, not what. The diff shows what.
- Footer: `Phase: XX  Task: X.Y` — always present
- No "Co-Authored-By" lines
- Plain `git commit -m "..."` — no heredoc

### Examples
```
feat(topic): add per-user shuffle seed for anti-cheat task instances

Shuffle seed is SHA-256(userId+taskId+enrollmentId) to ensure each
user receives a unique option ordering without storing per-user copies.

Phase: 02  Task: 2.2
```

```
fix(identity): store magic link token as SHA-256 hash not plaintext

Raw token in DB means a breach exposes all pending auth sessions.
Hash before storage, verify by re-hashing the presented token.

Phase: 01  Task: 1.4
```

---

## §7 ADR Protocol

### When to Write a Full ADR
Write an ADR (in `docs/adr/ADR-0XX.md`) when:
- An architectural boundary is established or changed
- A technology is added or replaced
- A security mechanism is designed
- A decision will be hard to reverse and affects multiple components
- A future session might reasonably make the opposite choice without knowing why

### When a DECISIONS.md Entry Is Enough
Write in `agentic/DECISIONS.md` when:
- The decision is tactical (naming, ordering, minor design choice)
- It only affects one file or one feature
- It is obviously correct in hindsight and has no realistic alternative
- It is temporary (will be revisited in a known future phase)

### Numbering Rule
The latest ADR in `docs/adr/` is ADR-016. The next ADR is **ADR-017**.
Number sequentially. Do not skip numbers. Do not reuse numbers.

### Template
Use `agentic/adr/TEMPLATE.md`. Copy to `docs/adr/ADR-0XX.md`. Fill every section.
No placeholders. A partial ADR is worse than no ADR.

### ADR Trigger Conditions Per Phase
Triggers noted in phase files with "ADR trigger: yes". When triggered, write the ADR
before implementing, not after.

---

## §8 Hard Stops

Stop immediately and report to the human when any of the following occurs:

1. **Task complete** — PR is open. Do not start the next task without instruction.
2. **Phase complete** — all tasks in the phase file are checked. Report to human.
3. **Secret required** — a new environment variable or API key is needed. Do not fake it.
4. **Roadmap deviation needed** — the plan cannot be executed as written. Write a
   `ROADMAP_CHANGES.md` entry and wait for approval.
5. **Flaky test** — a test fails intermittently. Document in ISSUES.md. Do not merge.
6. **Security issue found out of scope** — found a vulnerability not in the current task.
   Add it to THREAT-LOG.md and ISSUES.md. Do not fix it silently without telling the human.
7. **Compilation error not resolved in 2 attempts** — report the error, do not loop.
8. **Architectural ambiguity** — two reasonable approaches exist and the choice matters.
   Write a DECISIONS.md entry with both options and ask the human to choose.

---

## §9 What You Never Do

- Push to `main` directly
- Merge your own PR
- Commit secrets, tokens, or credentials (not even in tests)
- Skip writing tests before implementation (no exceptions)
- Log PII — email addresses, user IDs paired with personal data, raw tokens
- Insert unvalidated AI output into the database
- Make HTTP requests to user-supplied URLs without SSRF validation
- Modify an existing Flyway migration file
- Add a `@Suppress` annotation to silence a Detekt warning without a comment explaining why
- Break the dependency rule (§2) and call it a "temporary" exception
- Remove a test to make a build pass

---

## §10 Fresh Session Orient Checklist

Run these steps at the start of every session, in this order:

```bash
# 1 — confirm working directory
pwd
# expect: .../play4change

# 2 — check git state
git status
git log --oneline -5

# 3 — read the current phase
# (determine from ROADMAP.md which phase is IN PROGRESS)
cat agentic/ROADMAP.md

# 4 — read the active phase file
# (substitute the correct phase number)
cat agentic/phases/phase-XX-*.md

# 5 — check open issues
cat agentic/ISSUES.md

# 6 — check known hacks that affect your task
cat agentic/HACKS.md

# 7 — check open threats if security-relevant task
cat agentic/security/THREAT-LOG.md

# 8 — confirm the test suite is currently green
./gradlew :server:test --info 2>&1 | tail -20

# 9 — confirm Docker Compose is up if needed
docker compose ps
```

Only after all 9 steps are complete: begin the task.

---

## §11 Quick Reference

### Server
```bash
./gradlew :server:test                    # run all server tests
./gradlew :server:detektMain              # run Detekt linter
./gradlew :server:bootRun                 # run server locally (no Docker)
./gradlew :server:dependencyCheckAnalyze  # OWASP CVE scan
./gradlew :server:build                   # full build + test
```

### Docker Compose
```bash
docker compose up --build                 # build and start all services
docker compose up --build server          # rebuild and restart only the server
docker compose down -v                    # stop and remove volumes (full reset)
docker compose logs server -f             # follow server logs
docker compose ps                         # list running services
```

### Database (psql into Docker container)
```bash
docker compose exec postgres psql -U play4change -d play4change
```

### Mobile (composeApp)
```bash
./gradlew :composeApp:assembleDebug       # build Android APK
./gradlew :composeApp:testDebugUnitTest   # run mobile unit tests
```

### Admin Web (play4change-admin — separate repo)
```bash
npm install && npm run dev                # local dev server on :5173
npm run build                             # production build
```

### GitHub CLI
```bash
gh pr create --title "..." --body "..."   # open a PR
gh pr view                                # view current branch PR
gh pr list                                # list all open PRs
```
