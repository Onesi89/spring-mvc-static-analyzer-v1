---
title: Windows runner report line ending test failure
date: 2026-06-17
category: test-failures
module: report-output
problem_type: test_failure
component: testing_framework
symptoms:
  - "GitHub Actions windows-latest failed in :test before windowsRuntimeZip packaging"
  - "AnalysisWarningTest.formatsWarningForReport failed only on Windows"
  - "TextReportWriterTest.omitsWarningsSectionWhenNoWarnings failed only on Windows"
root_cause: config_error
resolution_type: code_fix
severity: medium
tags: [windows, line-endings, github-actions, reports]
---

# Windows Runner Report Line Ending Test Failure

## Problem
The Windows runtime ZIP workflow failed before packaging because tests compared multiline report output with OS-dependent line endings. The report writer used the host JVM's line separator, so Windows produced CRLF while the expected text blocks used LF.

## Symptoms
- GitHub Actions failed during `./gradlew test windowsRuntimeZip`.
- The failure happened in `:test`, not in `jpackage` or `windowsRuntimeZip`.
- `AnalysisWarningTest.formatsWarningForReport` and `TextReportWriterTest.omitsWarningsSectionWhenNoWarnings` failed on `windows-latest`.
- The same tests passed on local Ubuntu because Linux `System.lineSeparator()` is LF.

## What Didn't Work
- Treating the failure as a Windows packaging issue would have chased the wrong component. The log showed `windowsRuntimeZip` never ran because `:test` failed first.
- Keeping `System.lineSeparator()` in report output would make golden text comparisons and generated txt files differ by operating system.

## Solution
Fix generated report text to use LF explicitly instead of `System.lineSeparator()`.

In `AnalysisWarning`, warning formatting now joins lines with `"\n"`:

```java
return "[" + code + "] " + path.toString().replace('\\', '/') + "\n"
        + "  " + message;
```

In `TextReportWriter`, report assembly uses a fixed newline constant:

```java
private static final String NEW_LINE = "\n";
```

Regression tests assert that formatted report strings do not contain CRLF:

```java
assertThat(report).doesNotContain("\r\n");
```

## Why This Works
The analyzer produces human-readable UTF-8 txt reports, and those reports are easier to compare, test, and document when their line endings are deterministic. Fixing line endings at the report boundary also avoids making every test normalize output before asserting.

## Prevention
- For golden-file-like text output, choose a canonical line ending and write it explicitly.
- Do not use `System.lineSeparator()` for persisted report formats unless OS-native line endings are a deliberate requirement.
- When adding Windows CI jobs, expect previously hidden OS differences in paths, process launchers, and line endings.
- If a Windows workflow fails before packaging, inspect the failing Gradle task first; the packaging task may not have started.

## Related Issues
- No existing `docs/solutions` entry covered this specific Windows line-ending failure.
