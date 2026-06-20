---
title: Shared analysis runner and policy boundary
date: 2026-06-20
category: architecture-patterns
module: analyzer-refactoring
problem_type: architecture_pattern
component: service_object
severity: medium
applies_when:
  - "CLI and GUI execute the same analysis workflow"
  - "Static analyzer behavior grows from simple graph building into policy decisions"
  - "A refactor must preserve readable report output"
tags: [analysis-runner, policy-boundary, cli, gui, static-analysis]
---

# Shared Analysis Runner And Policy Boundary

## Context

Development 3 refactored duplicated CLI and GUI execution paths without changing
the core analysis output. Before the refactor, the CLI and Swing GUI each knew
how to run analysis, format the report, and write the result file.

The graph builder also contained a small controller-specific rule that suppressed
unsupported UI/framework noise such as `Thread.sleep()` and `model.addAttribute()`.
That was acceptable at first, but it made future repository policies harder to
add without turning `CallGraphBuilder` into a mixed collection of rules.

## Guidance

Keep user entry points thin:

- CLI parses arguments and returns the runner exit code.
- GUI collects paths, manages button state, and displays log messages.
- `AnalysisRunner` owns analysis execution and report writing.
- `CallResolutionPolicy` owns suppress/keep decisions for call edges.

This keeps the main pipeline boring:

```text
CLI / GUI
→ AnalysisRunner
→ Analyzer
→ TextReportWriter
→ result.txt
```

Use a policy boundary for analyzer heuristics:

```java
if (policy.shouldSuppress(owner, edge)) {
    continue;
}
```

Do not restructure marker types or add future-looking abstractions until a
specific policy needs them. Development 3 deliberately left marker type
structure for a later task because string markers still satisfy the current
report contract.

## Why This Matters

Static analyzers grow by adding heuristics. If each heuristic lands in the graph
builder, the builder stops being a graph builder and becomes the hardest class to
reason about.

The shared runner also prevents CLI and GUI drift. A fix to input errors, report
writing, or warning handling now has one main execution path instead of two.

## When to Apply

- A CLI and GUI both execute the same workflow.
- UI code starts doing file writes, report formatting, or analysis orchestration.
- A builder class starts carrying layer-specific behavior rules.
- A future feature, such as Spring Data JPA inherited repository methods, needs
  a place for heuristic decisions.

## Examples

Before:

```java
if (owner.layer() == Layer.CONTROLLER
        && !edge.resolved()
        && edge.markerText().startsWith("unsupported: ")) {
    continue;
}
```

After:

```java
if (policy.shouldSuppress(owner, edge)) {
    continue;
}
```

For Spring Data JPA, keep the same principle: add repository method heuristics
behind policy classes, then let the graph builder consume the policy result.

## Related

- docs/superpowers/specs/2026-06-16-spring-mvc-static-analyzer-design3.ko.md
- docs/usage.md
- src/main/java/com/onesi/smsa/app/AnalysisRunner.java
- src/main/java/com/onesi/smsa/graph/policy/CallResolutionPolicy.java
