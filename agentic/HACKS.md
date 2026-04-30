# Play4Change — Known Hacks & Technical Debt

## Format

A hack is any place in the code where the implementation is not the right solution —
it is a shortcut, stub, workaround, or temporary measure.

Every hack must be tracked here until it is fixed. When a hack is fixed, mark it as
FIXED with the phase and commit reference. Do not delete fixed entries.

**Entry format:**
```
## [ID] [OPEN|FIXED] — [Short title]

**Location:** [file:line or package]
**What it is:** [Describe the hack clearly — what is wrong with this code]
**Why it exists:** [Why the hack was introduced — time pressure, missing dependency, etc.]
**Fix:** [What the correct solution is]
**Fix in:** [Phase and task number where this is scheduled to be fixed]
**Fixed:** [If FIXED: phase, task, commit hash]
```

---

## H01 [FIXED] — CI JDK version mismatch (17 vs 21)

**Location:** `.github/workflows/ci.yml:14` (the `java-version` field in `setup-java` step)

**What it is:** The CI workflow runs on JDK 17, but the server Gradle build targets JVM 21
(`jvmToolchain(21)` in `server/build.gradle.kts`). This means CI compiles and tests against
an older JVM than the target runtime. Language features and optimisations available in JVM 21
are not exercised in CI.

**Why it exists:** The CI was scaffolded early and the JVM version was not updated when the
server module set its toolchain target.

**Fix:** Change `.github/workflows/ci.yml` `java-version: '17'` to `java-version: '21'`.
Use `distribution: 'temurin'`.

**Fix in:** Phase 01, Task 1.2

**Fixed:** Phase 01, Task 1.2 — changed `java-version` from `'17'` to `'21'` in `.github/workflows/ci.yml`.

---

## H02 [FIXED] — Name.kt and Password.kt value object stubs

**Location:** `server/src/main/kotlin/*/domain/model/Name.kt`,
`server/src/main/kotlin/*/domain/model/Password.kt` (exact paths may vary — search for these files)

**What it is:** These value objects accept any string without validation. A `Name` constructed
from an empty string or a 1000-character control-character string is indistinguishable from a
valid name in the domain model. `Password.kt` may be unused entirely. Neither class enforces
any invariant.

**Why it exists:** They were scaffolded as stubs when the domain model was initially built
and were never completed before the auth system was added on top.

**Fix:**
- `Name`: validate non-blank, length 2–100, no control characters. Return `Either<NameError, Name>`.
- `Password`: if unused, delete the file. If used, implement: ≥12 chars, uppercase, digit,
  special character. Validate on construction.
- Write unit tests for both.

**Fix in:** Phase 01, Task 1.4

**Fixed:** Phase 01, Task 1.4, commit cc798c4 — Name.kt validates non-blank, 2–100 chars, no control characters. Password.kt deleted (unused).

---

## H04 [OPEN] — NVD API key required to seed OWASP dependency-check database

**Location:** `server/build.gradle.kts` (`dependencyCheck {}` block), `.github/workflows/dependency-check.yml`

**What it is:** The OWASP dependency-check plugin 9.x uses the NVD API v2 to download CVE data.
The NVD API v2 rate-limits unauthenticated requests heavily enough that the bulk download
immediately returns 403. The local H2 database (`~/.gradle/dependency-check-data/9.0/odc.mv.db`)
cannot be seeded without an API key, so `./gradlew :server:dependencyCheckAnalyze` always fails
locally unless `NVD_API_KEY` is set in the environment.

**Why it exists:** The NVD deprecated its legacy data feeds in late 2023 and migrated to API v2.
Unauthenticated API access is throttled to 5 requests per 30 seconds, which is insufficient for
the bulk data download the OWASP tool needs. The NVD recommends all users register a free API key.

**Fix:** Register a free NVD API key at https://nvd.nist.gov/developers/request-an-api-key
and set `export NVD_API_KEY=<key>` before running the scan locally. The CI workflow already
passes the key via the `NVD_API_KEY` GitHub repository secret.

**Fix in:** Developer environment setup — not a code fix. Document in HOW_TO_RUN.md in Phase 08.

---

## H05 [OPEN] — Jackson classpath override in root build.gradle.kts for OWASP compatibility

**Location:** `build.gradle.kts` (root project, `buildscript {}` block)

**What it is:** `spring-boot-gradle-plugin:3.2.3` transitively loads `spring-boot-buildpack-platform`
which bundles `jackson-databind:2.14.2` into the buildscript classloader. Due to JVM parent-first
classloading, the OWASP dependency-check plugin's `jackson-module-blackbird` (compiled against
Jackson 2.16.0) finds `NativeImageUtil` from 2.14.2, which lacks `isInNativeImage()`, causing a
`NoSuchMethodError` at runtime. A `resolutionStrategy.eachDependency {}` override in the root
`buildscript {}` block forces all `com.fasterxml.jackson.*` to 2.16.1 to resolve the conflict.

**Why it exists:** Spring Boot 3.2 and OWASP dependency-check 9.x cannot coexist without this
override because of the classloader hierarchy leak from the Spring Boot Gradle plugin.

**Fix:** Upgrade to Spring Boot 3.3.x (which bundles Jackson 2.17.x in `spring-boot-buildpack-platform`)
— the Jackson versions would then be consistent. Scheduled when the project upgrades Spring Boot.

**Fix in:** Unscheduled — will be removed when Spring Boot is upgraded to 3.3.x or later.

---

## H03 [OPEN] — All mobile repositories mocked (Ktor not wired)

**Location:** `composeApp/src/commonMain/kotlin/*/data/` — all `Mock*Repository.kt` files

**What it is:** Every repository in the KMP mobile client is a mock implementation.
The `MockAuthRepository`, `MockTopicRepository`, `MockEnrollmentRepository`,
`MockStruggleRepository`, `MockPeerReviewRepository`, `MockProfileRepository`, and
`MockBadgeRepository` return hardcoded or randomly delayed fake data. No real HTTP calls
are made to the server.

**Why it exists:** The mobile client UI was built against mocks to allow frontend development
to proceed independently of backend development. This is the correct approach for parallel
development — the hack is that the wiring to real HTTP was never done.

**Fix:** Replace each mock repository with an `Http*Repository` implementation using Ktor
`HttpClient`. Implement `TokenStorage` with `EncryptedSharedPreferences` (Android) and
Keychain (iOS). Implement 401→refresh→retry logic.

**Fix in:** Phase 04, Tasks 4.1–4.5

**Fixed:** *(to be filled when task is complete)*

---

*(New entries are prepended above existing open items)*
