# Play4Change — DAST Report (OWASP ZAP)

## Overview

This file records the results of OWASP ZAP dynamic application security testing (DAST)
runs against the Play4Change server running in the local Docker Compose stack.

DAST runs are performed in **Phase 07** (security hardening). This file is populated
during Task 7.6 and updated after remediations in Task 7.7.

---

## How to Run ZAP Against the Local Stack

### Prerequisites
- Docker installed and running
- Play4Change Docker Compose stack running at `http://localhost:8080`
  ```bash
  docker compose up --build
  curl http://localhost:8080/actuator/health  # expect {"status":"UP"}
  ```

### Baseline Scan (passive + active, unauthenticated)

```bash
docker run --network host \
  -v $(pwd)/zap-output:/zap/wrk/:rw \
  ghcr.io/zaproxy/zaproxy:stable \
  zap-baseline.py \
  -t http://localhost:8080 \
  -r /zap/wrk/zap-report.html \
  -J /zap/wrk/zap-report.json \
  -l WARN
```

The HTML and JSON reports are written to `./zap-output/` on the host.

**Do not commit these report files.** Add to `.gitignore`:
```
zap-output/
zap-report.html
zap-report.json
```

Transcribe all WARN and FAIL findings into the findings table below.

### Authenticated Scan (Phase 07 — run after baseline)

ZAP authenticated scan configuration requires:
1. Obtain a valid ADMIN JWT (via magic link or test fixture)
2. Pass the JWT as a ZAP request header:
   ```bash
   docker run --network host \
     -v $(pwd)/zap-output:/zap/wrk/:rw \
     ghcr.io/zaproxy/zaproxy:stable \
     zap-full-scan.py \
     -t http://localhost:8080 \
     -r /zap/wrk/zap-auth-report.html \
     -H "Authorization: Bearer ${ADMIN_JWT}"
   ```

---

## How to Interpret ZAP Severity Levels

| ZAP Level | Definition | Required action |
|-----------|-----------|----------------|
| **Critical** | Immediate compromise possible — remote code execution, authentication bypass, data exfiltration | Must fix before phase is DONE. No exceptions. |
| **High** | Significant risk — SQL injection, XSS, SSRF, broken auth | Must fix or document as verified false positive. |
| **Medium** | Moderate risk — missing security headers, clickjacking, information disclosure | Fix if low-effort; risk-accept with written justification if not. |
| **Low** | Minor risk — verbose error messages, non-essential information disclosure | Acknowledge; fix opportunistically. |
| **Informational** | No risk — configuration notes, version disclosure | No action required. |

---

## Must-Fix vs Risk-Accept Decision Criteria

**Must fix:**
- Any finding with a real, reproducible exploit path
- Any finding where user data could be exfiltrated
- Any Critical or High finding that is not a false positive

**Risk-accept (with justification):**
- False positives: the finding alert fires but the actual vulnerability does not apply
  (e.g. ZAP reports "Missing Anti-CSRF Token" on a stateless JWT API — CSRF does not apply)
- Low business impact: the finding is technically valid but the risk in this deployment
  context is negligible (document the context explicitly)
- Deferred: the finding is real but the fix is out of scope for this phase
  (must be tracked in ISSUES.md and addressed before any production deployment)

**Justification format for each risk-accepted finding:**
```
**Finding:** [ZAP alert name]
**Why accepted:** [Specific reason — not "low risk" but why it is low risk HERE]
**Approver:** [Operator name, date]
**Follow-up:** [Phase/ticket where this will be re-evaluated, or "none — permanent accept"]
```

---

## Findings Table

*(Populated in Phase 07, Task 7.6 — after the ZAP scan runs)*

### Run 1 — Baseline Scan (unauthenticated)

**Date:** 2026-05-30
**Stack version:** dbe3d21
**ZAP version:** 2.17.0 (ghcr.io/zaproxy/zaproxy:stable)
**Target:** http://localhost:8080
**Summary:** FAIL-NEW: 0 | WARN-NEW: 1 | PASS: 66

| # | Risk Level | ZAP Alert (ID) | Affected URL | Status | Notes |
|---|-----------|---------------|-------------|--------|-------|
| 1 | Informational | Non-Storable Content [10049] | http://localhost:8080, http://localhost:8080/robots.txt | ACCEPTED | Server returns `no-store` on 401 responses — correct and intentional. Cache-Control: no-store is a security best practice for authenticated endpoints. Not a vulnerability. |

**Finding detail — Non-Storable Content:**
- **Risk code:** 0 (Informational)
- **Confidence:** Medium
- **Evidence:** `no-store` in Cache-Control response header
- **Why accepted:** The ZAP scanner only reached 2 URLs (both returned HTTP 401 Unauthorized because the entire API requires a JWT). The `no-store` directive is intentionally set by Spring Security on 401 responses to prevent caching of authentication challenges. This is correct behaviour per RFC 7234 and Spring Security defaults. No action required.
- **Approver:** Radesh Govind, 2026-05-30
- **Follow-up:** None — permanent accept

### Run 2 — Post-Remediation Scan

*(To be run after Task 7.7 remediations. Compare against Run 1 findings.)*

**Date:** *(to be filled)*
**Findings resolved:** *(count)*
**Findings remaining:** *(count — must be 0 Critical, 0 High)*

| # | Risk Level | ZAP Alert | Affected URL | Resolution | Commit |
|---|-----------|-----------|-------------|-----------|--------|
| — | — | *(populated after Phase 07 remediation)* | — | — | — |

---

## Known False Positives (Pre-Populated)

The following findings are expected from ZAP and are documented false positives for this stack.
When ZAP reports these, mark them as FALSE POSITIVE in the findings table with this reference.

| ZAP Alert | Why it is a false positive for this stack |
|-----------|------------------------------------------|
| "Anti-CSRF Tokens Not Used" | Play4Change uses stateless JWT authentication. CSRF attacks require session cookies. Stateless JWT is not vulnerable to CSRF. SessionCreationPolicy.STATELESS in SecurityConfig. ADR-011. |
| "X-Powered-By Header" | Spring Boot does not set `X-Powered-By` by default. If ZAP reports this, check the Nginx config — it should not forward this header. |
| "Cookie Without SameSite Attribute" | Play4Change does not use session cookies. If ZAP flags a cookie, identify which one and assess whether SameSite applies. |
| "Content Security Policy Header Not Set" (server API) | The server API returns JSON, not HTML. CSP is relevant for HTML responses. The admin web (React) must have CSP — add it to the Nginx config for the `:5173` or proxied admin route. |

---

## Remediation Summary

*(Updated after Task 7.6 — no Critical/High/Medium findings found)*

| Metric | Before Phase 07 | After Phase 07 |
|--------|----------------|---------------|
| Critical findings | — | 0 |
| High findings | — | 0 |
| Medium findings | — | 0 |
| Low findings | — | 0 |
| Informational findings | — | 1 (Non-Storable Content — accepted, no action) |
| Risk-accepted (with justification) | — | 1 (see Run 1 finding detail) |

**Phase 07 DAST pass status:** COMPLETE — 2026-05-30
**Authenticated scans:** See `DAST-REPORT-AUTHENTICATED.md`

### Accepted Residual Risk — CSP unsafe-inline

**Finding:** Content-Security-Policy with `style-src 'unsafe-inline'` in SPA `/` location
**Why accepted:** The Vite/React build inlines CSS styles by default. Removing `'unsafe-inline'`
requires migrating to CSS modules with nonce-based CSP. This is a planned improvement but
out of scope for Phase 07.
**Approver:** Radesh Govind, 2026-05-30
**Follow-up:** CSP nonce migration deferred to a post-Phase-07 hardening ticket in ISSUES.md
