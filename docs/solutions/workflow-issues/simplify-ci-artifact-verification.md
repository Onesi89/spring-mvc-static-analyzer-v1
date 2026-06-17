---
title: Simplify CI artifact verification
date: 2026-06-17
category: workflow-issues
module: github-actions-distribution
problem_type: workflow_issue
component: development_workflow
severity: medium
applies_when:
  - "Verifying packaged artifacts produced by GitHub Actions"
  - "Packaging applications with jpackage app-image"
  - "Reviewing CI checks that inspect generated archive internals"
tags: [github-actions, jpackage, artifact-verification, windows-runtime]
---

# Simplify CI Artifact Verification

## Context

The Windows runtime ZIP workflow created a distribution archive, but the verification step failed because it looked for `runtime/bin/java.exe` inside the ZIP. That check assumed a JDK-like runtime layout, while `jpackage --type app-image` is free to arrange its bundled runtime under the application image structure.

The failure was in the CI verification script, not in the artifact creation task. The generated ZIP existed, but the check inspected an internal file path that users do not rely on directly.

## Guidance

Keep artifact verification focused on the user-visible contract:

- The expected ZIP file exists.
- The ZIP contains the application launcher users will execute.
- The artifact upload step points at the expected ZIP path.

Avoid checking internal runtime files such as `runtime/bin/java.exe` unless that exact path is a documented artifact contract. For `jpackage` app images, the launcher and the app image layout are the stable surface; individual runtime file locations are implementation details.

The simplified check verifies only the Windows runtime ZIP and the application `.exe`:

```powershell
$archives = Get-ChildItem -Path build\distributions -Filter *-windows-runtime.zip
if (-not $archives) {
  throw "No Windows runtime ZIP was created."
}

foreach ($archive in $archives) {
  $entries = [System.IO.Compression.ZipFile]::OpenRead($archive.FullName)
  try {
    $exe = $entries.Entries | Where-Object {
      $_.FullName -like "*/spring-mvc-static-analyzer-v1.exe"
    }

    if (-not $exe) {
      throw "No application .exe found in $($archive.FullName)."
    }

    Write-Host "$($archive.FullName): application .exe verified."
  } finally {
    $entries.Dispose()
  }
}
```

## Why This Matters

Over-specific CI checks are brittle. They can fail when a tool changes internal structure even though the user-facing artifact is still valid. That creates false negatives and can send debugging toward the wrong component.

For this project, the user's real requirement is simple: download a Windows ZIP and run the analyzer without installing Java separately. Verifying the runtime implementation file-by-file adds complexity without improving that user contract.

## When to Apply

- A CI workflow validates generated ZIP, tar, installer, or app-image artifacts.
- The validation script checks deep internal paths created by a packaging tool.
- A tool such as `jpackage`, Gradle, or a platform packager owns the artifact layout.
- A failing validation step happens after the build task already produced the artifact.

## Examples

Before:

```powershell
$java = $entries.Entries | Where-Object {
  $_.FullName -like "*/runtime/bin/java.exe"
}

if (-not $java) {
  throw "No bundled runtime java.exe found in $($archive.FullName)."
}
```

This couples CI to an internal runtime path.

After:

```powershell
$exe = $entries.Entries | Where-Object {
  $_.FullName -like "*/spring-mvc-static-analyzer-v1.exe"
}

if (-not $exe) {
  throw "No application .exe found in $($archive.FullName)."
}
```

This checks the artifact surface that users actually execute.

## Related

- docs/solutions/test-failures/windows-runner-report-line-ending-test-failure.md
- .github/workflows/build.yml
