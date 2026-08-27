# 06 — Scenario 3: Ambiguous requirement

**The requirement, as received:** *"Add rate limiting to the service."*

Nine words. It names no endpoint, no subject, no limit, no window, no behaviour on breach, and no storage. Implementing it directly would mean inventing five product decisions and presenting them as engineering. This document separates what was asked from what had to be decided.

**Scope:** tasks D1–D5 in `docs/03-task-breakdown.md`
**Status:** §1 answered and signed · §2 implemented (D2–D4) · §3 complete

---

## 1. Ambiguity resolution (task D1 — no code)

### 1.1 What is actually unspecified

| # | Question | Why it matters | AI recommended default (prototype) | Production delta |
|---|----------|----------------|------------------------------------|------------------|
| **Scope** |
| Q1 | Which endpoints are limited? | Limiting the redirect protects the service; limiting it *too tightly* breaks legitimate viral traffic, since many real users share one NAT'd IP | `POST /api/v1/urls` and `GET /{code}`. Metadata/stats reads inherit the redirect limit. | Separate limits per endpoint class; redirect limit likely much higher or edge-enforced |
| Q2 | Is `/actuator/**` exempt? | If not, k8s probes and Prometheus scrapes will be throttled and the service will look unhealthy | **Yes, exempt** | Same, plus network-level restriction of actuator |
| Q3 | Does the limiter apply before or after the 404/410 check? | Limiting *after* means an attacker enumerating codes still consumes DB reads | **Before** — filter runs ahead of the controller | Same |
| **Identity** |
| Q4 | What is a "client"? | With no auth (AR-11) the only identity available is the network address | **Client IP** | API key per tenant; IP becomes a fallback |
| Q5 | Trust `X-Forwarded-For`? | Behind a proxy every request appears to come from the proxy; trusting the header blindly lets any caller spoof identity and evade the limit entirely | **Do not trust it.** Use `request.getRemoteAddr()`. Documented limitation: behind a reverse proxy all clients collapse to one bucket. | Trust only from a configured allow-list of proxy addresses (`server.forward-headers-strategy=NATIVE` + trusted-proxy config) |
| Q6 | IPv6 granularity — /128 or /64? | A single IPv6 client can trivially rotate addresses within its /64 | **/128 (whole address)** — simpler; note the evasion | Bucket IPv6 by /64 |
| **Limits** |
| Q7 | What limits and what window? | The heart of the requirement, and the part most likely to be wrong | **10/min create, 100/min redirect**, per client IP (engineer pre-confirmed, docs/03 D2) | Derived from measured p99 traffic; tiered by plan |
| Q8 | Fixed window or token bucket? | Fixed windows allow a 2× burst across a boundary | **Token bucket** with steady refill | Same |
| Q9 | Are limits configurable without a rebuild? | Hard-coded limits cannot be tuned during an incident | **Yes** — bound in `ShortenerProperties` | Same, plus dynamic reload |
| **Breach behaviour** |
| Q10 | What status and body? | Clients need a machine-readable signal | **429** with RFC 7807 problem+json, consistent with ADR-006 | Same |
| Q11 | Include `Retry-After`? | Without it a client can only guess and will hammer | **Yes**, seconds until a token is available | Same, plus `RateLimit-*` headers (RFC 9239 draft) |
| Q12 | Fail open or closed if the limiter itself errors? | A limiter that fails closed becomes a self-inflicted outage | **Fail open** — log and allow | Fail open, alert on limiter error rate |
| **Storage** |
| Q13 | Where does bucket state live? | Determines multi-node correctness | **In-process** (`ConcurrentHashMap` of buckets) — single instance per ADR-005 | Redis, so all instances share a counter |
| Q14 | How is the map bounded? | An unbounded map keyed by IP is a memory-exhaustion vector — the limiter becomes the DoS | **Caffeine cache**, max 100k entries, 10-min expiry after access | Redis with TTL |
| **Observability** |
| Q15 | What is measured? | "Is the limiter firing?" must be answerable without a debugger | Counter `ratelimit.rejected` tagged by endpoint class; WARN log with correlation ID | Same, plus per-tenant dashboards and alerting |

### 1.2 Questions with no good default — engineer must decide

| # | Question | Options | Consequence |
|---|----------|---------|-------------|
| **Q-A** | Should the **redirect** path be limited at all? | (a) Yes, 100/min — protects the service, but a genuinely popular link behind corporate NAT will 429 real users; (b) No — redirects unlimited, only writes limited | Engineer pre-answered **(a)** in docs/03: keep the redirect limit unless requirements explicitly remove it. Recorded as a deliberate product trade-off, not an oversight. |
| **Q-B** | Is a shared-IP false positive acceptable in a prototype? | (a) Accept and document; (b) Add an allow-list of exempt CIDRs | Affects whether an evaluator running a load test from one host sees 429s |

### 1.3 Normalized specification

> Requests are limited per client IP address using a token bucket. `POST /api/v1/urls` allows 10 requests per minute; `GET /{code}` allows 100 per minute. All other `/api/v1/**` endpoints share the redirect bucket. `/actuator/**` is exempt. Limits are enforced in a servlet filter ahead of controller dispatch. A breached limit returns **429** with `application/problem+json` (`type=.../rate-limited`) and a `Retry-After` header in seconds. Bucket state is held in-process in a bounded, expiring cache; the limiter fails open on internal error. Rejections increment `ratelimit.rejected` and log at WARN with the correlation ID. Client identity is `request.getRemoteAddr()`; `X-Forwarded-For` is **not** trusted.

### 1.4 Acceptance criteria
1. 10 POSTs in one minute succeed; the 11th returns 429 with `Retry-After`.
2. 100 redirects in one minute succeed; the 101st returns 429.
3. Two different client IPs have independent buckets.
4. Tokens refill over time — after the window advances, a previously limited client succeeds again.
5. `/actuator/health` never returns 429 regardless of request volume.
6. 429 body is problem+json and leaks no internal detail.
7. Limits are read from configuration, not constants.
8. Rejection increments `ratelimit.rejected`.
9. An exception inside the limiter allows the request through (fail open).
10. No existing test changes behaviour — the limiter is transparent below the threshold.

### 1.5 Test list

| Test | Tier | Asserts |
|------|------|---------|
| `TokenBucketTest` | unit, fixed clock | consume until empty; refill after advance; never exceeds capacity |
| `RateLimiterTest` | unit | per-key isolation; unknown key starts full; bounded cache evicts |
| `RateLimitFilterTest` | unit (MockMvc-free) | exempt paths bypass; endpoint→bucket mapping |
| `RateLimitIT` | integration | AC 1, 2, 3, 5, 6, 8 end-to-end on Postgres |
| `RateLimitFailOpenTest` | unit | limiter throwing → request proceeds, error logged |

Time is driven by the injected `Clock` (`MutableClock` in tests) — no `Thread.sleep`, consistent with the greenfield suite.

### 1.6 What would change for production
1. Redis-backed buckets (`RateLimiter` interface makes this one implementation swap).
2. API-key identity with per-tenant tiers; IP only as anonymous fallback.
3. Trusted-proxy configuration so `X-Forwarded-For` becomes usable.
4. Enforcement at the edge (CDN/API gateway) with the service limiter as defence in depth.
5. `RateLimit-Limit` / `RateLimit-Remaining` response headers.

### 1.7 Product-owner answers — engineer to complete

| # | Question | Answer |
|---|----------|--------|
| Q-A | Limit the redirect path? | ☑ (a) Yes, 100/min |
| Q-B | Shared-IP false positives | ☑ (a) Accept + document |
| Q5 | Trust `X-Forwarded-For`? | ☑ No — `getRemoteAddr()` only |
| Q12 | Limiter internal error | ☑ Fail open |
| Q7 | Limits | ☑ 10 / 100 per minute |

Signed: Masud Siraj   Date: 2026-08-27

---

## 2. Implementation (D2–D4)

Commit `5b31204`.

### 2.1 Structure

| Class | Role | Why it is shaped this way |
|-------|------|---------------------------|
| `RateLimiter` (interface) | `check(clientId, bucket) -> Decision` | The port. Swapping to Redis for multi-node (docs/06 §1.6) replaces one implementation and touches nothing else. |
| `TokenBucket` | steady-refill bucket, driven by an injected `Instant` | Chosen over a fixed window because a fixed window permits a 2× burst across the boundary (Q8). Time is a parameter, so tests never sleep. |
| `InMemoryRateLimiter` | per-`(client, bucket)` buckets in a Caffeine cache, max 100k, 10-min expiry | The bound is the point: an unbounded map keyed by client IP would make the limiter itself a memory-exhaustion vector (Q14). |
| `RateLimitFilter` | `OncePerRequestFilter`, `@Order(1)` | Runs ahead of controller dispatch so a rejected request never reaches the database (Q3). |
| `ShortenerProperties.RateLimit` | `enabled`, `createPerMinute`, `redirectPerMinute`, `maxTrackedClients` | Limits are configuration, not constants (Q9) — tunable during an incident without a rebuild. |

### 2.2 The answered questions, in code

| Decision | Where it lives |
|----------|----------------|
| Q5 — do not trust `X-Forwarded-For` | `RateLimitFilter` uses `request.getRemoteAddr()`; the header is never read |
| Q12 — fail open | `catch (RuntimeException)` around the limiter call logs and calls `chain.doFilter` |
| Q2 — actuator exempt | `shouldNotFilter` covers `/actuator`, `/v3/api-docs`, `/swagger-ui`, `/h2-console` |
| Q10/Q11 — 429 semantics | `ProblemDetail` with `type=.../rate-limited` plus a `Retry-After` header in seconds |
| Q15 — observability | `ratelimit.rejected` counter tagged by bucket; WARN log per rejection |

---

## 3. Validation and production delta (D5)

### 3.1 Result

```
[INFO] Tests run: 97, Failures: 0, Errors: 0, Skipped: 0    -- surefire (unit)
[INFO] Tests run: 22, Failures: 0, Errors: 0, Skipped: 0    -- failsafe (Testcontainers)
[INFO] All coverage checks have been met.
[INFO] BUILD SUCCESS
```

Acceptance criteria §1.4 map to tests as planned in §1.5: `TokenBucketTest` (AC 4), `InMemoryRateLimiterTest` (AC 3, 4), `RateLimitFilterTest` (AC 5, 6, 8, 9), `RateLimitIT` (AC 1, 5, 6, and AC 7 implicitly — see below).

### 3.2 The trade-off, demonstrated rather than asserted

Enabling the limiter globally immediately broke three unrelated integration tests:

```
[ERROR] UrlApiIT ... internalHostIs422           Expected status code <422> but was <429>.
[ERROR] UrlApiIT ... metadataEndpointReturns...  Expected status code <201> but was <429>.
[ERROR] UrlApiIT ... reservedAliasIs400          Expected status code <400> but was <429>.
[ERROR] AliasConcurrencyIT ... [exactly one winner]
```

Every test client connects from `127.0.0.1`, so the whole suite shared one bucket and exhausted 10 creates per minute within seconds. This is precisely the shared-IP false positive accepted under **Q-B (a)** — surfaced here as a concrete failure rather than a paragraph of speculation.

**Resolution:** the limiter is disabled by default for integration tests (`AbstractPostgresIT` sets `shortener.rate-limit.enabled=false`), and `RateLimitIT` opts back in. Production configuration is unchanged — `application.yml` ships with `enabled: true`.

An engineer edit was needed to make that work: `@DynamicPropertySource` in the base class takes precedence over `@TestPropertySource` in a subclass, so the first attempt left the limiter off in `RateLimitIT` itself and the 429 assertion failed with a 201. The base class now reads a system property that `RateLimitIT` sets in a static initializer.

`RateLimitIT` also lowers the limits to 3 and 5 via `@TestPropertySource`. That keeps the test fast **and** proves acceptance criterion 7 as a side effect: if limits were constants rather than configuration, the test could not have changed them.

### 3.3 Operational consequence for evaluators

The service ships with the limiter **on** at 10 creates/minute per IP. Running the README smoke commands repeatedly from one machine will produce a `429` on the 11th create within a minute. That is correct behaviour, not a defect. To exercise the API freely:

```bash
SHORTENER_RATE_LIMIT_ENABLED=false docker compose up
```

### 3.4 Limitations carried to `docs/08`
- **Shared IP:** clients behind one NAT or proxy share a bucket (accepted, Q-B a).
- **Reverse proxy blindness:** because `X-Forwarded-For` is untrusted (Q5), deploying behind a proxy collapses all clients into one bucket — the limiter effectively stops discriminating. The secure default was chosen over the useful-but-spoofable one; production needs trusted-proxy configuration.
- **Single node:** buckets are per-process. Two instances behind a load balancer give each client 2× the intended budget.
- **IPv6 /128 granularity:** a client with a /64 can rotate addresses to evade the limit (Q6).

### 3.5 What this scenario demonstrates
The nine-word requirement contained fifteen unstated decisions. None of them were technically difficult; the engineering was in surfacing them, proposing defensible defaults, getting an explicit answer, and then implementing exactly what was agreed — with the consequences of those answers documented and, in the Q-B case, demonstrated by a failing test suite.
