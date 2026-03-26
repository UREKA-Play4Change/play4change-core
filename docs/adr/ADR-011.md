# ADR-011 — Authentication Module: Magic Link + OAuth 2.0 + JWT Dual-Token

**Status:** Accepted  
**Date:** March 2026  
**Author:** Radesh Govind  
**Relates to:** ADR-001 (Result Type), ADR-004 (i18n), ADR-006 (Module Boundary), ADR-008 (Client Architecture)

---

## Context

The platform requires verified user identity before scores, progress, and
badges can be reliably attributed. Without authentication, any client can
submit completions on behalf of any user — which breaks the educational
integrity of the platform and makes the scoring system meaningless.

Three requirements shaped the auth design simultaneously:

1. **No credential storage** — the platform must never hold user passwords.
   A breach of the database must not compromise user credentials or enable
   lateral movement to other services the user trusts.

2. **Mobile-first, offline-tolerant** — the client is a KMP application on
   Android and iOS. Authentication tokens must be manageable in a mobile
   context: survives app restart, expires predictably, refreshes silently.

3. **Architecturally coherent** — the auth module must follow the same
   hexagonal pattern as the rest of the server (ADR-006) and the same
   typed error approach (ADR-001). It cannot be a special case bolted on
   the outside.

---

## Decision

**Passwordless authentication via two mechanisms, both issuing the same
dual-token JWT pair:**

| Mechanism | How | When |
|-----------|-----|------|
| Magic link | Email → one-time token → JWT pair | Primary flow |
| OAuth 2.0 | Google/Facebook native SDK → ID token → JWT pair | Social login |

Both mechanisms share a single `TokenService` that issues and rotates tokens.
The auth module is implemented as a strict hexagonal slice inside the server,
with no structural coupling to the existing course/task domain.

---

## Why No Passwords

Storing passwords — even hashed with bcrypt — means the platform holds
sensitive credentials. A database breach exposes hashed passwords to offline
cracking. Users frequently reuse passwords. A breach of this platform would
therefore compromise user accounts on unrelated services.

Eliminating credential storage is not a hardening measure applied after the
fact. It is an architectural choice that removes the attack surface entirely.
Magic links and OAuth delegation both achieve verified identity without the
platform ever handling a password. The credential risk is pushed to
infrastructure the user already trusts (their email provider, Google, Facebook)
at a hardening scale no individual application can match.

---

## Architecture: Hexagonal Slice

The auth module follows the same port/adapter structure established in ADR-006
for the AI agent boundary. The dependency rule is enforced by package structure,
not by Gradle module separation (both live in `:server` at this stage):

```
auth/
├── domain/model/         ← pure Kotlin data classes, zero framework imports
│   ├── User
│   ├── MagicLinkToken    (with isValid(), isExpired() domain logic)
│   ├── RefreshToken      (with isValid(), isExpired() domain logic)
│   ├── TokenPair
│   ├── OAuthClaims
│   └── AuthProvider      (MAGIC_LINK, GOOGLE, FACEBOOK)
│
├── port/
│   ├── inbound/          ← what the domain offers to the world (interfaces)
│   │   ├── AuthUseCase
│   │   ├── OAuthUseCase
│   │   └── TokenUseCase
│   └── outbound/         ← what the domain needs from the world (interfaces)
│       ├── UserRepository
│       ├── MagicLinkTokenRepository
│       ├── RefreshTokenRepository
│       ├── EmailPort
│       └── OAuthVerifierPort
│
├── application/          ← implements inbound ports, uses only outbound ports
│   ├── MagicLinkService
│   ├── OAuthService
│   └── TokenService
│
└── adapter/
    ├── inbound/
    │   ├── web/          ← AuthController, DTOs, AuthExceptionHandler
    │   └── security/     ← JwtAuthFilter, SecurityConfig
    └── outbound/
        ├── persistence/  ← JPA entities, Spring repositories, adapter impls
        ├── email/        ← SmtpEmailAdapter, ConsoleEmailAdapter
        └── oauth/        ← GoogleOAuthAdapter, FacebookOAuthAdapter, Registry
```

**The invariant:** nothing in `domain/` or `port/` imports Spring, JPA,
Jakarta, or Jackson. Application services import only port interfaces — never
adapter classes. The controller imports only use case interfaces — never
service classes directly. This is verifiable by inspecting imports.

---

## Magic Link Flow

Magic links are chosen over OTP codes because they require no second step —
the user clicks a link rather than switching apps to copy a code. On mobile,
deep links can redirect directly back into the app.

```
Client                    Server                    Email
  │                          │                         │
  ├─ POST /auth/magic-link ──►│                         │
  │  { email }               │                         │
  │                          ├─ generate 32-byte token  │
  │                          ├─ store token (expires 15m)│
  │                          ├─ send link ─────────────►│
  │◄─ 202 Accepted ──────────┤                         │
  │                          │                         │
  │     [user clicks link in email]                    │
  │                          │                         │
  ├─ GET /auth/verify?token= ►│                         │
  │                          ├─ findByToken            │
  │                          ├─ check isValid()        │
  │                          ├─ markUsed()             │
  │                          ├─ upsert user            │
  │                          ├─ issue(userId, email)   │
  │◄─ 200 { accessToken,     │                         │
  │         refreshToken,    │                         │
  │         expiresIn }  ────┤                         │
```

**Token hygiene:** the raw token is generated with `SecureRandom` (32 bytes,
hex-encoded = 64 chars). It is stored verbatim in `magic_link_tokens.token`
with a unique index. The token is single-use — `used = true` is set atomically
before the user record is resolved. A replayed magic link returns 400.

**Account creation:** `verifyMagicLink` performs an upsert — if a user with
that email already exists (from a previous magic link or OAuth), the existing
user is returned. New users are created with `provider = MAGIC_LINK` and
`provider_id = null`. This means magic link users can later link a social
provider without creating duplicate accounts.

---

## OAuth 2.0 Flow

The OAuth dance (redirect, callback, code exchange) is handled entirely by the
native SDK on the client — Google Sign-In SDK on Android, Facebook iOS SDK.
The server never participates in the OAuth redirect flow. It only receives
and verifies the resulting ID token.

```
Client                    Google/Facebook           Server
  │                              │                     │
  ├─ SDK login ─────────────────►│                     │
  │◄─ ID token ──────────────────┤                     │
  │                              │                     │
  ├─ POST /auth/oauth ───────────────────────────────►│
  │  { provider, idToken }       │                     │
  │                              │                     │
  │                              │◄── verify token ────┤
  │                              ├─── claims ─────────►│
  │                              │                     ├─ loginOrRegister()
  │                              │                     ├─ upsert user
  │◄──────────────────────────────────── TokenPair ────┤
```

**Verification:** Google ID tokens are verified by calling
`https://oauth2.googleapis.com/tokeninfo?id_token={token}`. Facebook access
tokens are verified via `https://graph.facebook.com/me?fields=id,name,email`.
Both are stateless — no OAuth client secret is required on the server because
the native SDK already completed the authorisation flow.

**OAuthVerifierRegistry** dispatches to the correct adapter by `AuthProvider`
enum. `OAuthService` injects the registry as a single `OAuthVerifierPort` —
it has no knowledge of Google or Facebook as distinct systems. Adding Apple
Sign-In requires: one new adapter implementing `OAuthVerifierPort`, one new
`when` branch in the registry, and no changes to `OAuthService`.

**Account linking:** `OAuthService.loginOrRegister()` applies a three-step
resolution:
1. `findByProviderAndProviderId` — exact match (returning user, same provider)
2. `findByEmail` — same email from a different provider (link accounts silently)
3. `save(newUser)` — brand new user

This means a user who first signed in with magic link and later uses Google
with the same email address gets the same account and the same score history.

---

## JWT Dual-Token Architecture

A single long-lived token is a security liability — if stolen, the attacker
has access until expiry with no detection mechanism. A single short-lived token
requires frequent re-authentication, which breaks the daily engagement loop.

The dual-token architecture resolves this tension:

| Token | Lifetime | Storage | Purpose |
|-------|----------|---------|---------|
| Access token | 15 minutes | Memory only | API authentication |
| Refresh token | 7 days | Secure persistent store | Silent renewal |

**Access token:** HS256 JWT signed with `JWT_SECRET`. Claims: `sub` (userId),
`email`, `iat`, `exp`. Parsed in `JwtAuthFilter` on every authenticated request.
Never written to disk — lives only in application memory on the client.

**Refresh token:** 32 random bytes hex-encoded. Stored as SHA-256 hash in
`refresh_tokens` — the raw token is never stored. The client holds the raw token;
the server holds only its hash. A database breach reveals hashes that cannot
be reversed to active tokens.

### Rotation and Theft Detection

Every refresh call rotates the refresh token:

```
Client                         Server
  │                               │
  ├─ POST /auth/refresh ─────────►│
  │  { refreshToken }             ├─ sha256(token) → find in DB
  │                               ├─ if used=true → THEFT DETECTED
  │                               │    revokeAllByFamilyId()
  │                               │    throw SecurityException
  │                               ├─ if expired → 400
  │                               ├─ markUsed(old token)
  │                               ├─ issue new access + new refresh
  │◄─ { newAccessToken,           │    (same familyId)
  │     newRefreshToken } ────────┤
```

**Family ID** is the theft detection mechanism. Every token belongs to a family
(UUID assigned at first issue). On rotation, the new token inherits the same
family. If a stolen token is presented after the legitimate client has already
rotated it, the server sees `used=true`. It then revokes every token in the
family, forcing re-authentication on all devices. This is the implementation
of the security property described in section 11 of the engineering decisions
document.

---

## Database Schema (V7)

Three new tables — `VARCHAR(36)` PKs throughout to match the existing
`user_subscriptions.user_id` and `user_tasks.user_id` String convention:

```sql
users
  id VARCHAR(36) PK, email UNIQUE, name, provider, provider_id,
  created_at, UNIQUE(provider, provider_id)

magic_link_tokens
  id VARCHAR(36) PK, token UNIQUE (indexed), email,
  expires_at, used, created_at

refresh_tokens
  id VARCHAR(36) PK, token_hash UNIQUE (indexed),
  user_id → users(id) ON DELETE CASCADE,
  family_id (indexed), expires_at, used, created_at
```

The `users` table is intentionally minimal — identity only, no profile data.
User profile data lives in the `user_subscriptions` and `user_tasks` tables
that reference `user_id` as a String FK. The `users` table is the authoritative
source of that ID.

---

## Email Adapter: Environment-Conditional

Two implementations of `EmailPort` exist, selected at runtime by
`@ConditionalOnProperty`:

| Bean | Condition | Behaviour |
|------|-----------|-----------|
| `ConsoleEmailAdapter` (`@Primary`) | `spring.mail.password` absent or empty | Logs magic link to INFO log |
| `SmtpEmailAdapter` | `spring.mail.password` is set | Sends real email via JavaMailSender |

This pattern means the development environment works without any email
configuration — the magic link appears in the server log and can be copy-pasted
directly. Production is enabled by setting one environment variable.
No code changes, no profile switches, no feature flags.

---

## SecurityConfig Replacement

The existing `SecurityConfig` had two Spring profile beans:
- `demo`/`default` profile: permit all (correct for dev)
- `prod` profile: `oauth2ResourceServer` with Spring's built-in JWT validation

The `oauth2ResourceServer` approach was replaced entirely. Spring's built-in
JWT resource server requires additional configuration for key management and
does not integrate cleanly with our `TokenService.parseUserIdFromAccessToken()`
which already handles verification correctly.

The replacement is a single `SecurityFilterChain` with no profile annotation:

```
.authorizeHttpRequests {
    .requestMatchers("/auth/**", "/actuator/health", "/actuator/prometheus").permitAll()
    .anyRequest().authenticated()
}
.addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter::class.java)
```

`JwtAuthFilter` (a `OncePerRequestFilter`) extracts the Bearer token, calls
`TokenService.parseUserIdFromAccessToken()`, and sets a
`UsernamePasswordAuthenticationToken` in the `SecurityContextHolder`. On any
exception (expired, malformed, missing), it clears the context and returns 401
directly — it does not propagate exceptions to Spring's error handling.

---

## Common Module: Shared Auth Contracts

Three additions were made to the `common` KMP module (Android + iOS + JVM targets):

**`auth/AuthProvider.kt`** — `@Serializable` enum: `MAGIC_LINK, GOOGLE, FACEBOOK`.
Shared so the client can pass a typed provider in the OAuth request body without
stringly-typed values.

**`auth/AuthTokens.kt`** — `@Serializable` data class carrying `accessToken`,
`refreshToken`, `expiresIn`. This is the canonical token response shape shared
between server HTTP response and client domain model. The client's `AuthResult`
wraps this.

**`error/client/Unauthorized.kt`** — extended with auth-specific subtypes:
`InvalidOrExpiredMagicLink`, `InvalidToken`, `TokenExpired`,
`RefreshTokenNotFound`, `RefreshTokenReuse`, `MissingCredentials`. Each carries
a `messageKey` consistent with ADR-004's server-side i18n strategy. The
`RefreshTokenReuse` subtype corresponds directly to the theft detection event.

The server domain uses its own `AuthProvider` enum (identical values, separate
type) to maintain the hexagonal rule that domain models have zero external
dependencies. The common module's `AuthProvider` is used in HTTP DTOs and on
the client.

---

## Client Changes (composeApp)

The KMP client required coordinated updates to consume the new token contract:

**`AuthResult`** was extended from `AuthResult(userId, token)` to
`AuthResult(userId, tokens: AuthTokens)` with a `val token` computed property
delegating to `tokens.accessToken`. This preserves backward compatibility with
the mock, which compiled against the old shape.

**`AuthRepository`** interface gained two methods:
- `verifyMagicLink(token: String): AuthResult?` — step 2 of the magic link flow
- `refresh(refreshToken: String): AuthResult?` — silent token renewal

**`MockAuthRepository`** implements the full new interface with realistic fake
`AuthTokens` objects and simulated delays on every method.

The mock's `AuthTokens` carry placeholder values (`"mock-access-token"` etc.)
that are structurally valid — the presentation layer can parse them, the UI can
display them, and the state machine can transition correctly. The mock does not
simulate JWT parsing; that is correctly outside the client's mock responsibility.

---

## Error Handling

Application services throw two exception types that cross the adapter boundary:

| Exception | Cause | HTTP |
|-----------|-------|------|
| `IllegalArgumentException` | Invalid token, expired link, bad request | 400 |
| `SecurityException` | Refresh token reuse detected | 401 |

`AuthExceptionHandler` (`@RestControllerAdvice`) catches both and returns
structured `MessageResponse` JSON. Stack traces never reach the client.

The `SecurityException` for token reuse deliberately returns 401 (not 403)
because the client's correct response is to redirect to login — the session
is no longer valid, not merely forbidden.

---

## What Was Not Done (Intentional Gaps)

**Rate limiting on `/auth/magic-link`:** A malicious client can request magic
links for any email address. Without rate limiting, this can be used to spam
users. The token bucket algorithm described in the engineering decisions document
(section 16) should be applied to this endpoint. Redis is already in the
dependency graph and available for a token bucket implementation. This is
deferred — the endpoint is not publicly indexed and the demo scope does not
warrant it at this stage.

**Apple Sign-In:** Required for App Store submission of iOS apps that offer
other social login options. The `OAuthVerifierRegistry` is designed to accept a
third adapter with zero changes to `OAuthService`. Apple ID token verification
uses JWT verification against Apple's public key set (JWKS endpoint) rather
than a tokeninfo HTTP call — a slightly different verification pattern but the
same port contract.

**Refresh token expiry cleanup:** Expired tokens accumulate in `refresh_tokens`
indefinitely. A scheduled cleanup job (`DELETE FROM refresh_tokens WHERE
expires_at < NOW() AND used = true`) should run nightly alongside the task
generation batch. Not implemented at this stage.

---

## Consequences

- The `users` table is now the source of identity for the entire platform.
  `user_subscriptions.user_id` and `user_tasks.user_id` reference it
  semantically (not via a hard FK, to avoid migration complexity on existing
  tables) — the String values are now guaranteed to be valid user IDs.

- Every API endpoint except `/auth/**`, `/actuator/health`, and
  `/actuator/prometheus` now requires a valid Bearer JWT. Existing
  `CourseController` and `TaskController` endpoints are now protected.
  The demo profile's "permit all" behaviour is replaced.

- Adding a new social provider (Apple, GitHub) requires one new adapter
  implementing `OAuthVerifierPort` and one `when` branch in
  `OAuthVerifierRegistry`. Zero changes to `OAuthService`, `TokenService`,
  or any other application service.

- The `JWT_SECRET` environment variable must be set before the server starts.
  The default `local-dev-secret-...` value in `application.yml` is intentionally
  long enough to satisfy JJWT's 256-bit minimum but is not safe for production.
  Production deployments must inject a cryptographically random secret via
  environment variable or secrets manager.

---

## References

- ADR-001 — Arrow Either for typed error handling
- ADR-004 — Internationalisation (messageKey on Unauthorized subtypes)
- ADR-006 — Module boundary pattern (applied to auth adapter structure)
- ADR-008 — Client architecture (AuthRepository interface, MockAuthRepository)
- [JJWT documentation](https://github.com/jwtk/jjwt)
- [Google ID token verification](https://developers.google.com/identity/sign-in/web/backend-auth)
- [Facebook Graph API](https://developers.facebook.com/docs/graph-api/reference/user)
- Engineering decisions document — sections 10 (Identity), 11 (Token protection)