# 07 — Testing Approach, Limitations and Trade-offs

**As of commit:** post-`F2` · **Totals:** 101 unit + 22 integration, JaCoCo gate met, `mvn verify` green

---

## 1. Strategy

Two tiers, split by what each is good at.

| Tier | Framework | Database | Runs in | Answers |
|------|-----------|----------|---------|---------|
| Unit (`*Test`) | TestNG + Mockito + AssertJ | H2 only where a context is needed; most tests need none | surefire, `mvn test` | "Is this logic correct in isolation?" |
| Integration (`*IT`) | REST Assured + `@SpringBootTest` | **Testcontainers PostgreSQL 16** | failsafe, `mvn verify` | "Does the wired system behave as specified against the real engine?" |

Concurrency and constraint behaviour are tested **only** in the integration tier, against real
PostgreSQL. Testing a unique-index race on H2 would prove nothing about production (ADR-003).

### Determinism
No test sleeps. Time is injected: `AppConfig` exposes a `Clock` bean, and tests substitute
`MutableClock`, advancing it explicitly. Link expiry is tested by moving the clock six minutes, not
by waiting. The one place waiting is unavoidable — asserting that an asynchronous write eventually
lands — uses Awaitility with a bounded timeout rather than a fixed sleep.

### Isolation
Each test creates its own data with unique codes; no test depends on another's state or on
execution order. This was enforced the hard way — see §4.

---

## 2. Quality gates

Every gate runs locally in `mvn verify` and again in CI. A gate that only runs in CI is a gate
developers learn to ignore.

| Gate | Tool | Threshold | Catches |
|------|------|-----------|---------|
| Formatting | Spotless (google-java-format) | any deviation | review noise, diff churn |
| Static analysis | Checkstyle | complexity ≤ 10, method ≤ 60 lines, no star imports, no empty catch | the AI's 11-complexity validator method (docs/09 row 7) |
| Unit tests | TestNG via surefire | all pass | logic regressions |
| Integration tests | failsafe + Testcontainers | all pass | wiring, SQL, concurrency, HTTP contract |
| Coverage | JaCoCo | ≥ 80 % line on `com.siraj.shortener.application` | untested branches in the layer that holds the rules |
| Dependencies | OWASP dependency-check | **fails on CVSS ≥ 7** | vulnerable transitive dependencies |
| Container | `docker build` | must succeed | a broken image that tests would never notice |

**Scoping the coverage gate to `application` is deliberate.** A repository-wide percentage is easy
to satisfy with tests of getters. The application package is where the decisions live, so that is
where the threshold is enforced.

**Dependency-check runs nightly and on demand, not on every push.** The NVD download made a cold
run exceed 25 minutes, which would have made the feedback loop unusable. It remains a failing gate
— the trade-off is latency of detection, not strength of the gate.

---

## 3. What each gate actually caught

Not hypothetical — these are the failures that occurred.

| Gate | What it caught |
|------|----------------|
| Checkstyle | AI-generated `UrlValidator.rejectUnsafeHost` at cyclomatic complexity 11 |
| JaCoCo | **That the test suite was running zero tests.** Surefire had auto-selected the JUnit provider; the build was "green" because nothing ran. Only visible once real code existed and coverage read 0.00. |
| Integration tier | Hibernate `validate` rejecting `CHAR(64)` on H2; a bean-name collision on `clock`; the alias race (49× 500) |
| CI | An order-dependent test that passed locally more than twenty times (§4) |

The zero-tests incident is the single most valuable thing the gates did. A suite that runs nothing
looks exactly like a suite that passes everything.

---

## 4. Test-design lesson: global state is an ordering bug waiting to happen

Enabling rate limiting broke three unrelated integration tests, because every test client connects
from `127.0.0.1` and therefore shared one bucket. The first fix used a system property set in a
static initializer and cleared in `@AfterClass`.

That worked locally and failed on CI. TestNG ordered the classes differently there, so the flag was
still set when `AnalyticsIT` ran, and it hit 429s. The property was global mutable state with
order-dependent cleanup — the local pass was luck, not correctness.

The fix removed the shared flag entirely: each integration test declares its own
`@TestPropertySource`. No ordering assumption remains. Recorded as docs/09 rows 25–26.

---

## 5. Coverage summary

| Package | Tested by | Gate |
|---------|-----------|------|
| `application` | `UrlServiceTest`, `UrlValidatorTest` (38 cases), `AliasPolicyTest`, `AnalyticsServiceTest`, `IpHasherTest` | ≥ 80 % enforced |
| `ratelimit` | `TokenBucketTest`, `InMemoryRateLimiterTest`, `RateLimitFilterTest` | covered, not gated |
| `infrastructure` | `ClickEventListenerTest`, both generator contract tests | covered, not gated |
| `config` | `SaltGuardTest`, `AnalyticsExecutorConfigTest` | covered, not gated |
| `api` | `UrlApiIT`, `AnalyticsIT`, `RateLimitIT`, `AliasConcurrencyIT`, `HealthIT` | integration only |

---

## 6. What is deliberately not tested

Stating these is the point; an untested area that nobody has named is a gap, one that is named is a
decision.

| Not tested | Why | Risk accepted |
|------------|-----|---------------|
| Response flushed **before** the click row is written | Would require response-time instrumentation; the guarantee comes from `@Async` semantics | A refactor removing `@Async` would not fail a test (docs/08 T-1) |
| Load and latency (k6) | Time-boxed out on Day 3 | No measured p95 for the redirect path; the scaling ceiling in docs/08 C-1 is reasoned, not measured |
| Multi-node behaviour | Single-instance prototype by design (ADR-005) | Per-process cache and rate limiter are untested in a clustered topology |
| Failure injection (DB down mid-request) | No chaos tooling in scope | Graceful degradation is designed and code-reviewed, not proven |
| Browser-level redirect behaviour | API-only service | `Cache-Control: no-store` is asserted as a header, not observed in a browser |
| Generated-code collision at scale | 62⁷ keyspace makes it unreachable in a test | Retry path is unit-tested with a stubbed repository instead |

---

## 7. Trade-offs in the test strategy itself

1. **Testcontainers over an embedded database for the integration tier** — slower (~30–70 s) but the
   only way the alias race proves anything. Unit tests stay on H2 for a fast inner loop.
2. **TestNG over JUnit 5** — matches the engineer's day-to-day stack. Cost: it is not Spring Boot's
   default, which is exactly how the zero-tests defect was introduced.
3. **Table-driven validator tests** — 38 cases in one method rather than 38 methods. Cheaper to
   extend; a failure reports the offending row.
4. **Contract test for generators** — `ShortCodeGeneratorContract` is inherited by both
   implementations, so a third generator gets the same guarantees for free.
5. **No mocking of the database in integration tests** — if it does not run against PostgreSQL, it
   is not an integration test.
