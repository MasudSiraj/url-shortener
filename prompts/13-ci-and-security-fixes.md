# Prompt 13 — Security fixes and CI pipeline (tasks F2, A6)

INTENT: Implement the engineer's triage of the docs/08 review, and wire the quality gates into CI.
CONTEXT: Engineer decision — fix S-1, S-3, S-10, T-2; accept all other findings as documented. CVSS >= 7 must fail the build.
CONSTRAINTS:
 - S-1: refuse startup when the built-in IP-hash salt is active outside dev/test; warn (not fail) in dev.
 - S-3: add CR/LF cases to the validator's rejection table; no production code change if it already rejects them.
 - T-2: prove a saturated analytics executor drops events, counts them, and never throws.
 - S-10: dependency-check as a separate CI job with -DfailBuildOnCVSS=7 and NVD caching.
 - CI must run the same gates as local `mvn verify`; Testcontainers needs Docker on the runner.
ACCEPTANCE CRITERIA: `mvn verify` green locally; CI green on main; a deliberately un-set salt in the docker profile fails startup.
OUTPUT FORMAT: Java, test files, .github/workflows/ci.yml.

Tool: Claude (Anthropic), 2026-08-27.
