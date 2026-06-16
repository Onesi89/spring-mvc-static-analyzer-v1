package com.onesi.smsa;

import com.onesi.smsa.cli.AnalyzeCommand;
import picocli.CommandLine;

public final class Main {
    private Main() {
    }

    public static void main(String[] args) {
        int exitCode = new CommandLine(new AnalyzeCommand()).execute(args);
        System.exit(exitCode);
    }
}
