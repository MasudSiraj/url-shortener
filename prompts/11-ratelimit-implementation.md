# Prompt 11 — Implement rate limiting (tasks D2–D4)

INTENT: Implement the normalized spec in docs/06 §1.3 after engineer sign-off.
CONTEXT: Answers recorded in docs/06 §1.7 — limit redirects (Q-A a); accept shared-IP false positives and document (Q-B a); do NOT trust X-Forwarded-For (Q5); fail open on limiter error (Q12); 10 create/min and 100 read/min (Q7).
CONSTRAINTS: RateLimiter interface so a Redis impl can replace the in-process one; token bucket driven by the injected Clock (no sleeps in tests); bounded Caffeine cache of buckets; OncePerRequestFilter ahead of controllers; /actuator, /v3/api-docs, /swagger-ui, /h2-console exempt; 429 + RFC 7807 + Retry-After; counter ratelimit.rejected tagged by bucket; limits bound in ShortenerProperties, not constants; no existing test may change behaviour.
ACCEPTANCE CRITERIA: the ten acceptance criteria in docs/06 §1.4; the five tests in §1.5; `mvn verify` green; JaCoCo gate met.
OUTPUT FORMAT: Java files under ratelimit package, config/yml changes, test files.

Tool: Claude (Anthropic), 2026-08-27.
