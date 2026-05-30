# Play4Change — Security Threat Log

## Format Specification

Every entry in this file follows one of two formats: **ALREADY IMPLEMENTED** (controls that are
in place and verified) or **KNOWN RISKS** (risks that require action, tracking their status).

### Adding a new entry

**ALREADY IMPLEMENTED** — when a security control is confirmed present in the codebase:
```
| ✔ Short description | OWASP category | Where in codebase | ADR/commit reference |
```

**KNOWN RISKS** — when a risk is identified but not yet remediated:
```
| OWASP | Description | Severity | Status | Planned phase | Notes |
```

**Updating a KNOWN RISK row** — when a risk is fixed, change Status to `FIXED (phase XX)`.
Do not delete rows. The audit trail of known risks matters.

**When to add a STRIDE entry** — at the start of each phase that introduces a new
attack surface, a STRIDE analysis is added as a subsection.

---

## Initial Security Posture (documented Phase 01, Task 1.5)

### Already Implemented Controls

| Control | OWASP category | Location | Reference |
|---------|---------------|----------|-----------|
| ✔ Stateless JWT (no server-side session) | A04 Insecure Design | `SecurityConfig.kt` — `SessionCreationPolicy.STATELESS` | ADR-011 |
| ✔ 15-minute access token / 7-day refresh token | A07 Auth Failures | `TokenService.kt` — `ACCESS_TOKEN_EXPIRY`, `REFRESH_TOKEN_EXPIRY` | ADR-011 |
| ✔ Full JWT family invalidation on logout | A07 Auth Failures | `TokenService.revokeFamily()` — all tokens with same `family_id` revoked | ADR-016 G6 |
| ✔ Refresh token reuse detection (theft detection) | A07 Auth Failures | `TokenService` — `used=true` check triggers family revocation | ADR-011 |
| ✔ Magic link token stored as SHA-256 hash | A02 Cryptographic Failures | `MagicLinkService.kt` — raw token never persisted | ADR-016 G1 |
| ✔ Magic link token claim is atomic (TOCTOU fixed) | A04 Insecure Design | `MagicLinkTokenJpaRepository` — atomic CTE UPDATE | ADR-016 G2 |
| ✔ Magic link single-use, 15-minute TTL | A07 Auth Failures | `MagicLinkToken.isValid()` domain logic | ADR-011 |
| ✔ Google OAuth JWKS verification with 1-hour cache | A08 Software Integrity | `GoogleOAuthAdapter.kt` | ADR-011 |
| ✔ Google OAuth audience validation — blank client ID is a hard error | A08 Software Integrity | `GoogleOAuthAdapter.kt` — throws `IllegalStateException` if blank | ADR-016 G3 |
| ✔ Role-based access: `/admin/**` requires `ROLE_ADMIN` | A01 Broken Access Control | `SecurityConfig.kt` — `hasRole("ADMIN")` matcher | ADR-011 |
| ✔ CORS configured from env var, not wildcard | A05 Security Misconfiguration | `SecurityConfig.kt` — `allowedOriginPatterns(corsOrigins)` | ADR-011 |
| ✔ All secrets via environment variables, no hardcoded secrets | A02 Cryptographic Failures | `application.yml` — all secrets use `${ENV_VAR}` | ADR-009 |
| ✔ Docker container runs as non-root user (`appuser`) | A05 Security Misconfiguration | `Dockerfile` — `USER appuser` | ADR-009 |
| ✔ Refresh tokens stored as SHA-256 hash, never raw | A02 Cryptographic Failures | `TokenService.kt` — `hashToken()` before persistence | ADR-011 |
| ✔ Public routes do not 401 on stale Bearer tokens | A07 Auth Failures | `JwtAuthFilter.kt` — exception caught on public routes | ADR-016 G7 |
| ✔ All auth error responses use unified `MessageResponse` shape | A09 Logging Failures | `AuthExceptionHandler.kt` — consistent error contract | ADR-016 G8 |
| ✔ Device tokens associated with userId; all tokens revoked on logout | A07 Auth Failures | `DeviceTokenRepositoryAdapter.deleteAllByUserId()` called in `AuthController` logout handlers | Phase 05, Task 5.1 |

---

## Known Risks Requiring Action

| ID | OWASP | Description | Severity | Status | Planned Phase | Notes |
|----|-------|-------------|----------|--------|--------------|-------|
| R01 | A04 Insecure Design | CSRF disabled — acceptable for stateless JWT API | Low | ACCEPTED | Phase 01 | Documented. Stateless JWT means CSRF tokens add no security benefit. The accept decision is intentional, not oversight. |
| R02 | A05 Security Misconfiguration | Swagger UI publicly accessible without auth | Medium | FIXED (Phase 07, Task 7.2) | Phase 07 | Gated behind `hasRole("ADMIN")` when `spring.profiles.active=prod`. Free in dev/default profiles. `SecurityConfig.kt` + `JwtAuthFilter.kt`. `SwaggerAccessControlTest` (4 tests) passes. |
| R03 | A05 Security Misconfiguration | `/actuator/prometheus` reachable via public Nginx proxy | Medium | FIXED (Phase 07, Task 7.3) | Phase 07 | `management.server.port: 9090` moves all actuator endpoints to internal port. Nginx no longer proxies `/actuator/**` except `/actuator/health` (proxied to port 9090). Prometheus scrape config updated to `server:9090`. Docker healthcheck updated to port 9090. Port 9090 not listed in server `ports:` section — internal only. |
| R04 | A03 Injection | `Name.kt` and `Password.kt` value object stubs — accept any string | High | FIXED (Phase 01, Task 1.4) | Phase 01 | Name.kt now validates: non-null, non-blank, 2–100 chars, no control characters. Password.kt deleted (unused). Fixed 2026-04-30. |
| R05 | A07 Auth Failures | No rate limiting on `/auth/**` endpoints | High | FIXED (Phase 07, Task 7.1) | Phase 07 | Bucket4j per-IP token-bucket filter applied to all `/auth/**` endpoints. Limits: `/auth/magic-link` 5/10 min, `/auth/verify` 10/10 min, `/auth/oauth` 10/10 min, `/auth/refresh` 20/10 min, `/auth/logout` 10/10 min. Returns 429 + `Retry-After` header. X-Forwarded-For validated — private IPs rejected to prevent header injection. See `RateLimitFilter.kt`, `RateLimitService.kt`. `RateLimitTest` (4 tests) passes. |
| R06 | A10 Server-Side Request Forgery | SSRF on `/admin/topics` URL ingestion — no IP range validation | High | PARTIALLY FIXED (Phase 02, Task 2.4) | Phase 07 | `UrlSsrfValidator` now rejects non-HTTPS schemes and RFC 1918 / loopback / link-local IPs via DNS resolution. Residual risk: DNS rebinding attack not prevented (see ADR-019). Full mitigation (socket-level IP check) tracked for Phase 07. |
| R07 | A08 Software & Data Integrity | AI-generated content not validated before persistence | High | FIXED (Phase 02, Task 2.4) | Phase 02 | `AiOutputSanitiser` (jsoup `parse().text()`) strips all HTML and decodes entities on all generated string fields before persistence. Schema validation with single retry prevents partial results. See ADR-019. |
| R08 | A04 Insecure Design | `RestTemplate` no timeout on JWKS endpoint fetch | Low | OPEN | Phase 07 | A slow or hung JWKS server could block auth threads. Default `RestTemplate` has no timeout. Fix: set connection and read timeouts. |
| R09 | A07 Auth Failures | Facebook OAuth token not app-verified (audience not checked) | Medium | DEFERRED | Not scheduled | Facebook access tokens accepted from any app. Full hardening requires `FACEBOOK_APP_ID` + `FACEBOOK_APP_SECRET`. Deferred per ADR-016 G4. |
| R10 | A09 Logging Failures | No secret scanning in CI — committed secrets enter git history permanently | High | FIXED (Phase 01, Task 1.7) | Phase 01 extension | gitleaks-action@v2 added to CI pre-build step. Full history scan clean. Suppressions in .gitleaks.toml. |
| R11 | A03 Injection | No security-specific SAST — Detekt does not detect Spring/JWT security anti-patterns | High | OPEN | Phase 07 Task 7.9 | SpotBugs + FindSecBugs to be added to server Gradle build per ADR-018. |
| R12 | A06 Vulnerable Components | Mobile SCA gap — composeApp and common modules have no CVE scanning | High | OPEN | Phase 07 Task 7.10 | OWASP dep-check to be extended to KMP modules per ADR-018. |
| R13 | A07 Auth Failures | Authenticated DAST gap — ZAP baseline covers unauthenticated surface only | Medium | OPEN | Phase 07 Task 7.6 extension | Authenticated ZAP scan with learner + admin JWT required per ADR-018. |
| R14 | A05 Security Misconfiguration | Missing CSP, HSTS, and Referrer-Policy headers in Nginx config | Medium | OPEN | Phase 07 Task 7.7 extension | Full header set defined in ADR-018. CSP unsafe-inline accepted as residual risk pending nonce migration. |
| R15 | A04 Insecure Design | No OWASP Threat Dragon model — trust boundaries documented in prose only | Low | FIXED | Phase 07 | threat-model.td created in agentic/security/. All Phase 01 and Phase 02 STRIDE threats pre-populated. Maintenance guide in THREAT-DRAGON-MAINTENANCE.md. |
| R16 | A03 Injection | No `@Valid` on `AuthController` `@RequestBody` params; `requestMagicLink` normalises but never validates email format — empty string or non-email string passes through to the DB and email port | Medium | OPEN | Phase 07 | Fix: add `@field:Email @field:NotBlank` to `MagicLinkRequest.email`; add `@Valid` to all `AuthController` `@RequestBody` parameters. Discovered Phase 01 Task 1.5 code review (2026-04-30). |
| R17 | A01 Broken Access Control | Badge endpoints access-controlled: `GET /profile/badges` requires any valid JWT; `GET /admin/topics/{id}/badges` requires ROLE_ADMIN. SecurityConfig also fixed to return 401 (not 403) for unauthenticated requests by adding explicit `AuthenticationEntryPoint`. | Low | FIXED (Phase 02, Task 2.7) | Phase 02 | `BadgeController` and `AdminBadgeController` covered by `BadgeControllerTest` (`@WebMvcTest`). |

---

## Phase 02 STRIDE Analysis

### Attack Surfaces in Scope
- URL ingestion endpoint (`POST /admin/topics`)
- Mistral API call (outbound)
- AI output persistence
- Per-user task instance generation
- Badge issuance

| Attack Surface | STRIDE Category | Threat | OWASP | Severity | Mitigation Task |
|----------------|----------------|--------|-------|----------|----------------|
| URL ingestion | Spoofing | Admin provides a URL that resolves to an internal service, fetching internal content | A10 SSRF | High | Task 2.4, Task 7.4 |
| URL ingestion | Denial of Service | Admin provides a URL to a slow server, blocking the generation thread pool | A04 | Low | Task 2.4 — connection + read timeout |
| Mistral API call | Tampering | Prompt injection: malicious content in the URL page manipulates the Mistral system prompt | A08 | Medium | Task 2.4 — output schema validation |
| AI output persistence | Tampering | Mistral returns HTML/script content that is stored and later rendered as XSS | A03 | High | Task 2.4 — jsoup sanitisation |
| AI output persistence | Information Disclosure | Mistral repeats sensitive content from the ingested URL back into the database | A08 | Low | Task 2.4 — schema validation strips unexpected fields |
| Per-user instance generation | Repudiation | No audit trail linking which user received which task instance | A09 | Low | Accepted — task assignment table records userId+taskId |
| Per-user instance generation | Spoofing | User A crafts the correct seed for User B's instance to predict answers | A04 | Low | Seed includes enrollmentId (not guessable by other users) |
| Badge issuance | Elevation of Privilege | Badge issuance endpoint called without completing all tasks | A01 | Medium | BadgeIssuanceService validates task completion before issuing |
| Badge issuance | Tampering | Double-issuance of a badge by race condition | A04 | Low | Task 2.6 — unique DB constraint on (userId, microCompetenceId) |

---

## Phase 06 Security Note — Admin Web Token Storage

| Control | OWASP | Location | Note |
|---------|-------|----------|------|
| ✔ Admin web access token in sessionStorage (not localStorage) | A02 Cryptographic Failures | `apiClient.ts` — `sessionStorage.setItem(SESSION_ACCESS_KEY, ...)` | Cleared on tab close. Not accessible cross-origin. XSS risk lower than localStorage. |
| ⚠ Refresh token in JS-set cookie (not httpOnly) | A02 Cryptographic Failures | `apiClient.ts` — `setCookie(COOKIE_REFRESH_KEY, ...)` | SameSite=Strict; Secure on HTTPS. NOT httpOnly — XSS can read it. Accepted for Phase 06 (internal admin tool, not public-facing). httpOnly server-set cookie deferred to Phase 07. See DECISIONS.md [2026-05-29] [admin-web]. |

---

## Phase 06 Security Note — User Promotion

| Control | OWASP | Location | Note |
|---------|-------|----------|------|
| ✔ User promotion endpoint restricted to ADMIN role | A01 Broken Access Control | `AdminUserController` — `/admin/users/{userId}/promote` under `/admin/**` requires ROLE_ADMIN | Demotion not possible via API — requires direct DB intervention, which is intentional. OWASP A01. |

---

## Phase 04 Security Note

| Control | OWASP | Location | Note |
|---------|-------|----------|------|
| ✔ Mobile tokens in EncryptedSharedPreferences (Android) | A02 Cryptographic Failures | `EncryptedSharedPreferencesTokenStorage.kt` | AES256_SIV key + AES256_GCM value. Added Phase 04. |
| ✔ Mobile tokens in Keychain (iOS) | A02 Cryptographic Failures | `KeychainTokenStorage.kt` | kSecAttrAccessibleAfterFirstUnlock. Added Phase 04. |
| ✔ Device tokens revoked on user logout | A07 Auth Failures | `TokenService.logout()` — deletes all device_tokens for user | Added Phase 05. |

---

## Anti-Cheat Shuffle (Phase 02, Task 2.2)

| Control | OWASP category | Location | Reference |
|---------|---------------|----------|-----------|
| ✔ Deterministic per-user option shuffle prevents answer-sharing between users | A04 Insecure Design | `TaskShuffleSeed.kt` — seed = SHA-256(userId+taskId+enrollmentId), first 8 bytes as Long for `java.util.Random` | Phase 02 Task 2.2 |

---

## OWASP Dependency-Check State (Phase 01, Task 1.6)

| Status | Notes |
|--------|-------|
| Plugin added | `id("org.owasp.dependencycheck") version "9.0.10"` in `server/build.gradle.kts` |
| CI workflow | `.github/workflows/dependency-check.yml` — weekly Monday 6 AM UTC + `workflow_dispatch` |
| Suppression file | `server/dependency-check-suppression.xml` — no suppressions yet; first real scan may add false-positives |
| CVE threshold | `failBuildOnCVSS = 7.0f` — CVE ≥7.0 blocks the build |
| Local scan status | **Requires NVD_API_KEY** — NVD API v2 rate-limits unauthenticated bulk downloads (see HACKS.md H04). CI workflow supplies the key via GitHub secret `NVD_API_KEY`. |
| Current dependency CVE state | Not yet assessed — full scan requires seeded NVD database. No suppressions or known HIGH CVEs have been accepted. First CI run will establish baseline. |

---

## Remediation Log

*(Populated as KNOWN RISKS are fixed)*

| Risk ID | Fixed in Phase | Commit | Summary |
|---------|--------------|--------|---------|
| R04 | Phase 01, Task 1.4 | cc798c4 | Name.kt validates non-null/blank/2–100 chars/no control chars. Password.kt deleted (unused). |
| R10 | Phase 01, Task 1.7 | 2ff82e1 | gitleaks CLI 8.18.4 added to CI pre-build step. fetch-depth: 0 enables full history scan. First scan found 6 findings in demo/*.http — assessed as expired development artefacts (magic-link tokens: 15-min single-use TTL; refresh tokens: rotated/revoked). Suppressed via path allowlist in .gitleaks.toml with full justification. No live credentials were present. |
| R07 | Phase 02, Task 2.4 | — | AiOutputSanitiser applies jsoup parse().text() to all AI-generated string fields. Schema validation retry prevents malformed partial content. NoOpLanguageGenerationAdapter replaced with real LanguageGenerationAdapter. See ADR-019. |

---

## ADR-018 Gap Register (documented April 2026)

Six toolchain gaps were identified by a full security posture review and recorded
in ADR-018. Each gap maps to a KNOWN RISK row above and a concrete phase task.

| Gap ID | Description | THREAT-LOG row | Phase task |
|--------|-------------|---------------|------------|
| G1 | No Threat Dragon model | R15 | Phase 07 |
| G2 | Authenticated DAST missing | R13 | Phase 07 Task 7.6 extension |
| G3 | Security SAST (FindSecBugs) missing | R11 | Phase 07 Task 7.9 |
| G4 | CSP / HSTS / Referrer-Policy missing | R14 | Phase 07 Task 7.7 extension |
| G5 | Mobile SCA (composeApp/common) missing | R12 | Phase 07 Task 7.10 |
| G6 | Secret scanning (gitleaks) missing | R10 | Phase 01 extension |

See ADR-018 for alternatives considered and residual risks accepted.
