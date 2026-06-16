package com.onesi.smsa.core;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class JavaFileScannerTest {
    @TempDir
    Path tempDir;

    @Test
    void findsJavaFilesRecursivelyInStableOrder() throws Exception {
        Files.createDirectories(tempDir.resolve("src/main/java/demo"));
        Path b = Files.writeString(tempDir.resolve("src/main/java/demo/B.java"), "class B {}");
        Path a = Files.writeString(tempDir.resolve("src/main/java/demo/A.java"), "class A {}");
        Files.writeString(tempDir.resolve("README.md"), "ignored");

        List<Path> files = new JavaFileScanner().scan(tempDir);

        assertThat(files).containsExactly(a, b);
    }

    @Test
    void returnsEmptyListWhenNoJavaFilesExist() throws Exception {
        Files.writeString(tempDir.resolve("README.md"), "ignored");

        List<Path> files = new JavaFileScanner().scan(tempDir);

        assertThat(files).isEmpty();
    }
}
