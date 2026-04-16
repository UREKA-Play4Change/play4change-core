# Play4Change — How To Run (Local Demo)

---

## 1. Prerequisites

| Tool | Version | Notes |
|------|---------|-------|
| Docker Desktop | 4.x or later | Engine 24+ recommended; must have Compose v2 (`docker compose`, not `docker-compose`) |
| Java | 21 | Only needed if running the server **outside** Docker (Gradle builds inside Docker by default) |
| curl | any | Used for health checks in this guide |
| Git | any | To clone the repo |

Optional but useful:
- **httpie** (`brew install httpie`) — friendlier alternative to curl for manual API calls
- **JetBrains IDE** (IntelliJ / HTTP Client plugin) — to run the `.http` files in `demo/`

---

## 2. Start the Stack

### 2a. (Optional) Configure secrets before starting

If you want real AI generation or real email delivery, create a `.env` file
in the repo root **before** running `docker compose up`:

```bash
# repo root
cat > .env <<'EOF'
MISTRAL_API_KEY=sk-your-real-key-here
RESEND_API_KEY=re_your-real-key-here
GOOGLE_CLIENT_ID=your-google-client-id.apps.googleusercontent.com
JWT_SECRET=change-this-to-a-random-string-of-at-least-32-chars
EOF
```

Without a `.env` file the stack still starts cleanly — AI generation falls
back to `NoOpTaskGenerationAdapter` and emails are printed to server logs.

### 2b. Build and start everything

```bash
docker compose up --build
```

The first build downloads base images and compiles the Kotlin server (~2–4 min).
Subsequent starts are fast (~20–30 s).

### 2c. Expected healthy output

Watch for these lines (order may vary):

```
play4change-postgres  | database system is ready to accept connections
play4change-minio     | API: http://0.0.0.0:9000  Console: http://0.0.0.0:9001
play4change-redis     | Ready to accept connections tcp
play4change-server    | Started Play4ChangeApplicationKt in X.XXX seconds
play4change-prometheus| Server is ready to receive web requests
play4change-grafana   | HTTP Server Listen [::]:3000
```

The server will not start until postgres, minio, and redis pass their
healthchecks (enforced by `depends_on: condition: service_healthy`).

---

## 3. Verify Each Service is Up

### PostgreSQL

```bash
docker exec play4change-postgres pg_isready -U play4change -d play4change
```

Expected output:
```
/var/run/postgresql:5432 - accepting connections
```

### MinIO

Open in browser: **http://localhost:9001**

Default credentials: `minioadmin` / `minioadmin`

The `play4change` bucket is created automatically on first topic upload.

### Redis

```bash
docker exec play4change-redis redis-cli ping
```

Expected output:
```
PONG
```

### Server — health endpoint

```bash
curl -s http://localhost:8080/actuator/health | python3 -m json.tool
```

Expected response:
```json
{
    "status": "UP"
}
```

### Swagger UI

Open in browser: **http://localhost:8080/swagger-ui.html**

All endpoints are documented here. Authenticated endpoints can be tested by
clicking **Authorize** and pasting a JWT from the auth flow.

### Prometheus

Open in browser: **http://localhost:9090**

Try: `http_server_requests_seconds_count` in the expression browser.

### Grafana

Open in browser: **http://localhost:3000**

Default credentials: `admin` / `admin`

---

## 4. How Magic Link Email Works in Local Dev

### Without RESEND_API_KEY (default)

When `RESEND_API_KEY` is **not** set, the `ConsoleEmailAdapter` is active
instead of `ResendEmailAdapter`. No real email is sent — the magic link is
printed directly to the server's stdout/log.

```bash
docker logs play4change-server --follow 2>&1 | grep -A3 "MAGIC LINK"
```

You will see output like this (class name abbreviated by Logback):

```
INFO  c.u.p.a.a.o.e.ConsoleEmailAdapter - === MAGIC LINK (dev mode — no real email sent) ===
INFO  c.u.p.a.a.o.e.ConsoleEmailAdapter - To: user@example.com
INFO  c.u.p.a.a.o.e.ConsoleEmailAdapter - Link: http://localhost:8080/auth/verify?token=3f8a2d...c19e
INFO  c.u.p.a.a.o.e.ConsoleEmailAdapter - ===================================================
```

Copy the full token value (64 hex characters) from the `Link:` line.
The token is valid for **15 minutes** and is single-use.

### With RESEND_API_KEY set

When `RESEND_API_KEY` is set, `ResendEmailAdapter` is active and the magic
link is delivered to the user's inbox via [Resend](https://resend.com). Nothing
is printed to the server log — check the recipient's inbox instead.

In sandbox mode (no verified sender domain configured), Resend delivers from
the default sandbox sender: **`onboarding@resend.dev`**. The email may land
in the spam folder if the recipient address is not the Resend account owner's
verified address.

---

## 5. Environment Variables for Full Features

Create a `.env` file in the repo root. `docker compose` picks it up automatically.

| Variable | Default | What It Enables |
|----------|---------|-----------------|
| `MISTRAL_API_KEY` | *(not set — falls back to `NoOpTaskGenerationAdapter`)* | Real AI task generation via Mistral Small. Without it, topic generation completes instantly with empty/stub tasks. |
| `RESEND_API_KEY` | *(not set — falls back to `ConsoleEmailAdapter`)* | Real magic link emails delivered to the user's inbox via resend.com. Without it, links are printed to server logs. |
| `GOOGLE_CLIENT_ID` | *(empty — audience claim not validated)* | Validates the `aud` claim in Google ID tokens. Set to your OAuth 2.0 client ID from Google Cloud Console. |
| `FACEBOOK_APP_ID` | *(not set — Facebook OAuth deferred)* | Facebook App ID for OAuth token verification. **Not yet required** — Facebook OAuth integration is deferred pending Meta service availability. |
| `FACEBOOK_APP_SECRET` | *(not set — Facebook OAuth deferred)* | Facebook App Secret for OAuth token verification. **Not yet required** — same deferral as `FACEBOOK_APP_ID`. |
| `JWT_SECRET` | `local-dev-secret-change-in-production-must-be-32-chars-minimum` | JWT signing secret. The default is sufficient for local dev. **Must be replaced** for any shared or production environment. |
| `MINIO_ROOT_USER` | `minioadmin` | MinIO admin username |
| `MINIO_ROOT_PASSWORD` | `minioadmin` | MinIO admin password |
| `GF_SECURITY_ADMIN_PASSWORD` | `admin` | Grafana admin password |

Example `.env` for a fully-featured demo:

```dotenv
MISTRAL_API_KEY=sk-your-key-here
RESEND_API_KEY=re_your-key-here
GOOGLE_CLIENT_ID=123456789.apps.googleusercontent.com
JWT_SECRET=a-strong-random-secret-at-least-32-characters-long
```

---

## 6. First Admin User

There is no seeded admin account. Promote a user to `ADMIN` manually after
they have signed in at least once (so a `users` row exists):

```bash
docker exec -it play4change-postgres psql -U play4change -d play4change -c \
  "UPDATE users SET role = 'ADMIN' WHERE email = 'your@email.com';"
```

The change takes effect on the user's **next login**. The role is embedded
in the JWT at issue time — existing tokens are not retroactively upgraded.
The user must log out and log back in to receive a JWT with `role=ADMIN`.

Verify the promotion:

```bash
docker exec -it play4change-postgres psql -U play4change -d play4change -c \
  "SELECT id, email, role FROM users WHERE email = 'your@email.com';"
```

---

## 7. Stopping and Resetting

### Stop all containers (data is preserved)

```bash
docker compose down
```

### Full reset — destroy all data (DB, MinIO, Redis, Grafana volumes)

```bash
docker compose down -v
```

Use this when you want a completely clean state — Flyway will re-run all
migrations (V1–V10) on the next `docker compose up`.
