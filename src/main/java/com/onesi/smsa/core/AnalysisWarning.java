package com.onesi.smsa.core;

import java.nio.file.Path;

public record AnalysisWarning(String code, Path path, String message) {
    public String format() {
        return "[" + code + "] " + path.toString().replace('\\', '/') + System.lineSeparator()
                + "  " + message;
    }
}
