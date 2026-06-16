# Spring MVC Static Analyzer 1 Design

## Project Goal

Spring MVC Static Analyzer 1 is a Java CLI tool that statically analyzes legacy Spring MVC projects and writes a human-readable text report showing call flows from Controller methods to Service and Repository/DAO/Mapper methods.

The tool is not intended to be a perfect Java analyzer. Its purpose is to help engineers quickly understand unfamiliar legacy applications by showing the most useful call paths in a readable format.

## Scope

### Supported in MVP

- Java 8 or later source code
- Annotation-based Spring MVC
- `@Controller`
- `@RestController`
- `@Service`
- `@Repository`
- `@Autowired` field injection
- Constructor injection
- Public Controller methods as analysis entry points
- Calls from Controller to Service and from Service to Repository/DAO/Mapper
- UTF-8 text report output

### Unsupported in MVP

- Struts
- XML bean definitions
- AOP behavior
- Reflection
- Dynamic proxies
- WebFlux
- Runtime analysis
- MyBatis Mapper XML and SQL tracing

Unsupported patterns must be shown explicitly in the report when they affect the call flow.

## Recommended Architecture

The application should be implemented as a Gradle Java 17 CLI project.

Primary libraries:

- JavaParser
- JavaSymbolSolver
- Picocli
- SLF4J + Logback
- JUnit 5
- AssertJ

Main components:

- `AnalyzeCommand`: Picocli command that accepts the target project path and output file path.
- `JavaFileScanner`: Finds Java source files under the target project.
- `SourceParser`: Parses Java files with JavaParser and records parse warnings.
- `ClassModelExtractor`: Extracts class names, package names, annotations, fields, constructors, and methods.
- `LayerClassifier`: Classifies classes as Controller, Service, Repository, DAO, Mapper, or Unknown.
- `InjectionResolver`: Resolves field injection and constructor injection into likely class dependencies.
- `MethodCallExtractor`: Extracts method calls from method bodies.
- `CallGraphBuilder`: Builds an internal graph of method-to-method relationships.
- `CallTreeBuilder`: Builds readable call trees from public Controller methods.
- `TextReportWriter`: Writes the final UTF-8 text report.

## Data Flow

1. The user runs the CLI with a target project path.
2. The scanner collects `.java` files.
3. The parser converts Java files into ASTs.
4. The extractor builds class and method models.
5. The classifier assigns architectural layers.
6. The injection resolver maps fields and constructor parameters to project classes.
7. The method call extractor collects calls from each method body.
8. The call graph builder links calls to known project methods when possible.
9. The call tree builder starts from public Controller methods.
10. The report writer outputs readable call trees and warnings.

## Call Resolution Rules

The MVP should prefer predictable and explainable rules over aggressive inference.

Supported resolution:

- `userService.createUser()` resolves through injected fields or constructor parameters.
- `this.validate()` resolves to a method in the same class.
- `validate()` resolves to a method in the same class when there is a matching method.
- Repository, DAO, and Mapper classes are recognized by annotation or name suffix.

Unresolved calls:

- A call that appears to target project code but cannot be linked to a known class or method is marked as `unresolved`.

Unsupported calls:

- A call that belongs to an explicitly unsupported pattern or external dependency is marked as `unsupported`.

Circular calls:

- If a call path repeats a method already present in the current path, the tree prints `circular` and stops that branch.

Maximum depth:

- The default maximum call depth is `20`.
- If the depth is exceeded, the tree prints `unsupported: max call depth exceeded`.

## Error Handling

The analyzer should use a fail-soft approach. It should preserve useful partial results whenever possible.

### Fatal Errors

The CLI exits with an error when:

- The input path does not exist.
- The input path is not a directory.
- The output file cannot be written.
- No Java files are found.
- CLI options are invalid.
- An unexpected fatal runtime error prevents analysis from continuing.

Recommended exit codes:

- `0`: Success
- `1`: User input error
- `2`: Fatal analysis error

### Non-Fatal Analysis Warnings

The analyzer continues and records warnings when:

- A Java file cannot be parsed.
- A Java file cannot be read.
- A class or method cannot be resolved.
- A method call uses an unsupported pattern.
- An unsupported framework or runtime feature is detected.

Warnings should be shown at the end of the report in a dedicated `Warnings` section.

Example:

```text
==================================================
Warnings
==================================================

[parse-error] src/main/java/com/example/BrokenController.java
  Could not parse Java source.

[unsupported] src/main/java/com/example/UserService.java
  Reflection-based call cannot be analyzed.
```

The text report must not include stack traces. Stack traces and implementation details belong in Logback logs.

## Report Format

The report should be optimized for reading, not machine processing.

Example:

```text
==================================================
UserController.createUser()
==================================================

UserController.createUser()
└─ UserService.validate()
   └─ UserRepository.findByEmail()

UserController.createUser()
└─ UserService.createUser()
   ├─ UserRepository.save()
   └─ HistoryService.saveHistory()
      └─ HistoryRepository.save()
```

When a branch cannot be fully analyzed:

```text
UserController.createUser()
└─ UserService.createUser()
   ├─ UserRepository.save()
   └─ unsupported: externalClient.send()
```

## Testing Strategy

Testing should verify behavior, not internal implementation details.

### Unit Tests

Unit tests should cover small components independently.

Recommended test classes:

- `JavaFileScannerTest`
- `SourceParserTest`
- `ClassModelExtractorTest`
- `LayerClassifierTest`
- `InjectionResolverTest`
- `MethodCallExtractorTest`
- `CallTreeBuilderTest`
- `TextReportWriterTest`

Important cases:

- `@Controller` and `@RestController` are classified as Controller.
- `@Service` is classified as Service.
- `@Repository` is classified as Repository.
- Class names ending with `Dao`, `DAO`, or `Mapper` are classified correctly.
- Public Controller methods are selected as entry points.
- Private Controller methods are not selected as entry points.
- Field injection is resolved.
- Constructor injection is resolved.
- Same-class method calls are resolved.
- Unresolved calls are represented explicitly.
- Circular calls stop safely.

### Integration Tests

Integration tests should use fixture projects under `src/test/resources/fixtures`.

Recommended fixture:

```text
simple-spring-mvc
├── UserController
├── UserService
├── UserRepository
├── HistoryService
└── HistoryRepository
```

The integration test should run the full analyzer pipeline and verify that the generated report contains the expected Controller to Service to Repository flow.

### Golden File Tests

Report formatting should be tested with golden files.

Recommended location:

```text
src/test/resources/expected/simple-result.txt
```

Tests should normalize line endings from `\r\n` to `\n` before comparison.

Golden file tests should verify:

- Section separators
- Method names
- Tree indentation
- Unsupported markers
- Warning section formatting

### CLI Tests

CLI tests should verify Picocli behavior.

Cases:

- `analyze <path> -o <file>` creates the output file.
- A missing input path returns exit code `1`.
- A path with no Java files returns exit code `1`.
- Omitting `-o` writes to the default `result.txt`.

### Error Handling Tests

Error handling tests should verify that the analyzer keeps useful partial results.

Cases:

- A broken Java file records a parse warning and does not stop analysis.
- An unreadable file records a read warning if the runtime exposes the condition.
- An unknown project method is printed as `unresolved`.
- An external dependency call is printed as `unsupported`.
- A circular method call is printed as `circular`.
- Maximum depth protection stops overly deep call chains.

## Implementation Order

1. Create the Gradle Java CLI project.
2. Add dependencies and basic package structure.
3. Write tests for layer classification.
4. Implement Java file scanning.
5. Implement source parsing and warning collection.
6. Implement class model extraction.
7. Implement layer classification.
8. Implement Controller public method detection.
9. Implement injection resolution.
10. Implement method call extraction.
11. Implement call graph and call tree building.
12. Implement text report output.
13. Implement CLI command behavior.
14. Add integration and golden file tests.
15. Write README usage documentation.

## Acceptance Criteria

- The project builds with Java 17 and Gradle.
- The CLI accepts a target Spring MVC project path.
- The analyzer finds public Controller methods.
- The analyzer prints readable Controller to Service to Repository/DAO/Mapper flows.
- Unsupported and unresolved calls are visible in the report.
- Parse errors in individual files do not stop the full analysis.
- Tests cover unit behavior, integration behavior, report formatting, CLI behavior, and error handling.
- README explains usage, supported scope, unsupported scope, and output format.
