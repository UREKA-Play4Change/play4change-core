# Play4Change — Design Decisions Log

## Format

Each entry records a non-obvious design choice that does not warrant a full ADR.
Use this file when: the decision is tactical, affects one feature, is temporary,
or is obviously correct in hindsight with no realistic alternative.

For architectural decisions (new technology, new boundary, major security mechanism,
hard-to-reverse choices) — write a full ADR in `docs/adr/` instead.

**Entry format:**
```
## [YYYY-MM-DD] [Scope] — [Short title]

**Context:** Why this decision was needed.
**Decision:** What was decided.
**Why not [alternative]:** Why the obvious alternative was rejected.
**Phase:** Which phase introduced this.
```

---

## [2026-05-09] [auth] — Magic link GET /auth/verify redirects to play4change:// instead of returning JSON

**Context:** The email magic link URL is `https://radesh-govind.com/auth/verify?token=...`. When
clicked from an email client on iOS, Safari opens the URL and calls the server. The original
`GET /auth/verify` returned JSON tokens, which displayed in the browser — the app was never
opened. Bug B10.

**Decision:** `GET /auth/verify` now returns `302 Found` to `play4change://auth/verify?token={token}`.
The token is NOT consumed in the redirect. The app opens via its registered custom URL scheme,
receives the URL in `.onOpenURL`, extracts the token, and calls `POST /auth/magic-link/verify`
to consume the token and obtain JWTs. `HttpAuthRepository.verifyMagicLink` was updated from
`GET /auth/verify` to `POST /auth/magic-link/verify`.

**Why not Universal Links (AASA):** Universal Links require an `apple-app-site-association` file
at `https://radesh-govind.com/.well-known/apple-app-site-association` and an Associated Domains
entitlement in the app. This requires control of the DNS/server and App Store provisioning
profile changes. The custom scheme redirect achieves the same result with no infrastructure
changes and works in the simulator without entitlements.

**Why not consume the token in the redirect:** Consuming the token server-side during the
redirect would leave no way for the app to authenticate. The redirect is a pass-through;
the actual single-use consumption happens in `POST /auth/magic-link/verify`.

**Phase:** 04, fix/session-fixes-05

---

## [2026-05-09] [network] — Ktor 3.0.0 bearer auth: refreshTokens must fall back to stored refresh token when oldTokens is null

**Context:** In Ktor 3.0.0, `sendWithoutRequest { true }` does NOT cause `loadTokens` / `addRequestHeaders`
to be called proactively. The first authenticated request after login goes out without an `Authorization`
header, the server returns 401, and `refreshTokens` is invoked with `oldTokens = null`. The original
code read `oldTokens?.refreshToken` which evaluates to null, triggering `onSessionExpired()` immediately
after every successful login (Bug B9).

**Decision:** Changed `val refreshToken = oldTokens?.refreshToken` to
`val refreshToken = oldTokens?.refreshToken ?: tokenStorage.getRefreshToken()` in
`HttpClientFactory.kt`. This makes the 401-on-first-request case transparent to the user: Ktor
calls `refreshTokens`, we read the stored token directly from `TokenStorage`, issue `POST /auth/refresh`,
get a fresh access token, and retry the original request — all without the user knowing a refresh
happened.

**Why not configure Ktor differently:** Ktor 3.0.0 does not offer a first-class "always send auth
header without challenge" mechanism that is reliably cross-platform and works with bearer tokens.
The `sendWithoutRequest` predicate controls whether the client waits for a challenge, but the
`addRequestHeaders` / `loadTokens` proactive path has a known issue in 3.0.0 where it is not called
on the first request. The fallback approach is more robust and does not depend on undocumented
plugin internals.

**Phase:** 04, fix/session-fixes-05

---

## [2026-05-09] [iosMain] — KeychainTokenStorage must use kSecUseDataProtectionKeychain on iOS 13+

**Context:** On iOS 26.2 simulator, all SecItemAdd calls in `KeychainTokenStorage.saveItem()`
were failing silently. Running the app with B7 status-check logging revealed the system log
entry: `System Keychain Always Supported set via feature flag to disabled`. The iOS 26.2
simulator ships with the legacy System Keychain disabled via a kernel feature flag. Standard
SecItemAdd without `kSecUseDataProtectionKeychain` targets the System Keychain and always
fails on iOS 26.2 sim.

**Decision:** Add `kSecUseDataProtectionKeychain = kCFBooleanTrue` to every Keychain
query in `KeychainTokenStorage` (addQuery, searchQuery, deleteQuery). This routes all
operations to the Data Protection Keychain (per-app, sandboxed) instead of the System
Keychain. `kSecUseDataProtectionKeychain` is available since iOS 13.0 and macOS 10.15.

**Why not a different accessibility attribute:** `kSecAttrAccessibleAfterFirstUnlock` is kept
unchanged — it controls when the item is readable, not which keychain it uses.
`kSecUseDataProtectionKeychain` is an orthogonal selector for the keychain store.

**Why not remove kSecAttrAccessibleAfterFirstUnlock:** That attribute is still required on
real device builds to allow background token refresh after device reboot.

**Phase:** 04, fix/session-fixes-05

---

## [2026-05-09] [iosMain] — isDebugBuild via Platform.isDebugBinary

**Context:** `BuildInfo.ios.kt` had `actual val isDebugBuild: Boolean = false` hardcoded.
The in-app token paste field (guarded by `if (isDebugBuild)`) was permanently hidden on iOS,
blocking the Phase 04 manual test recipe's primary auth path on the simulator (Bug B8).

**Decision:** Replace with `Platform.isDebugBinary` from the Kotlin/Native standard library
(`kotlin.native.Platform`). This value is set by the Kotlin/Native linker at build time:
debug framework builds produce `true`; release/App Store builds produce `false`. No Gradle
build config injection or freeCompilerArgs flag is needed — the value is already correct for
simulator debug builds.

**Why not freeCompilerArgs / Gradle buildConfig injection:** Those approaches require
adding a build-time constant via `freeCompilerArgs += "-Xbinary=..."` or a separate
`buildConfig` plugin and are more fragile (the constant must be wired through the build
scripts). `Platform.isDebugBinary` is already present in the Kotlin/Native runtime for
exactly this purpose and requires zero build-script changes.

**Why not a separate iosDebug source set:** Kotlin Multiplatform does not expose a clean
iosDebug/iosRelease source set split in the current Gradle DSL without significant
restructuring. `Platform.isDebugBinary` is the idiomatic Kotlin/Native solution.

**Phase:** 04, fix/session-fixes-05

---

## [2026-05-09] [composeApp] — Debug-only token paste field for magic link testing when Resend is active

**Context:** `ResendEmailAdapter` delivers magic link emails to a real inbox, not the server
console. During manual testing and CI-adjacent development, testers cannot access the inbox.
The operator can extract the raw token from `docker compose logs server | grep token`, but
there is no in-app path to paste it — the app expects the deep link to arrive automatically.

**Decision:** Added a "Paste your verification token" `OutlinedTextField` and "Verify token"
button to the `LinkSentContent` composable, guarded by `if (isDebugBuild)`. Tapping the
button calls `GET /auth/verify?token=<pasted>` directly via `AuthRepository.verifyMagicLink`.
On success, navigates to the home screen exactly as a real deep link would. The field is
completely absent from release builds (`isDebugBuild = BuildConfig.DEBUG`).

**Why not a separate debug screen:** The extra UI lives inline in the "link sent" state where
the operator already is. A separate screen would require navigation changes and adds friction.

**Why not always-visible:** Exposing a raw-token input in release would allow phishing apps
to redirect users to paste their tokens. Debug-only is the safe default.

**Phase:** 04

---

## [2026-05-07] [composeApp] — Align composeApp Java compileOptions to JVM 21

**Context:** The root `build.gradle.kts` uses `allprojects { tasks.withType<KotlinCompile> { jvmTarget = JVM_21 } }` to enforce JVM 21 for the server module. This override also applied to `compileDebugKotlinAndroid`, but `composeApp/build.gradle.kts` still declared `compileOptions { sourceCompatibility = VERSION_11 }`, causing an AGP8 inconsistency error when running `testDebugUnitTest` for the first time in Phase 04.
**Decision:** Updated `composeApp` to `VERSION_21` / `JvmTarget.JVM_21` for both Java and Kotlin targets. D8/R8 handles the bytecode translation to support minSdk 24 at runtime.
**Why not keep VERSION_11:** The root `allprojects` override cannot be disabled per-module without restructuring the root build script. Aligning to 21 is consistent with the rest of the project.
**Phase:** 04

---

## [2026-04-29] [agentic] — Adopt agentic/ operating system for shared sessions

**Context:** Three operators share Claude Code Pro accounts on this project with a 2–3 week
deadline. Without a shared operating system, each session starts blind: the AI does not know
which phase is active, what security decisions have been made, what hacks exist, or what
the handoff state is. ADR culture is already established in the project (ADR-001 through ADR-016).
The agentic/ folder extends that culture to the AI session layer.

**Decision:** Create an `agentic/` folder at the repository root containing: operating rules
for the AI (AI.md), a human operator guide (INSTRUCTIONS.md), a phased roadmap (ROADMAP.md),
logging files for security (THREAT-LOG.md), hacks (HACKS.md), issues (ISSUES.md), and decisions
(this file). Every Claude Code session reads AI.md §10 first.

**Why not rely on git log and ADRs alone:** Git log describes what changed but not why
a session stopped, what is in progress, or what constraint applies to the next task.
ADRs capture architectural decisions but not the tactical operating state.
The agentic/ system fills the gap between those two.

**Why not a JIRA board or external tool:** The session must be able to orient from the
repository alone, without external service access. A markdown folder in the repo is always
available to any Claude Code session.

**Phase:** 01 (this is the Phase 01 deliverable)

---

## [2026-04-30] [ci] — Detekt 2.0.0-alpha.2 instead of 1.23.x; baseline file is detekt-baseline-main.xml

**Context:** Phase 01 Task 1.3 spec says to use Detekt `"1.23.x"`. The project uses Kotlin 2.3.0.
Detekt 1.23.8 (latest stable) was compiled with Kotlin 2.0.21 and hard-fails with "detekt was
compiled with Kotlin 2.0.10/2.0.21 but is currently running with 2.3.0. This is not supported."
There is no stable Detekt release that supports Kotlin 2.3.0.

**Decision:** Use `dev.detekt` (new Maven group) version `2.0.0-alpha.2`, which targets Kotlin 2.3.0
and is published to Maven Central. The plugin ID changed from `io.gitlab.arturbosch.detekt` to
`dev.detekt` in the 2.x series. Detekt 2.x also introduced per-source-set baseline tasks:
`detektBaselineMain` generates `detekt-baseline-main.xml`, which is what `detektMain` reads.
The phase spec's expected filename `detekt-baseline.xml` is not what Detekt 2.x produces for
the main source set; the correct file is `detekt-baseline-main.xml`.

**Why not downgrade Kotlin:** Kotlin 2.x is already required by the Kotlin 2.3.0 DSL features
used throughout the build. Downgrading Kotlin to 2.0.21 would break more than it fixes.

**Why not wait for a stable Detekt 2.x:** No stable 2.x release exists as of 2026-04-30.
The alpha is the only available path for Kotlin 2.3.0 support. The alpha is functionally
complete for the rules this project uses.

**Phase:** 01, Task 1.3

---

## [2026-04-30] [ci] — OWASP dependency-check plugin version 9.0.10 (not 9.2.x or 10.x)

**Context:** Phase 01 Task 1.6 spec says to use `id("org.owasp.dependencycheck") version "9.x.x"`.
Version 9.2.x and 10.0.x both fail with Jackson classpath conflicts when combined with Spring Boot
3.2 Gradle plugin (see HACKS.md H05 for root cause). Version 9.0.10 is the latest 9.x release that
is stable with this project's toolchain combination (Gradle 8.14.3, Spring Boot 3.2.3).

**Decision:** Use `id("org.owasp.dependencycheck") version "9.0.10"`.

**Why not 9.2.x:** Version 9.2.x uses `jackson-module-blackbird` 2.16.x compiled against Jackson
2.16.0 APIs (`NativeImageUtil.isInNativeImage()`). Spring Boot Gradle plugin 3.2.3 loads
`jackson-databind:2.14.2` into the buildscript classloader; parent-first JVM delegation means
`jackson-module-blackbird` finds 2.14.2's `NativeImageUtil` (no `isInNativeImage()`) and fails.
Version 9.0.10 is affected by the same root cause — the fix is in `build.gradle.kts` root
`buildscript {}` block (see H05), not in the plugin version.

**Why not 10.0.4:** Uses Jackson 2.17.2 APIs (`JsonFormat.Feature.READ_DATE_TIMESTAMPS_AS_NANOSECONDS`
added in 2.16.0) — same root cause, different field.

**Phase:** 01, Task 1.6

---

## [2026-05-01] [enrollment] — Language gating at read time via supported-languages whitelist

**Context:** Task 2.3 requires that users receive task templates in their preferred BCP 47
language. The system must gate generation: only produce a language variant when the user's
language is in the whitelist AND no variant already exists. If the language is not supported,
fall back to the topic's source language immediately (no 202, no generation trigger).

**Decision:** Implement `LanguageGatingService` in the application layer with two outcomes:
`Available` (serve template in effective language) or `Pending` (trigger generation, return
HTTP 202 + `X-Generation-Status: PENDING`). Unsupported languages fall back to the topic
source language and return `Available` without triggering generation. Language variants are
stored as additional `TaskTemplate` rows with a `language` column. The unique DB index is
`(module_id, day_index, pool_index, language) WHERE is_current = TRUE`. Variants are generated
lazily on first request, not eagerly at topic creation time.

**Why not eager generation at topic creation time:** Eager generation would call Mistral N×L
times on every topic creation (N tasks × L languages). At 3 supported languages this triples
AI cost for every topic even if no user in that language ever enrols. Lazy gating reduces cost
and allows the supported-languages list to grow without retroactive regeneration.

**Why not a separate TaskInstance table:** Re-using `TaskTemplate` with a `language` column
is simpler; the existing unique index cleanly prevents duplicates with no new entity needed.

**Phase:** 02, Task 2.3

---

## [2026-05-06] [Security] — jsoup XSS sanitisation on task report reason (OWASP A03)

**Context:** Task 3.3 adds a free-text `reason` field that learners submit when flagging a bad
question. This field is stored as plain text and later rendered in the admin dashboard. Without
sanitisation a stored XSS payload in the reason field would execute when an admin views reports.

**Decision:** The `reason` field is stored as-is (preserving the raw input for auditability).
On every read path (`getById`, `listByStatus`) the field is sanitised with
`org.jsoup.Jsoup.clean(reason, Safelist.none())`, which strips all HTML tags and attributes
before the value reaches the response DTO. The jsoup library (already on the classpath via
Phase 2 content extraction) requires no new dependency.

**Why not sanitise on write:** Sanitising on write destroys the original input permanently,
making it impossible to audit what the learner actually submitted. Sanitising on read keeps
the raw value in the DB for audit trails while ensuring no HTML ever reaches the browser.

**Why not a Spring `@HtmlEscape` annotation:** `@HtmlEscape` escapes HTML entities rather
than stripping tags; entities can still be rendered as markup in some contexts. `Safelist.none()`
strips tags entirely, which is the correct defence for a field that should contain plain text.

**Phase:** 03, Task 3.3

---

## [2026-05-12] [mobile/auth] — Ktor BearerAuthProvider always costs one extra round-trip after login

**Context:** After a successful magic-link or OAuth login the app navigates to home. The Ktor
bearer plugin caches the `loadTokens` result per `HttpClient` instance. A fresh client (created
at DI startup) has an empty cache, so the first post-login request fires without an `Authorization`
header, the server returns 401, `refreshTokens` is invoked, and the request is retried with the
access token (ADR-021 B.3, P001 F-02).

**Decision:** Accept the extra round-trip. It costs ~31 ms and is invisible to the user. The
only fix would be replacing the entire Ktor bearer plugin with a custom `HttpSend` interceptor
that proactively injects the stored access token — a high-risk rewrite with no user-visible benefit.

**Why not a custom interceptor:** The Ktor `Auth` plugin handles 401 retry logic, expiry, and
thread-safe token refresh. Replacing it removes those guarantees for marginal gain.

**Phase:** P001 audit fix batch (2026-05-12)

---

## [2026-05-12] [server/struggle] — Struggle generation shares generationExecutor pool with topic generation

**Context:** Both `@Async("generationExecutor")` calls — topic generation and struggle adaptive branch
generation — share the same pool (pool-size: 3, queue: 25). With many concurrent admins creating
topics the queue could theoretically starve struggle generation for learners.

**Decision:** Accept the shared pool for the current POC scale (≤5 concurrent admins, ≤50 concurrent
learners). The queue capacity of 25 provides sufficient buffer. A dedicated `struggleExecutor` pool
can be split out in Phase 06 if concurrent admin topic creation is observed to starve struggle
generation in a load test (P001 F-20, ADR-021 D2).

**Phase:** P001 audit fix batch (2026-05-12)

---

## [2026-05-12] [server/struggle] — Adaptive task option shuffle uses unseeded java.util.Random

**Context:** When adaptive tasks are generated, option shuffle uses `(options.indices.toMutableList()).also { it.shuffle() }`
— `shuffle()` with no seed defaults to `java.util.Random()` which is time-seeded and non-deterministic.

**Decision:** Accept the unseeded shuffle. Adaptive tasks are remediation content, not anti-cheat
content. The shuffle is stored in the `AdaptiveTask.optionOrder` column at generation time, so it is
stable per session even though it varies between sessions. Users cannot share answers via session
comparison because each session is tied to a unique enrollment. If a session is ABANDONED and a new
one is triggered, the user gets a different option ordering — which is acceptable and slightly improves
remediation value (P001 F-16).

**Phase:** P001 audit fix batch (2026-05-12)

---

*(New entries are prepended above this line — most recent first)*
