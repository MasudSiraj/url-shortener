# Prompt 12 — Adversarial security and quality review (task F1)

INTENT: Review the codebase as a skeptical senior reviewer before engineer sign-off for submission.
CONTEXT: Full source at commit f8b99c0 — api/application/domain/infrastructure/ratelimit/config, 3 Flyway migrations, 97 unit + 22 integration tests, no authentication (AR-11), single instance, Docker Compose deployment.
CONSTRAINTS: Check OWASP Top 10 relevance specifically — open redirect, SSRF, injection, input validation, information leakage, dependency vulnerabilities, secrets in config, log injection, header injection. Also code quality: complexity, null handling, exception hygiene, naming, test weaknesses. Do not fix anything. Distinguish "must fix before submission" from "accepted risk, document" from "nice to have". Assume the reviewer will argue with each finding.
ACCEPTANCE CRITERIA: findings table with id, severity, file, why it matters, proposed fix, and a recommended disposition; engineer triage column left blank.
OUTPUT FORMAT: Markdown for docs/08-risks-tradeoffs.md section 1.

Tool: Claude (Anthropic), 2026-08-27.
