# 04 — Scenario 1: Greenfield (core APIs)

**Scope:** tasks B1–B8 in `docs/03-task-breakdown.md`
**Tag:** `v0.1-greenfield`
**Status:** Complete — `mvn verify` green, 65 unit + 14 integration tests, JaCoCo gate met

---

## 1. Decomposition

| Task | What | Prompt | Risk |
|------|------|--------|------|
| B1 | `ShortUrl` entity, repository, DTOs, RFC 7807 handler | 05 | L |
| B2 | `UrlValidator` — scheme/length/SSRF policy | 05 | **H** |
| B3 | `ShortCodeGenerator` port + `RandomBase62Generator`, `AliasPolicy` | 05 | M |
| B4 | `UrlService.create` — alias vs generated, collision retry | 05 | **H** |
| B5 | `UrlService.resolve` + `RedirectController` — 302/404/410 | 05 | **H** |
| B6 | `UrlController` — POST/GET/DELETE, Bean Validation | 05 | M |
| B7 | Unit + integration test suite | 06 | M |

Sequencing followed the dependency graph in `docs/03`: entity → validator/generator → service → controllers → tests. The clock was made injectable from the start (`AppConfig.clock()`) so expiry could be tested without sleeps.

## 2. Execution

### 2.1 What the AI generated
27 main-source files and 7 test files from Prompts 05 and 06 (`prompts/05-core-apis.md`, `prompts/06-greenfield-tests.md`). Prompts followed the INTENT / CONTEXT / CONSTRAINTS / ACCEPTANCE CRITERIA / OUTPUT FORMAT template.

### 2.2 What the engineer changed (see `docs/09` for full rows)

| # | AI output | Gate / trigger | Engineer action |
|---|-----------|----------------|-----------------|
| 1 | `UrlValidator.rejectUnsafeHost` | Checkstyle: cyclomatic complexity 11 > 10 | **Edited** — extracted `isInternalAddress()`; behaviour unchanged |
| 2 | Surefire/Failsafe config (scaffold) | Coverage gate failed with `0.00` once application code existed; log showed `JUnitPlatformProvider`, `Tests run: 0` | **Edited** — pinned `surefire-testng` provider on both plugins. Prior "green" runs had executed zero tests. |
| 3 | `TestClockConfig.clock()` bean | IT context failed: `BeanDefinitionOverrideException` on name `clock` | **Edited** — renamed to `testClock()`, rely on `@Primary` |
| 4 | Planted-defect comment in `UrlService.createWithAlias` | Engineer decision | **Rejected** — removed so the brownfield defect is discovered, not signposted |
| 5 | DNS resolution in `UrlValidator` (AI offered as option) | Engineer decision | **Rejected** — literal-IP blocking only; rationale in class Javadoc |

### 2.3 Engineer decisions recorded
- Validator blocks literal IPs / localhost names only; no DNS resolution (determinism, rebinding makes it ineffective, 302 is client-side not server-side fetch).
- `Cache-Control: no-store` on redirects retained — every click must reach the service for analytics and expiry checks.
- Alias `DataIntegrityViolationException` left unmapped at v0.1 (returns 500). This is the brownfield defect for tasks C5/C6.

## 3. Validation

### 3.1 Manual smoke (dev profile, H2)
```
POST /api/v1/urls {"longUrl":"https://example.com/some/long/path"}
  → 201 {"shortCode":"qYQEbOM", ...}
GET /qYQEbOM
  → 302  Location: https://example.com/some/long/path  Cache-Control: no-store
GET /nope1234
  → 404  application/problem+json  type=.../not-found
POST /api/v1/urls {"longUrl":"http://169.254.169.254/latest"}
  → 422  application/problem+json  detail="Host is not permitted"
POST /api/v1/urls {"longUrl":"https://example.com","customAlias":"my-link"}
  → 201  {"shortCode":"my-link","customAlias":true, ...}
DELETE /api/v1/urls/my-link
  → 204
GET /my-link
  → 410  application/problem+json  type=.../gone
```

### 3.2 Automated
```
[INFO] Using configured provider org.apache.maven.surefire.testng.TestNGProvider
[INFO] Tests run: 65, Failures: 0, Errors: 0, Skipped: 0   -- surefire (unit, H2)
[INFO] Tests run: 14, Failures: 0, Errors: 0, Skipped: 0   -- failsafe (Testcontainers postgres:16-alpine)
[INFO] All coverage checks have been met.                    -- JaCoCo, application package >= 80% line
[INFO] BUILD SUCCESS
```

Unit tier: `UrlValidatorTest` (35 table-driven cases), `AliasPolicyTest`, `RandomBase62GeneratorTest`, `UrlServiceTest` (collision retry, exhaustion, expiry, gone, idempotent delete).
Integration tier: `UrlApiIT` (13 cases: every status code, actuator not shadowed, no stack-trace leakage), `HealthIT`. Expiry is tested by advancing a `MutableClock` bean — no `Thread.sleep`.

### 3.3 Not covered at v0.1 (deliberate)
Concurrent creation of the same custom alias. Reproduced red-first in the brownfield scenario (`docs/05`).

## 4. Sign-off

| Task | Reviewed | Evidence | Decision |
|------|----------|----------|----------|
| B2 `UrlValidator` | Sam | 35 unit cases incl. metadata IP, CGNAT, IPv6 loopback, credentials-in-URL; manual 422 | Approved |
| B4 `UrlService.create` | Sam | Retry ×3 then 503 proven with stubbed repository; alias path reviewed | Approved (alias 409 mapping deferred to C6 by design) |
| B5 redirect hot path | Sam | 302/404/410 ITs on Postgres; `no-store` verified | Approved |
