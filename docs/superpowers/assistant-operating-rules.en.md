# Assistant Operating Rules

Read this first when I may have forgotten the project rules.

## 1. Before Work

- Check and read relevant plugins/skills before acting.
- If Superpowers may apply, start with `superpowers:using-superpowers`.
- Do implementation on a feature branch. Do not implement directly on `main`.
- Do not push unless the user explicitly asks.

## 2. Plugins / Skills

- Superpowers: required for planning, TDD, debugging, verification, and branch finishing.
- Ponytail: use minimal-change refactoring and avoid speculative abstractions.
- Caveman: use terse communication when requested or required.
- If a plugin seems inactive: check available skills, read `SKILL.md`, then report if unavailable.

## 3. Subagents

Only the first message to any subagent must start with:

```text
/caveman lite
Role: <Implementer | Spec Compliance Reviewer | Code Quality Reviewer | Test Verifier | Final Reviewer>
Task: <task name>
```

Rules:

- Do not repeat `/caveman lite` or the role in later messages to the same subagent.
- Provide task text, relevant files, success criteria, and verification commands.
- Implementer runs task-specific tests because TDD requires it.
- Test Verifier is an independent verification gate.
- After the last code implementation task, run Spec Reviewer, Code Quality Reviewer, and Test Verifier.
- Close all task subagents after the task report.
- Subagents must not push.

## 4. Compound

At the end, write compound docs for lessons from review, verification, or failure.

Capture:

- CI/test/build failures
- subagent role confusion
- verification changes
- architecture boundaries or policy decisions
- mistakes likely to repeat

Locations:

- workflow: `docs/solutions/workflow-issues/`
- test/build: `docs/solutions/test-failures/`, `docs/solutions/build-errors/`
- architecture: `docs/solutions/architecture-patterns/`

Validate compound frontmatter.

## 5. After All Tasks: Merge To Main

When all tasks are done:

```bash
./gradlew test
git switch main
git merge --ff-only <feature-branch>
./gradlew test
git branch -d <feature-branch>
```

Rules:

- Merge only to local `main`.
- User handles push.
- Re-run tests on merged `main`.

## 6. Verification

Do not claim completion without fresh command output.

Default:

```bash
./gradlew test
git diff --check
```

Fixture when needed:

```bash
./gradlew run --args="src/test/resources/fixtures/simple-spring-mvc -o build/dev3-result.txt"
```

Check:

- `UserController.createUser()` exists
- `unsupported: externalClient.send()` exists
- `unsupported: Thread.sleep()` absent
- `unsupported: model.addAttribute()` absent

## 7. Project Policy

- Goal: readable Spring MVC flow reports, not a perfect Java analyzer.
- Readability may beat analysis precision.
- Hide Controller noise.
- Keep Service/Repository unsupported calls visible.
- Future Spring Data JPA inherited methods should display like `UserRepository.findAll()`.
- CLI and GUI share `AnalysisRunner`.

## 8. Read Also

1. `docs/superpowers/subagent-roles.md`
2. `docs/superpowers/specs/2026-06-16-spring-mvc-static-analyzer-design3.ko.md`
3. `docs/solutions/workflow-issues/subagent-role-and-test-responsibility-clarity.md`
4. `docs/solutions/architecture-patterns/shared-analysis-runner-and-policy-boundary.md`
5. `docs/usage.md`
