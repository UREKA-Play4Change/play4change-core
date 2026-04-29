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

---

## Known Risks Requiring Action

| ID | OWASP | Description | Severity | Status | Planned Phase | Notes |
|----|-------|-------------|----------|--------|--------------|-------|
| R01 | A04 Insecure Design | CSRF disabled — acceptable for stateless JWT API | Low | ACCEPTED | Phase 01 | Documented. Stateless JWT means CSRF tokens add no security benefit. The accept decision is intentional, not oversight. |
| R02 | A05 Security Misconfiguration | Swagger UI publicly accessible without auth | Medium | OPEN | Phase 07 | Any user can browse the API schema. Low risk for a POC; must be gated before any public deployment. |
| R03 | A05 Security Misconfiguration | `/actuator/prometheus` reachable via public Nginx proxy | Medium | OPEN | Phase 07 | Prometheus metrics expose internal timing and count data. Not a critical risk but should not be public. |
| R04 | A03 Injection | `Name.kt` and `Password.kt` value object stubs — accept any string | High | OPEN | Phase 01 | Invalid names can propagate through the domain. Fix: proper validation with Arrow Either. |
| R05 | A07 Auth Failures | No rate limiting on `/auth/**` endpoints | High | OPEN | Phase 07 | `/auth/magic-link` can be used to spam any email address. `/auth/oauth` accepts any number of verification attempts. |
| R06 | A10 Server-Side Request Forgery | SSRF on `/admin/topics` URL ingestion — no IP range validation | High | OPEN | Phase 07 | Admin can submit internal network URLs. Mistral fetcher may reach internal services. Fix: validate and block RFC 1918 ranges before fetch. |
| R07 | A08 Software & Data Integrity | AI-generated content not validated before persistence | High | OPEN | Phase 02 | Mistral output is inserted into the database as-is. Malformed or injected content could reach learners. Fix: jsoup sanitisation + schema validation. |
| R08 | A04 Insecure Design | `RestTemplate` no timeout on JWKS endpoint fetch | Low | OPEN | Phase 07 | A slow or hung JWKS server could block auth threads. Default `RestTemplate` has no timeout. Fix: set connection and read timeouts. |
| R09 | A07 Auth Failures | Facebook OAuth token not app-verified (audience not checked) | Medium | DEFERRED | Not scheduled | Facebook access tokens accepted from any app. Full hardening requires `FACEBOOK_APP_ID` + `FACEBOOK_APP_SECRET`. Deferred per ADR-016 G4. |
| R10 | A09 Logging Failures | No secret scanning in CI — committed secrets enter git history permanently | High | OPEN | Phase 01 extension | gitleaks to be added to GitHub Actions as pre-build step per ADR-018. |
| R11 | A03 Injection | No security-specific SAST — Detekt does not detect Spring/JWT security anti-patterns | High | OPEN | Phase 07 Task 7.9 | SpotBugs + FindSecBugs to be added to server Gradle build per ADR-018. |
| R12 | A06 Vulnerable Components | Mobile SCA gap — composeApp and common modules have no CVE scanning | High | OPEN | Phase 07 Task 7.10 | OWASP dep-check to be extended to KMP modules per ADR-018. |
| R13 | A07 Auth Failures | Authenticated DAST gap — ZAP baseline covers unauthenticated surface only | Medium | OPEN | Phase 07 Task 7.6 extension | Authenticated ZAP scan with learner + admin JWT required per ADR-018. |
| R14 | A05 Security Misconfiguration | Missing CSP, HSTS, and Referrer-Policy headers in Nginx config | Medium | OPEN | Phase 07 Task 7.7 extension | Full header set defined in ADR-018. CSP unsafe-inline accepted as residual risk pending nonce migration. |
| R15 | A04 Insecure Design | No OWASP Threat Dragon model — trust boundaries documented in prose only | Low | OPEN | Phase 07 | threat-model.td to be created in agentic/security/ per ADR-018. |

---

## Phase 02 STRIDE Analysis

*(To be written at the start of Phase 02, Task 2.1)*

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

## Phase 04 Security Note

| Control | OWASP | Location | Note |
|---------|-------|----------|------|
| ✔ Mobile tokens in EncryptedSharedPreferences (Android) | A02 Cryptographic Failures | `EncryptedSharedPreferencesTokenStorage.kt` | AES256_SIV key + AES256_GCM value. Added Phase 04. |
| ✔ Mobile tokens in Keychain (iOS) | A02 Cryptographic Failures | `KeychainTokenStorage.kt` | kSecAttrAccessibleAfterFirstUnlock. Added Phase 04. |
| ✔ Device tokens revoked on user logout | A07 Auth Failures | `TokenService.logout()` — deletes all device_tokens for user | Added Phase 05. |

---

## Remediation Log

*(Populated as KNOWN RISKS are fixed)*

| Risk ID | Fixed in Phase | Commit | Summary |
|---------|--------------|--------|---------|
| R04 | Phase 01, Task 1.4 | — | Name.kt implements validation with Arrow Either. Password.kt deleted (unused). |

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
