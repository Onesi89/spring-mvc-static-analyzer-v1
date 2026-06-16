package com.onesi.smsa.cli;

import com.onesi.smsa.core.AnalysisResult;
import com.onesi.smsa.core.Analyzer;
import com.onesi.smsa.report.TextReportWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "analyze", mixinStandardHelpOptions = true, description = "Analyze Spring MVC call flows.")
public class AnalyzeCommand implements Callable<Integer> {
    @Parameters(index = "0", description = "Target Spring MVC project path")
    private Path targetPath;

    @Option(names = {"-o", "--output"}, defaultValue = "result.txt", description = "Output report path")
    private Path outputPath;

    @Override
    public Integer call() {
        try {
            if (!Files.exists(targetPath) || !Files.isDirectory(targetPath)) {
                System.err.println("ERROR: Input path must be an existing directory: " + targetPath);
                return 1;
            }

            AnalysisResult result = new Analyzer().analyze(targetPath);
            boolean inputError = result.warnings().stream()
                    .anyMatch(warning -> warning.code().equals("input-error"));
            if (inputError) {
                result.warnings().forEach(warning -> System.err.println("ERROR: " + warning.message()));
                return 1;
            }

            String report = new TextReportWriter().write(result);
            Files.writeString(outputPath, report, StandardCharsets.UTF_8);
            return 0;
        } catch (Exception ex) {
            System.err.println("ERROR: Analysis failed: " + ex.getMessage());
            return 2;
        }
    }
}
