package com.onesi.smsa.core;

import static org.assertj.core.api.Assertions.assertThat;

import com.onesi.smsa.report.TextReportWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class AnalyzerIntegrationTest {
    @Test
    void analyzesSimpleSpringMvcFixture() throws Exception {
        Path fixture = Path.of("src/test/resources/fixtures/simple-spring-mvc");
        Path expected = Path.of("src/test/resources/expected/simple-result.txt");

        String actual = new TextReportWriter().write(new Analyzer().analyze(fixture)).stripTrailing();
        String expectedText = Files.readString(expected).replace("\r\n", "\n").stripTrailing();

        assertThat(actual.replace("\r\n", "\n")).isEqualTo(expectedText);
    }
}
