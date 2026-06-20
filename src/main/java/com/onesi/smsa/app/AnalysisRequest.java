package com.onesi.smsa.app;

import java.nio.file.Path;

public record AnalysisRequest(Path targetPath, Path outputPath) {}
