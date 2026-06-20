package com.onesi.smsa.app;

import com.onesi.smsa.core.AnalysisWarning;
import java.nio.file.Path;
import java.util.List;

public record AnalysisExecutionResult(
        int exitCode,
        Path targetPath,
        Path outputPath,
        boolean reportWritten,
        List<AnalysisWarning> warnings,
        String message) {
    public AnalysisExecutionResult {
        warnings = List.copyOf(warnings);
    }
}
