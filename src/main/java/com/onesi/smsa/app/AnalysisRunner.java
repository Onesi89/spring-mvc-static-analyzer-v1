package com.onesi.smsa.app;

import com.onesi.smsa.core.AnalysisResult;
import com.onesi.smsa.core.AnalysisWarning;
import com.onesi.smsa.core.Analyzer;
import com.onesi.smsa.report.TextReportWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

public class AnalysisRunner {
    public AnalysisExecutionResult run(AnalysisRequest request) {
        if (!Files.exists(request.targetPath()) || !Files.isDirectory(request.targetPath())) {
            return new AnalysisExecutionResult(
                    1,
                    request.targetPath(),
                    request.outputPath(),
                    false,
                    List.of(),
                    "Input path must be an existing directory: " + request.targetPath());
        }

        try {
            AnalysisResult result = new Analyzer().analyze(request.targetPath());
            List<AnalysisWarning> warnings = result.warnings();
            boolean inputError = warnings.stream().anyMatch(warning -> warning.code().equals("input-error"));
            if (inputError) {
                String message = warnings.stream()
                        .filter(warning -> warning.code().equals("input-error"))
                        .findFirst()
                        .map(AnalysisWarning::message)
                        .orElse("Input error.");
                return new AnalysisExecutionResult(
                        1, request.targetPath(), request.outputPath(), false, warnings, message);
            }

            String report = new TextReportWriter().write(result);
            Files.writeString(request.outputPath(), report, StandardCharsets.UTF_8);
            return new AnalysisExecutionResult(
                    0,
                    request.targetPath(),
                    request.outputPath(),
                    true,
                    warnings,
                    "Analysis complete.");
        } catch (Exception ex) {
            return new AnalysisExecutionResult(
                    2,
                    request.targetPath(),
                    request.outputPath(),
                    false,
                    List.of(),
                    "Analysis failed: " + ex.getMessage());
        }
    }
}
