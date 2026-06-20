package com.onesi.smsa.cli;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

class AnalyzeCommandTest {
    private static final Path SIMPLE_FIXTURE = Path.of("src/test/resources/fixtures/simple-spring-mvc");
    private static final Path ANALYZE_COMMAND_SOURCE = Path.of("src/main/java/com/onesi/smsa/cli/AnalyzeCommand.java");

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

    @Test
    void returnsZeroAndWritesResultForValidFixture() {
        Path output = tempDir.resolve("result.txt");

        int exitCode = new CommandLine(new AnalyzeCommand()).execute(
                SIMPLE_FIXTURE.toString(), "-o", output.toString());

        assertThat(exitCode).isEqualTo(0);
        assertThat(output).exists();
        assertThat(output).content().contains("UserController");
    }

    @Test
    void delegatesAnalysisAndReportWritingToAnalysisRunner() throws IOException {
        String source = Files.readString(ANALYZE_COMMAND_SOURCE);

        assertThat(source).contains("AnalysisRunner", "AnalysisRequest");
        assertThat(source)
                .doesNotContain("import com.onesi.smsa.core.Analyzer;")
                .doesNotContain("import com.onesi.smsa.report.TextReportWriter;")
                .doesNotContain("Files.writeString");
    }
}
