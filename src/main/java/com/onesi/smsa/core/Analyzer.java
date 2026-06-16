package com.onesi.smsa.core;

import com.onesi.smsa.extract.ClassModelExtractor;
import com.onesi.smsa.extract.ControllerEntryPointFinder;
import com.onesi.smsa.extract.InjectionResolver;
import com.onesi.smsa.extract.LayerClassifier;
import com.onesi.smsa.extract.MethodCallExtractor;
import com.onesi.smsa.graph.CallGraph;
import com.onesi.smsa.graph.CallGraphBuilder;
import com.onesi.smsa.model.ClassInfo;
import com.onesi.smsa.model.MethodRef;
import com.onesi.smsa.parser.ParsedSource;
import com.onesi.smsa.parser.SourceParser;
import com.onesi.smsa.tree.CallTreeBuilder;
import com.onesi.smsa.tree.CallTreeNode;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Analyzer {
    public AnalysisResult analyze(Path targetPath) throws IOException {
        List<AnalysisWarning> warnings = new ArrayList<>();
        List<Path> javaFiles = new JavaFileScanner().scan(targetPath);
        if (javaFiles.isEmpty()) {
            return new AnalysisResult(
                    List.of(), List.of(new AnalysisWarning("input-error", targetPath, "No Java files found.")));
        }

        List<ParsedSource> parsedSources = new SourceParser().parse(javaFiles, warnings);
        List<ClassInfo> classes = new ClassModelExtractor(new LayerClassifier()).extract(parsedSources);
        Map<String, Map<String, String>> injections = new InjectionResolver().resolve(classes);
        CallGraph graph = new CallGraphBuilder(new MethodCallExtractor()).build(classes, injections);
        CallTreeBuilder treeBuilder = new CallTreeBuilder(20);
        List<CallTreeNode> roots = new ControllerEntryPointFinder().find(classes).stream()
                .map((MethodRef entryPoint) -> treeBuilder.build(entryPoint, graph))
                .toList();

        return new AnalysisResult(roots, warnings);
    }
}
