# Prompt 9 — Alias race: red test, fix, refactor (tasks C5–C7)

INTENT: Reproduce, fix, and then refactor around the concurrent custom-alias defect on the existing codebase.
CONTEXT: Baseline after C4. UrlService.createWithAlias calls saveAndFlush; the DB unique index on short_url.code rejects the loser but DataIntegrityViolationException is not mapped. Engineer decision (docs/03): branch fix/alias-conflict, commit 1 = failing test only (red), commit 2 = fix (green), commit 3 = refactor, merged via PR.
CONSTRAINTS:
 - Test first: 50 threads POST the same alias simultaneously via REST Assured against Testcontainers Postgres; assert exactly one 201 and the rest 409.
 - Fix must be at BOTH levels: DB unique constraint (already present, verify) AND service mapping DataIntegrityViolationException -> AliasConflictException (409).
 - Refactor: add SequenceBase62Generator selected by shortener.generator=sequence; no behaviour change to default; both generators pass a shared contract test.
ACCEPTANCE CRITERIA: red commit shows the failure output (pasted in docs/05 §3); green commit passes; refactor keeps all tests green; JaCoCo gate met.
OUTPUT FORMAT: test file (commit 1), then diff (commit 2), then generator + contract test (commit 3).

Tool: Claude (Anthropic), 2026-08-27.
