package com.onesi.smsa.core;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class AnalysisWarningTest {
    @Test
    void formatsWarningForReport() {
        AnalysisWarning warning = new AnalysisWarning(
                "parse-error",
                Path.of("src/main/java/Broken.java"),
                "Could not parse Java source.");

        assertThat(warning.format()).isEqualTo("""
                [parse-error] src/main/java/Broken.java
                  Could not parse Java source.""".stripTrailing());
        assertThat(warning.format()).doesNotContain("\r\n");
    }
}
