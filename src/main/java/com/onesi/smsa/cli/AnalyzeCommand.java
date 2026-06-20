package com.onesi.smsa.cli;

import com.onesi.smsa.app.AnalysisExecutionResult;
import com.onesi.smsa.app.AnalysisRequest;
import com.onesi.smsa.app.AnalysisRunner;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "analyze", mixinStandardHelpOptions = true, description = "Analyze Spring MVC call flows.")
public class AnalyzeCommand implements Callable<Integer> {
    @Parameters(index = "0", arity = "0..1", description = "Target Spring MVC project path")
    private Path targetPath;

    @Option(names = {"-o", "--output"}, defaultValue = "result.txt", description = "Output report path")
    private Path outputPath;

    @Override
    public Integer call() {
        try {
            if (targetPath == null) {
                System.err.println("ERROR: Input path is required.");
                return 1;
            }

            AnalysisExecutionResult result = new AnalysisRunner().run(new AnalysisRequest(targetPath, outputPath));
            if (result.exitCode() != 0) {
                System.err.println("ERROR: " + result.message());
            }
            return result.exitCode();
        } catch (Exception ex) {
            System.err.println("ERROR: Analysis failed: " + ex.getMessage());
            return 2;
        }
    }
}
