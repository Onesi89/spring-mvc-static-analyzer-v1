# Spring MVC 정적 분석기 1 설계

## 프로젝트 목표

Spring MVC 정적 분석기 1은 레거시 Spring MVC 프로젝트를 정적으로 분석하고, Controller 메서드에서 Service 및 Repository/DAO/Mapper 메서드로 이어지는 호출 흐름을 사람이 읽기 쉬운 텍스트 리포트로 출력하는 Java CLI 도구이다.

이 도구는 완벽한 Java 분석기를 목표로 하지 않는다. 낯선 레거시 애플리케이션을 인수한 개발자가 핵심 호출 경로를 빠르게 이해할 수 있도록 돕는 것이 목적이다.

## 범위

### MVP 지원 범위

- Java 8 이상 소스 코드
- Annotation 기반 Spring MVC
- `@Controller`
- `@RestController`
- `@Service`
- `@Repository`
- `@Autowired` 필드 주입
- 생성자 주입
- public Controller 메서드를 분석 시작점으로 사용
- Controller에서 Service, Service에서 Repository/DAO/Mapper로 이어지는 호출
- UTF-8 텍스트 리포트 출력

### MVP 미지원 범위

- Struts
- XML bean 정의
- AOP 동작
- Reflection
- Dynamic proxy
- WebFlux
- 런타임 분석
- MyBatis Mapper XML 및 SQL 추적

미지원 패턴이 호출 흐름에 영향을 주는 경우 리포트에 명시적으로 표시해야 한다.

## 권장 아키텍처

애플리케이션은 Gradle 기반 Java 17 CLI 프로젝트로 구현한다.

주요 라이브러리:

- JavaParser
- JavaSymbolSolver
- Picocli
- SLF4J + Logback
- JUnit 5
- AssertJ

주요 컴포넌트:

- `AnalyzeCommand`: 분석 대상 프로젝트 경로와 출력 파일 경로를 받는 Picocli 명령.
- `JavaFileScanner`: 분석 대상 프로젝트 아래의 Java 소스 파일을 찾는다.
- `SourceParser`: JavaParser로 Java 파일을 파싱하고 파싱 경고를 기록한다.
- `ClassModelExtractor`: 클래스명, 패키지명, 어노테이션, 필드, 생성자, 메서드를 추출한다.
- `LayerClassifier`: 클래스를 Controller, Service, Repository, DAO, Mapper, Unknown으로 분류한다.
- `InjectionResolver`: 필드 주입과 생성자 주입을 프로젝트 내부 클래스 의존성으로 해석한다.
- `MethodCallExtractor`: 메서드 본문에서 메서드 호출을 추출한다.
- `CallGraphBuilder`: 메서드 간 호출 관계를 내부 그래프로 만든다.
- `CallTreeBuilder`: public Controller 메서드부터 읽기 쉬운 호출 트리를 만든다.
- `TextReportWriter`: 최종 UTF-8 텍스트 리포트를 작성한다.

## 데이터 흐름

1. 사용자가 CLI에 분석 대상 프로젝트 경로를 전달한다.
2. 스캐너가 `.java` 파일을 수집한다.
3. 파서가 Java 파일을 AST로 변환한다.
4. 추출기가 클래스와 메서드 모델을 만든다.
5. 분류기가 아키텍처 계층을 지정한다.
6. 주입 해석기가 필드와 생성자 파라미터를 프로젝트 클래스에 매핑한다.
7. 메서드 호출 추출기가 각 메서드 본문의 호출을 수집한다.
8. 호출 그래프 빌더가 가능한 경우 호출을 프로젝트 내부 메서드와 연결한다.
9. 호출 트리 빌더가 public Controller 메서드를 시작점으로 삼는다.
10. 리포트 작성기가 호출 트리와 경고를 출력한다.

## 호출 해석 규칙

MVP에서는 과도한 추론보다 예측 가능하고 설명 가능한 규칙을 우선한다.

지원하는 해석:

- `userService.createUser()`는 주입된 필드 또는 생성자 파라미터를 통해 해석한다.
- `this.validate()`는 같은 클래스의 메서드로 해석한다.
- `validate()`는 같은 클래스에 일치하는 메서드가 있으면 해당 메서드로 해석한다.
- Repository, DAO, Mapper 클래스는 어노테이션 또는 이름 접미사로 인식한다.

해석 불가 호출:

- 프로젝트 내부 코드처럼 보이지만 알려진 클래스나 메서드에 연결할 수 없는 호출은 `unresolved`로 표시한다.

미지원 호출:

- 명시적으로 미지원인 패턴이나 외부 의존성에 속하는 호출은 `unsupported`로 표시한다.

순환 호출:

- 현재 호출 경로에 이미 포함된 메서드가 다시 등장하면 `circular`를 출력하고 해당 분기를 중단한다.

최대 깊이:

- 기본 최대 호출 깊이는 `20`으로 한다.
- 깊이를 초과하면 `unsupported: max call depth exceeded`를 출력한다.

## 오류 처리

분석기는 fail-soft 방식을 사용한다. 가능한 경우 유용한 부분 결과를 보존해야 한다.

### 치명적 오류

다음 경우 CLI는 오류로 종료한다.

- 입력 경로가 존재하지 않는다.
- 입력 경로가 디렉터리가 아니다.
- 출력 파일을 쓸 수 없다.
- Java 파일을 하나도 찾지 못했다.
- CLI 옵션이 잘못되었다.
- 분석을 계속할 수 없는 예상치 못한 치명적 런타임 오류가 발생했다.

권장 종료 코드:

- `0`: 성공
- `1`: 사용자 입력 오류
- `2`: 치명적 분석 오류

### 치명적이지 않은 분석 경고

다음 경우 분석을 계속하고 경고를 기록한다.

- Java 파일을 파싱할 수 없다.
- Java 파일을 읽을 수 없다.
- 클래스 또는 메서드를 해석할 수 없다.
- 메서드 호출이 미지원 패턴을 사용한다.
- 미지원 프레임워크 또는 런타임 기능이 감지된다.

경고는 리포트 끝의 별도 `Warnings` 섹션에 표시한다.

예:

```text
==================================================
Warnings
==================================================

[parse-error] src/main/java/com/example/BrokenController.java
  Could not parse Java source.

[unsupported] src/main/java/com/example/UserService.java
  Reflection-based call cannot be analyzed.
```

텍스트 리포트에는 스택트레이스를 넣지 않는다. 스택트레이스와 구현 상세 정보는 Logback 로그에 남긴다.

## 리포트 형식

리포트는 기계 처리보다 사람이 읽기 쉽게 만드는 것을 우선한다.

예:

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

완전히 분석할 수 없는 분기는 다음처럼 출력한다.

```text
UserController.createUser()
└─ UserService.createUser()
   ├─ UserRepository.save()
   └─ unsupported: externalClient.send()
```

## 테스트 전략

테스트는 내부 구현 방식보다 동작을 검증해야 한다.

### 단위 테스트

단위 테스트는 작은 컴포넌트를 독립적으로 검증한다.

권장 테스트 클래스:

- `JavaFileScannerTest`
- `SourceParserTest`
- `ClassModelExtractorTest`
- `LayerClassifierTest`
- `InjectionResolverTest`
- `MethodCallExtractorTest`
- `CallTreeBuilderTest`
- `TextReportWriterTest`

중요 케이스:

- `@Controller`와 `@RestController`는 Controller로 분류된다.
- `@Service`는 Service로 분류된다.
- `@Repository`는 Repository로 분류된다.
- 이름이 `Dao`, `DAO`, `Mapper`로 끝나는 클래스는 올바르게 분류된다.
- public Controller 메서드는 시작점으로 선택된다.
- private Controller 메서드는 시작점에서 제외된다.
- 필드 주입이 해석된다.
- 생성자 주입이 해석된다.
- 같은 클래스 내부 메서드 호출이 해석된다.
- 해석 불가 호출이 명시적으로 표현된다.
- 순환 호출이 안전하게 중단된다.

### 통합 테스트

통합 테스트는 `src/test/resources/fixtures` 아래의 fixture 프로젝트를 사용한다.

권장 fixture:

```text
simple-spring-mvc
├── UserController
├── UserService
├── UserRepository
├── HistoryService
└── HistoryRepository
```

통합 테스트는 전체 분석 파이프라인을 실행하고, 생성된 리포트에 Controller에서 Service, Repository로 이어지는 기대 호출 흐름이 포함되는지 검증한다.

### Golden File 테스트

리포트 포맷은 golden file로 테스트한다.

권장 위치:

```text
src/test/resources/expected/simple-result.txt
```

테스트에서는 비교 전에 줄바꿈을 `\r\n`에서 `\n`으로 정규화한다.

Golden file 테스트 검증 항목:

- 섹션 구분선
- 메서드 이름
- 트리 들여쓰기
- unsupported 표시
- warning 섹션 형식

### CLI 테스트

CLI 테스트는 Picocli 동작을 검증한다.

케이스:

- `analyze <path> -o <file>` 실행 시 출력 파일이 생성된다.
- 존재하지 않는 입력 경로는 종료 코드 `1`을 반환한다.
- Java 파일이 없는 경로는 종료 코드 `1`을 반환한다.
- `-o`를 생략하면 기본 `result.txt`에 출력한다.

### 오류 처리 테스트

오류 처리 테스트는 분석기가 유용한 부분 결과를 유지하는지 검증한다.

케이스:

- 깨진 Java 파일은 parse warning을 기록하고 분석을 중단하지 않는다.
- 런타임에서 조건을 노출할 수 있다면 읽을 수 없는 파일은 read warning을 기록한다.
- 알 수 없는 프로젝트 메서드는 `unresolved`로 출력된다.
- 외부 의존성 호출은 `unsupported`로 출력된다.
- 순환 메서드 호출은 `circular`로 출력된다.
- 최대 깊이 보호가 지나치게 깊은 호출 체인을 중단한다.

## 구현 순서

1. Gradle Java CLI 프로젝트를 생성한다.
2. 의존성과 기본 패키지 구조를 추가한다.
3. 계층 분류 테스트를 작성한다.
4. Java 파일 스캔을 구현한다.
5. 소스 파싱과 warning 수집을 구현한다.
6. 클래스 모델 추출을 구현한다.
7. 계층 분류를 구현한다.
8. Controller public 메서드 감지를 구현한다.
9. 주입 해석을 구현한다.
10. 메서드 호출 추출을 구현한다.
11. 호출 그래프와 호출 트리 생성을 구현한다.
12. 텍스트 리포트 출력을 구현한다.
13. CLI 명령 동작을 구현한다.
14. 통합 테스트와 golden file 테스트를 추가한다.
15. README 사용법 문서를 작성한다.

## 인수 기준

- 프로젝트가 Java 17과 Gradle로 빌드된다.
- CLI가 분석 대상 Spring MVC 프로젝트 경로를 입력받는다.
- 분석기가 public Controller 메서드를 찾는다.
- 분석기가 Controller에서 Service, Repository/DAO/Mapper로 이어지는 읽기 쉬운 흐름을 출력한다.
- 미지원 호출과 해석 불가 호출이 리포트에 표시된다.
- 개별 파일의 파싱 오류가 전체 분석을 중단하지 않는다.
- 단위 동작, 통합 동작, 리포트 포맷, CLI 동작, 오류 처리를 테스트로 검증한다.
- README가 사용법, 지원 범위, 미지원 범위, 출력 형식을 설명한다.
