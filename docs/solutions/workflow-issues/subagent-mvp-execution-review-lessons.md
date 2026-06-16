---
title: Subagent MVP Execution Review Lessons
date: 2026-06-17
category: docs/solutions/workflow-issues
module: Spring MVC Static Analyzer MVP
problem_type: workflow_issue
component: development_workflow
severity: medium
applies_when:
  - "Running a subagent-driven implementation plan with task-level review gates"
  - "Bootstrapping a Gradle project in an empty repository"
  - "Verifying CLI and build behavior from a sandboxed Codex environment"
tags: [subagent-review, gradle-wrapper, cli-exit-codes, immutable-graphs, codex-sandbox]
---

# Subagent MVP Execution Review Lessons

## Context

The MVP implementation used a subagent-driven workflow: each task was implemented by a fresh implementer, then checked by a spec reviewer and a code quality reviewer. The process worked, but several review-cycle findings showed where future plans need stronger guardrails.

The important pattern was that most issues were not large design failures. They were small process gaps that would have repeated without review: missing bootstrap tooling, environment-specific verification failures, insufficient test coverage, mutable graph data, and CLI exit-code ambiguity.

## Guidance

When running a similar implementation cycle, add these checks before or during execution:

- Include build bootstrap files in Task 1 when the repository is empty. If the plan requires `./gradlew test`, the plan must also create a working `gradlew` path or explicitly state how the wrapper is generated.
- Treat sandbox build failures as environment evidence before changing code. In this project, Gradle failed in the sandbox because daemon socket creation was blocked, while escalated execution passed.
- Require review of collection immutability for graph-like data structures. `CallGraph` initially copied only the outer map; reviewers caught that mutable edge lists could still change traversal behavior.
- Require CLI tests for missing arguments, not only invalid paths. Picocli can return its own usage-code before `call()` runs unless parameters are optional and validated by the command.
- Accept an integration test that passes immediately only when earlier implementation already satisfies the new golden output and reviewers explicitly approve it as behavior-locking coverage.
- Capture review findings as durable knowledge after final approval, before merging back to `main`.

## Why This Matters

Subagent workflows make it easy to move quickly, but they can also hide repeated small mistakes if the coordinator only looks at final success. The review loop caught issues that individual task tests did not fully protect:

- A missing Gradle wrapper made the first verification command impossible.
- The local sandbox blocked Gradle daemon sockets, so normal command failure did not mean code failure.
- A shallow immutable copy in `CallGraph` left traversal order mutable.
- A required CLI parameter let Picocli return exit code `2`, conflicting with the project's intended fatal-error code.
- Task 10 could not produce a RED integration failure because the existing pipeline already matched the golden file.

Recording these lessons keeps future implementation plans sharper and makes review effort compound instead of evaporating at the end of the session.

## When to Apply

- Starting a greenfield CLI project from an empty repository.
- Using Gradle in a restricted or sandboxed execution environment.
- Building call graphs, trees, dependency graphs, or any structure where traversal order matters.
- Mapping CLI exit codes for automation or scripts.
- Running subagent-driven development with review gates.

## Examples

### Build bootstrap

Before:

```text
Task 1 says: Run ./gradlew test
Repository state: no gradlew, no system gradle
Result: verification blocked
```

After:

```text
Task 1 includes .gitignore and gradlew bootstrap behavior
Verification path exists before tests are expected to run
```

### CLI exit-code mapping

Before:

```java
@Parameters(index = "0", description = "Target Spring MVC project path")
private Path targetPath;
```

Picocli can reject missing input before `call()` and return its default usage error.

After:

```java
@Parameters(index = "0", arity = "0..1", description = "Target Spring MVC project path")
private Path targetPath;
```

The command owns the missing-input case and can return the project's user-error code.

### Graph immutability

Before:

```java
edges = Map.copyOf(edges);
```

The outer map is immutable, but callers can still mutate lists stored inside it.

After:

```java
edges = edges.entrySet().stream()
        .collect(Collectors.toUnmodifiableMap(
                Map.Entry::getKey,
                entry -> List.copyOf(entry.getValue())));
```

Both the map and edge lists are protected.

## Related

- docs/superpowers/plans/2026-06-16-mvp-implementation.md
- docs/superpowers/subagent-roles.md
