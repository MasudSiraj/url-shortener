# url-shortener

A URL shortener built as an AI-assisted engineering exercise. The service is the artefact; the
`docs/` directory is the point — it records how the requirement was interpreted, decomposed,
executed with AI assistance, and validated.

[![CI](https://github.com/MasudSiraj/url-shortener/actions/workflows/ci.yml/badge.svg)](https://github.com/MasudSiraj/url-shortener/actions/workflows/ci.yml)

---

## Quick start

### With Docker (PostgreSQL) — recommended

```bash
docker compose up --build
curl localhost:8080/actuator/health
```

### Without Docker (H2, in-memory)

```bash
mvn spring-boot:run
```

### Try it

```bash
# create a short link
curl -s -X POST localhost:8080/api/v1/urls \
  -H 'Content-Type: application/json' \
  -d '{"longUrl":"https://example.com/some/long/path"}'
# → 201 {"shortCode":"qYQEbOM","shortUrl":"http://localhost:8080/qYQEbOM", ...}

# follow it
curl -i localhost:8080/qYQEbOM
# → 302  Location: https://example.com/some/long/path

# custom alias, optional expiry
curl -s -X POST localhost:8080/api/v1/urls -H 'Content-Type: application/json' \
  -d '{"longUrl":"https://example.com","customAlias":"my-link","expiresAt":"2027-01-01T00:00:00Z"}'

# analytics
curl -s localhost:8080/api/v1/urls/my-link/stats

# metadata / soft delete
curl -s localhost:8080/api/v1/urls/my-link
curl -i -X DELETE localhost:8080/api/v1/urls/my-link   # → 204; the link then returns 410
```

> **Rate limiting is on by default** — 10 creates and 100 reads per minute per client IP. Repeating
> the commands above quickly will produce a `429` with a `Retry-After` header. That is correct
> behaviour (see `docs/06`). To disable it while exploring:
> `SHORTENER_RATE_LIMIT_ENABLED=false docker compose up`

Interactive API docs: <http://localhost:8080/swagger-ui.html>

---

## API

| Method | Path | Purpose | Success | Errors |
|--------|------|---------|---------|--------|
| POST | `/api/v1/urls` | create a short link | 201 + `Location` | 400 invalid, 409 alias taken, 422 unsafe host, 429 |
| GET | `/{code}` | redirect | 302 | 404 unknown, 410 expired/deleted, 429 |
| GET | `/api/v1/urls/{code}` | metadata | 200 | 404 |
| DELETE | `/api/v1/urls/{code}` | soft delete | 204 | 404 |
| GET | `/api/v1/urls/{code}/stats` | click analytics | 200 | 404 |
| GET | `/actuator/health`, `/actuator/prometheus` | ops | 200 | — |

All errors are RFC 7807 `application/problem+json`.

---

## Running the tests

```bash
mvn test      # 101 unit tests (TestNG, H2) — no Docker needed
mvn verify    # + 22 integration tests (REST Assured on Testcontainers PostgreSQL),
              #   Spotless, Checkstyle, JaCoCo (>= 80% on the application package)
mvn dependency-check:check   # OWASP scan; first run downloads the NVD database (slow)
```

Docker must be running for `mvn verify` — the integration tier uses Testcontainers.

---

## Project structure

```
src/main/java/com/siraj/shortener/
├── api/              controllers, DTOs, RFC 7807 exception handling
├── application/      use cases, validation, analytics, ports
├── domain/           entities and repository ports
├── infrastructure/   generators, async click listener
├── ratelimit/        token bucket, limiter, servlet filter
└── config/           typed properties, executor, clock, salt guard
src/main/resources/db/migration/   Flyway V1–V3
```

Dependency rule: `api → application → domain ← infrastructure`. Schema is owned by Flyway;
Hibernate runs with `ddl-auto: validate`.

---

## Configuration

| Property / env var | Default | Purpose |
|--------------------|---------|---------|
| `SHORTENER_IP_SALT` | dev-only default | Salt for hashing click IPs. **The app refuses to start outside `dev`/`test` if unset** (finding S-1). |
| `shortener.generator` | `random` | `random` or `sequence` (ADR-002) |
| `shortener.code-length` | 7 | generated code length |
| `shortener.rate-limit.enabled` | `true` | master switch |
| `shortener.rate-limit.create-per-minute` | 10 | POST budget per IP |
| `shortener.rate-limit.redirect-per-minute` | 100 | read budget per IP |
| `shortener.cache.max-size` / `.ttl` | 10000 / 10m | redirect cache |

---

## Documentation

| Document | What it covers |
|----------|----------------|
| [`docs/01-requirement-analysis.md`](docs/01-requirement-analysis.md) | intent, FR/NFR, ambiguity register with adopted assumptions |
| [`docs/02-architecture.md`](docs/02-architecture.md) | components, control flow, option analysis · [ADRs](docs/adr) |
| [`docs/03-task-breakdown.md`](docs/03-task-breakdown.md) | 44 tasks, dependencies, critical path, sign-off register |
| [`docs/04-scenario-greenfield.md`](docs/04-scenario-greenfield.md) | **Scenario 1** — core APIs |
| [`docs/05-scenario-brownfield.md`](docs/05-scenario-brownfield.md) | **Scenario 2** — impact analysis, analytics, red→green defect fix |
| [`docs/06-scenario-ambiguous.md`](docs/06-scenario-ambiguous.md) | **Scenario 3** — 15 questions from a 9-word requirement |
| [`docs/07-testing-approach.md`](docs/07-testing-approach.md) | test strategy, gates, what is not tested |
| [`docs/08-risks-tradeoffs.md`](docs/08-risks-tradeoffs.md) | security review findings, risk register, limitations |
| [`docs/09-ai-traceability-log.md`](docs/09-ai-traceability-log.md) | every AI interaction: generated / edited / rejected, with rationale |
| [`docs/10-final-engineering-summary.md`](docs/10-final-engineering-summary.md) | plan, rationale, evidence against each evaluation criterion |
| [`prompts/`](prompts) | every prompt used, verbatim |

---

## Stack

Java 21 · Spring Boot 3.3 · PostgreSQL 16 / H2 · Flyway · Caffeine · Micrometer · TestNG ·
REST Assured · Testcontainers · JaCoCo · Spotless · Checkstyle · OWASP dependency-check ·
Docker · GitHub Actions
