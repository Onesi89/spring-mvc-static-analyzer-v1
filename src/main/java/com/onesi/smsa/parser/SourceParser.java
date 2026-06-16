package com.onesi.smsa.parser;

import com.github.javaparser.ParseProblemException;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.Problem;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.onesi.smsa.core.AnalysisWarning;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class SourceParser {
    public List<ParsedSource> parse(List<Path> files, List<AnalysisWarning> warnings) {
        StaticJavaParser.getParserConfiguration().setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_17);
        List<ParsedSource> parsedSources = new ArrayList<>();
        for (Path file : files) {
            try {
                CompilationUnit compilationUnit = StaticJavaParser.parse(file);
                parsedSources.add(new ParsedSource(file, compilationUnit));
            } catch (ParseProblemException ex) {
                String message = ex.getProblems().stream()
                        .findFirst()
                        .map(Problem::getMessage)
                        .orElse("Could not parse Java source.");
                warnings.add(new AnalysisWarning("parse-error", file, message));
            } catch (IOException ex) {
                warnings.add(new AnalysisWarning("read-error", file, "Could not read Java source."));
            }
        }
        return parsedSources;
    }
}
