# 06 — Scenario 3: Ambiguous requirement

**The requirement, as received:** *"Add rate limiting to the service."*

Nine words. It names no endpoint, no subject, no limit, no window, no behaviour on breach, and no storage. Implementing it directly would mean inventing five product decisions and presenting them as engineering. This document separates what was asked from what had to be decided.

**Scope:** tasks D1–D5 in `docs/03-task-breakdown.md`
**Status:** §1 questions raised — awaiting engineer (product owner) answers · §2–§3 pending

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
| Q-A | Limit the redirect path? | ☐ (a) Yes, 100/min ☐ (b) No |
| Q-B | Shared-IP false positives | ☐ (a) Accept + document ☐ (b) Allow-list CIDRs |
| Q5 | Trust `X-Forwarded-For`? | ☐ No (recommended) ☐ Yes |
| Q12 | Limiter internal error | ☐ Fail open (recommended) ☐ Fail closed |
| Q7 | Limits | ☐ 10 / 100 per min ☐ Other: ______ |

Signed: ____________ Date: ________

---

## 2. Implementation (D2–D4)
_Pending answers in §1.7._

## 3. Validation and production delta (D5)
_Pending._
