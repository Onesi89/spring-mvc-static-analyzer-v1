package com.onesi.smsa.cli;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

class AnalyzeCommandTest {
    @TempDir
    Path tempDir;

    @Test
    void returnsOneForMissingInputArgument() {
        int exitCode = new CommandLine(new AnalyzeCommand()).execute();

        assertThat(exitCode).isEqualTo(1);
    }

    @Test
    void returnsOneForMissingInputPath() {
        int exitCode = new CommandLine(new AnalyzeCommand()).execute(tempDir.resolve("missing").toString());

        assertThat(exitCode).isEqualTo(1);
    }

    @Test
    void returnsOneWhenNoJavaFilesExist() {
        int exitCode = new CommandLine(new AnalyzeCommand()).execute(
                tempDir.toString(), "-o", tempDir.resolve("result.txt").toString());

        assertThat(exitCode).isEqualTo(1);
    }
}
