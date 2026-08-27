# Schwab Assignment — AI-Assisted URL Shortener: Step-by-Step Prompt Playbook

**Goal:** Produce a runnable prototype *plus* the evidence trail the graders want (requirement analysis, decomposition, three scenarios, traceability, quality gates, final summary).

**Stack (recommended):** Java 21, Spring Boot 3, Spring Data JPA, H2 (dev) / PostgreSQL via Docker Compose (prod-like), Caffeine or Redis cache, Flyway, TestNG + REST Assured + Testcontainers, JaCoCo, Spotless/Checkstyle, OWASP Dependency-Check, GitHub Actions CI.

**Repo layout to create on Day 1 (so docs accumulate as you go):**
```
url-shortener/
├── src/main/java/...            # code
├── src/test/java/...            # unit + integration tests
├── docs/
│   ├── 01-requirement-analysis.md
│   ├── 02-architecture.md
│   ├── 03-task-breakdown.md
│   ├── 04-scenario-greenfield.md
│   ├── 05-scenario-brownfield.md
│   ├── 06-scenario-ambiguous.md
│   ├── 07-testing-approach.md
│   ├── 08-risks-tradeoffs.md
│   ├── 09-ai-traceability-log.md
│   └── 10-final-engineering-summary.md
├── prompts/                     # every prompt you used, numbered (this file's prompts)
├── .github/workflows/ci.yml
├── docker-compose.yml
└── README.md
```

**Prompt discipline (use this template for every prompt — the assignment explicitly asks for it):**
```
INTENT: <what outcome and why>
CONTEXT: <stack, existing code/files, constraints already decided>
CONSTRAINTS: <what NOT to do, standards, security rules>
ACCEPTANCE CRITERIA: <how I will judge the output>
OUTPUT FORMAT: <files, diff, markdown table, etc.>
```

**Traceability log entry (add one row per AI interaction in docs/09):**
| # | Task | Prompt file | Status (Generated / Edited / Rejected) | What I changed & why | Quality gate result |

---

## Evidence Map — how each graded capability becomes visible in the repo

Evaluators cannot watch you work; each capability must leave an artifact, and the seam between AI draft and engineer decision must be visible.

| Capability (§1) | Artifact | What makes it convincing |
|-----------------|----------|--------------------------|
| Requirement understanding | `docs/01-requirement-analysis.md` | Ambiguity register rows marked **Edited/Rejected** with your rationale — proof a human interpreted the requirement, not just accepted the AI's reading. |
| Task decomposition | `docs/03-task-breakdown.md` + GitHub issues/project board | Issues carry dependency links, acceptance criteria, and a risk label; commit messages reference issue IDs (`feat(analytics): add click table [#12]`) so requirement → task → commit is traceable. |
| Multi-step execution | Git history + `docs/09-ai-traceability-log.md` + `prompts/` | One commit/PR per step (scaffold → API → tests → analysis doc → analytics → failing test → fix → refactor). Each log row cites the prompt file, commit hash, and what you changed. Rejections carry more weight than acceptances. |
| Output generation/validation | CI + `docs/07-testing-approach.md` | Pasted, real outputs: the failing concurrent test before the fix, JaCoCo %, dependency-check result, k6 latency numbers. Evidence, not claims. |

**Engineer-led, not autonomous — three practices that signal it:**
1. **You write the prompt, you state the constraints.** Every `prompts/NN-*.md` opens with your INTENT and CONSTRAINTS.
2. **Sign-off notes on high-impact changes.** Schema migration, SSRF validation, concurrency fix — each gets a short log note: "Reviewed diff line-by-line, verified unique constraint in V1 migration, approved — Sam." Three is enough.
3. **One deliberate rejection per scenario.** Greenfield, brownfield, ambiguous — each shows a place you overruled the AI and did something different.

Mental model: **AI drafts, you decide, the repo shows the seam.**

---

## Phase 0 — Requirement Understanding (Req 1)

### Prompt 1 — Normalize the requirement
```
INTENT: Turn the attached Schwab assignment into a clear engineering problem statement.
CONTEXT: I am a senior SDET/engineer building a URL shortener prototype over 2-3 days using AI assistance. The graders evaluate process quality as much as code.
CONSTRAINTS: Do not propose a design yet. Do not invent requirements that aren't implied by the document.
ACCEPTANCE CRITERIA:
 - Explicit intent statement (1 paragraph)
 - Functional requirements list (numbered, each traceable to a section of the document)
 - Non-functional requirements (reliability, security, observability, performance)
 - Ambiguity register: every unclear/unstated item, with 2-3 possible interpretations and the assumption I should adopt, with rationale
 - Out-of-scope list
OUTPUT FORMAT: Markdown for docs/01-requirement-analysis.md
```
*Your job after:* edit the assumptions — you decide, not the AI. Log the edits.

---

## Phase 1 — Architecture (Deliverable: Architecture overview)

### Prompt 2 — Architecture options & decision
```
INTENT: Choose an architecture for the URL shortener that is production-shaped but buildable in 2-3 days.
CONTEXT: Java 21, Spring Boot 3, Maven. Requirements in docs/01-requirement-analysis.md (paste it).
CONSTRAINTS: Single deployable service; no Kubernetes required but must be containerized; must run locally with one command; no cloud-only dependencies.
ACCEPTANCE CRITERIA:
 - Component diagram (Mermaid) with control flow for shorten and redirect
 - Compare 2 options each for: ID generation (base62 counter vs random/hash), storage (Postgres vs H2 vs Redis-only), caching, analytics capture (sync vs async/event)
 - Recommend one per category with trade-offs and failure scenarios
 - List of ADRs to write (title + decision + consequences)
 - Explain where AI is used vs where engineer owns decisions
OUTPUT FORMAT: Markdown for docs/02-architecture.md plus separate ADR files under docs/adr/
```

---

## Phase 2 — Task Decomposition (Req 2)

### Prompt 3 — Backlog with dependencies and sequencing
```
INTENT: Break the architecture into an ordered, executable task list for a 2-3 day build.
CONTEXT: docs/01 and docs/02 (paste both).
CONSTRAINTS: Each task ≤ 2 hours; every task has acceptance criteria and a test strategy; mark which tasks are high-impact (need human sign-off) vs low-impact.
ACCEPTANCE CRITERIA:
 - Table: ID, task, depends on, day/sequence, acceptance criteria, quality gate, risk level
 - Tasks grouped into: Foundation, Greenfield (core APIs), Brownfield (analytics + refactor/bug fix), Ambiguous (see Phase 5), Reliability, Docs
 - Critical path called out
OUTPUT FORMAT: Markdown for docs/03-task-breakdown.md
```

---

## Phase 3 — Greenfield Scenario: Core APIs (Req 5, Scenario 1)

### Prompt 4 — Project scaffold
```
INTENT: Generate the Spring Boot project skeleton.
CONTEXT: Java 21, Maven, Spring Boot 3.x, Spring Web, Spring Data JPA, Flyway, H2 (dev profile), Postgres (docker profile), Actuator, Validation. Package: com.<yourname>.shortener
CONSTRAINTS: Layered structure (controller/service/repository/domain/config); no Lombok on domain entities (explicit code, easier to review); externalized config via application.yml with dev/docker profiles; no secrets in code.
ACCEPTANCE CRITERIA: `mvn spring-boot:run` starts on port 8080; /actuator/health returns UP; Flyway V1 migration creates the url table.
OUTPUT FORMAT: Full file tree with contents.
```

### Prompt 5 — Core API implementation
```
INTENT: Implement the core shortener APIs.
CONTEXT: Scaffold from Prompt 4. Decisions from docs/02 (e.g., base62 over DB sequence / 7-char code).
CONSTRAINTS:
 - POST /api/v1/urls {longUrl, customAlias?, expiresAt?} → 201 {shortCode, shortUrl, longUrl, expiresAt}
 - GET /{shortCode} → 302 redirect; 404 if unknown; 410 if expired
 - GET /api/v1/urls/{shortCode} → metadata
 - Validate URL (scheme http/https only, max length, reject private/loopback hosts to prevent SSRF)
 - Idempotent: same longUrl from same client returns existing code (or document why not)
 - Global exception handler returning RFC 7807 problem+json
 - No business logic in controllers
ACCEPTANCE CRITERIA: OpenAPI spec generated (springdoc) and matches behavior; all inputs validated; collision handling on alias uniqueness.
OUTPUT FORMAT: Code files + openapi.yaml.
```

### Prompt 6 — Tests for core APIs
```
INTENT: Generate unit and integration tests for the core APIs.
CONTEXT: Code from Prompt 5.
CONSTRAINTS: TestNG for unit (service + ID generator), REST Assured + @SpringBootTest for integration, Testcontainers Postgres for one DB-backed suite; AAA structure; no test depends on another; deterministic (fixed clock injected).
ACCEPTANCE CRITERIA: Cover happy path, invalid URL, SSRF-blocked host, alias collision, expired link, unknown code; JaCoCo ≥ 80% on service layer; tests run in `mvn verify`.
OUTPUT FORMAT: Test files + note on what is NOT covered and why.
```
*Record in docs/04-scenario-greenfield.md:* decomposition → execution → validation, with the test output pasted.

---

## Phase 4 — Brownfield Scenario: Analytics + Refactor/Bug Fix (Req 3, Scenario 2)

### Prompt 7 — Codebase impact analysis (do this BEFORE the change)
```
INTENT: Analyze the existing codebase to add click analytics without breaking the redirect path.
CONTEXT: Paste current package tree and key classes (RedirectController, UrlService, UrlRepository, entity, migrations).
CONSTRAINTS: Analysis only, no code. Redirect latency must not increase measurably.
ACCEPTANCE CRITERIA:
 - Impacted modules/classes/APIs/data flows listed
 - Data flow diagram before/after (Mermaid)
 - Options: synchronous insert vs Spring ApplicationEvent async vs Kafka; recommend with trade-offs for a 2-day prototype
 - Migration plan (new table, indexes) and backward-compatibility notes
 - Test impact: which existing tests change, which new ones are needed
OUTPUT FORMAT: Markdown for docs/05-scenario-brownfield.md (section 1)
```

### Prompt 8 — Implement analytics
```
INTENT: Implement click analytics per the approved plan.
CONTEXT: docs/05 section 1 (approved by me), current code.
CONSTRAINTS: Async event capture (@Async + ApplicationEvent) with bounded executor; store timestamp, referrer, user-agent, hashed IP (never raw IP — privacy); GET /api/v1/urls/{code}/stats → total clicks, clicks by day (last 30), top referrers; Flyway V2 migration; do not touch redirect response semantics.
ACCEPTANCE CRITERIA: Existing tests still pass; new integration test proves redirect returns before analytics write commits; stats endpoint tested.
OUTPUT FORMAT: Diff-style changes per file.
```

### Prompt 9 — Planted bug fix + refactor (shows debugging with AI)
```
INTENT: Diagnose and fix a concurrency bug: two simultaneous requests for the same custom alias can both succeed.
CONTEXT: UrlService.create(), repository, migration V1.
CONSTRAINTS: Fix at the DB level (unique constraint) AND service level (handle DataIntegrityViolationException → 409); write a failing concurrent test first (ExecutorService, 50 threads), then make it pass. Then refactor UrlService to extract ShortCodeGenerator as an interface with two implementations (Base62Sequence, RandomAlphanumeric) selected by config.
ACCEPTANCE CRITERIA: Failing test reproduced and pasted; fix; test green; refactor keeps all tests green; no behavior change documented.
OUTPUT FORMAT: Test first, then fix, then refactor, each as a separate commit message + diff.
```

---

## Phase 5 — Ambiguous Scenario (Scenario 3)

Pick one deliberately vague requirement. Good candidates: **"Add rate limiting"** (per what? per IP, per API key, per endpoint? what limits? what response?) or **"Links should expire"** (default TTL? hard delete vs soft? who can extend?).

### Prompt 10 — Ambiguity resolution
```
INTENT: Resolve the ambiguous requirement "Add rate limiting to the service" into a concrete spec.
CONTEXT: Current architecture and APIs.
CONSTRAINTS: Do not implement yet. Treat me as the product owner — surface questions, propose defaults, and identify what changes depending on the answer.
ACCEPTANCE CRITERIA:
 - Clarifying questions grouped by: scope (which endpoints), identity (IP vs API key), limits/windows, behavior on breach (429 + Retry-After), storage (in-memory vs Redis), observability
 - For each, a recommended default for a prototype with rationale and what I'd change for production
 - Resulting normalized spec + acceptance criteria + test list
OUTPUT FORMAT: Markdown for docs/06-scenario-ambiguous.md
```
*Then:* you answer the questions in the doc (this is the "engineer-led" evidence), and run an implementation prompt in the same shape as Prompt 8, followed by tests as in Prompt 6.

---

## Phase 6 — Reliability, Security & Quality Gates (Req 4, 6)

### Prompt 11 — Reliability features
```
INTENT: Add production-shaped reliability to the service.
CONTEXT: Current code.
CONSTRAINTS: Read-through cache on redirect lookup (Caffeine, TTL, size bound, eviction on delete/expiry); Actuator health with DB indicator; readiness/liveness endpoints; graceful shutdown; structured JSON logging with correlation ID filter; Micrometer metrics for redirect latency and cache hit ratio; Resilience4j circuit breaker is optional—justify if included.
ACCEPTANCE CRITERIA: Cache hit path avoids DB (verified by test using repository spy); metrics visible at /actuator/prometheus.
OUTPUT FORMAT: Code diffs.
```

### Prompt 12 — Security & quality review (AI as reviewer)
```
INTENT: Review the codebase as a senior security-minded reviewer before I sign off.
CONTEXT: Full source (or list of files).
CONSTRAINTS: Check OWASP Top 10 relevance: open redirect, SSRF, injection, input validation, information leakage in errors, dependency vulnerabilities, secrets in config, log injection, header injection on redirect. Also code quality: cyclomatic complexity, null handling, exception hygiene, naming.
ACCEPTANCE CRITERIA: Findings table with severity, file/line, why it matters, proposed fix. Distinguish "must fix" from "nice to have". I will decide which to accept.
OUTPUT FORMAT: Markdown findings table.
```
*Then run the tooling* (Spotless/Checkstyle, JaCoCo, OWASP dependency-check, `mvn verify`) and paste results in docs/07 and docs/09.

### Prompt 13 — CI pipeline
```
INTENT: Create a GitHub Actions workflow enforcing the quality gates.
CONSTRAINTS: Jobs: build → lint (Spotless check) → unit tests → integration tests (Testcontainers) → JaCoCo threshold → dependency-check → Docker build. Cache Maven. Fail on coverage < 80% service layer.
ACCEPTANCE CRITERIA: Green run on main; badge in README.
OUTPUT FORMAT: .github/workflows/ci.yml
```

### Prompt 14 — Performance sanity check
```
INTENT: Produce a lightweight load test for the redirect endpoint.
CONSTRAINTS: k6 or Gatling script; 500 RPS for 60s against local Docker; report p50/p95/p99 and error rate; document results and bottleneck hypotheses.
OUTPUT FORMAT: Script + docs/07 section.
```

---

## Phase 7 — Documentation & Final Summary (Req 8, Deliverables)

### Prompt 15 — README & setup
```
INTENT: Write the README for evaluators who will clone the repo and run it in 5 minutes.
CONSTRAINTS: Sections: What it is; Quick start (docker compose up → curl examples for shorten/redirect/stats); Run tests; Project structure; Links to all docs/ files; API reference (link to Swagger UI); Config reference. No marketing language.
ACCEPTANCE CRITERIA: A reader with Docker and Java can run every command without guessing.
```

### Prompt 16 — Risks, trade-offs, limitations
```
INTENT: Document risks, trade-offs, failure scenarios and guardrails.
CONTEXT: All ADRs and scenario docs.
ACCEPTANCE CRITERIA: Table of risks (likelihood/impact/mitigation/detection); trade-offs per ADR; known limitations of the prototype vs production (e.g., single node, no distributed ID, no auth); what I would do next with one more week.
OUTPUT FORMAT: docs/08-risks-tradeoffs.md
```

### Prompt 17 — Final engineering summary
```
INTENT: Write the final engineering summary the assignment requires.
CONTEXT: All docs/ files and the traceability log.
CONSTRAINTS: First person, engineer voice. Must contain: plan and rationale; artifacts produced (with paths); how AI was used per phase and where I rejected/edited its output (cite log rows); risks/trade-offs/validation; assumptions; limitations; evidence of human sign-off on high-impact changes.
ACCEPTANCE CRITERIA: Every evaluation criterion in section 6 of the assignment is addressed explicitly with a pointer to evidence.
OUTPUT FORMAT: docs/10-final-engineering-summary.md
```

### Prompt 18 — Self-grade against the rubric
```
INTENT: Grade this repo against the assignment's evaluation criteria as a skeptical Schwab interviewer.
CONTEXT: Paste section 6 of the assignment + the README + final summary.
ACCEPTANCE CRITERIA: Score each criterion 1-5 with the strongest gap and the cheapest fix. I will address the top 3 gaps before submitting.
```

---

## Suggested day plan
- **Day 1:** Prompts 1–6 (analysis, architecture, decomposition, greenfield). Commit often; write log rows as you go.
- **Day 2:** Prompts 7–10 + ambiguous implementation, Prompt 11.
- **Day 3:** Prompts 12–18, polish, final CI green, submit `https://github.com/<you>/url-shortener.git`.

## Submission checklist
- [ ] Repo public (or shared with reviewer), link ends in `.git`
- [ ] `docker compose up` works from clean clone
- [ ] CI green
- [ ] docs/ 01–10 all present, prompts/ folder contains every prompt used
- [ ] Traceability log has Generated/Edited/Rejected entries with rationale (rejections matter — show judgment)
- [ ] High-impact changes (schema, security, concurrency fix) show explicit sign-off notes
