package com.onesi.smsa.app;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AnalysisRunnerTest {
    @TempDir
    Path tempDir;

    @Test
    void writesUtf8ReportForValidFixture() throws Exception {
        Path targetPath = Path.of("src/test/resources/fixtures/simple-spring-mvc");
        Path outputPath = tempDir.resolve("report.txt");

        AnalysisExecutionResult result = new AnalysisRunner().run(new AnalysisRequest(targetPath, outputPath));

        assertThat(result.exitCode()).isEqualTo(0);
        assertThat(result.targetPath()).isEqualTo(targetPath);
        assertThat(result.outputPath()).isEqualTo(outputPath);
        assertThat(result.reportWritten()).isTrue();
        assertThat(result.warnings()).isEmpty();
        assertThat(result.message()).isEqualTo("Analysis complete.");
        assertThat(Files.readString(outputPath, StandardCharsets.UTF_8)).contains("UserController.createUser()");
    }

    @Test
    void missingTargetPathReturnsOneWithoutWritingReport() {
        Path targetPath = tempDir.resolve("missing");
        Path outputPath = tempDir.resolve("report.txt");

        AnalysisExecutionResult result = new AnalysisRunner().run(new AnalysisRequest(targetPath, outputPath));

        assertThat(result.exitCode()).isEqualTo(1);
        assertThat(result.targetPath()).isEqualTo(targetPath);
        assertThat(result.outputPath()).isEqualTo(outputPath);
        assertThat(result.reportWritten()).isFalse();
        assertThat(result.warnings()).isEmpty();
        assertThat(result.message()).isEqualTo("Input path must be an existing directory: " + targetPath);
        assertThat(outputPath).doesNotExist();
    }

    @Test
    void directoryWithNoJavaFilesReturnsOneAndPreservesInputErrorWarning() throws Exception {
        Path outputPath = tempDir.resolve("report.txt");

        AnalysisExecutionResult result = new AnalysisRunner().run(new AnalysisRequest(tempDir, outputPath));

        assertThat(result.exitCode()).isEqualTo(1);
        assertThat(result.reportWritten()).isFalse();
        assertThat(result.warnings()).singleElement().satisfies(warning -> {
            assertThat(warning.code()).isEqualTo("input-error");
            assertThat(warning.path()).isEqualTo(tempDir);
            assertThat(warning.message()).isEqualTo("No Java files found.");
        });
        assertThat(result.message()).isEqualTo("No Java files found.");
        assertThat(outputPath).doesNotExist();
    }

    @Test
    void reportWriteFailureReturnsTwoWithoutMarkingReportWritten() throws Exception {
        Path targetPath = Path.of("src/test/resources/fixtures/simple-spring-mvc");

        AnalysisExecutionResult result = new AnalysisRunner().run(new AnalysisRequest(targetPath, tempDir));

        assertThat(result.exitCode()).isEqualTo(2);
        assertThat(result.reportWritten()).isFalse();
        assertThat(result.warnings()).isEmpty();
        assertThat(result.message()).startsWith("Analysis failed: ");
    }
}
