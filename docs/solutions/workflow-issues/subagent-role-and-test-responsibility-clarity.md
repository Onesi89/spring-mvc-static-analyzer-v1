---
title: Clarify subagent roles and test responsibility
date: 2026-06-20
category: workflow-issues
module: subagent-development-workflow
problem_type: workflow_issue
component: development_workflow
severity: medium
applies_when:
  - "Dispatching implementation and review subagents"
  - "Using task-level subagent workflow with separate reviewer agents"
  - "Deciding which agent owns tests during TDD"
tags: [subagents, tdd, reviews, workflow]
---

# Clarify Subagent Roles And Test Responsibility

## Context

During the development 3 workflow, the coordinator dispatched implementation
subagents and reviewer subagents, but the first subagent prompt did not always
state the assigned role clearly enough. The user later asked why an implementer
agent ran tests when review and test agents also exist.

The confusion came from an unclear boundary between two valid responsibilities:
the implementer must run task-specific tests as part of TDD, while the Test
Verifier is an independent quality gate.

## Guidance

Every first message to a subagent must explicitly state the role assigned for
that task. Start with the communication style and role before the task details:

```text
/caveman lite
Role: Implementer Subagent
Task: Task 2 - Route CLI through AnalysisRunner
```

Use the same pattern for reviewers:

```text
/caveman lite
Role: Test Verifier Subagent
Scope: Final code implementation verification
```

Keep test responsibility split this way:

- The Implementer runs task-specific tests because TDD requires writing or
  selecting a failing test, making it pass, and verifying the local change.
- The Test Verifier runs independent verification after implementation, using
  commands supplied by the coordinator.
- In this project workflow, the Test Verifier runs after the last code
  implementation task unless the user explicitly asks for per-task verifier
  checks.
- After each task report, close all task subagents so stale role assumptions do
  not leak into the next task.

## Why This Matters

Clear roles prevent the coordinator from treating subagents as interchangeable.
They also make test ownership explicit. An implementer running tests is not a
role violation; skipping tests would be the violation.

Separate verification still matters because it gives the branch a second check
from an agent that did not write the code. The value is independence, not
exclusive ownership of all test execution.

## When to Apply

- Starting a new task with subagent-driven development.
- Assigning implementer, spec reviewer, code quality reviewer, test verifier,
  or final reviewer roles.
- Using TDD while also planning independent verification.
- A user asks why a subagent performed work that seems to overlap with another
  role.

## Examples

Before:

```text
/caveman lite
Task 2 진행해. CLI를 AnalysisRunner로 연결해.
```

This asks for work, but the role is implicit.

After:

```text
/caveman lite
Role: Implementer Subagent
Task 2 진행해. CLI를 AnalysisRunner로 연결해.
Run task-specific tests before reporting.
```

This makes the agent's authority and expected verification explicit.

For final verification:

```text
/caveman lite
Role: Test Verifier Subagent
Verify the final implementation branch. Do not edit files.
Run the coordinator-provided commands and report exact outcomes.
```

## Related

- docs/superpowers/subagent-roles.md
- docs/solutions/workflow-issues/simplify-ci-artifact-verification.md
