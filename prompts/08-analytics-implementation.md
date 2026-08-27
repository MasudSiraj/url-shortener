# Prompt 8 — Implement click analytics (tasks C2–C4)

INTENT: Implement click analytics per the signed-off analysis in docs/05 §1.
CONTEXT: Baseline v0.1-greenfield. Decisions D-1…D-6 accepted by engineer: async in-process event; resolve() returns ResolvedTarget(id,longUrl); executor 2/4/1000 with DiscardPolicy + dropped counter; salted SHA-256 IP hash; stats available for deleted links; top 5 referrers.
CONSTRAINTS: Flyway V2 additive only; V1 untouched. Event carries primitives, never an entity. Listener swallows and logs all failures. Redirect response semantics unchanged (302 + Cache-Control: no-store). No raw IP persisted or logged. Existing tests must keep passing except the one mechanical resolve() signature change.
ACCEPTANCE CRITERIA: V2 applies on H2 and Postgres; AnalyticsIT proves async persistence, per-day/referrer aggregation, stats after delete, 404 for unknown; unit tests for IpHasher, AnalyticsService, ClickEventListener; JaCoCo ≥ 80% on application; `mvn verify` green.
OUTPUT FORMAT: Java + SQL files by layer; test files.

Tool: Claude (Anthropic), 2026-08-27.
