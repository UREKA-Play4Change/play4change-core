# Phase 07 Manual Test Recipe
## Security Hardening: Full Verification Walkthrough

This recipe verifies all Phase 07 human-checkpoint criteria.
Execute the sections in order against the local Docker Compose stack.

---

## Prerequisites

1. Start the full stack:
   ```bash
   docker compose up --build -d
   ```
2. Confirm server health:
   ```bash
   curl -s http://localhost:8080/actuator/health
   # Expected: {"status":"UP"}
   ```
3. Docker must be running (ZAP and the stack both use Docker).

---

## Section 1 — Rate Limiting on /auth/** (Task 7.1)

**Goal:** Verify that excessive requests to `/auth/magic-link` are rate-limited.

1. Send 6 rapid POST requests to `/auth/magic-link`:
   ```bash
   for i in $(seq 1 6); do
     curl -s -o /dev/null -w "Request $i: %{http_code}\n" \
       -X POST http://localhost:8080/auth/magic-link \
       -H "Content-Type: application/json" \
       -d '{"email":"test@example.com"}'
   done
   ```
2. Expected: First 5 requests return `200` (or `202`). Request 6 returns `429 Too Many Requests`.
3. Wait 1 minute for the rate limit bucket to refill, then retry — the next request should return `200`.

---

## Section 2 — Swagger Gating in prod Profile (Task 7.2)

### 2a — Default profile: Swagger is accessible

1. Confirm the server is running in the **default** (non-prod) profile:
   ```bash
   curl -s http://localhost:8080/swagger-ui/index.html | grep -i "swagger"
   # Expected: HTML containing "swagger" (200 OK)
   ```

### 2b — prod profile: Swagger requires ADMIN JWT

This requires temporarily restarting the server with `SPRING_PROFILES_ACTIVE=prod`.

1. Restart the server container with prod profile:
   ```bash
   docker compose stop server
   SPRING_PROFILES_ACTIVE=prod docker compose up -d server
   ```
2. Access Swagger without auth — expect 401:
   ```bash
   curl -s -o /dev/null -w "%{http_code}\n" http://localhost:8080/swagger-ui/index.html
   # Expected: 401
   ```
3. Access with a USER JWT — expect 403:
   ```bash
   USER_JWT="<learner JWT>"
   curl -s -o /dev/null -w "%{http_code}\n" \
     -H "Authorization: Bearer $USER_JWT" \
     http://localhost:8080/swagger-ui/index.html
   # Expected: 403
   ```
4. Access with an ADMIN JWT — expect 200:
   ```bash
   ADMIN_JWT="<admin JWT>"
   curl -s -o /dev/null -w "%{http_code}\n" \
     -H "Authorization: Bearer $ADMIN_JWT" \
     http://localhost:8080/swagger-ui/index.html
   # Expected: 200 or redirect to swagger-ui/index.html
   ```
5. Restore default profile:
   ```bash
   docker compose stop server && docker compose up -d server
   ```

---

## Section 3 — Actuator Internal Port (Task 7.3)

**Goal:** `/actuator/prometheus` is NOT reachable on port 8080 (public port),
but IS reachable on port 9090 (internal management port).

1. Verify `/actuator/health` is accessible on the public port:
   ```bash
   curl -s -o /dev/null -w "%{http_code}\n" http://localhost:8080/actuator/health
   # Expected: 200
   ```
2. Verify `/actuator/prometheus` returns 404 (not proxied by nginx) on port 80:
   ```bash
   curl -s -o /dev/null -w "%{http_code}\n" http://localhost:80/actuator/prometheus
   # Expected: 404 (nginx has no route for /actuator/prometheus)
   ```
3. Verify `/actuator/prometheus` IS reachable directly on the Spring management port:
   ```bash
   curl -s -o /dev/null -w "%{http_code}\n" http://localhost:9090/actuator/prometheus
   # Expected: 200 (management port is not exposed publicly — localhost works from host in dev)
   ```
   Note: In production, port 9090 would not be in the `ports:` section of docker-compose,
   making it reachable only from within the Docker network.

---

## Section 4 — SSRF Validator (Task 7.4)

**Goal:** `POST /admin/topics` with a URL pointing to a private IP is rejected with 400.

Obtain an ADMIN JWT first (see Section 2 or use the magic link flow).

1. Submit a topic with a loopback URL:
   ```bash
   curl -s -w "\nHTTP %{http_code}\n" \
     -X POST http://localhost:8080/admin/topics \
     -H "Content-Type: application/json" \
     -H "Authorization: Bearer $ADMIN_JWT" \
     -d '{"title":"SSRF test","description":"test","urls":["http://127.0.0.1/secret"],"durationDays":7}'
   # Expected: HTTP 400 with body containing "private" or "loopback"
   ```
2. Submit a topic with a private RFC 1918 URL:
   ```bash
   curl -s -w "\nHTTP %{http_code}\n" \
     -X POST http://localhost:8080/admin/topics \
     -H "Content-Type: application/json" \
     -H "Authorization: Bearer $ADMIN_JWT" \
     -d '{"title":"SSRF test","description":"test","urls":["http://192.168.1.1/admin"],"durationDays":7}'
   # Expected: HTTP 400
   ```
3. Submit a topic with a legitimate URL:
   ```bash
   curl -s -w "\nHTTP %{http_code}\n" \
     -X POST http://localhost:8080/admin/topics \
     -H "Content-Type: application/json" \
     -H "Authorization: Bearer $ADMIN_JWT" \
     -d '{"title":"Valid topic","description":"test","urls":["https://www.example.com"],"durationDays":7}'
   # Expected: HTTP 202 (accepted for async processing)
   ```

---

## Section 5 — Input Validation (Task 7.5)

**Goal:** All endpoints reject blank/invalid input with structured 400 responses.

1. Magic link with blank email:
   ```bash
   curl -s -X POST http://localhost:8080/auth/magic-link \
     -H "Content-Type: application/json" \
     -d '{"email":""}'
   # Expected: {"message":"Validation failed","errors":["email: must not be blank",...]}
   ```
2. Magic link with non-email string:
   ```bash
   curl -s -X POST http://localhost:8080/auth/magic-link \
     -H "Content-Type: application/json" \
     -d '{"email":"not-an-email"}'
   # Expected: HTTP 400 with message "Validation failed"
   ```
3. Refresh with blank token:
   ```bash
   curl -s -X POST http://localhost:8080/auth/refresh \
     -H "Content-Type: application/json" \
     -d '{"refreshToken":""}'
   # Expected: HTTP 400 with message "Validation failed"
   ```
4. Report task with reason exceeding 500 chars:
   ```bash
   LONG=$(python3 -c "print('x'*501)")
   curl -s -w "\nHTTP %{http_code}\n" \
     -X POST http://localhost:8080/tasks/fake-task-id/report \
     -H "Content-Type: application/json" \
     -H "Authorization: Bearer $USER_JWT" \
     -d "{\"reason\":\"$LONG\"}"
   # Expected: HTTP 400
   ```

---

## Section 6 — OWASP ZAP Baseline Scan (Task 7.6)

**Goal:** ZAP scan produces zero Critical or High findings.

1. Run the baseline scan (unauthenticated):
   ```bash
   docker run --rm --network host \
     -v $(pwd)/agentic/security:/zap/wrk \
     ghcr.io/zaproxy/zaproxy:stable \
     zap-baseline.py -t http://localhost:8080 \
     -r zap-report.html -J zap-report.json -I
   ```
2. Expected output:
   ```
   FAIL-NEW: 0   WARN-NEW: 1   PASS: 66
   ```
   The only WARN is "Non-Storable Content" (Informational) which is accepted.
   See `agentic/security/DAST-REPORT.md`.

---

## Section 7 — Security Headers (Task 7.7 ext)

**Goal:** All nginx responses include the required security headers.

1. Check headers on the root endpoint:
   ```bash
   curl -sI http://localhost:80/ | grep -E "Strict-Transport|Referrer-Policy|X-Content-Type|X-Frame|Content-Security"
   ```
   Expected headers present:
   - `Strict-Transport-Security: max-age=63072000; includeSubDomains`
   - `Referrer-Policy: strict-origin-when-cross-origin`
   - `X-Content-Type-Options: nosniff`
   - `X-Frame-Options: DENY`
   - `Content-Security-Policy: default-src 'self'; ...` (only on `/` SPA route)

2. Verify API routes do NOT have CSP (it would interfere with JSON responses):
   ```bash
   curl -sI http://localhost:80/auth/magic-link | grep "Content-Security"
   # Expected: no Content-Security-Policy header on API routes
   ```

---

## Section 8 — Tests Pass

**Goal:** All server unit and integration tests pass after Phase 07 changes.

```bash
./gradlew :server:test --rerun-tasks
# Expected: BUILD SUCCESSFUL
```

---

## Sign-Off Checklist

| # | Check | Pass? |
|---|-------|-------|
| 1 | Rate limit: 6th request to /auth/magic-link returns 429 | ☐ |
| 2 | Swagger requires ADMIN JWT in prod profile | ☐ |
| 3 | /actuator/prometheus not reachable on port 8080/80 | ☐ |
| 4 | SSRF: 127.0.0.1 URL blocked with 400 | ☐ |
| 5 | SSRF: 192.168.x.x URL blocked with 400 | ☐ |
| 6 | Input validation: blank email returns 400 with structured error | ☐ |
| 7 | ZAP scan: FAIL-NEW: 0, no Critical/High findings | ☐ |
| 8 | Security headers present in nginx responses | ☐ |
| 9 | ./gradlew :server:test BUILD SUCCESSFUL | ☐ |
