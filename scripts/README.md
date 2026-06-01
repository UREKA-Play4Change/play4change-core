# Scripts

All scripts must be run from the **project root**. They resolve their own path, so you can call them from anywhere:

```bash
./scripts/check-health.sh
# or from inside scripts/
bash scripts/check-health.sh
```

Scripts that touch `.env` load it automatically. No manual exporting required.

---

## Quick Reference

| Script | What it does | When to use |
|---|---|---|
| [`setup.sh`](#setupsh) | Full wipe + rebuild + start + Cloudflare tunnel | First setup or clean slate |
| [`restart-server.sh`](#restart-serversh) | Rebuild server JAR only, restart server container | Code changes — keeps DB/cache/storage intact |
| [`check-health.sh`](#check-healthsh) | Wait for all services and report status | After `docker compose up` to confirm everything is up |
| [`minio-init.sh`](#minio-initsh) | Create required MinIO buckets | After first `setup.sh` or volume reset |
| [`logs.sh`](#logssh) | Tail logs with optional service and grep filter | Debugging |
| [`db-shell.sh`](#db-shellsh) | Open interactive psql session | Exploring data |
| [`db-query.sh`](#db-querysh) | Run a single SQL statement | Quick checks from terminal |
| [`migration-status.sh`](#migration-statussh) | Show Flyway migration history | After startup or migration issues |
| [`promote-admin.sh`](#promote-adminsh) | Promote a user to ADMIN role | Onboarding a new admin |
| [`run-tests.sh`](#run-testssh) | Run server and/or AI agent tests | Before opening a PR |
| [`build-android.sh`](#build-androidsh) | Build Android APK | Manual device testing |
| [`check-deps.sh`](#check-depssh) | OWASP CVE scan on all dependencies | Security review |

---

## Typical dev workflow

```bash
# First time
./scripts/setup.sh         # wipe + build + start everything
./scripts/check-health.sh      # confirm all services are healthy
./scripts/minio-init.sh        # create the 'play4change' MinIO bucket

# Daily iteration
./scripts/restart-server.sh    # change server code → rebuild → restart
./scripts/logs.sh server ERROR # watch for errors

# Before PR
./scripts/run-tests.sh all
```

---

## Script Details

### `setup.sh`

Destroys all containers and volumes, rebuilds from scratch, and starts the Cloudflare tunnel.

```bash
./scripts/setup.sh
```

> **Warning:** This wipes all data — postgres, minio, redis. Use [`restart-server.sh`](#restart-serversh) if you only changed server code and want to preserve data.

Requires `cloudflared` installed and one of:
- `CLOUDFLARE_TUNNEL_TOKEN` in `.env`
- Token file at `~/.cloudflared/<TUNNEL_ID>.token`
- Credentials JSON at `~/.cloudflared/<TUNNEL_ID>.json`

---

### `restart-server.sh`

Builds a fresh server JAR with Gradle, stops and removes only the server container, then starts it again. Postgres, Redis, and MinIO keep their state.

```bash
./scripts/restart-server.sh
```

Tests and Detekt are skipped for speed. Tails server logs after startup.

---

### `check-health.sh`

Polls each service until it reports healthy (or times out), then prints all useful endpoints.

```bash
./scripts/check-health.sh
./scripts/check-health.sh --timeout 120   # extend timeout for slow machines
```

Default timeout: 90 seconds per service.

Output example:
```
Infrastructure:
  PostgreSQL... healthy
  Redis......... healthy
  MinIO......... healthy

Application:
  Server (8080).. UP

Observability:
  Prometheus.... UP
  Grafana....... UP

Endpoints:
  Swagger UI  → http://localhost:8080/swagger-ui.html
  Grafana     → http://localhost:3000
  Prometheus  → http://localhost:9090
  MinIO UI    → http://localhost:9001
```

---

### `minio-init.sh`

Creates the `play4change` bucket in the running MinIO instance. The server will fail to store any file (PDF uploads, photo submissions) if this bucket does not exist.

```bash
./scripts/minio-init.sh
```

Requires `mc` (MinIO Client):
```bash
brew install minio/stable/mc   # macOS
```

Safe to run multiple times — uses `--ignore-existing`.

---

### `logs.sh`

Tails docker compose logs with optional service and case-insensitive grep filter.

```bash
./scripts/logs.sh                       # all services, last 50 lines each
./scripts/logs.sh server                # server only, last 100 lines
./scripts/logs.sh server ERROR          # server errors only
./scripts/logs.sh server generation     # AI generation pipeline logs
./scripts/logs.sh nginx                 # nginx access/error logs
```

Available services: `server`, `postgres`, `redis`, `minio`, `nginx`, `prometheus`, `grafana`.

---

### `db-shell.sh`

Opens an interactive `psql` session inside the postgres container.

```bash
./scripts/db-shell.sh
```

You land directly in the `play4change` database. Type `\dt` to list tables, `\q` to quit.

---

### `db-query.sh`

Runs a single SQL statement non-interactively. Useful for scripts and quick checks.

```bash
./scripts/db-query.sh "SELECT id, email, role FROM users;"
./scripts/db-query.sh "SELECT count(*) FROM topics WHERE status = 'ACTIVE';"
./scripts/db-query.sh "SELECT version, description, success FROM flyway_schema_history ORDER BY installed_rank;"
```

---

### `migration-status.sh`

Prints the full Flyway migration history in a readable table. Useful when diagnosing why the server fails to start or when a migration looks stuck.

```bash
./scripts/migration-status.sh
```

Output columns: rank, version, description, status (OK / FAILED), installed_on, execution time (ms).

---

### `promote-admin.sh`

Promotes an existing user to `ADMIN` role. The user must have signed in at least once so a row exists in `users`.

```bash
./scripts/promote-admin.sh radesh.govind@gmail.com
```

The user must log out and log back in for the new role to take effect (JWT must be re-issued).

---

### `run-tests.sh`

Runs the test suite with the `test` Spring profile active. `--continue` ensures all tests run even if some fail.

```bash
./scripts/run-tests.sh              # all modules
./scripts/run-tests.sh server       # server module only
./scripts/run-tests.sh ai           # ai-agent langchain module only
./scripts/run-tests.sh server MagicLinkServiceTest   # single test class
./scripts/run-tests.sh all --info   # verbose output
```

Reports open at:
- `server/build/reports/tests/test/index.html`
- `ai-agent/langchain/build/reports/tests/test/index.html`

---

### `build-android.sh`

Builds the Android APK for the Compose Multiplatform app.

```bash
./scripts/build-android.sh           # debug APK (default)
./scripts/build-android.sh release   # release APK
```

Prints the APK path and size, and the `adb install` command to sideload it.

Requires Android SDK installed and `ANDROID_HOME` set (via Android Studio or `brew install --cask android-studio`).

---

### `check-deps.sh`

Runs the OWASP dependency-check Gradle task and opens the HTML report. Fails the build if any dependency has a CVSS score ≥ 7.0.

```bash
./scripts/check-deps.sh
```

> **Slow:** First run downloads the NVD database (~2-5 minutes). Subsequent runs use the cached NVD data.

CI runs this automatically via `.github/workflows/dependency-check.yml`. Run locally before adding or upgrading a dependency.
