package com.onesi.smsa.parser;

import com.github.javaparser.ast.CompilationUnit;
import java.nio.file.Path;

public record ParsedSource(Path path, CompilationUnit compilationUnit) {
}
