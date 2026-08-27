# Prompt 7 — Codebase impact analysis for click analytics (task C1)

INTENT: Analyze the existing v0.1-greenfield codebase to add click analytics without degrading or breaking the redirect path.
CONTEXT: Tag v0.1-greenfield. Layers api/application/domain/infrastructure. Key classes: RedirectController, UrlService, ShortUrl, ShortUrlRepository, V1__create_short_url.sql, GlobalExceptionHandler, AppConfig (Clock bean), ShortenerProperties (analytics.ip-hash-salt already reserved). ADR-004 proposes async in-process events. AR-4 scopes analytics to: total clicks, clicks per day (last 30), top referrers.
CONSTRAINTS: Analysis only — no code. Redirect latency must not increase measurably. Redirect must succeed even if analytics storage fails. No raw IP addresses stored.
ACCEPTANCE CRITERIA: impacted modules/classes/APIs/data flows; before/after data-flow diagrams; sync vs Spring event vs Kafka comparison with a recommendation for a 2-day prototype; migration plan with indexes and backward-compatibility notes; test impact (existing tests that change, new tests needed); explicit decision block for engineer sign-off.
OUTPUT FORMAT: Markdown for docs/05-scenario-brownfield.md section 1.

Tool: Claude (Anthropic), 2026-08-27.
