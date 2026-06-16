# Spring MVC Static Analyzer MVP Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a Java 17 Gradle CLI that scans a legacy Spring MVC project and writes readable Controller -> Service -> Repository/DAO/Mapper call flows to a UTF-8 text file.

**Architecture:** Implement a small fail-soft static analysis pipeline: scan Java files, parse them with JavaParser, extract project models, classify layers, resolve simple dependency injection, build call trees from public Controller methods, and write a readable report. The MVP favors deterministic local rules and explicit `unsupported`, `unresolved`, and `circular` markers over aggressive inference.

**Tech Stack:** Java 17, Gradle, JavaParser, JavaSymbolSolver, Picocli, SLF4J + Logback, JUnit 5, AssertJ.

---

## File Structure

- Create: `settings.gradle`
  - Defines the root Gradle project name.
- Create: `build.gradle`
  - Java 17 application build, dependencies, test configuration, and main class.
- Create: `src/main/resources/logback.xml`
  - Console logging configuration for diagnostics.
- Create: `src/main/java/com/onesi/smsa/Main.java`
  - Picocli entry point.
- Create: `src/main/java/com/onesi/smsa/cli/AnalyzeCommand.java`
  - CLI command, argument validation, exit codes, output write orchestration.
- Create: `src/main/java/com/onesi/smsa/core/Analyzer.java`
  - Coordinates the full scan -> parse -> model -> tree -> report pipeline.
- Create: `src/main/java/com/onesi/smsa/core/AnalysisResult.java`
  - Holds call trees and warnings.
- Create: `src/main/java/com/onesi/smsa/core/AnalysisWarning.java`
  - Warning type, file path, and readable message.
- Create: `src/main/java/com/onesi/smsa/core/JavaFileScanner.java`
  - Recursively finds `.java` files.
- Create: `src/main/java/com/onesi/smsa/parser/SourceParser.java`
  - Parses files with JavaParser and records parse/read warnings.
- Create: `src/main/java/com/onesi/smsa/model/Layer.java`
  - Enum for `CONTROLLER`, `SERVICE`, `REPOSITORY`, `DAO`, `MAPPER`, `UNKNOWN`.
- Create: `src/main/java/com/onesi/smsa/model/ClassInfo.java`
  - Package, simple name, qualified name, annotations, fields, constructors, methods, and layer.
- Create: `src/main/java/com/onesi/smsa/model/FieldInfo.java`
  - Field name, type name, and annotations.
- Create: `src/main/java/com/onesi/smsa/model/ConstructorInfo.java`
  - Constructor parameter names and types.
- Create: `src/main/java/com/onesi/smsa/model/MethodInfo.java`
  - Method name, visibility, owner class, JavaParser method declaration, and calls.
- Create: `src/main/java/com/onesi/smsa/model/MethodRef.java`
  - Stable method identity: class name + method name.
- Create: `src/main/java/com/onesi/smsa/extract/ClassModelExtractor.java`
  - Converts parsed compilation units into project class models.
- Create: `src/main/java/com/onesi/smsa/extract/LayerClassifier.java`
  - Classifies layers from annotations and class name suffixes.
- Create: `src/main/java/com/onesi/smsa/extract/ControllerEntryPointFinder.java`
  - Finds public Controller methods.
- Create: `src/main/java/com/onesi/smsa/extract/InjectionResolver.java`
  - Resolves field and constructor injection names to project classes.
- Create: `src/main/java/com/onesi/smsa/extract/MethodCallExtractor.java`
  - Extracts JavaParser method call expressions from method bodies.
- Create: `src/main/java/com/onesi/smsa/graph/CallGraph.java`
  - Stores resolved and unresolved call edges.
- Create: `src/main/java/com/onesi/smsa/graph/CallGraphBuilder.java`
  - Builds method call edges using class models and injection maps.
- Create: `src/main/java/com/onesi/smsa/tree/CallTreeNode.java`
  - Report tree node with display text and children.
- Create: `src/main/java/com/onesi/smsa/tree/CallTreeBuilder.java`
  - Builds bounded readable trees with circular detection.
- Create: `src/main/java/com/onesi/smsa/report/TextReportWriter.java`
  - Formats call trees and warnings as UTF-8 text.
- Create: `src/test/java/com/onesi/smsa/...`
  - Unit and integration tests mirroring production package names.
- Create: `src/test/resources/fixtures/simple-spring-mvc/...`
  - Minimal Spring MVC fixture source files.
- Create: `src/test/resources/expected/simple-result.txt`
  - Golden output for the integration report.
- Create: `README.md`
  - Usage, scope, unsupported patterns, and output examples.

---

## Task 1: Gradle CLI Project Skeleton

**Files:**
- Create: `settings.gradle`
- Create: `build.gradle`
- Create: `src/main/java/com/onesi/smsa/Main.java`
- Create: `src/main/resources/logback.xml`
- Test command: `./gradlew test`

- [ ] **Step 1: Create Gradle settings**

```groovy
pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
    }
}

rootProject.name = 'spring-mvc-static-analyzer-v1'
```

- [ ] **Step 2: Create Gradle build**

```groovy
plugins {
    id 'java'
    id 'application'
}

group = 'com.onesi'
version = '0.1.0'

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

application {
    mainClass = 'com.onesi.smsa.Main'
}

dependencies {
    implementation 'com.github.javaparser:javaparser-core:3.26.3'
    implementation 'com.github.javaparser:javaparser-symbol-solver-core:3.26.3'
    implementation 'info.picocli:picocli:4.7.6'
    implementation 'org.slf4j:slf4j-api:2.0.13'
    runtimeOnly 'ch.qos.logback:logback-classic:1.5.6'

    annotationProcessor 'info.picocli:picocli-codegen:4.7.6'

    testImplementation 'org.junit.jupiter:junit-jupiter:5.10.3'
    testImplementation 'org.assertj:assertj-core:3.26.3'
}

tasks.withType(JavaCompile).configureEach {
    options.encoding = 'UTF-8'
}

tasks.withType(Test).configureEach {
    useJUnitPlatform()
}
```

- [ ] **Step 3: Create minimal entry point**

```java
package com.onesi.smsa;

import com.onesi.smsa.cli.AnalyzeCommand;
import picocli.CommandLine;

public final class Main {
    private Main() {
    }

    public static void main(String[] args) {
        int exitCode = new CommandLine(new AnalyzeCommand()).execute(args);
        System.exit(exitCode);
    }
}
```

- [ ] **Step 4: Create temporary CLI command so the project compiles**

Create `src/main/java/com/onesi/smsa/cli/AnalyzeCommand.java`.

```java
package com.onesi.smsa.cli;

import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "analyze", mixinStandardHelpOptions = true)
public class AnalyzeCommand implements Callable<Integer> {
    @Parameters(index = "0", arity = "0..1", description = "Target Spring MVC project path")
    private String targetPath;

    @Option(names = {"-o", "--output"}, defaultValue = "result.txt", description = "Output report path")
    private String outputPath;

    @Override
    public Integer call() {
        if (targetPath == null || targetPath.isBlank()) {
            return 1;
        }
        return 0;
    }
}
```

- [ ] **Step 5: Create logback config**

```xml
<configuration>
    <appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{HH:mm:ss.SSS} %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>

    <root level="INFO">
        <appender-ref ref="STDOUT"/>
    </root>
</configuration>
```

- [ ] **Step 6: Run build verification**

Run: `./gradlew test`

Expected: Gradle downloads dependencies, compiles `Main` and `AnalyzeCommand`, and reports `BUILD SUCCESSFUL`.

- [ ] **Step 7: Commit**

```bash
git add settings.gradle build.gradle src/main/java src/main/resources
git commit -m "chore: create java cli skeleton"
```

---

## Task 2: Core Result and Warning Types

**Files:**
- Create: `src/main/java/com/onesi/smsa/core/AnalysisWarning.java`
- Create: `src/main/java/com/onesi/smsa/core/AnalysisResult.java`
- Create: `src/test/java/com/onesi/smsa/core/AnalysisWarningTest.java`

- [ ] **Step 1: Write failing warning test**

```java
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
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew test --tests com.onesi.smsa.core.AnalysisWarningTest`

Expected: FAIL because `AnalysisWarning` does not exist.

- [ ] **Step 3: Implement warning and result records**

```java
package com.onesi.smsa.core;

import java.nio.file.Path;

public record AnalysisWarning(String code, Path path, String message) {
    public String format() {
        return "[" + code + "] " + path.toString().replace('\\', '/') + System.lineSeparator()
                + "  " + message;
    }
}
```

```java
package com.onesi.smsa.core;

import com.onesi.smsa.tree.CallTreeNode;
import java.util.List;

public record AnalysisResult(List<CallTreeNode> roots, List<AnalysisWarning> warnings) {
    public AnalysisResult {
        roots = List.copyOf(roots);
        warnings = List.copyOf(warnings);
    }
}
```

- [ ] **Step 4: Add temporary tree node record**

Create `src/main/java/com/onesi/smsa/tree/CallTreeNode.java`.

```java
package com.onesi.smsa.tree;

import java.util.List;

public record CallTreeNode(String text, List<CallTreeNode> children) {
    public CallTreeNode {
        children = List.copyOf(children);
    }

    public static CallTreeNode leaf(String text) {
        return new CallTreeNode(text, List.of());
    }
}
```

- [ ] **Step 5: Run test to verify it passes**

Run: `./gradlew test --tests com.onesi.smsa.core.AnalysisWarningTest`

Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/onesi/smsa/core src/main/java/com/onesi/smsa/tree src/test/java/com/onesi/smsa/core
git commit -m "feat: add analysis result types"
```

---

## Task 3: Java File Scanner

**Files:**
- Create: `src/main/java/com/onesi/smsa/core/JavaFileScanner.java`
- Create: `src/test/java/com/onesi/smsa/core/JavaFileScannerTest.java`

- [ ] **Step 1: Write failing scanner tests**

```java
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
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew test --tests com.onesi.smsa.core.JavaFileScannerTest`

Expected: FAIL because `JavaFileScanner` does not exist.

- [ ] **Step 3: Implement scanner**

```java
package com.onesi.smsa.core;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

public class JavaFileScanner {
    public List<Path> scan(Path root) throws IOException {
        try (Stream<Path> paths = Files.walk(root)) {
            return paths
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".java"))
                    .sorted(Comparator.comparing(path -> path.toString().replace('\\', '/')))
                    .toList();
        }
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew test --tests com.onesi.smsa.core.JavaFileScannerTest`

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/onesi/smsa/core/JavaFileScanner.java src/test/java/com/onesi/smsa/core/JavaFileScannerTest.java
git commit -m "feat: scan java source files"
```

---

## Task 4: Project Model and Layer Classification

**Files:**
- Create: `src/main/java/com/onesi/smsa/model/Layer.java`
- Create: `src/main/java/com/onesi/smsa/model/ClassInfo.java`
- Create: `src/main/java/com/onesi/smsa/model/FieldInfo.java`
- Create: `src/main/java/com/onesi/smsa/model/ConstructorInfo.java`
- Create: `src/main/java/com/onesi/smsa/model/MethodInfo.java`
- Create: `src/main/java/com/onesi/smsa/model/MethodRef.java`
- Create: `src/main/java/com/onesi/smsa/extract/LayerClassifier.java`
- Create: `src/test/java/com/onesi/smsa/extract/LayerClassifierTest.java`

- [ ] **Step 1: Write failing layer classifier tests**

```java
package com.onesi.smsa.extract;

import static org.assertj.core.api.Assertions.assertThat;

import com.onesi.smsa.model.Layer;
import java.util.Set;
import org.junit.jupiter.api.Test;

class LayerClassifierTest {
    private final LayerClassifier classifier = new LayerClassifier();

    @Test
    void classifiesSpringAnnotations() {
        assertThat(classifier.classify("UserController", Set.of("Controller"))).isEqualTo(Layer.CONTROLLER);
        assertThat(classifier.classify("UserApi", Set.of("RestController"))).isEqualTo(Layer.CONTROLLER);
        assertThat(classifier.classify("UserService", Set.of("Service"))).isEqualTo(Layer.SERVICE);
        assertThat(classifier.classify("UserRepository", Set.of("Repository"))).isEqualTo(Layer.REPOSITORY);
    }

    @Test
    void classifiesLegacySuffixes() {
        assertThat(classifier.classify("UserRepository", Set.of())).isEqualTo(Layer.REPOSITORY);
        assertThat(classifier.classify("UserDao", Set.of())).isEqualTo(Layer.DAO);
        assertThat(classifier.classify("UserDAO", Set.of())).isEqualTo(Layer.DAO);
        assertThat(classifier.classify("UserMapper", Set.of())).isEqualTo(Layer.MAPPER);
    }

    @Test
    void unknownWhenNoKnownAnnotationOrSuffixExists() {
        assertThat(classifier.classify("UserDto", Set.of())).isEqualTo(Layer.UNKNOWN);
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew test --tests com.onesi.smsa.extract.LayerClassifierTest`

Expected: FAIL because model and classifier types do not exist.

- [ ] **Step 3: Implement model types**

```java
package com.onesi.smsa.model;

public enum Layer {
    CONTROLLER,
    SERVICE,
    REPOSITORY,
    DAO,
    MAPPER,
    UNKNOWN
}
```

```java
package com.onesi.smsa.model;

public record MethodRef(String className, String methodName) {
    public String displayName() {
        return className + "." + methodName + "()";
    }
}
```

```java
package com.onesi.smsa.model;

import java.util.Set;

public record FieldInfo(String name, String typeName, Set<String> annotations) {
    public FieldInfo {
        annotations = Set.copyOf(annotations);
    }
}
```

```java
package com.onesi.smsa.model;

public record ConstructorInfo(String parameterName, String parameterType) {
}
```

```java
package com.onesi.smsa.model;

import com.github.javaparser.ast.body.MethodDeclaration;
import java.util.List;

public record MethodInfo(
        MethodRef ref,
        boolean publicMethod,
        MethodDeclaration declaration,
        List<String> rawCalls) {
    public MethodInfo {
        rawCalls = List.copyOf(rawCalls);
    }
}
```

```java
package com.onesi.smsa.model;

import java.util.List;
import java.util.Set;

public record ClassInfo(
        String packageName,
        String simpleName,
        String qualifiedName,
        Set<String> annotations,
        List<FieldInfo> fields,
        List<ConstructorInfo> constructors,
        List<MethodInfo> methods,
        Layer layer) {
    public ClassInfo {
        annotations = Set.copyOf(annotations);
        fields = List.copyOf(fields);
        constructors = List.copyOf(constructors);
        methods = List.copyOf(methods);
    }
}
```

- [ ] **Step 4: Implement classifier**

```java
package com.onesi.smsa.extract;

import com.onesi.smsa.model.Layer;
import java.util.Set;

public class LayerClassifier {
    public Layer classify(String simpleName, Set<String> annotations) {
        if (annotations.contains("Controller") || annotations.contains("RestController")) {
            return Layer.CONTROLLER;
        }
        if (annotations.contains("Service")) {
            return Layer.SERVICE;
        }
        if (annotations.contains("Repository")) {
            return Layer.REPOSITORY;
        }
        if (simpleName.endsWith("Repository")) {
            return Layer.REPOSITORY;
        }
        if (simpleName.endsWith("DAO") || simpleName.endsWith("Dao")) {
            return Layer.DAO;
        }
        if (simpleName.endsWith("Mapper")) {
            return Layer.MAPPER;
        }
        return Layer.UNKNOWN;
    }
}
```

- [ ] **Step 5: Run test to verify it passes**

Run: `./gradlew test --tests com.onesi.smsa.extract.LayerClassifierTest`

Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/onesi/smsa/model src/main/java/com/onesi/smsa/extract/LayerClassifier.java src/test/java/com/onesi/smsa/extract/LayerClassifierTest.java
git commit -m "feat: classify spring mvc layers"
```

---

## Task 5: Parsing and Class Model Extraction

**Files:**
- Create: `src/main/java/com/onesi/smsa/parser/ParsedSource.java`
- Create: `src/main/java/com/onesi/smsa/parser/SourceParser.java`
- Create: `src/main/java/com/onesi/smsa/extract/ClassModelExtractor.java`
- Create: `src/test/java/com/onesi/smsa/parser/SourceParserTest.java`
- Create: `src/test/java/com/onesi/smsa/extract/ClassModelExtractorTest.java`

- [ ] **Step 1: Write failing parser tests**

```java
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
```

- [ ] **Step 2: Write failing extractor test**

```java
package com.onesi.smsa.extract;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.javaparser.StaticJavaParser;
import com.onesi.smsa.model.ClassInfo;
import com.onesi.smsa.model.Layer;
import com.onesi.smsa.parser.ParsedSource;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class ClassModelExtractorTest {
    @Test
    void extractsClassAnnotationsFieldsConstructorsAndPublicMethods() {
        String source = """
                package demo;

                import org.springframework.stereotype.Controller;
                import org.springframework.beans.factory.annotation.Autowired;

                @Controller
                public class UserController {
                    @Autowired
                    private UserService userService;

                    public UserController(HistoryService historyService) {
                    }

                    public void createUser() {
                        userService.createUser();
                    }

                    private void helper() {
                    }
                }
                """;
        ParsedSource parsed = new ParsedSource(Path.of("UserController.java"), StaticJavaParser.parse(source));

        List<ClassInfo> classes = new ClassModelExtractor(new LayerClassifier()).extract(List.of(parsed));

        assertThat(classes).hasSize(1);
        ClassInfo controller = classes.get(0);
        assertThat(controller.packageName()).isEqualTo("demo");
        assertThat(controller.simpleName()).isEqualTo("UserController");
        assertThat(controller.qualifiedName()).isEqualTo("demo.UserController");
        assertThat(controller.layer()).isEqualTo(Layer.CONTROLLER);
        assertThat(controller.fields()).extracting("name").containsExactly("userService");
        assertThat(controller.constructors()).extracting("parameterType").containsExactly("HistoryService");
        assertThat(controller.methods()).extracting(method -> method.ref().methodName()).contains("createUser", "helper");
    }
}
```

- [ ] **Step 3: Run tests to verify they fail**

Run: `./gradlew test --tests com.onesi.smsa.parser.SourceParserTest --tests com.onesi.smsa.extract.ClassModelExtractorTest`

Expected: FAIL because parser and extractor types do not exist.

- [ ] **Step 4: Implement parser**

```java
package com.onesi.smsa.parser;

import com.github.javaparser.ast.CompilationUnit;
import java.nio.file.Path;

public record ParsedSource(Path path, CompilationUnit compilationUnit) {
}
```

```java
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
```

- [ ] **Step 5: Implement class model extractor**

```java
package com.onesi.smsa.extract;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.onesi.smsa.model.ClassInfo;
import com.onesi.smsa.model.ConstructorInfo;
import com.onesi.smsa.model.FieldInfo;
import com.onesi.smsa.model.Layer;
import com.onesi.smsa.model.MethodInfo;
import com.onesi.smsa.model.MethodRef;
import com.onesi.smsa.parser.ParsedSource;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class ClassModelExtractor {
    private final LayerClassifier layerClassifier;

    public ClassModelExtractor(LayerClassifier layerClassifier) {
        this.layerClassifier = layerClassifier;
    }

    public List<ClassInfo> extract(List<ParsedSource> sources) {
        List<ClassInfo> result = new ArrayList<>();
        for (ParsedSource source : sources) {
            String packageName = source.compilationUnit().getPackageDeclaration()
                    .map(packageDeclaration -> packageDeclaration.getName().asString())
                    .orElse("");
            for (ClassOrInterfaceDeclaration declaration : source.compilationUnit().findAll(ClassOrInterfaceDeclaration.class)) {
                String simpleName = declaration.getNameAsString();
                String qualifiedName = packageName.isBlank() ? simpleName : packageName + "." + simpleName;
                Set<String> annotations = annotationNames(declaration);
                Layer layer = layerClassifier.classify(simpleName, annotations);
                result.add(new ClassInfo(
                        packageName,
                        simpleName,
                        qualifiedName,
                        annotations,
                        fields(declaration),
                        constructors(declaration),
                        methods(simpleName, declaration),
                        layer));
            }
        }
        return result;
    }

    private Set<String> annotationNames(ClassOrInterfaceDeclaration declaration) {
        return declaration.getAnnotations().stream()
                .map(annotation -> annotation.getName().getIdentifier())
                .collect(Collectors.toSet());
    }

    private List<FieldInfo> fields(ClassOrInterfaceDeclaration declaration) {
        List<FieldInfo> fields = new ArrayList<>();
        for (FieldDeclaration field : declaration.getFields()) {
            Set<String> annotations = field.getAnnotations().stream()
                    .map(annotation -> annotation.getName().getIdentifier())
                    .collect(Collectors.toSet());
            field.getVariables().forEach(variable -> fields.add(new FieldInfo(
                    variable.getNameAsString(),
                    variable.getType().asString(),
                    annotations)));
        }
        return fields;
    }

    private List<ConstructorInfo> constructors(ClassOrInterfaceDeclaration declaration) {
        List<ConstructorInfo> constructors = new ArrayList<>();
        for (ConstructorDeclaration constructor : declaration.getConstructors()) {
            constructor.getParameters().forEach(parameter -> constructors.add(new ConstructorInfo(
                    parameter.getNameAsString(),
                    parameter.getType().asString())));
        }
        return constructors;
    }

    private List<MethodInfo> methods(String simpleName, ClassOrInterfaceDeclaration declaration) {
        List<MethodInfo> methods = new ArrayList<>();
        for (MethodDeclaration method : declaration.getMethods()) {
            methods.add(new MethodInfo(
                    new MethodRef(simpleName, method.getNameAsString()),
                    method.isPublic(),
                    method,
                    List.of()));
        }
        return methods;
    }
}
```

- [ ] **Step 6: Run tests and fix compile issues**

Run: `./gradlew test --tests com.onesi.smsa.parser.SourceParserTest --tests com.onesi.smsa.extract.ClassModelExtractorTest`

Expected: PASS. If `SourceParser` fails to compile, replace the parse block with `StaticJavaParser.parse(file)` inside the `try` block and keep the same warning behavior for parse exceptions.

- [ ] **Step 7: Commit**

```bash
git add src/main/java/com/onesi/smsa/parser src/main/java/com/onesi/smsa/extract/ClassModelExtractor.java src/test/java/com/onesi/smsa/parser src/test/java/com/onesi/smsa/extract/ClassModelExtractorTest.java
git commit -m "feat: parse sources and extract class models"
```

---

## Task 6: Entry Point and Dependency Resolution

**Files:**
- Create: `src/main/java/com/onesi/smsa/extract/ControllerEntryPointFinder.java`
- Create: `src/main/java/com/onesi/smsa/extract/InjectionResolver.java`
- Create: `src/test/java/com/onesi/smsa/extract/ControllerEntryPointFinderTest.java`
- Create: `src/test/java/com/onesi/smsa/extract/InjectionResolverTest.java`

- [ ] **Step 1: Write failing entry point test**

```java
package com.onesi.smsa.extract;

import static org.assertj.core.api.Assertions.assertThat;

import com.onesi.smsa.model.ClassInfo;
import com.onesi.smsa.model.Layer;
import com.onesi.smsa.model.MethodInfo;
import com.onesi.smsa.model.MethodRef;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ControllerEntryPointFinderTest {
    @Test
    void findsOnlyPublicControllerMethods() {
        MethodInfo publicMethod = new MethodInfo(new MethodRef("UserController", "createUser"), true, null, List.of());
        MethodInfo privateMethod = new MethodInfo(new MethodRef("UserController", "helper"), false, null, List.of());
        ClassInfo controller = new ClassInfo("", "UserController", "UserController", Set.of("Controller"),
                List.of(), List.of(), List.of(publicMethod, privateMethod), Layer.CONTROLLER);
        ClassInfo service = new ClassInfo("", "UserService", "UserService", Set.of("Service"),
                List.of(), List.of(), List.of(publicMethod), Layer.SERVICE);

        List<MethodRef> entryPoints = new ControllerEntryPointFinder().find(List.of(controller, service));

        assertThat(entryPoints).containsExactly(new MethodRef("UserController", "createUser"));
    }
}
```

- [ ] **Step 2: Write failing injection resolver test**

```java
package com.onesi.smsa.extract;

import static org.assertj.core.api.Assertions.assertThat;

import com.onesi.smsa.model.ClassInfo;
import com.onesi.smsa.model.ConstructorInfo;
import com.onesi.smsa.model.FieldInfo;
import com.onesi.smsa.model.Layer;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class InjectionResolverTest {
    @Test
    void resolvesAutowiredFieldsAndConstructorParametersByType() {
        ClassInfo controller = new ClassInfo("", "UserController", "UserController", Set.of("Controller"),
                List.of(new FieldInfo("userService", "UserService", Set.of("Autowired"))),
                List.of(new ConstructorInfo("historyService", "HistoryService")),
                List.of(),
                Layer.CONTROLLER);
        ClassInfo userService = new ClassInfo("", "UserService", "UserService", Set.of("Service"),
                List.of(), List.of(), List.of(), Layer.SERVICE);
        ClassInfo historyService = new ClassInfo("", "HistoryService", "HistoryService", Set.of("Service"),
                List.of(), List.of(), List.of(), Layer.SERVICE);

        Map<String, Map<String, String>> resolved = new InjectionResolver().resolve(List.of(controller, userService, historyService));

        assertThat(resolved.get("UserController"))
                .containsEntry("userService", "UserService")
                .containsEntry("historyService", "HistoryService");
    }
}
```

- [ ] **Step 3: Run tests to verify they fail**

Run: `./gradlew test --tests com.onesi.smsa.extract.ControllerEntryPointFinderTest --tests com.onesi.smsa.extract.InjectionResolverTest`

Expected: FAIL because the new extractor classes do not exist.

- [ ] **Step 4: Implement entry point finder**

```java
package com.onesi.smsa.extract;

import com.onesi.smsa.model.ClassInfo;
import com.onesi.smsa.model.Layer;
import com.onesi.smsa.model.MethodRef;
import java.util.List;

public class ControllerEntryPointFinder {
    public List<MethodRef> find(List<ClassInfo> classes) {
        return classes.stream()
                .filter(classInfo -> classInfo.layer() == Layer.CONTROLLER)
                .flatMap(classInfo -> classInfo.methods().stream())
                .filter(method -> method.publicMethod())
                .map(method -> method.ref())
                .toList();
    }
}
```

- [ ] **Step 5: Implement injection resolver**

```java
package com.onesi.smsa.extract;

import com.onesi.smsa.model.ClassInfo;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class InjectionResolver {
    public Map<String, Map<String, String>> resolve(List<ClassInfo> classes) {
        Map<String, ClassInfo> bySimpleName = classes.stream()
                .collect(Collectors.toMap(ClassInfo::simpleName, Function.identity(), (left, right) -> left));
        Map<String, Map<String, String>> result = new HashMap<>();

        for (ClassInfo classInfo : classes) {
            Map<String, String> dependencies = new HashMap<>();
            classInfo.fields().stream()
                    .filter(field -> field.annotations().contains("Autowired"))
                    .filter(field -> bySimpleName.containsKey(field.typeName()))
                    .forEach(field -> dependencies.put(field.name(), field.typeName()));
            classInfo.constructors().stream()
                    .filter(parameter -> bySimpleName.containsKey(parameter.parameterType()))
                    .forEach(parameter -> dependencies.put(parameter.parameterName(), parameter.parameterType()));
            result.put(classInfo.simpleName(), dependencies);
        }

        return result;
    }
}
```

- [ ] **Step 6: Run tests to verify they pass**

Run: `./gradlew test --tests com.onesi.smsa.extract.ControllerEntryPointFinderTest --tests com.onesi.smsa.extract.InjectionResolverTest`

Expected: PASS.

- [ ] **Step 7: Commit**

```bash
git add src/main/java/com/onesi/smsa/extract/ControllerEntryPointFinder.java src/main/java/com/onesi/smsa/extract/InjectionResolver.java src/test/java/com/onesi/smsa/extract/ControllerEntryPointFinderTest.java src/test/java/com/onesi/smsa/extract/InjectionResolverTest.java
git commit -m "feat: resolve controller entry points and injections"
```

---

## Task 7: Method Calls, Call Graph, and Tree Building

**Files:**
- Create: `src/main/java/com/onesi/smsa/extract/ExtractedCall.java`
- Create: `src/main/java/com/onesi/smsa/extract/MethodCallExtractor.java`
- Create: `src/main/java/com/onesi/smsa/graph/CallEdge.java`
- Create: `src/main/java/com/onesi/smsa/graph/CallGraph.java`
- Create: `src/main/java/com/onesi/smsa/graph/CallGraphBuilder.java`
- Modify: `src/main/java/com/onesi/smsa/tree/CallTreeNode.java`
- Create: `src/main/java/com/onesi/smsa/tree/CallTreeBuilder.java`
- Create: tests under `src/test/java/com/onesi/smsa/extract`, `graph`, and `tree`

- [ ] **Step 1: Write failing method call extractor test**

```java
package com.onesi.smsa.extract;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.body.MethodDeclaration;
import org.junit.jupiter.api.Test;

class MethodCallExtractorTest {
    @Test
    void extractsScopedAndUnscopedCallsInEncounterOrder() {
        MethodDeclaration method = StaticJavaParser.parseMethodDeclaration("""
                public void createUser() {
                    validate();
                    userService.createUser();
                    this.audit();
                }
                """);

        assertThat(new MethodCallExtractor().extract(method))
                .extracting(ExtractedCall::displayText)
                .containsExactly("validate()", "userService.createUser()", "this.audit()");
    }
}
```

- [ ] **Step 2: Write failing tree test**

```java
package com.onesi.smsa.tree;

import static org.assertj.core.api.Assertions.assertThat;

import com.onesi.smsa.graph.CallEdge;
import com.onesi.smsa.graph.CallGraph;
import com.onesi.smsa.model.MethodRef;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class CallTreeBuilderTest {
    @Test
    void stopsCircularCalls() {
        MethodRef start = new MethodRef("UserService", "a");
        MethodRef next = new MethodRef("UserService", "b");
        CallGraph graph = new CallGraph(Map.of(
                start, List.of(CallEdge.resolved(next)),
                next, List.of(CallEdge.resolved(start))));

        CallTreeNode root = new CallTreeBuilder(20).build(start, graph);

        assertThat(root.children().get(0).children().get(0).text())
                .isEqualTo("circular: UserService.a()");
    }
}
```

- [ ] **Step 3: Run tests to verify they fail**

Run: `./gradlew test --tests com.onesi.smsa.extract.MethodCallExtractorTest --tests com.onesi.smsa.tree.CallTreeBuilderTest`

Expected: FAIL because call extraction and graph types do not exist.

- [ ] **Step 4: Implement extracted call and call extractor**

```java
package com.onesi.smsa.extract;

public record ExtractedCall(String scope, String methodName) {
    public String displayText() {
        return scope == null || scope.isBlank() ? methodName + "()" : scope + "." + methodName + "()";
    }
}
```

```java
package com.onesi.smsa.extract;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import java.util.List;

public class MethodCallExtractor {
    public List<ExtractedCall> extract(MethodDeclaration declaration) {
        return declaration.findAll(MethodCallExpr.class).stream()
                .map(call -> new ExtractedCall(
                        call.getScope().map(Object::toString).orElse(null),
                        call.getNameAsString()))
                .toList();
    }
}
```

- [ ] **Step 5: Implement graph types**

```java
package com.onesi.smsa.graph;

import com.onesi.smsa.model.MethodRef;

public record CallEdge(MethodRef target, String markerText) {
    public static CallEdge resolved(MethodRef target) {
        return new CallEdge(target, null);
    }

    public static CallEdge marker(String markerText) {
        return new CallEdge(null, markerText);
    }

    public boolean resolved() {
        return target != null;
    }
}
```

```java
package com.onesi.smsa.graph;

import com.onesi.smsa.model.MethodRef;
import java.util.List;
import java.util.Map;

public record CallGraph(Map<MethodRef, List<CallEdge>> edges) {
    public List<CallEdge> outgoing(MethodRef methodRef) {
        return edges.getOrDefault(methodRef, List.of());
    }
}
```

- [ ] **Step 6: Implement tree builder**

```java
package com.onesi.smsa.tree;

import java.util.List;

public record CallTreeNode(String text, List<CallTreeNode> children) {
    public CallTreeNode {
        children = List.copyOf(children);
    }

    public static CallTreeNode leaf(String text) {
        return new CallTreeNode(text, List.of());
    }
}
```

```java
package com.onesi.smsa.tree;

import com.onesi.smsa.graph.CallEdge;
import com.onesi.smsa.graph.CallGraph;
import com.onesi.smsa.model.MethodRef;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class CallTreeBuilder {
    private final int maxDepth;

    public CallTreeBuilder(int maxDepth) {
        this.maxDepth = maxDepth;
    }

    public CallTreeNode build(MethodRef root, CallGraph graph) {
        return build(root, graph, new LinkedHashSet<>(), 0);
    }

    private CallTreeNode build(MethodRef current, CallGraph graph, Set<MethodRef> path, int depth) {
        if (depth > maxDepth) {
            return CallTreeNode.leaf("unsupported: max call depth exceeded");
        }
        if (path.contains(current)) {
            return CallTreeNode.leaf("circular: " + current.displayName());
        }

        Set<MethodRef> nextPath = new LinkedHashSet<>(path);
        nextPath.add(current);
        List<CallTreeNode> children = new ArrayList<>();
        for (CallEdge edge : graph.outgoing(current)) {
            if (edge.resolved()) {
                children.add(build(edge.target(), graph, nextPath, depth + 1));
            } else {
                children.add(CallTreeNode.leaf(edge.markerText()));
            }
        }
        return new CallTreeNode(current.displayName(), children);
    }
}
```

- [ ] **Step 7: Implement call graph builder**

Create a first pass that supports same-class calls, `this.method()`, injected field calls, unresolved project-looking calls, and unsupported external-looking calls.

```java
package com.onesi.smsa.graph;

import com.onesi.smsa.extract.ExtractedCall;
import com.onesi.smsa.extract.MethodCallExtractor;
import com.onesi.smsa.model.ClassInfo;
import com.onesi.smsa.model.MethodInfo;
import com.onesi.smsa.model.MethodRef;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class CallGraphBuilder {
    private final MethodCallExtractor methodCallExtractor;

    public CallGraphBuilder(MethodCallExtractor methodCallExtractor) {
        this.methodCallExtractor = methodCallExtractor;
    }

    public CallGraph build(List<ClassInfo> classes, Map<String, Map<String, String>> injections) {
        Map<String, ClassInfo> bySimpleName = classes.stream()
                .collect(Collectors.toMap(ClassInfo::simpleName, classInfo -> classInfo, (left, right) -> left));
        Map<MethodRef, List<CallEdge>> edges = new HashMap<>();

        for (ClassInfo owner : classes) {
            Set<String> ownerMethodNames = owner.methods().stream()
                    .map(method -> method.ref().methodName())
                    .collect(Collectors.toSet());
            for (MethodInfo method : owner.methods()) {
                if (method.declaration() == null) {
                    edges.put(method.ref(), List.of());
                    continue;
                }
                List<CallEdge> outgoing = new ArrayList<>();
                for (ExtractedCall call : methodCallExtractor.extract(method.declaration())) {
                    outgoing.add(resolveCall(owner, ownerMethodNames, injections, bySimpleName, call));
                }
                edges.put(method.ref(), outgoing);
            }
        }
        return new CallGraph(edges);
    }

    private CallEdge resolveCall(
            ClassInfo owner,
            Set<String> ownerMethodNames,
            Map<String, Map<String, String>> injections,
            Map<String, ClassInfo> bySimpleName,
            ExtractedCall call) {
        String scope = call.scope();
        if (scope == null || scope.isBlank() || scope.equals("this")) {
            if (ownerMethodNames.contains(call.methodName())) {
                return CallEdge.resolved(new MethodRef(owner.simpleName(), call.methodName()));
            }
            return CallEdge.marker("unresolved: " + call.displayText());
        }

        String targetClass = injections.getOrDefault(owner.simpleName(), Map.of()).get(scope);
        if (targetClass != null && bySimpleName.containsKey(targetClass)) {
            boolean methodExists = bySimpleName.get(targetClass).methods().stream()
                    .anyMatch(method -> method.ref().methodName().equals(call.methodName()));
            if (methodExists) {
                return CallEdge.resolved(new MethodRef(targetClass, call.methodName()));
            }
            return CallEdge.marker("unresolved: " + call.displayText());
        }

        return CallEdge.marker("unsupported: " + call.displayText());
    }
}
```

- [ ] **Step 8: Run tests to verify they pass**

Run: `./gradlew test --tests com.onesi.smsa.extract.MethodCallExtractorTest --tests com.onesi.smsa.tree.CallTreeBuilderTest`

Expected: PASS.

- [ ] **Step 9: Commit**

```bash
git add src/main/java/com/onesi/smsa/extract/ExtractedCall.java src/main/java/com/onesi/smsa/extract/MethodCallExtractor.java src/main/java/com/onesi/smsa/graph src/main/java/com/onesi/smsa/tree src/test/java/com/onesi/smsa/extract/MethodCallExtractorTest.java src/test/java/com/onesi/smsa/tree/CallTreeBuilderTest.java
git commit -m "feat: build method call trees"
```

---

## Task 8: Text Report Writer

**Files:**
- Create: `src/main/java/com/onesi/smsa/report/TextReportWriter.java`
- Create: `src/test/java/com/onesi/smsa/report/TextReportWriterTest.java`

- [ ] **Step 1: Write failing report writer test**

```java
package com.onesi.smsa.report;

import static org.assertj.core.api.Assertions.assertThat;

import com.onesi.smsa.core.AnalysisResult;
import com.onesi.smsa.core.AnalysisWarning;
import com.onesi.smsa.tree.CallTreeNode;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class TextReportWriterTest {
    @Test
    void writesTreeAndWarnings() {
        CallTreeNode root = new CallTreeNode("UserController.createUser()", List.of(
                new CallTreeNode("UserService.createUser()", List.of(
                        CallTreeNode.leaf("UserRepository.save()")))));
        AnalysisResult result = new AnalysisResult(List.of(root), List.of(
                new AnalysisWarning("parse-error", Path.of("Broken.java"), "Could not parse Java source.")));

        String report = new TextReportWriter().write(result);

        assertThat(report).contains("UserController.createUser()");
        assertThat(report).contains("└─ UserService.createUser()");
        assertThat(report).contains("   └─ UserRepository.save()");
        assertThat(report).contains("Warnings");
        assertThat(report).contains("[parse-error] Broken.java");
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew test --tests com.onesi.smsa.report.TextReportWriterTest`

Expected: FAIL because `TextReportWriter` does not exist.

- [ ] **Step 3: Implement report writer**

```java
package com.onesi.smsa.report;

import com.onesi.smsa.core.AnalysisResult;
import com.onesi.smsa.tree.CallTreeNode;

public class TextReportWriter {
    private static final String SEPARATOR = "==================================================";

    public String write(AnalysisResult result) {
        StringBuilder builder = new StringBuilder();
        for (CallTreeNode root : result.roots()) {
            builder.append(SEPARATOR).append(System.lineSeparator());
            builder.append(root.text()).append(System.lineSeparator());
            builder.append(SEPARATOR).append(System.lineSeparator()).append(System.lineSeparator());
            appendTree(builder, root, "");
            builder.append(System.lineSeparator());
        }

        if (!result.warnings().isEmpty()) {
            builder.append(SEPARATOR).append(System.lineSeparator());
            builder.append("Warnings").append(System.lineSeparator());
            builder.append(SEPARATOR).append(System.lineSeparator()).append(System.lineSeparator());
            result.warnings().forEach(warning -> builder.append(warning.format()).append(System.lineSeparator()).append(System.lineSeparator()));
        }
        return builder.toString();
    }

    private void appendTree(StringBuilder builder, CallTreeNode node, String prefix) {
        builder.append(prefix).append(node.text()).append(System.lineSeparator());
        for (int i = 0; i < node.children().size(); i++) {
            CallTreeNode child = node.children().get(i);
            boolean last = i == node.children().size() - 1;
            builder.append(prefix).append(last ? "└─ " : "├─ ").append(child.text()).append(System.lineSeparator());
            appendChildren(builder, child, prefix + (last ? "   " : "│  "));
        }
    }

    private void appendChildren(StringBuilder builder, CallTreeNode node, String prefix) {
        for (int i = 0; i < node.children().size(); i++) {
            CallTreeNode child = node.children().get(i);
            boolean last = i == node.children().size() - 1;
            builder.append(prefix).append(last ? "└─ " : "├─ ").append(child.text()).append(System.lineSeparator());
            appendChildren(builder, child, prefix + (last ? "   " : "│  "));
        }
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew test --tests com.onesi.smsa.report.TextReportWriterTest`

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/onesi/smsa/report src/test/java/com/onesi/smsa/report
git commit -m "feat: write text analysis report"
```

---

## Task 9: Analyzer Pipeline and CLI Behavior

**Files:**
- Create: `src/main/java/com/onesi/smsa/core/Analyzer.java`
- Modify: `src/main/java/com/onesi/smsa/cli/AnalyzeCommand.java`
- Create: `src/test/java/com/onesi/smsa/cli/AnalyzeCommandTest.java`

- [ ] **Step 1: Write failing CLI validation test**

```java
package com.onesi.smsa.cli;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

class AnalyzeCommandTest {
    @TempDir
    Path tempDir;

    @Test
    void returnsOneForMissingInputPath() {
        int exitCode = new CommandLine(new AnalyzeCommand()).execute(tempDir.resolve("missing").toString());

        assertThat(exitCode).isEqualTo(1);
    }

    @Test
    void returnsOneWhenNoJavaFilesExist() {
        int exitCode = new CommandLine(new AnalyzeCommand()).execute(tempDir.toString(), "-o", tempDir.resolve("result.txt").toString());

        assertThat(exitCode).isEqualTo(1);
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew test --tests com.onesi.smsa.cli.AnalyzeCommandTest`

Expected: At least one assertion fails because the temporary command does not validate directories and Java files.

- [ ] **Step 3: Implement analyzer pipeline**

```java
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
            return new AnalysisResult(List.of(), List.of(new AnalysisWarning("input-error", targetPath, "No Java files found.")));
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
```

- [ ] **Step 4: Implement CLI command**

```java
package com.onesi.smsa.cli;

import com.onesi.smsa.core.AnalysisResult;
import com.onesi.smsa.core.Analyzer;
import com.onesi.smsa.report.TextReportWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "analyze", mixinStandardHelpOptions = true, description = "Analyze Spring MVC call flows.")
public class AnalyzeCommand implements Callable<Integer> {
    @Parameters(index = "0", description = "Target Spring MVC project path")
    private Path targetPath;

    @Option(names = {"-o", "--output"}, defaultValue = "result.txt", description = "Output report path")
    private Path outputPath;

    @Override
    public Integer call() {
        try {
            if (!Files.exists(targetPath) || !Files.isDirectory(targetPath)) {
                System.err.println("ERROR: Input path must be an existing directory: " + targetPath);
                return 1;
            }

            AnalysisResult result = new Analyzer().analyze(targetPath);
            boolean inputError = result.warnings().stream().anyMatch(warning -> warning.code().equals("input-error"));
            if (inputError) {
                result.warnings().forEach(warning -> System.err.println("ERROR: " + warning.message()));
                return 1;
            }

            String report = new TextReportWriter().write(result);
            Files.writeString(outputPath, report, StandardCharsets.UTF_8);
            return 0;
        } catch (Exception ex) {
            System.err.println("ERROR: Analysis failed: " + ex.getMessage());
            return 2;
        }
    }
}
```

- [ ] **Step 5: Run CLI tests to verify they pass**

Run: `./gradlew test --tests com.onesi.smsa.cli.AnalyzeCommandTest`

Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/onesi/smsa/core/Analyzer.java src/main/java/com/onesi/smsa/cli/AnalyzeCommand.java src/test/java/com/onesi/smsa/cli/AnalyzeCommandTest.java
git commit -m "feat: connect analyzer pipeline to cli"
```

---

## Task 10: Fixture Integration and Golden File Test

**Files:**
- Create: `src/test/resources/fixtures/simple-spring-mvc/src/main/java/demo/UserController.java`
- Create: `src/test/resources/fixtures/simple-spring-mvc/src/main/java/demo/UserService.java`
- Create: `src/test/resources/fixtures/simple-spring-mvc/src/main/java/demo/UserRepository.java`
- Create: `src/test/resources/fixtures/simple-spring-mvc/src/main/java/demo/HistoryService.java`
- Create: `src/test/resources/fixtures/simple-spring-mvc/src/main/java/demo/HistoryRepository.java`
- Create: `src/test/resources/expected/simple-result.txt`
- Create: `src/test/java/com/onesi/smsa/core/AnalyzerIntegrationTest.java`

- [ ] **Step 1: Add fixture sources**

```java
package demo;

import org.springframework.stereotype.Controller;

@Controller
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    public void createUser() {
        userService.validate();
        userService.createUser();
    }

    private void helper() {
    }
}
```

```java
package demo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserService {
    @Autowired
    private UserRepository userRepository;

    private final HistoryService historyService;

    public UserService(HistoryService historyService) {
        this.historyService = historyService;
    }

    public void validate() {
        userRepository.findByEmail();
    }

    public void createUser() {
        userRepository.save();
        historyService.saveHistory();
        externalClient.send();
    }
}
```

```java
package demo;

import org.springframework.stereotype.Repository;

@Repository
public class UserRepository {
    public void findByEmail() {
    }

    public void save() {
    }
}
```

```java
package demo;

import org.springframework.stereotype.Service;

@Service
public class HistoryService {
    private final HistoryRepository historyRepository;

    public HistoryService(HistoryRepository historyRepository) {
        this.historyRepository = historyRepository;
    }

    public void saveHistory() {
        historyRepository.save();
    }
}
```

```java
package demo;

public class HistoryRepository {
    public void save() {
    }
}
```

- [ ] **Step 2: Add expected golden file**

```text
==================================================
UserController.createUser()
==================================================

UserController.createUser()
├─ UserService.validate()
│  └─ UserRepository.findByEmail()
└─ UserService.createUser()
   ├─ UserRepository.save()
   ├─ HistoryService.saveHistory()
   │  └─ HistoryRepository.save()
   └─ unsupported: externalClient.send()
```

- [ ] **Step 3: Write failing integration test**

```java
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
```

- [ ] **Step 4: Run integration test to verify it fails**

Run: `./gradlew test --tests com.onesi.smsa.core.AnalyzerIntegrationTest`

Expected: FAIL until the call graph builder and formatter produce the exact expected tree.

- [ ] **Step 5: Adjust implementation to pass the golden file**

Make focused fixes only in:

- `ClassModelExtractor`
- `InjectionResolver`
- `CallGraphBuilder`
- `TextReportWriter`

Keep the intended semantics:

- Constructor injection parameter names resolve to service/repository class names.
- `HistoryRepository` is treated as a repository-like class because its name ends with `Repository`.
- External scoped calls with no resolved injected dependency print `unsupported`.
- Line endings normalize in tests only; production output uses `System.lineSeparator()`.

- [ ] **Step 6: Run integration test to verify it passes**

Run: `./gradlew test --tests com.onesi.smsa.core.AnalyzerIntegrationTest`

Expected: PASS.

- [ ] **Step 7: Run all tests**

Run: `./gradlew test`

Expected: PASS.

- [ ] **Step 8: Commit**

```bash
git add src/test/resources src/test/java/com/onesi/smsa/core/AnalyzerIntegrationTest.java src/main/java
git commit -m "test: add spring mvc fixture integration coverage"
```

---

## Task 11: README and Final Verification

**Files:**
- Create: `README.md`

- [ ] **Step 1: Write README**

```markdown
# Spring MVC Static Analyzer 1

Spring MVC Static Analyzer 1 is a Java CLI tool that statically analyzes legacy Spring MVC projects and writes a readable text report showing call flows from Controller methods to Service and Repository/DAO/Mapper methods.

## Requirements

- Java 17
- Gradle

## Usage

```bash
./gradlew run --args="/path/to/legacy-project -o result.txt"
```

## Supported MVP Scope

- Java 8 or later source code
- Annotation-based Spring MVC
- `@Controller`
- `@RestController`
- `@Service`
- `@Repository`
- `@Autowired` field injection
- Constructor injection
- Public Controller methods as entry points

## Unsupported MVP Scope

- Struts
- XML bean definitions
- AOP behavior
- Reflection
- Dynamic proxies
- WebFlux
- Runtime analysis
- MyBatis Mapper XML and SQL tracing

Unsupported or unresolved calls are printed explicitly in the report.

## Output Example

```text
==================================================
UserController.createUser()
==================================================

UserController.createUser()
├─ UserService.validate()
│  └─ UserRepository.findByEmail()
└─ UserService.createUser()
   ├─ UserRepository.save()
   └─ unsupported: externalClient.send()
```

## Error Handling

The analyzer keeps useful partial results where possible. Broken Java files are recorded in the `Warnings` section instead of stopping the whole analysis.

Fatal input errors return exit code `1`. Fatal analysis errors return exit code `2`.
```

- [ ] **Step 2: Run full verification**

Run: `./gradlew test`

Expected: PASS.

Run: `./gradlew run --args="src/test/resources/fixtures/simple-spring-mvc -o build/simple-result.txt"`

Expected: exit code `0` and `build/simple-result.txt` exists.

- [ ] **Step 3: Commit**

```bash
git add README.md
git commit -m "docs: add usage documentation"
```

- [ ] **Step 4: Final status check**

Run: `git status --short`

Expected: no output.

Run: `git log --oneline -5`

Expected: shows the most recent implementation commits.

- [ ] **Step 5: Run final review**

Dispatch the Final Reviewer Subagent defined in `docs/superpowers/subagent-roles.md`.

Input:

- Full plan: `docs/superpowers/plans/2026-06-16-mvp-implementation.md`
- Design spec: `docs/superpowers/specs/2026-06-16-spring-mvc-static-analyzer-design.md`
- Final diff or commit range from the implementation branch
- Verification output from `./gradlew test`
- Verification output from `./gradlew run --args="src/test/resources/fixtures/simple-spring-mvc -o build/simple-result.txt"`

Expected: Final Reviewer reports `APPROVED`, or reports concrete findings that must be fixed before completion.

- [ ] **Step 6: Capture compound lessons from the execution cycle**

After the Final Reviewer reports `APPROVED`, run the Compound Knowledge Capture process defined in `docs/superpowers/subagent-roles.md`.

Use the installed `ce-compound` skill in headless mode.

Invocation context:

```text
ce-compound mode:headless Spring MVC static analyzer MVP execution lessons:
capture implementation mistakes, review findings, test gaps, command failures,
Git/authentication issues, and prevention rules discovered during the subagent
review cycle.
```

Expected:

- A `docs/solutions/` learning document is created or updated when there are concrete lessons.
- The learning document records what went wrong, why it happened, how it was fixed, and how future tasks should avoid repeating it.
- If no concrete lesson exists, the coordinator records that no compound document was needed.

- [ ] **Step 7: Verify and commit compound learning**

Run: `git status --short`

Expected: shows only the generated `docs/solutions/` learning document and any intentional discoverability file update.

Run: `git diff -- docs/solutions`

Expected: the learning document is accurate, specific, and does not contain invented events.

Commit:

```bash
git add docs/solutions
git commit -m "docs: capture mvp execution lessons"
```

---

## Self-Review

- Spec coverage: The plan covers Gradle setup, Java file scanning, parsing, model extraction, layer classification, public Controller entry points, injection resolution, method call extraction, call tree generation, text output, tests, README, and explicit unsupported/unresolved/circular handling.
- Scope control: The plan does not include Struts, XML beans, AOP, reflection resolution, dynamic proxy analysis, WebFlux, runtime analysis, or MyBatis XML SQL tracing.
- Error handling: Fatal input cases are handled in CLI; parse/read issues are warnings; unresolved and unsupported calls are visible in report trees.
- Testing: The plan uses TDD at each component, fixture integration testing, golden file output comparison, CLI validation, and final full verification.
- Knowledge compounding: The plan adds a final `ce-compound mode:headless` step after final review approval so mistakes, review findings, and prevention rules from the execution cycle become durable project knowledge.
- Type consistency: `AnalysisResult`, `AnalysisWarning`, `ClassInfo`, `MethodInfo`, `MethodRef`, `CallGraph`, `CallEdge`, and `CallTreeNode` are introduced before later tasks use them.
