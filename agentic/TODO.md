# Play4Change — Deferred Items

## Format

Items that are known, intentionally deferred, and not scheduled in the current roadmap.
These are not forgotten — they are consciously out of scope for now with a documented reason.

When an item moves into an active phase, remove it from here and create a task in the
relevant phase file.

**Entry format:**
```
## [ID] — [Short title]

**What:** [What the feature or fix is]
**Why deferred:** [Specific reason — competing priority, missing dependency, out of deadline scope]
**Trigger to re-evaluate:** [What event or condition would bring this back into scope]
**Risk if not done:** [What happens if this is permanently skipped]
```

---

## D01 — Facebook OAuth audience verification

**What:** Verify Facebook access tokens using `GET /debug_token` with `FACEBOOK_APP_ID` and
`FACEBOOK_APP_SECRET` to confirm the token was issued for Play4Change's Facebook App.
Currently, any valid Facebook access token from any app is accepted.

**Why deferred:** `FACEBOOK_APP_ID` and `FACEBOOK_APP_SECRET` environment variables are not
configured. The Meta developer app configuration is incomplete. Documented in ADR-016 (G4).

**Trigger to re-evaluate:** When Facebook OAuth is being tested in production and
`FACEBOOK_APP_ID` is available as an environment variable.

**Risk if not done:** Any Facebook user can authenticate with a token issued to any other
Facebook application. This is a real vulnerability in a production deployment with Facebook OAuth.
Medium severity (THREAT-LOG.md R09).

---

## D02 — SSRF mitigation on URL ingestion

**What:** Validate user-supplied URLs in `POST /admin/topics` by resolving the host and
blocking RFC 1918, loopback, and link-local IP ranges before making the HTTP fetch.

**Why deferred:** This is scheduled in Phase 07, Task 7.4. It is listed here because it
was identified as a known risk in Phase 01 and partially designed in Phase 02, but full
implementation is in Phase 07.

**Trigger to re-evaluate:** Phase 07 begins. Remove this entry when Task 7.4 is done.

**Risk if not done:** An admin user could trigger server-side HTTP requests to internal
network services (metadata endpoints, other Docker containers, etc.). High severity.

---

## D03 — Swagger UI gated in production

**What:** Require ADMIN JWT to access `/swagger-ui.html` and `/v3/api-docs/**` when the
`prod` Spring profile is active.

**Why deferred:** Scheduled in Phase 07, Task 7.2.

**Trigger to re-evaluate:** Phase 07 begins. Remove this entry when Task 7.2 is done.

**Risk if not done:** API schema publicly exposed. Medium severity.

---

## D04 — /actuator/prometheus internal-only

**What:** Move the Prometheus scrape endpoint off the public Nginx port to an internal
management port (`:9090`) accessible only within the Docker network.

**Why deferred:** Scheduled in Phase 07, Task 7.3.

**Trigger to re-evaluate:** Phase 07 begins. Remove this entry when Task 7.3 is done.

**Risk if not done:** Internal metrics (timing, counts, gauge values) publicly accessible.
Medium severity.

---

## D05 — Rate limiting on /auth/**

**What:** Token-bucket rate limiting per IP on all `/auth/**` endpoints using Bucket4j with
Redis backend. Limits: 5/10min on magic link, 10/10min on verify and OAuth.

**Why deferred:** Scheduled in Phase 07, Task 7.1.

**Trigger to re-evaluate:** Phase 07 begins. Remove this entry when Task 7.1 is done.

**Risk if not done:** `/auth/magic-link` can be used to spam any email address.
`/auth/oauth` accepts unlimited verification attempts. High severity.

---

## D06 — RestTemplate timeout on JWKS endpoint

**What:** Set connection timeout (5s) and read timeout (10s) on the `RestTemplate` used to
fetch Google's JWKS endpoint (`https://www.googleapis.com/oauth2/v3/certs`).

**Why deferred:** Low priority — the Google JWKS endpoint is highly reliable. A hung call
would block one request thread, not the server. Scheduled in Phase 07 as part of the
security hardening pass.

**Trigger to re-evaluate:** Phase 07 begins. Fix as part of Task 7.7 (catch-all hardening).

**Risk if not done:** If Google's JWKS endpoint hangs (network issue, DNS failure),
the OAuth verify call blocks the thread until the JVM socket timeout (default: OS-level,
potentially minutes). Low severity in practice.

---

## D07 — Desktop KMP target

**What:** Add a macOS/Windows desktop target to the KMP `composeApp` module using
Compose Desktop.

**Why deferred:** The current priority is Android and iOS for the demo. Desktop adds
build complexity (separate signing, different window management) without demo value.
The Compose Desktop API is still evolving.

**Trigger to re-evaluate:** A stakeholder specifically requests a desktop client, or the
iOS/Android implementation is stable and there is project timeline remaining.

**Risk if not done:** No desktop access to the learner interface. Low priority for the
current demo scope.

---

## D08 — Kubernetes migration

**What:** Migrate the Docker Compose deployment to Kubernetes (AWS EKS, GKE, or self-hosted).

**Why deferred:** Docker Compose on a single-node VPS is sufficient at the current scale
(1–5 concurrent demo users). The cost and operational overhead of EKS is not justified.
ADR-009 documents the full migration path and trigger conditions.

**Trigger to re-evaluate:** Any one of the four trigger conditions in ADR-009 is met:
(1) batch/serving resource contention measurable in P95 latency,
(2) multi-institution deployment requiring namespace isolation,
(3) zero-downtime deployment SLA requirement,
(4) LLM inference becomes the latency bottleneck.

**Risk if not done:** Single point of failure (one VPS). Restart gap during deployments.
Acceptable for the current demo deployment. Not acceptable for a production service with SLA.
