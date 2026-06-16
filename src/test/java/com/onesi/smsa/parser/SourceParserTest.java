package com.onesi.smsa.parser;

import static org.assertj.core.api.Assertions.assertThat;

import com.onesi.smsa.core.AnalysisWarning;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SourceParserTest {
    @TempDir
    Path tempDir;

    @Test
    void parsesValidSource() throws Exception {
        Path source = Files.writeString(tempDir.resolve("UserController.java"), "class UserController {}");
        List<AnalysisWarning> warnings = new ArrayList<>();

        List<ParsedSource> parsed = new SourceParser().parse(List.of(source), warnings);

        assertThat(parsed).hasSize(1);
        assertThat(warnings).isEmpty();
    }

    @Test
    void recordsWarningForBrokenSourceAndContinues() throws Exception {
        Path source = Files.writeString(tempDir.resolve("Broken.java"), "class Broken {");
        List<AnalysisWarning> warnings = new ArrayList<>();

        List<ParsedSource> parsed = new SourceParser().parse(List.of(source), warnings);

        assertThat(parsed).isEmpty();
        assertThat(warnings).singleElement()
                .extracting(AnalysisWarning::code)
                .isEqualTo("parse-error");
    }
}
