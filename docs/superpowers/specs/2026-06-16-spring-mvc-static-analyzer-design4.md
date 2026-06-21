# Spring MVC Static Analyzer Dev 4 Design

## Purpose

Dev 4 is a behavior-preserving Ponytail refactor pass. The goal is to use a
Ponytail audit to find over-engineering, then remove or shrink only the parts
that are already heavier than the current product needs.

This phase follows Dev 3, which already introduced the shared `AnalysisRunner`
and the call-resolution policy boundary. Dev 4 should not reopen that architecture
unless the audit finds a smaller shape that preserves the same CLI, GUI, report,
and fixture behavior.

## Scope

- Run a Ponytail-style audit before code changes.
- Rank over-engineering candidates by how much code, dependency weight, or
  indirection can be removed.
- Apply one small refactor task at a time.
- Preserve public behavior and expected report text.
- Keep CLI and GUI on the shared `AnalysisRunner` path.
- Keep Controller unsupported noise suppressed.
- Keep Service and Repository unsupported calls visible.
- Update or add tests only where the refactor changes a protected boundary.
- Capture any reusable lesson with compound at the end.

## Non-goals

- No new analyzer features.
- No JavaSymbolSolver precision upgrade.
- No Spring Data JPA inherited-method implementation.
- No MyBatis XML tracing.
- No GUI redesign.
- No installer work.
- No speculative package split.
- No marker model rewrite unless a current test-protected simplification requires it.

## Ponytail Audit Criteria

Use these checks only for over-engineering and complexity. Correctness bugs,
security issues, and performance tuning are separate review passes.

- `delete`: code, dependency, option, or layer that current behavior does not use.
- `stdlib`: hand-rolled behavior that Java, Gradle, Swing, or picocli already gives.
- `native`: dependency or wrapper replaced by a built-in platform feature.
- `yagni`: abstraction with one implementation, one caller, or no current product need.
- `shrink`: same behavior with fewer branches, classes, or repeated code.

Reject a candidate if it needs a speculative future feature to justify the work.
Accept a candidate only when the expected diff can be tested against current
behavior.

## Current Over-engineering Candidate Areas, Ranked

1. `build.gradle` includes `javaparser-symbol-solver-core`, but current main code
   uses only `javaparser-core`. Candidate: remove the unused dependency if a full
   test run proves no behavior depends on it.

2. `TextReportWriter` has two recursive tree-writing methods with near-identical
   child traversal. Candidate: collapse the recursion into one helper while
   preserving the exact expected report fixture.

3. `AnalyzerGui` validates target directory existence before calling
   `AnalysisRunner`, which performs the same directory validation. Candidate:
   either keep only blank-field validation in the GUI or document why the GUI needs
   the earlier dialog. Preserve user-facing error behavior if changed.

4. `CallResolutionPolicy` is a one-rule class. Dev 3 intentionally created this
   boundary for future analyzer policy, so it is not automatically waste. Candidate:
   run Ponytail review against any proposed change; keep it if removing it would
   mix Controller noise policy back into `CallGraphBuilder`.

5. `Analyzer` constructs every pipeline dependency inline. This is still small and
   readable. Candidate: do nothing unless a refactor needs test injection or removes
   real duplication. Do not introduce a factory or container.

6. `AnalysisExecutionResult` carries fields used differently by CLI and GUI. It
   should remain unless a specific field is proven unused by tests and callers.

## Task Plan, One Task at a Time

1. Run a repo-wide Ponytail audit and record the ranked findings in the task notes.

2. Remove unused dependency candidate, if confirmed:
   - Edit only `build.gradle`.
   - Run `./gradlew test`.
   - Stop and revert this task's own edit if tests fail because the dependency is
     still needed.

3. Shrink report tree recursion:
   - Add or keep a fixture assertion that protects exact text output.
   - Refactor `TextReportWriter` only.
   - Run `./gradlew test --tests com.onesi.smsa.report.TextReportWriterTest`.
   - Run the simple fixture check if output text changed.

4. Review GUI validation duplication:
   - Compare `AnalyzerGui` tests and `AnalysisRunnerTest`.
   - Change only if the user-visible behavior stays clear.
   - Run GUI and runner tests for the changed surface.

5. Review policy boundary:
   - Prefer keeping `CallResolutionPolicy` unless a smaller shape still preserves
     the architecture lesson from Dev 3.
   - Run `CallResolutionPolicyTest` and `CallGraphBuilderTest` after any change.

6. Run Ponytail review after each accepted diff, or once at the end if the diffs
   are documentation-only or very small.

7. Run full verification and compound:
   - `./gradlew test`
   - fixture command from the operating rules
   - `git diff --check`
   - compound doc only if the work produced a reusable lesson.

## Verification Plan

Minimum verification for implementation work:

```bash
./gradlew test
git diff --check
```

Fixture verification when report output or analyzer flow changes:

```bash
./gradlew run --args="src/test/resources/fixtures/simple-spring-mvc -o build/dev4-result.txt"
```

Check fixture output for:

- `UserController.createUser()`
- `unsupported: externalClient.send()`
- no `unsupported: Thread.sleep()`
- no `unsupported: model.addAttribute()`

For documentation-only changes, `git diff --check` is enough unless the doc edits
change commands, paths, or expected behavior.

## Subagent and Review Rules

- Only the first message to a subagent must start with `/caveman lite`.
- Only the first message to a subagent must state that subagent's role.
- Later messages to the same subagent do not repeat `/caveman lite` or the role.
- Implementer subagents get one bounded task and task-specific tests.
- Use Ponytail audit before implementation work.
- Use Ponytail review after each meaningful code diff, or at final review for a
  small batch.
- After the last code implementation task, run spec compliance review, code quality
  review, and independent test verification.
- The coordinator delegates code checks, diff review, Ponytail review, test
  verification, and documentation changes to subagents, then reads their reports.
- Documentation creation, modification, and edits are subagent work.
- The coordinator does not directly edit docs except for emergency coordination
  notes or explicit user instruction.
- The coordinator may perform only minimal git state checks needed to coordinate.
- Subagents must not push.
- Do not merge to `main` unless the user asks for the main-merge phase.

## Completion Criteria

- Dev 4 changes preserve CLI behavior, GUI behavior, LF UTF-8 reports, warning
  behavior, and fixture output.
- Each accepted refactor removes or shrinks current complexity.
- No speculative abstraction is added.
- Required tests pass from fresh command output.
- `git diff --check` passes.
- Any useful repeated lesson is captured under `docs/solutions/`.
- Final report lists status, files, verification, commit, and concerns.

## Risks

- Removing `javaparser-symbol-solver-core` may affect transitive parser behavior
  or future planned work. Mitigation: remove only if current tests pass; document
  future re-add as a feature task.

- Collapsing report recursion may change spacing or tree glyph placement.
  Mitigation: protect with expected fixture output before and after the change.

- Removing GUI pre-validation may make errors less friendly. Mitigation: keep
  blank-field validation and compare existing GUI tests before changing dialogs.

- Deleting the policy boundary may undo Dev 3's architecture improvement.
  Mitigation: treat `CallResolutionPolicy` as a keep-by-default candidate unless
  the final shape is both smaller and clearer.

- Ponytail can over-cut useful seams. Mitigation: every cut must preserve current
  behavior and be justified by present code, not future guesses.
