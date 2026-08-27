# 01 — Requirement Analysis

**Source:** Charles Schwab Interview Assignment — "Build an AI-Assisted Software Engineering System – URL Shortener"
**Author:** Masud "Sam" Siraj, Principal Quality Architect
**Status:** Draft generated with AI assistance (Prompt 1) → pending engineer review and sign-off
**Traceability:** Section references (§) point to the assignment document.

---

## 1. Intent Statement

The assignment asks for a working URL shortener service built from scratch, but the service is the vehicle rather than the point. The real deliverable is evidence of a disciplined, engineer-led development process in which AI is used as an accelerator within clearly defined tasks: requirements are interpreted and de-ambiguated, decomposed into sequenced tasks, executed with AI assistance under explicit prompts and constraints, validated through quality gates, and summarized with defensible rationale. Success means an evaluator can clone the repository, run the service end-to-end, and trace every significant decision, AI interaction, and risk mitigation back to the engineer who owned it (§1, §7).

---

## 2. Functional Requirements

| ID | Requirement | Source |
|----|-------------|--------|
| FR-1 | Build a URL shortener service from scratch with "core APIs" (at minimum: create a short link from a long URL, and resolve a short link back to the original). | §2 |
| FR-2 | Provide analytics capability on shortened links. The document does not define which metrics — see AR-4. | §2 |
| FR-3 | Provide reliability features. The document does not define which — see AR-5. | §2 |
| FR-4 | The prototype must be runnable end-to-end by the evaluator. | §5 |
| FR-5 | Deliver an architecture overview covering components, tools, execution approach, control flow, and key decisions. | §5 |
| FR-6 | Demonstrate three scenarios — greenfield, brownfield, ambiguous — each showing decomposition, execution, and validation. | §3, §5 |
| FR-7 | Provide setup instructions. | §5 |
| FR-8 | Document the testing approach, limitations, and trade-offs. | §5 |
| FR-9 | Produce production-quality code, API/schema definitions, unit and integration tests, and supporting documentation. | §4.5 |
| FR-10 | Produce a requirement-understanding artifact: interpret intent, identify ambiguity, normalize into a clear engineering problem (this document). | §4.1 |
| FR-11 | Produce a task decomposition artifact with dependencies and sequencing. | §4.2 |
| FR-12 | For the brownfield scenario, identify impacted modules/services/APIs/data flows and demonstrate architectural understanding. | §4.3 |
| FR-13 | Use AI across implementation, debugging, refactoring, test generation, documentation, and review preparation. | §4.4 |
| FR-14 | Define each AI task with intent, constraints, acceptance criteria, and technical context; use iterative prompt refinement. | §4.4 |
| FR-15 | Maintain a traceability record of AI output classified as generated / edited / rejected, with rationale. | §4.4 |
| FR-16 | Apply quality gates: static analysis, linting, tests, security, performance. | §4.4 |
| FR-17 | Enforce secure AI usage (no secrets, credentials, or sensitive data in prompts; review AI output for security defects). | §4.4 |
| FR-18 | Require explicit human sign-off for high-impact changes. | §4.4, §7 |
| FR-19 | Identify risks, trade-offs, and failure scenarios; define validation and safety guardrails. | §4.6 |
| FR-20 | Produce a final engineering summary: plan/rationale, artifacts, risks/trade-offs/validation, assumptions, limitations. | §4.8 |
| FR-21 | Submit as a GitHub repository; link must end in `.git`. | Submission instruction (email) |

---

## 3. Non-Functional Requirements

The document names categories but not targets. Targets below are marked **[assumed]** and are resolved in the Ambiguity Register.

### 3.1 Reliability (§2, §6)
- NFR-R1: Service must start cleanly and serve requests from a fresh clone with documented steps (FR-4).
- NFR-R2: Failure scenarios must be identified and guarded against (§4.6). Concrete scenarios **[assumed]**: datastore unavailable, short-code collision, malformed input, expired/unknown link.
- NFR-R3: "Reliability features" (§2) must be present in the running system, not just described — see AR-5.

### 3.2 Security (§4.4, §6)
- NFR-S1: Code must be "secure" per §6 core engineering principles.
- NFR-S2: Secure AI usage: no secrets in prompts or repository; AI-generated code reviewed for vulnerabilities (§4.4).
- NFR-S3: **[assumed]** Standard web-service hygiene applies: input validation, no open-redirect/SSRF vectors, no sensitive data leaked in error responses, dependency vulnerability scanning.

### 3.3 Observability
- NFR-O1: Not explicitly named in the document. **[assumed]** Implied by "reliability features" and "production-grade" (§7): health endpoint, structured logs, basic metrics sufficient to diagnose failures.

### 3.4 Performance (§4.4 quality gates, §6 scalable)
- NFR-P1: Performance is a named quality gate (§4.4) — some performance validation must exist.
- NFR-P2: No numeric targets given. **[assumed]** Redirect path is the hot path and must be measured; see AR-7.
- NFR-P3: Design must be "scalable" (§6) — the prototype need not be horizontally scaled, but decisions must not preclude it, and scaling limits must be documented.

### 3.5 Maintainability & Change Safety (§6)
- NFR-M1: Modular, testable code with clean design.
- NFR-M2: Safe change management — changes are traceable (version control, tests, sign-off).

### 3.6 Process (§4.4, §7)
- NFR-X1: Engineer leads and approves all outputs; AI never commits autonomously.
- NFR-X2: Time-box of 2–3 days (§2) — scope must fit.

---

## 4. Ambiguity Register

| ID | Ambiguous / unstated item | Possible interpretations | Adopted assumption | Rationale |
|----|---------------------------|--------------------------|--------------------|-----------|
| AR-1 | What are the "core APIs"? (§2) | (a) create + redirect only; (b) create, redirect, read metadata, delete; (c) full CRUD plus user accounts | **(b)** create, redirect, read metadata, delete | (a) is too thin to demonstrate decomposition; (c) introduces auth/users, unbounded for 2–3 days and not implied. |
| AR-2 | Custom aliases — supported? | (a) system-generated codes only; (b) optional user-supplied alias; (c) required alias | **(b)** optional alias | Common product expectation; creates a meaningful uniqueness/concurrency concern that strengthens the brownfield bug-fix scenario. |
| AR-3 | Link expiration | (a) links never expire; (b) optional expiry per link; (c) mandatory default TTL | **(b)** optional expiry | Not stated; (b) adds a clear failure scenario (expired link) for §4.6 without forcing a TTL policy decision the document doesn't support. Candidate for the *ambiguous scenario* — see AR-9. |
| AR-4 | What does "analytics" mean? (§2) | (a) total click count; (b) click count + time series + referrer/user-agent breakdown; (c) full event pipeline / dashboard | **(b)** | (a) is trivial; (c) exceeds time-box. (b) is enough to require a data-model change, making it a credible brownfield enhancement. |
| AR-5 | What are "reliability features"? (§2) | (a) health checks only; (b) health checks + caching + graceful degradation + input hardening; (c) full HA/multi-region | **(b)** | (c) impossible in scope; (a) too thin to be a "feature". |
| AR-6 | Deployment/runtime target | (a) local JVM; (b) Docker Compose; (c) Kubernetes/cloud | **(b)** with (a) also supported | §5 says "runnable end-to-end"; Docker Compose gives the evaluator one command without requiring a cluster. |
| AR-7 | Performance targets | (a) none required; (b) measure and report, no SLA; (c) meet a specific SLA | **(b)** | §4.4 names performance as a gate but gives no numbers; reporting measured latency with a stated method satisfies the gate honestly. |
| AR-8 | Persistence durability | (a) in-memory only; (b) embedded DB; (c) external relational DB | **(c)** for the runnable prototype, (b) for tests/dev | "Production-grade" (§7) and "scalable" (§6) point away from in-memory; an embedded DB in tests keeps the suite fast. *Design choice deferred to Architecture (Prompt 2).* |
| AR-9 | Which requirement serves as the "ambiguous scenario"? (§5) | (a) rate limiting; (b) expiration policy; (c) analytics privacy/retention | **(a)** rate limiting | Genuinely underspecified (scope, identity, limits, breach behavior, storage) and touches security, reliability, and performance at once. AR-3 stays as a normal feature. |
| AR-10 | Brownfield scenario on a greenfield repo | (a) skip; (b) treat the Day-1 core as "existing code" and enhance/fix it on Day 2; (c) fork a third-party repo | **(b)** | §3 lists brownfield explicitly; (c) would mix unknown code quality into the assessment. Analytics (AR-4) is the enhancement; a deliberately reproduced concurrency defect on alias uniqueness is the bug fix. |
| AR-11 | Authentication / multi-tenancy | (a) none; (b) API keys; (c) user accounts | **(a)** none, documented as a limitation | Not implied anywhere in the document; adding it consumes time without addressing an evaluation criterion. |
| AR-12 | "Engineer sign-off" evidence format | (a) implicit via commits; (b) explicit sign-off notes in the traceability log for high-impact changes; (c) formal PR approvals | **(b)** plus PR-per-scenario where practical | §4.4 requires sign-off to be "required," so it must be visible, not inferred. |
| AR-13 | Which AI tools and how disclosed | (a) unnamed; (b) named per prompt with model/version | **(b)** | Supports "traceability" and "secure AI usage" (§4.4). |
| AR-14 | Test coverage expectations | (a) any tests; (b) unit + integration with a coverage threshold enforced in CI | **(b)** | §4.5 requires both unit and integration tests; §4.4 requires tests as a gate — a gate needs a threshold. Numeric threshold deferred to Testing Approach. |
| AR-15 | Repository visibility | (a) public; (b) private with reviewer access | **(a)** unless instructed otherwise | Submission asks for a `.git` link; public avoids access friction. Confirm with recruiter. |

---

## 5. Out of Scope

Explicitly excluded to keep within the 2–3 day time-box; each is listed as a known limitation in the final summary.

- User accounts, authentication, authorization, or multi-tenant isolation (AR-11)
- Web UI / dashboard — API-only service
- Horizontal scaling, multi-node ID generation, or distributed cache deployment (design must not preclude these; they are documented, not built)
- Cloud deployment (AWS/Kubernetes) beyond a Docker Compose definition
- Analytics beyond click counts, daily time series, and referrer/user-agent breakdown (no geo-IP, no funnels, no export)
- Link editing after creation (delete only)
- Abuse/malware URL screening beyond basic URL validation
- Formal SLA commitments — performance is measured and reported, not guaranteed
- Autonomous/agentic orchestration — explicitly discouraged by §1

---

## 6. Open Questions for the Reviewer (if a channel exists)

1. Is a public repository acceptable? (AR-15)
2. Is a specific language/framework expected, or is choice part of the assessment?
3. Should the "three scenarios" be separate commits/PRs, or is documentation sufficient?

If no channel exists, the adopted assumptions above stand and are recorded as assumptions in the final engineering summary (§4.8).

---

## 7. Engineer Review Log

| Item | Generated / Edited / Rejected | Note |
|------|-------------------------------|------|
| Full document | Generated | Pending review — Sam to confirm or override each AR-* assumption before Prompt 2. |
