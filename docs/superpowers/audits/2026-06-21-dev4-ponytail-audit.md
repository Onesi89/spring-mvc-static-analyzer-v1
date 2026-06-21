# Dev 4 Ponytail Audit

Scope: repo-wide over-engineering pass only. No correctness, security, or
performance findings.

## Ranked Findings

1. `native:` Cut unused `javaparser-symbol-solver-core` dependency. Keep `javaparser-core`; current imports use parser AST/config only. [build.gradle]
2. `shrink:` Cut duplicate tree-recursion methods in report writer. Use one child-writing helper that prints edge prefix and recurses. [src/main/java/com/onesi/smsa/report/TextReportWriter.java]
3. `shrink:` Cut duplicate GUI target-directory existence check. Keep blank-field dialog in GUI; let shared `AnalysisRunner` own directory validation and message. [src/main/java/com/onesi/smsa/gui/AnalyzerGui.java]
4. `shrink:` Cut redundant `Files.exists(...) || !Files.isDirectory(...)` pair. Use `!Files.isDirectory(...)`; Java returns false for missing paths. [src/main/java/com/onesi/smsa/app/AnalysisRunner.java]
5. `yagni:` Cut defensive `Arrays.copyOf(args, args.length)` before picocli execution. Pass `args` directly; no current caller needs an immutable copy. [src/main/java/com/onesi/smsa/AppLauncher.java]
6. `yagni:` Do not cut `CallResolutionPolicy` now. It is one rule, but removing it pushes Controller unsupported-noise policy back into graph construction. [src/main/java/com/onesi/smsa/graph/policy/CallResolutionPolicy.java]
7. `yagni:` Do not add factories or injection around `Analyzer` construction. Inline pipeline construction is shorter than a container for one production path. [src/main/java/com/onesi/smsa/core/Analyzer.java]
8. `delete:` Reject trimming `AnalysisExecutionResult` fields. CLI, GUI, and tests currently consume the record shape across different result paths. [src/main/java/com/onesi/smsa/app/AnalysisExecutionResult.java]

net: -35 lines, -1 deps possible.

## Adopt / Defer / Reject

Adopt:

- Task 2: finding 1. Remove unused dependency after `./gradlew test`.
- Task 3: finding 2. Collapse report recursion with exact text fixture protected.
- Task 4: finding 3. Remove GUI directory pre-validation only if GUI tests preserve friendly blank-field behavior.
- Small cleanup: findings 4 and 5 can ride with nearby app/launcher refactor work, or be skipped if Dev 4 wants only named design candidates.

Defer:

- Task 5: finding 6. Keep `CallResolutionPolicy` unless a smaller shape preserves the Dev 3 policy boundary.
- Task 5: finding 7. Keep `Analyzer` inline construction; add no factory/container.

Reject:

- Task 6 candidate: finding 8. `AnalysisExecutionResult` is not proven dead; current callers use its fields differently.
- Any future-only JavaSymbolSolver, Spring Data, MyBatis, GUI redesign, installer, or package-split work.
