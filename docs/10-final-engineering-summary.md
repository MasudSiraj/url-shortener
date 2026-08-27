# 10 — Final Engineering Summary

**Repository:** https://github.com/MasudSiraj/url-shortener.git
**Final commit reviewed:** `d8792ca` (main)
**Author:** Masud Siraj, Principal Quality Architect
**Assignment:** Build an AI-Assisted Software Engineering System — URL Shortener (Charles Schwab)

---

## 1. Plan and rationale

The assignment states its own point plainly: build a URL shortener, but demonstrate *how* it was
built — requirement understanding, decomposition, AI-assisted execution with traceability, and
validation, across greenfield, brownfield, and ambiguous scenarios (§1, §4 of the assignment).

The plan followed that structure directly:

1. **Normalize the requirement** before writing any code (`docs/01`) — turn "build a URL shortener"
   into 21 functional requirements, non-functional requirements, and a 15-item ambiguity register
   with adopted assumptions and rationale for each.
2. **Design before implementing** (`docs/02`, seven ADRs) — architecture, component boundaries, and
   five option analyses (code generation, persistence, caching, analytics capture, error format)
   with an explicit recommendation and alternatives considered for each.
3. **Decompose into 44 sequenced tasks** across six groups (`docs/03`), each with acceptance
   criteria, a quality gate, and a risk level; high-impact tasks flagged for sign-off in advance.
4. **Execute in three scenarios**, as required: greenfield core APIs (`docs/04`), a brownfield
   enhancement plus a reproduced-and-fixed concurrency defect (`docs/05`), and an ambiguous
   requirement resolved through explicit clarifying questions answered by the engineer as product
   owner (`docs/06`).
5. **Validate continuously** — every commit ran through Spotless, Checkstyle, unit tests,
   Testcontainers integration tests, and a JaCoCo coverage gate, both locally and in CI.
6. **Review adversarially before submission** (`docs/08`) — an AI-as-reviewer pass produced 19
   findings; four were fixed, the rest were accepted as documented, deliberate trade-offs.

The guiding principle throughout, per the assignment's own framing (§1, §7): **the engineer leads
and owns every decision; AI accelerates execution within tasks it does not set the direction for.**

---

## 2. Artefacts

| # | Artefact | Path |
|---|----------|------|
| 1 | Requirement analysis, FR/NFR, ambiguity register | `docs/01-requirement-analysis.md` |
| 2 | Architecture, control flow, option analysis | `docs/02-architecture.md` |
| 3 | Seven Architecture Decision Records | `docs/adr/ADR-001` … `ADR-007` |
| 4 | Task breakdown: 44 tasks, dependency graph, critical path, sign-off register | `docs/03-task-breakdown.md` |
| 5 | Greenfield scenario | `docs/04-scenario-greenfield.md` |
| 6 | Brownfield scenario (impact analysis → analytics → defect red/green → refactor) | `docs/05-scenario-brownfield.md` |
| 7 | Ambiguous scenario (15 questions, signed answers, implementation, trade-off proof) | `docs/06-scenario-ambiguous.md` |
| 8 | Testing approach, gates, what each caught, deliberate gaps | `docs/07-testing-approach.md` |
| 9 | Security review, risk register, ADR trade-offs, limitations | `docs/08-risks-tradeoffs.md` |
| 10 | AI traceability log — 28 rows, Generated/Edited/Rejected | `docs/09-ai-traceability-log.md` |
| 11 | This document | `docs/10-final-engineering-summary.md` |
| 12 | Every prompt used, verbatim, numbered | `prompts/00` … `prompts/13` |
| 13 | Working service: 3 Flyway migrations, Docker Compose, CI pipeline | `src/`, `docker-compose.yml`, `.github/workflows/ci.yml` |
| 14 | 123 tests (101 unit + 22 integration), all green | `src/test/` |

---

## 3. How AI was used, by phase — and where it was overridden

The full record is `docs/09` (28 rows). Summarized by phase, with every **Edited** or **Rejected**
row — the evidence of engineer ownership, not just acceptance:

| Phase | Generated | Edited (engineer changed AI output) | Rejected (engineer discarded it) |
|-------|-----------|--------------------------------------|-----------------------------------|
| Requirement / architecture / decomposition | 3 | D2 rate-limit defaults confirmed; cut order confirmed; C5/C6 branch strategy set explicitly by engineer | — |
| Scaffold | 1 | **Row 5**: Surefire silently ran zero tests for two prompts before the coverage gate exposed it; pinned the TestNG provider | — |
| Greenfield | 1 | **Row 7**: Checkstyle rejected an AI method at complexity 11; refactored | **Row 8**: engineer removed an AI-written comment that would have signposted the brownfield defect instead of letting it be discovered |
| Brownfield | 3 | **Row 15**: H2 rejected `CHAR(64)`; **Row 16**: NPE from a signature change; **Row 21/27**: an AI file bundle twice overwrote a locally-applied fix (a real workflow risk, logged honestly rather than hidden) | — |
| Defect fix | 2 | — | — (the fix itself was accepted; the *comment hiding it* was rejected earlier) |
| Refactor | 1 | **Row 20**: AI asserted the wrong base62 value (`1C` vs `1c`) in its own test; corrected | — |
| Ambiguous scenario | 2 | **Row 23**: engineer answered all 15 questions explicitly, two of them (Q-A, Q-B) with no defensible AI default at all | — |
| Rate limiting | 2 | **Row 25**: enabling the limiter broke three unrelated tests — a direct, reproducible demonstration of the Q-B trade-off the engineer had accepted; **Row 26**: a second-order test-isolation bug from the first fix | — |
| Security review / CI | 2 | Findings triaged by the engineer: 4 fixed, 15 accepted with rationale | — |

**Every rejection was a judgment call, not a correction of a bug** — hiding a defect, or accepting an
AI-authored assertion without verifying its arithmetic, are the kind of choices this assignment asks
an engineer to make and own.

---

## 4. Risks, trade-offs, validation

Full detail in `docs/08`. Highest-signal items:

- **Security review found 19 issues; 4 were fixed** (default salt could silently defeat IP hashing;
  a missing header-injection test; a missing planned test for dropped analytics events; the
  dependency-check gate wasn't yet wired into CI). The other 15 are documented, deliberate
  trade-offs — most of them decisions made explicitly in the ADRs or the ambiguous scenario, not
  oversights the review discovered.
- **The rate-limiting trade-off was demonstrated, not just asserted.** Enabling the limiter broke
  three unrelated integration tests because every test client shares `127.0.0.1` — concrete proof
  of the shared-IP limitation accepted in `docs/06` Q-B, visible in a real test failure rather than
  a paragraph of speculation.
- **The alias-conflict defect was reproduced before being fixed.** Fifty concurrent requests for one
  alias produced one 201 and forty-nine 500s (`docs/05` §3.2) — committed as a failing test
  (`829b92d`) before the fix (`17183dc`).
- **CI caught a bug that 20+ local runs never found**: an order-dependent test using global mutable
  state passed locally by luck and failed on GitHub Actions' different test ordering. Fixed by
  removing the shared state entirely (`docs/07` §4).
- **Zero-tests incident**: for two prompt cycles, `mvn verify` reported success while running no
  tests at all, because Surefire had silently selected the wrong provider. The JaCoCo coverage gate
  is what exposed it once real code existed to cover. This is the single strongest argument in this
  submission for running every gate on every commit rather than trusting a green build at face
  value.

---

## 5. Assumptions

Recorded in full in `docs/01` §4 (15 items) and `docs/06` §1.7 (5 more, engineer-signed). The ones
that most shape the deliverable:

- Core APIs = create, redirect, read metadata, delete (no update).
- No authentication — explicitly out of scope, not an oversight (§out-of-scope, `docs/01`).
- Analytics = total clicks, per-day for 30 days, top-5 referrers.
- Rate limiting is the deliberately ambiguous requirement; expiration was left as an ordinary
  feature rather than a second ambiguous scenario.
- Single-instance deployment; multi-node is the documented upgrade path in every relevant ADR, not
  a current capability.

---

## 6. Limitations

Full list with likelihood/impact in `docs/08` §4. In order of what would most affect a production
decision: no auth; single-instance cache and rate limiter; rate limiting is blind behind a reverse
proxy (by design — the alternative, trusting `X-Forwarded-For`, is worse); analytics aggregation
does not scale past roughly 10⁵ clicks per link; click capture is at-most-once; no load test was
run (cut on Day 3, stated rather than hidden); no retention/purge job.

---

## 7. Mapping to the assignment's evaluation criteria (§6)

| Criterion | Evidence |
|-----------|----------|
| Effectiveness of AI-assisted execution | 14 numbered prompts, each with INTENT/CONTEXT/CONSTRAINTS/ACCEPTANCE CRITERIA; 28-row traceability log; §3 above |
| Architecture / system design quality | `docs/02` + 7 ADRs, each with alternatives considered and consequences |
| Depth of decomposition and execution quality | 44-task breakdown with dependency graph and critical path (`docs/03`); every task closed with a commit reference |
| Realism / quality of outputs | working service, real PostgreSQL integration tests, real CI, real Docker image |
| Validation and risk management rigor | `docs/07` (gates, what each caught), `docs/08` (19-finding security review, risk register), the alias-defect red→green sequence |
| Clarity and defensibility of decisions | every ADR states alternatives and trade-offs; every ambiguous-requirement answer is signed with rationale |
| Modular, testable, reliable, secure, scalable code with safe change management | layered architecture (ADR-001); 123 tests; `RateLimiter`/`ShortCodeGenerator` ports designed for swap-in production implementations; branch-based defect fix with PR merge |
| Engineering judgment | §3's rejection rows; the decision to cut k6 explicitly rather than silently; the decision to move dependency-check to nightly rather than let CI feedback become unusable |

---

## 8. If given one more week

Priority order, per `docs/08` §5: API-key authentication and link ownership; Redis for the cache and
rate limiter to make the service horizontally scalable; SQL-based analytics aggregation; Kafka for
click events; the k6 load profile with a published p95 for the redirect path; trusted-proxy
configuration so rate limiting works correctly behind a reverse proxy; a retention/purge job.
