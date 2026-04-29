# Phase 07 — Security Hardening & DAST

**Status:** `PENDING`
**Goal:** Systematic OWASP Top 10 pass, OWASP ZAP DAST run against Docker Compose stack,
all Critical/High findings remediated or risk-accepted with written justification,
rate limiting on `/auth/**`, Swagger gated in prod, actuator prometheus internal-only.

**Entry criteria:** Phases 01–06 all DONE.

---

## Tasks

### Task 7.1 — Rate limiting on /auth/**
- [ ] **What:** Add token-bucket rate limiting to all `/auth/**` endpoints to prevent
      brute-force magic link requests and credential stuffing on the OAuth endpoint.
- **Design constraints:**
  - Use Bucket4j with a Redis backend (Redis is already in the dependency graph).
    Bucket4j Spring Boot Starter (`com.giffing.bucket4j.spring.boot.starter`) simplifies
    filter configuration.
  - Rate limits (per IP address, using the `X-Forwarded-For` header when behind Nginx):
    - `POST /auth/magic-link`: 5 requests per 10 minutes per IP.
    - `GET /auth/verify`: 10 requests per 10 minutes per IP.
    - `POST /auth/oauth`: 10 requests per 10 minutes per IP.
    - `POST /auth/refresh`: 20 requests per 10 minutes per IP.
    - `DELETE /auth/logout`: 10 requests per 10 minutes per IP.
  - On rate limit exceeded: return `429 Too Many Requests` with body
    `{ "message": "Too many requests. Please wait before trying again." }` and
    `Retry-After: {seconds}` header.
  - The rate limit key is the client IP. When behind Nginx, extract from
    `X-Forwarded-For` first header. Validate that the extracted IP is not itself
    a private address (prevents IP spoofing via header injection from internal clients).
  - Rate limit configuration is in `application.yml` so it can be adjusted without
    a code change.
- **Tests required:**
  - `RateLimitTest` (`@SpringBootTest` with embedded Redis using Testcontainers):
    - 5 magic link requests from the same IP succeed.
    - The 6th request returns 429.
    - After the 10-minute window resets (use `ManualClock`), requests succeed again.
    - A request from a different IP is not rate-limited by another IP's count.
- **Security log requirement:** Update THREAT-LOG.md: mark "No rate limiting on /auth/**"
      (OWASP A07, High) as FIXED. Add the Bucket4j configuration details.
- **ADR trigger:** No — Bucket4j is a tactical implementation choice. Document in DECISIONS.md.
- **Exit criteria:** `RateLimitTest` passes. Manual test: 6 rapid curl requests to
      `/auth/magic-link` — the 6th returns 429.

---

### Task 7.2 — Gate Swagger UI in production
- [ ] **What:** Swagger UI must not be publicly accessible in the production Spring profile.
      In the `prod` profile, Swagger requires a valid ADMIN JWT. In other profiles, it remains
      open (for development convenience).
- **Design constraints:**
  - Add a `SecurityConfig` matcher rule that is conditional on the Spring profile:
    - Not `prod` profile: `requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()`
    - `prod` profile: `requestMatchers("/swagger-ui/**", "/v3/api-docs/**").hasRole("ADMIN")`
  - Use `@ConditionalOnExpression("'${spring.profiles.active}' != 'prod'")` or a
    `@Value("${spring.profiles.active}")` injection to drive the conditional behaviour.
    Do not use two separate `SecurityConfig` beans — use a single config with a conditional matcher.
  - Verify: in the `prod` profile, `GET /swagger-ui.html` without a JWT returns 401.
    With a USER JWT returns 403. With an ADMIN JWT returns 200.
- **Tests required:**
  - `SwaggerAccessControlTest` (`@SpringBootTest`):
    - In the default (non-prod) profile: Swagger is accessible without auth.
    - In the `prod` profile: Swagger returns 401 without JWT.
    - In the `prod` profile: Swagger returns 403 with USER JWT.
    - In the `prod` profile: Swagger returns 200 with ADMIN JWT.
- **Security log requirement:** Update THREAT-LOG.md: mark "Swagger public" (OWASP A05, Medium) as FIXED.
- **ADR trigger:** No.
- **Exit criteria:** `SwaggerAccessControlTest` passes. Starting the server with
      `--spring.profiles.active=prod` and accessing `/swagger-ui.html` without auth returns 401.

---

### Task 7.3 — Move /actuator/prometheus off public Nginx proxy
- [ ] **What:** The `/actuator/prometheus` endpoint must not be reachable via the public
      Nginx proxy. Move it to an internal port (`:9090`) accessible only within the
      Docker network.
- **Design constraints:**
  - Configure Spring Boot Actuator to expose management endpoints on a separate port:
    ```yaml
    management:
      server:
        port: 9090
    ```
    With this setting, `/actuator/prometheus` is available at `http://server:9090/actuator/prometheus`
    within the Docker network, but not on port 8080 (the externally proxied port).
  - Update the Nginx `nginx.conf` to not proxy requests to `/actuator/prometheus`
    (or any `/actuator/**` path except `/actuator/health` which remains public on port 8080).
  - Update the Prometheus scrape config in `docker-compose.yml` to scrape from
    `server:9090` instead of `server:8080`.
  - Verify: `curl http://localhost:8080/actuator/prometheus` returns 404.
    `curl http://localhost:9090/actuator/prometheus` returns 200 (only accessible within
    the Docker network — not reachable from the host unless port is exposed).
  - The `9090` management port must not be listed in the `ports:` section of the
    `server` service in `docker-compose.yml`.
- **Tests required:**
  - Manual verification only (see Human Checkpoint).
  - Add a note to `agentic/manual-testing/phase-07-recipe.md`.
- **Security log requirement:** Update THREAT-LOG.md: mark "/actuator/prometheus via Nginx"
      (OWASP A05, Medium) as FIXED.
- **ADR trigger:** No.
- **Exit criteria:** `curl http://localhost:8080/actuator/prometheus` returns 404 or 404.
      Prometheus in Grafana still receives metrics (scrape config updated to internal port).

---

### Task 7.4 — SSRF mitigation: block RFC 1918 addresses on URL ingestion
- [ ] **What:** The URL ingestion endpoint (`POST /admin/topics` with body `{ url }`)
      must validate the target URL before making an HTTP fetch. Block private IP ranges,
      loopback, and link-local addresses.
- **Design constraints:**
  - This was partially designed in Task 2.4. Confirm it is fully implemented:
    1. Parse URL with `java.net.URL`. Reject non-https schemes.
    2. Resolve host to IP via `InetAddress.getAllByName()` (all resolved addresses must pass).
    3. Reject if any resolved address is:
       - Loopback: `127.0.0.0/8`, `::1`
       - Private: `10.0.0.0/8`, `172.16.0.0/12`, `192.168.0.0/16`
       - Link-local: `169.254.0.0/16`, `fe80::/10`
       - Multicast: `224.0.0.0/4`
       - Reserved / documentation ranges
    4. Reject if DNS resolution returns 0 addresses (non-existent host).
    5. Set connection timeout 10s, read timeout 30s on the HTTP client making the fetch.
  - If Task 2.4 already implemented this, audit the implementation against the full
    list above and add any missing cases. Write or update the `UrlSsrfValidatorTest`.
  - The validator is a domain service (`UrlValidator`) — it has no Spring dependency.
    It is injected into the topic creation service via a port interface.
- **Tests required:**
  - `UrlSsrfValidatorTest` (confirm or extend from Task 2.4):
    - `http://` rejected (scheme).
    - `https://localhost` rejected.
    - `https://127.0.0.1` rejected.
    - `https://10.0.0.1` rejected.
    - `https://172.16.0.1` rejected.
    - `https://192.168.1.1` rejected.
    - `https://169.254.1.1` rejected.
    - `https://[::1]` rejected.
    - `https://nonexistent.tld.invalid` rejected (DNS fails).
    - `https://example.com` accepted.
- **Security log requirement:** Update THREAT-LOG.md: mark "SSRF on /admin/topics URL ingestion"
      (OWASP A10, High) as FIXED.
- **ADR trigger:** No.
- **Exit criteria:** All 10 `UrlSsrfValidatorTest` cases pass.

---

### Task 7.5 — Input validation audit: every @RequestBody and @RequestParam
- [ ] **What:** Read every controller class in the server. For each `@RequestBody` parameter,
      confirm that the DTO has Bean Validation annotations (`@NotNull`, `@NotBlank`, `@Size`,
      `@Pattern` as appropriate). For each `@RequestParam`, confirm it is validated or has
      a documented reason why it is not. Fix any missing validations.
- **Design constraints:**
  - Every `@RequestBody` DTO must have `@Valid` on the parameter in the controller.
  - Every field in a `@RequestBody` DTO that is required must have `@NotNull` or `@NotBlank`.
  - String fields that have a maximum safe length must have `@Size(max=N)`.
    If no max is defined, add one (use the domain model's constraint as the source of truth).
  - For fields that accept free text from users (task report reason, question text):
    `@Size(max=500)` for short text, `@Size(max=5000)` for long text.
  - A `@ControllerAdvice` handler for `MethodArgumentNotValidException` must return
    a structured error listing which fields failed validation.
    Check `AuthExceptionHandler.kt` — if this handler is not there, add it.
  - `@RequestParam` values used in queries must be validated for type (Spring handles
    this for primitives; add explicit validation for strings used in `LIKE` queries to
    prevent wildcard injection).
  - Write a findings table in ISSUES.md: for each DTO class, list whether it was
    already valid, needed fixes, or was found to be acceptable without validation.
- **Tests required:**
  - For each controller that was fixed, add or extend the `@WebMvcTest`:
    - Sending a request body with missing required fields returns 400 with a validation error list.
  - Regression: existing tests must still pass.
- **Security log requirement:** Add to THREAT-LOG.md: "Input validation audit complete.
      All @RequestBody DTOs have @Valid and field-level Bean Validation. Finding summary
      in ISSUES.md." OWASP A03.
- **ADR trigger:** No.
- **Exit criteria:** Every `@RequestBody` controller parameter has `@Valid`. The validation
      audit findings table is in ISSUES.md. No validation gap remains unfixed or undocumented.

---

### Task 7.6 — OWASP ZAP DAST run
- [ ] **What:** Run the OWASP ZAP baseline scan against the local Docker Compose stack.
      Document all findings in `agentic/security/DAST-REPORT.md`.
- **Design constraints:**
  - The ZAP scan target is `http://localhost:8080` (the Nginx proxy).
  - Use the ZAP Docker image: `ghcr.io/zaproxy/zaproxy:stable`.
  - Run the baseline scan (passive + active, no spider on authenticated endpoints):
    ```bash
    docker run --network host ghcr.io/zaproxy/zaproxy:stable \
      zap-baseline.py -t http://localhost:8080 \
      -r zap-report.html -J zap-report.json
    ```
  - Save `zap-report.html` and `zap-report.json` as CI artifacts (do not commit them —
    add to `.gitignore`). Their findings are transcribed to `DAST-REPORT.md`.
  - For each finding, record in `DAST-REPORT.md`: Risk level, ZAP alert name,
    affected URL, description, evidence, remediation applied (or risk-acceptance justification).
- **Tests required:** None — this is a tooling and documentation task.
- **Security log requirement:** The DAST-REPORT.md is the log for this task.
- **ADR trigger:** No.
- **Exit criteria:** ZAP scan completes without errors. All findings are recorded in
      DAST-REPORT.md with severity, URL, and remediation status.

---

### Task 7.7 — Remediate all Critical and High ZAP findings
- [ ] **What:** For every Critical or High finding from the ZAP scan (Task 7.6),
      implement a fix. For Medium findings, either fix or provide a written
      risk-acceptance justification in DAST-REPORT.md.
- **Design constraints:**
  - **Critical findings:** Must be fixed before this phase is DONE. No exceptions.
  - **High findings:** Must be fixed. If a fix is technically infeasible (e.g. a false
    positive), document the finding type, the evidence that it is a false positive,
    and tag it as "FALSE POSITIVE — accepted" in DAST-REPORT.md.
  - **Medium findings:** Fix if the fix is low-effort. Accept if the risk is justified.
    Justification must include: what the risk actually is in this deployment context,
    why it is acceptable, and who approved the acceptance (operator name + date).
  - **Low findings:** Acknowledged in DAST-REPORT.md. No mandatory fix.
  - Common findings and their standard mitigations for this stack:
    - "X-Content-Type-Options header missing": add `X-Content-Type-Options: nosniff` to Nginx config.
    - "X-Frame-Options header not set": add `X-Frame-Options: DENY` to Nginx config.
    - "Content Security Policy header not set": add a CSP header to Nginx config for the admin web.
    - "Missing Anti-clickjacking Header": covered by X-Frame-Options.
- **Tests required:**
  - Re-run ZAP after each batch of fixes. Document the before/after finding count in DAST-REPORT.md.
  - Confirm `./gradlew :server:test` still passes after Nginx config changes.
- **Security log requirement:** Update THREAT-LOG.md with a "Phase 07 DAST pass complete"
      entry including the finding counts (Critical: 0 remaining, High: 0 remaining, Medium: N accepted).
- **ADR trigger:** No.
- **Exit criteria:** Zero Critical findings in DAST-REPORT.md without a fix. Zero High
      findings without a fix or documented false-positive justification.

---

### Task 7.8 — Manual test recipe for Phase 07
- [ ] **What:** Write the full end-to-end manual test recipe for Phase 07 in
      `agentic/manual-testing/phase-07-recipe.md`.
- **Design constraints:** Must include the ZAP scan command, the rate limit test,
      the Swagger gate test, and the actuator internal port test.
- **Tests required:** None.
- **Security log requirement:** None.
- **ADR trigger:** No.
- **Exit criteria:** File exists and is accurate.

---

## Exit Criteria (Phase Level)

All 8 tasks are checked off. The following is true:
- Rate limiting on `/auth/**` is in place and tested.
- Swagger requires ADMIN JWT in the `prod` profile.
- `/actuator/prometheus` is not reachable on the public port.
- SSRF mitigation blocks all RFC 1918 and loopback URLs.
- All `@RequestBody` DTOs have field-level validation.
- ZAP scan has zero Critical or unaddressed High findings.
- DAST-REPORT.md is complete with all findings documented.

---

## Human Checkpoint

Before marking Phase 07 DONE:

**1. Rate limiting:**
```bash
for i in {1..6}; do
  curl -s -o /dev/null -w "Request $i: %{http_code}\n" \
    -X POST -H "Content-Type: application/json" \
    -d '{"email":"test@test.com"}' \
    http://localhost:8080/auth/magic-link
done
```
Expected: Requests 1–5 return 202. Request 6 returns 429.

**2. Swagger gate in prod:**
```bash
SPRING_PROFILES_ACTIVE=prod ./gradlew :server:bootRun &
curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/swagger-ui.html
```
Expected: 401.

**3. Actuator internal-only:**
```bash
curl http://localhost:8080/actuator/prometheus
```
Expected: 404 (not found at public port).
```bash
# From within the Docker network (exec into a container):
docker compose exec server curl http://localhost:9090/actuator/prometheus | head -5
```
Expected: Prometheus metric lines.

**4. ZAP findings:**
Open `DAST-REPORT.md`. Confirm:
- Column "Critical" has 0 open findings.
- Column "High" has 0 open findings (or 0 non-false-positive findings).

If any check fails, Phase 07 is not done.
