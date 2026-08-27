# 08 — Security Review, Risks and Trade-offs

**Reviewed commit:** `f8b99c0`
**Status:** §1 findings triaged and signed · §2–§4 complete

---

## 1. Adversarial review findings (task F1)

Severity: **H** exploitable or data-affecting · **M** real but bounded · **L** hygiene.
Disposition proposed by the reviewer; the engineer decides.

### 1.1 Security

| # | Sev | File | Finding | Why it matters | Proposed fix | Proposed disposition | Engineer |
|---|-----|------|---------|----------------|--------------|----------------------|----------|
| S-1 | **H** | `application.yml` | `shortener.analytics.ip-hash-salt` defaults to `dev-only-salt-change-me` | If deployed without `SHORTENER_IP_SALT`, the salt is public knowledge and IP hashes become reversible by rainbow table — the privacy control silently fails | Fail startup when the default salt is active outside the `dev` profile | **Must fix** — a security control that silently does nothing is worse than none | ☐ |
| S-2 | **H** | `UrlValidator` | Hostnames are not DNS-resolved, so `http://internal-box.corp/` is accepted | An attacker can mint links pointing at internal hosts; a user clicking one issues the request from inside the perimeter | Documented decision (docs/04 §2.3): resolution is nondeterministic, slow, and defeated by DNS rebinding; a 302 is client-side, not server-side fetch | **Accepted risk** — document prominently in §4 | ☐ |
| S-3 | M | `RedirectController` | `Location` header is taken from stored input | Header injection if a stored URL contained CR/LF | `URI.create()` rejects control characters, and `UrlValidator` runs at write time — but there is no explicit test | **Must fix** — add a test asserting CR/LF in `longUrl` is rejected at creation | ☐ |
| S-4 | M | `ClickEventListener` | Referrer and User-Agent are stored verbatim (truncated) | Values are attacker-controlled; if the stats API output is later rendered in a browser dashboard, stored XSS | Values are returned as JSON, never HTML, and no UI exists — but note the constraint for any future consumer | **Accepted risk** — document | ☐ |
| S-5 | M | whole service | No authentication on any endpoint (AR-11) | Anyone can create, read stats for, and delete any link — including links they did not create | Out of scope by AR-11 | **Accepted risk** — must appear in limitations, not be discovered by the reviewer | ☐ |
| S-6 | M | `RateLimitFilter` | `X-Forwarded-For` untrusted, so behind a proxy every client shares one bucket | The limiter effectively stops discriminating in the most likely production topology | Trusted-proxy allow-list (docs/06 §1.6) | **Accepted risk** — already documented in docs/06 §3.4 | ☐ |
| S-7 | L | `docker-compose.yml` | Postgres password `shortener` in plain text | Local development only; a reviewer will still flag it | Note that it is a dev-only credential; production uses secrets injection | **Document** in README | ☐ |
| S-8 | L | `application-dev.yml` | H2 console enabled on `/h2-console` | Exposes a DB console if the `dev` profile ever reached a shared environment | Confined to `dev`; `docker` profile does not enable it | **Accepted risk** — verify the profile split is correct | ☐ |
| S-9 | L | `GlobalExceptionHandler` | Detail messages echo the submitted code, e.g. `Unknown short code: <input>` | Reflected input in an error body; harmless as JSON, but an enumeration oracle | Acceptable — the code is the resource identifier | **No action** | ☐ |
| S-10 | M | dependencies | OWASP dependency-check is not bound to `mvn verify` | A vulnerable transitive dependency would not fail the local build | Runs in CI (task A6/F1) and on demand; first run downloads the NVD database (minutes) | **Must fix** — ensure it actually runs in CI before submission | ☐ |

### 1.2 Correctness and reliability

| # | Sev | File | Finding | Why it matters | Proposed fix | Proposed disposition | Engineer |
|---|-----|------|---------|----------------|--------------|----------------------|----------|
| C-1 | **H** | `AnalyticsService` | Aggregation loads every click in a 30-day window into memory | A popular link with millions of clicks would OOM the service on a stats request | SQL `GROUP BY date_trunc(...)`; or cap the rows fetched | **Accepted risk for prototype** — document the ceiling explicitly with a number, not a vague "won't scale" | ☐ |
| C-2 | M | `InMemoryRateLimiter` | Buckets are per-process | Two instances behind a load balancer give each client 2× the intended budget | Redis-backed limiter | **Accepted risk** — documented | ☐ |
| C-3 | M | `ClickEventListener` | At-most-once delivery | Clicks are lost on crash or queue saturation | Kafka (ADR-004 upgrade path); `analytics.events.dropped` counter makes loss visible | **Accepted risk** — documented in ADR-004 | ☐ |
| C-4 | M | `UrlService.create` | Generated-code collision retry is bounded at 3, then 503 | Under pathological collision rates a client sees 503 rather than a code | Keyspace is 62⁷; the metric would show it long before users did | **No action** | ☐ |
| C-5 | L | `ShortUrl.isGone` | Expiry uses `!expiresAt.isAfter(now)` — exactly-at-expiry counts as gone | Boundary behaviour should be deliberate, not incidental | Correct as written; add a comment | **Nice to have** | ☐ |

### 1.3 Test quality

| # | Sev | Finding | Why it matters | Proposed disposition | Engineer |
|---|-----|---------|----------------|----------------------|----------|
| T-1 | M | `AnalyticsIT` asserts the click row appears asynchronously, but not that the HTTP response was flushed first | The ordering guarantee rests on `@Async` semantics, not on an assertion. A future refactor removing `@Async` would not fail this test. | **Document the gap** in docs/07; a stronger test would need response-time instrumentation | ☐ |
| T-2 | M | No test covers a saturated analytics executor incrementing `analytics.events.dropped` | Planned in docs/05 §1.7 as `ClickEventRejectionTest`; never written | **Must fix or explicitly drop** — do not leave a planned test silently missing | ☐ |
| T-3 | L | `RandomBase62GeneratorTest.producesNoDuplicatesInASmallSample` asserts 10 000 unique draws | Probabilistic; astronomically unlikely to flake given 62⁷, but it is a non-deterministic assertion | **No action** — documented in the test comment | ☐ |
| T-4 | L | No test asserts that `/api/v1/urls/{code}` (metadata) is reachable when the limiter is enabled | Bucket mapping for non-redirect reads is unit-tested but not exercised end-to-end | **Nice to have** | ☐ |

### 1.4 Reviewer's summary

Three findings are worth acting on before submission: **S-1** (default salt), **S-3** (CR/LF test), **T-2** (missing planned test), plus verifying **S-10** (dependency-check runs in CI). Everything else is either a documented, deliberate trade-off or cosmetic. The codebase's main weakness is not a vulnerability — it is that several correct decisions (S-2, C-1, C-2, C-3) are only defensible *because* they are written down, so the limitations section carries real weight.

### 1.5 Engineer triage

| Finding | Decision | Rationale |
|---------|----------|-----------|
| S-1 | **Fix** | A privacy control that silently does nothing is worse than no control. Fixed by `SaltGuard`. |
| S-3 | **Fix** | Cheap to close. Turned out the validator already rejected CR/LF — now proven by test rather than assumed. |
| S-10 | **Fix** | Wired into CI as a failing gate at CVSS ≥ 7; moved to nightly because a cold NVD download exceeded 25 minutes. |
| T-2 | **Fix** | A test that was planned in docs/05 and quietly never written is exactly what a reviewer should find. Written. |
| All others | **Accept as proposed** | Each is a deliberate trade-off already documented in the relevant ADR or scenario. |

Signed: Masud Siraj   Date: 2026-08-27

---

## 2. Risk register

| Risk | Likelihood | Impact | Mitigation | Detection |
|------|-----------|--------|------------|-----------|
| Deployed with the default IP-hash salt | Low (now) | High — click IPs become reversible | `SaltGuard` refuses to start outside `dev`/`test` (S-1) | Startup failure with an explicit message |
| Short link points at an internal host via DNS name | Medium | Medium — SSRF-adjacent; user's browser makes the request | Literal IP/localhost blocking; DNS resolution deliberately not attempted (S-2) | None automated — accepted |
| Analytics queue saturates and drops clicks | Medium under load | Low — click counts only | Bounded executor, discard policy | `analytics.events.dropped` counter |
| Stats request on a very high-volume link exhausts heap | Low at prototype scale | High — OOM | 30-day window plus index; **ceiling is roughly 10⁵ clicks per link** | Heap metrics; no automated guard (C-1) |
| Rate limiter ineffective behind a reverse proxy | High in production | Medium — limiter stops discriminating | Documented; needs trusted-proxy config | `ratelimit.rejected` collapsing to near-zero variance |
| Two instances double each client's rate budget | Certain if scaled out | Medium | Redis-backed limiter (one implementation swap) | Per-instance metric divergence |
| Unauthenticated deletion of any link | Certain if exposed publicly | High | None — out of scope by AR-11 | None — must not be deployed publicly as-is |
| Vulnerable transitive dependency ships | Low | Varies | dependency-check fails on CVSS ≥ 7 | Nightly CI job |
| Clicks lost on process crash | Low | Low | At-most-once accepted (ADR-004) | Gap between redirect count and click count |

---

## 3. Trade-offs per ADR

| ADR | Decision | Bought | Paid |
|-----|----------|--------|------|
| 001 | Single Spring Boot service, layered | Fast to build, familiar to reviewers, strong test tooling | Verbose; layer rule enforced by convention, not by ArchUnit |
| 002 | Random base62 codes, generator behind an interface | Non-enumerable codes; DB-independent generation | Needs collision retry; keyspace argument rather than a guarantee |
| 003 | PostgreSQL runtime, H2 unit tests, Testcontainers ITs | Constraint and concurrency behaviour proven on the real engine | Docker required for `mvn verify`; two test tiers to maintain |
| 004 | Async in-process click events | Redirect latency unaffected; analytics failure isolated | At-most-once delivery; single-node only |
| 005 | Caffeine in-process cache | No extra infrastructure; measurable hit ratio | Per-process; stale for up to TTL after delete in a cluster |
| 006 | RFC 7807 everywhere | Predictable machine-readable contract; easy to assert | Redirect 404/410 return JSON rather than HTML |
| 007 | Soft delete | Analytics survive deletion; unambiguous 410; codes never reused | Unbounded retention; no purge job |

Cross-cutting: **rate limiting on by default** trades a good first-run experience for demonstrating
the feature honestly. The README says how to switch it off.

---

## 4. Known limitations

Ordered by how much they would matter in production.

1. **No authentication or authorization.** Anyone can create, inspect, and delete any link. Out of
   scope per AR-11; this alone makes the service unfit for public deployment as-is.
2. **Single instance.** The redirect cache and the rate limiter are per-process. Two instances mean
   stale cache entries after a delete and double the intended rate budget.
3. **Rate limiting is blind behind a proxy.** `X-Forwarded-For` is untrusted by design (docs/06 Q5),
   so a reverse proxy collapses every client into one bucket.
4. **Analytics aggregation is in-memory.** Roughly 10⁵ clicks per link is the practical ceiling for a
   stats request; beyond that it needs SQL aggregation.
5. **At-most-once click capture.** Crashes and queue saturation lose events; the counter makes the
   loss visible but does not prevent it.
6. **Hostname-based SSRF is not blocked.** Literal IPs and `localhost` are rejected; `internal.corp`
   is not (docs/04 §2.3).
7. **No load testing.** The k6 script (task E5) was cut on Day 3. There is no measured p95 for the
   redirect path.
8. **No retention or purge.** Soft-deleted links and click events accumulate indefinitely.
9. **IPv6 limited at /128.** A client with a /64 can rotate addresses to evade the rate limit.
10. **Dev credentials in `docker-compose.yml`.** Plain-text Postgres password, local development
    only; production would inject secrets.

---

## 5. What one more week would buy

In priority order: API-key authentication and ownership; Redis for the cache and rate limiter, making
the service horizontally scalable; SQL aggregation plus a rollup table for analytics; Kafka for
click events (durable, replayable); the k6 load profile with a published p95; trusted-proxy
configuration; and a retention job for expired links and old click data.
