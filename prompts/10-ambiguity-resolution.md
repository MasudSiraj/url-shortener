# Prompt 10 — Ambiguity resolution: "Add rate limiting" (task D1)

INTENT: Turn the deliberately vague requirement "Add rate limiting to the service" into a normalized, testable specification.
CONTEXT: Current architecture (docs/02), endpoints POST /api/v1/urls, GET /{code}, GET /api/v1/urls/{code}, DELETE, GET .../stats, /actuator/**. Single instance, no authentication (AR-11). Engineer pre-confirmed starting defaults in docs/03 D2: 10 POST/min and 100 redirect/min per client IP; redirect limit retained unless requirements explicitly remove it.
CONSTRAINTS: Do NOT implement. Treat the engineer as product owner: surface questions, propose a default for each with rationale, and state what changes depending on the answer. Distinguish prototype answer from production answer.
ACCEPTANCE CRITERIA: clarifying questions grouped by scope / identity / limits / breach behaviour / storage / observability; recommended default per question with rationale and production delta; resulting normalized spec; acceptance criteria; test list.
OUTPUT FORMAT: Markdown for docs/06-scenario-ambiguous.md section 1.

Tool: Claude (Anthropic), 2026-08-27.
