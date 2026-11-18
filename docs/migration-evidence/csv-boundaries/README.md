# CSV input boundary counterexamples

2026-11-18, Africa/Douala. Reviewed Desktop head `e993fd6054a3b73fa034be6bf4f5a0e406582fee`, AGENTS, architecture, work allocation, dataset evidence, delivery control and issues #2/#3/#4. No newer Desktop commits or handoff acknowledgments were found. Audit ownership remains provisional where unacknowledged.

## Result: FAIL — do not merge as passing acceptance

Java25.0.5 / Maven3.9.11 / repository Boot4 configuration executed **8 tests: 5 pass, 3 fail, 0 errors, 0 skips**. These are genuine failing regression assertions against the unchanged parser, not disabled tests or expected-failure wrappers. Complete output is `maven.log` (only trailing whitespace normalized); `result.json` binds source/test hashes.

| Counterexample | Observed defect | Required behavior |
| --- | --- | --- |
| `name\n"A\0B"` (actual NUL character) | Quoted NUL accepted | Reject NUL consistently, as already done for unquoted input |
| Two comma-separated rows of 41 fields, EOF after final field | 41 fields accepted | Enforce intended 40-field maximum after final cell insertion |
| Two semicolon-separated quoted rows of 41 fields, CRLF terminated | 41 fields accepted | Enforce same maximum at row completion |

Five passing tests cover unquoted NUL rejection, acceptance of 40 fields, 500-data-row limit, quoted 10000-character field limit, and French text with escaped quotes/embedded newlines. Limits derive from the parser's existing validation, not newly invented business restrictions. Architecture S63/S69 and D09 require validated import behavior; these helper tests do not accept either full workflow.

The NUL check currently occurs only in the unquoted branch. Column checks happen before the final field is appended; newline handling clears the row before the limit check. Minimum proposed correction after path handoff: reject NUL before quote-state branching, and validate completed row width after appending the final field both on newline and EOF. Preserve valid quoting, delimiter, UTF-8 and exact data content; do not silently truncate columns or strip invalid input. This correction has NOT been applied or tested here.

Repository search found no production invocation of `Csv.parse` at this head. Consequently this is a reproduced helper-level defect, not proof of an exploitable HTTP route or corrupted database. No database or HTTP tests ran. Wiring and scoped validation still need acceptance.

## Ownership and reproduction

Only NEW `backend/business-modules/operations/src/test/java/com/quincaisoft/operations/CsvInputBoundaryTest.java` and this evidence directory changed. Boundary announced in issue #4 comment 6973149457. Existing `operations/application/Csv.java` and importer integration are not acknowledged CLOUD write paths. No shared code/POM/security/migrations/UI edited. Desktop historical importer, manufacturer/brand/supplier/variant work and original UI remain preserved. No register or parity entry promoted.

From backend with Java25:

```sh
mvn -B -ntp -pl business-modules/operations -am \
  -Dtest=CsvInputBoundaryTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Execution used external runtime proxy settings, dependency cache and system TLS truststore. Exit code 1; compilation succeeded and failures were assertion failures. `node scripts/check-project-rules.mjs` and staged whitespace checks pass; neither overrides the red tests. No full-reactor, PostgreSQL, browser or deployment run was performed.

Next: acknowledge the exact Csv.java path via #4, apply the two narrow checks, rerun all eight assertions and import acceptance without weakening tests, then integrate through Desktop. These fixtures are not the historical Java importer and change none of its counts. Dataset ingestion/replay/exact stock/AR/AP/financial reconciliation remains blocked; original UI acceptance, usable provisioning, billing integration, production destination, security/deployment/restore evidence remain open. No merge or production readiness certification.

## Resolution after PR #11

The failing log above is retained as historical reproduction evidence at its original source commit. PR #11 fixes the parser and extends this suite to 13 passing tests. Its combined Java25/PostgreSQL18 verification is recorded in `../cloud-implementation/`. The current merge preserves that fixed parser and the full 13-case suite; this old failure log does not describe the merged source.
