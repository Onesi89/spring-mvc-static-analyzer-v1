# Subagent Roles

This document defines the subagent roles for executing
`docs/superpowers/plans/2026-06-16-mvp-implementation.md` with the
Subagent-Driven Development workflow.

The coordinator is the main agent in the current session. The coordinator reads
the implementation plan, dispatches fresh subagents, reviews their reports, and
decides whether to continue, request fixes, or escalate.

Subagents must not rely on hidden session history. Each subagent receives the
specific task text, relevant files, expected commands, and success criteria from
the coordinator.

## Common Rules

- Only the first message to any subagent must explicitly state the role assigned
  for that task, such as Implementer, Spec Compliance Reviewer, Code Quality
  Reviewer, Test Verifier, or Final Reviewer.
- Only the first message to any subagent must start with `/caveman lite` unless
  the user explicitly asks for another communication style.
- Work only on the assigned task or review scope.
- Follow the implementation plan exactly unless the coordinator updates it.
- Protect user changes. Do not revert unrelated changes.
- Prefer small, focused changes.
- Use TDD for implementation tasks.
- Run task-specific verification commands before reporting success.
- Report exact commands run and their outcomes.
- If blocked, report `BLOCKED` with the reason and the smallest useful next question.
- Close all task subagents after each task report.
- Do not push to remote repositories.

## Coordinator

Purpose:

Own the full workflow and context. The coordinator delegates code verification,
diff review, tests, documentation changes, and review passes to subagents, then
reviews their reports before moving forward.

Responsibilities:

- Read the implementation plan before execution.
- Track task progress.
- Dispatch one implementer subagent per task.
- Delegate code verification, diff review, Ponytail review, and test verification
  to the appropriate subagents instead of repeating that work directly.
- Delegate documentation creation, modification, and edits to subagents.
- Do not directly edit docs except for emergency coordination notes or when the
  user explicitly instructs it.
- Dispatch a spec compliance reviewer after each implementation.
- Dispatch a code quality reviewer only after spec compliance passes.
- Send review findings back to the implementer until both reviews pass.
- Read subagent reports and decide whether to continue, request fixes, or
  dispatch additional reviewers/verifiers.
- Use only minimal direct git state checks needed for coordination.
- After final review approval, run the Compound Knowledge Capture step to
  preserve mistakes, lessons, and prevention rules from the execution cycle.
- Keep commits focused and in plan order.

Status handling:

- `DONE`: proceed to spec compliance review.
- `DONE_WITH_CONCERNS`: read concerns, then decide whether to review or request a fix first.
- `NEEDS_CONTEXT`: provide missing context and re-dispatch.
- `BLOCKED`: resolve context, improve instructions, split the task, or escalate to the user.

## Implementer Subagent

Purpose:

Implement one task from the plan using TDD and commit the finished work.

Input from coordinator:

- Task number and title.
- Full task text from the implementation plan.
- Relevant design/spec references.
- Current repository status if relevant.
- Exact verification commands expected for the task.

Responsibilities:

- Inspect only files needed for the assigned task.
- Write the failing test first.
- Run the test and confirm the expected failure.
- Implement the smallest production change that passes the test.
- Run the task-specific verification command before reporting. This is part of
  TDD and does not replace the independent Test Verifier role.
- Refactor only after tests pass.
- Commit only files related to the assigned task.
- Report changed files, commit hash, tests run, and concerns.

Output format:

```text
Status: DONE | DONE_WITH_CONCERNS | NEEDS_CONTEXT | BLOCKED
Task: <task number and title>
Changed files:
- <path>
Verification:
- <command>: <result>
Commit:
- <sha> <message>
Concerns:
- <none or concise notes>
```

## Spec Compliance Reviewer Subagent

Purpose:

Check whether the implementation matches the assigned task and the design
document. This reviewer focuses on correctness against requirements, not code
style.

Input from coordinator:

- Task number and title.
- Full task text from the implementation plan.
- Relevant design/spec sections.
- Commit hash or diff range to review.

Responsibilities:

- Verify every required file, behavior, test, command, and commit expectation.
- Identify missing requirements.
- Identify extra behavior outside the task scope.
- Confirm unsupported, unresolved, warning, and error-handling behavior when relevant.
- Avoid broad refactors unless required for spec compliance.

Output format:

```text
Status: APPROVED | CHANGES_REQUESTED
Task: <task number and title>
Findings:
- <severity>: <file:line if available> <requirement gap>
Spec coverage:
- <brief checklist summary>
```

Approval rule:

Only report `APPROVED` when the task fully satisfies the plan and no required
behavior is missing.

## Code Quality Reviewer Subagent

Purpose:

Review the already spec-compliant implementation for maintainability, local
design quality, test quality, and risk.

Input from coordinator:

- Task number and title.
- Commit hash or diff range to review.
- Relevant production and test files.
- Spec compliance reviewer result.

Responsibilities:

- Look for bugs, brittle tests, poor names, avoidable duplication, weak boundaries, and error-handling risks.
- Prioritize findings by severity.
- Avoid unrelated refactors.
- Check that tests verify behavior rather than implementation details.
- Confirm the implementation remains small and consistent with local patterns.

Output format:

```text
Status: APPROVED | CHANGES_REQUESTED
Task: <task number and title>
Findings:
- <severity>: <file:line if available> <issue and why it matters>
Residual risk:
- <none or concise note>
```

Approval rule:

Only report `APPROVED` when there are no blocking or important quality issues
left for the assigned task.

## Test Verifier Subagent

Purpose:

Independently verify the implementation with the commands requested by the
coordinator. This role checks the result; it does not own implementation.

Input from coordinator:

- Task number and title, or final branch verification scope.
- Commit hash or diff range to verify.
- Exact verification commands to run.
- Expected pass/fail criteria.

Responsibilities:

- Run the requested verification commands from a clean understanding of the
  branch state.
- Report exact command outcomes and failures.
- Do not modify production or test code unless the coordinator explicitly
  changes the role to Implementer.
- For this project's current workflow, run after the last code implementation
  task unless the user asks for per-task verifier checks.

Output format:

```text
Status: VERIFIED | FAILED | BLOCKED
Scope: <task or branch scope>
Verification:
- <command>: <result>
Failures:
- <none or concise failure details>
```

## Final Reviewer Subagent

Purpose:

Review the complete implementation after all task-level reviews pass.

Input from coordinator:

- Full implementation plan path.
- Final diff or commit range.
- Final verification command outputs.

Responsibilities:

- Confirm all acceptance criteria are covered.
- Confirm README and docs match implemented behavior.
- Review integration points across tasks.
- Check for accidental unrelated files.
- Identify remaining test gaps or release risks.

Output format:

```text
Status: APPROVED | CHANGES_REQUESTED
Findings:
- <severity>: <file:line if available> <issue>
Acceptance criteria:
- <brief checklist summary>
Residual risk:
- <none or concise note>
```

## Compound Knowledge Capture

Purpose:

Capture the mistakes, review findings, fixes, and prevention rules discovered
during the implementation and review cycle so future work does not repeat them.

Trigger:

Run this after all implementation tasks pass, final verification succeeds, and
the Final Reviewer Subagent reports `APPROVED`.

Required skill:

- `ce-compound`

Recommended invocation:

```text
ce-compound mode:headless Spring MVC static analyzer MVP execution lessons:
capture implementation mistakes, review findings, test gaps, command failures,
Git/authentication issues, and prevention rules discovered during the subagent
review cycle.
```

Coordinator responsibilities:

- Summarize concrete events from the execution cycle before invoking the skill.
- Include review findings that required fixes, not just final successful state.
- Include failed commands and their root causes when they changed the process.
- Prefer prevention rules that can guide future tasks.
- Verify the generated `docs/solutions/` document, if one is created.
- Commit the generated learning document separately from feature code.

If `ce-compound` cannot run in the current Codex session because newly installed
skills require a restart, record the intended invocation and run it immediately
after restarting Codex.

Output format:

```text
Status: DONE | BLOCKED
Generated docs:
- <path or none>
Captured lessons:
- <lesson>
Verification:
- <command>: <result>
Commit:
- <sha> <message, or none>
```

## Task-to-Role Mapping

Each implementation task uses this loop:

1. Implementer Subagent executes the task.
2. Spec Compliance Reviewer checks the task against the plan.
3. Implementer Subagent fixes any spec gaps.
4. Code Quality Reviewer checks maintainability and risk.
5. Implementer Subagent fixes any quality issues.
6. Coordinator verifies and marks the task complete.

After every task passes and the final reviewer approves the whole branch:

1. Coordinator delegates Compound Knowledge Capture to a documentation subagent.
2. Coordinator reviews the generated learning document report for accuracy.
3. Coordinator commits the learning document.
4. Coordinator reports the final implementation status.

Task complexity guidance:

- Tasks 1-4: standard implementer, standard reviewers.
- Tasks 5-10: more capable implementer recommended because parsing, graph construction, CLI behavior, and integration tests involve multi-file coordination.
- Task 11: standard implementer and reviewers.
- Final review: most capable reviewer available.
- Compound Knowledge Capture: delegated to a documentation subagent using
  `ce-compound` in `mode:headless`.

## Non-Goals

- Subagents do not decide product scope.
- Subagents do not change the implementation plan without coordinator approval.
- Subagents do not push commits.
- Subagents do not skip tests to save time.
- Subagents do not merge branches or rewrite history.
