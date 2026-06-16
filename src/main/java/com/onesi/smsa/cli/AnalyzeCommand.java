package com.onesi.smsa.cli;

import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "analyze", mixinStandardHelpOptions = true)
public class AnalyzeCommand implements Callable<Integer> {
    @Parameters(index = "0", arity = "0..1", description = "Target Spring MVC project path")
    private String targetPath;

    @Option(names = {"-o", "--output"}, defaultValue = "result.txt", description = "Output report path")
    private String outputPath;

    @Override
    public Integer call() {
        if (targetPath == null || targetPath.isBlank()) {
            return 1;
        }
        return 0;
    }
}
