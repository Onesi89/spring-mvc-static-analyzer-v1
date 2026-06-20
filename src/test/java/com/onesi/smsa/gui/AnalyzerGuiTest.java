package com.onesi.smsa.gui;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class AnalyzerGuiTest {
    @Test
    void delegatesAnalysisAndReportWritingToAnalysisRunner() throws IOException {
        String source = Files.readString(Path.of("src/main/java/com/onesi/smsa/gui/AnalyzerGui.java"));

        assertThat(source).contains("AnalysisRunner", "AnalysisRequest");
        assertThat(source)
                .doesNotContain("import com.onesi.smsa.core.AnalysisResult;")
                .doesNotContain("import com.onesi.smsa.core.Analyzer;")
                .doesNotContain("import com.onesi.smsa.report.TextReportWriter;")
                .doesNotContain("import java.nio.charset.StandardCharsets;")
                .doesNotContain("new Analyzer()")
                .doesNotContain("Files.writeString(");
    }
}
