# Spring MVC Static Analyzer 1 Usage Guide

이 문서는 완성된 Spring MVC Static Analyzer 1을 실행하고 결과를 해석하는 방법을 설명합니다.

## 1. 준비 사항

GitHub Actions에서 제공하는 runtime 포함 Windows ZIP을 사용하는 경우:

- 별도 Java 설치가 필요하지 않습니다.

소스에서 직접 실행하거나 테스트하는 경우:

- Java 17
- 인터넷 연결

이 저장소에는 `gradlew` 실행 스크립트가 포함되어 있습니다. 처음 실행할 때 Gradle 8.8 배포판을 `.gradle/` 아래에 내려받습니다.

## 2. 기본 실행

분석 대상 Spring MVC 프로젝트 경로를 첫 번째 인자로 전달합니다.

```bash
./gradlew run --args="/path/to/legacy-project -o result.txt"
```

예:

```bash
./gradlew run --args="/home/user/workspace/legacy-project -o result.txt"
```

Windows 경로를 전달할 때는 따옴표로 감싸는 것을 권장합니다.

```bash
./gradlew run --args="C:\workspace\legacy-project -o result.txt"
```

## 3. 출력 파일 지정

`-o` 또는 `--output`으로 결과 파일 경로를 지정합니다.

```bash
./gradlew run --args="/path/to/legacy-project --output analysis-result.txt"
```

`-o`를 생략하면 현재 디렉터리에 `result.txt`가 생성됩니다.

```bash
./gradlew run --args="/path/to/legacy-project"
```

## 4. GitHub Actions Windows ZIP 사용

GitHub Actions의 `Build` workflow가 성공하면 Windows용 artifact를 내려받을 수 있습니다.

권장 artifact:

- `spring-mvc-static-analyzer-windows-runtime-zip`
- Java runtime을 포함합니다.
- 사용자 PC에 Java를 따로 설치하지 않아도 됩니다.

ZIP 파일을 압축 해제한 뒤 Windows 터미널에서 `.exe` 실행 파일을 사용합니다.

```bat
spring-mvc-static-analyzer-v1.exe C:\workspace\legacy-project -o result.txt
```

또는 `spring-mvc-static-analyzer-v1.exe`를 더블 클릭해서 UI 모드로 실행할 수 있습니다.

UI 모드에서는 다음 항목을 선택한 뒤 분석을 실행합니다.

- 대상 Spring MVC 프로젝트 폴더
- 결과 txt 파일 경로

분석 진행 상황과 오류는 UI의 `Execution log` 영역에 표시됩니다.

기존 artifact:

- `spring-mvc-static-analyzer-windows-bat-zip`
- Java runtime을 포함하지 않습니다.
- 사용자 PC에 Java 17이 설치되어 있어야 합니다.

```bat
bin\spring-mvc-static-analyzer-v1.bat C:\workspace\legacy-project -o result.txt
```

같은 workflow는 테스트 fixture로 생성한 `spring-mvc-static-analyzer-sample-result` artifact도 함께 업로드합니다.

## 5. 결과 예시

출력 파일은 UTF-8 텍스트 파일입니다.

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

의미:

- `UserController.createUser()`가 분석 시작점입니다.
- `UserService.validate()`와 `UserService.createUser()`가 Controller에서 호출됩니다.
- `UserRepository`, `HistoryRepository` 호출까지 이어지는 흐름을 보여줍니다.
- `unsupported: externalClient.send()`는 MVP 범위에서 해석할 수 없는 외부 또는 미지원 호출입니다.

## 6. 지원 범위

MVP에서 지원하는 패턴:

- Java 8 이상 소스
- Annotation 기반 Spring MVC
- `@Controller`
- `@RestController`
- `@Service`
- `@Repository`
- `@Autowired` 필드 주입
- 생성자 주입
- Controller의 public 메서드 시작점
- Controller -> Service -> Repository/DAO/Mapper 호출 흐름

## 7. 미지원 범위

다음은 MVP에서 분석하지 않습니다.

- Struts
- XML Bean
- AOP
- Reflection
- Dynamic Proxy
- WebFlux
- 런타임 분석
- MyBatis Mapper XML 및 SQL 추적

미지원 또는 해석 불가 호출은 가능한 경우 결과에 명시됩니다.

```text
unsupported: externalClient.send()
unresolved: someMethod()
circular: UserService.process()
unsupported: max call depth exceeded
```

## 8. 오류 처리

입력 오류는 exit code `1`을 반환합니다.

예:

- 입력 경로가 없음
- 입력 경로가 디렉터리가 아님
- Java 파일이 하나도 없음

분석 중 치명적 오류는 exit code `2`를 반환합니다.

개별 Java 파일 파싱 오류는 전체 분석을 중단하지 않고 결과 파일의 `Warnings` 섹션에 기록됩니다.

```text
==================================================
Warnings
==================================================

[parse-error] src/main/java/com/example/BrokenController.java
  Could not parse Java source.
```

## 9. 테스트 fixture로 동작 확인

저장소에 포함된 샘플 fixture로 프로그램 동작을 확인할 수 있습니다.

```bash
./gradlew run --args="src/test/resources/fixtures/simple-spring-mvc -o build/simple-result.txt"
```

결과 확인:

```bash
cat build/simple-result.txt
```

전체 테스트 실행:

```bash
./gradlew test
```

## 10. IDE에서 demo 파일 오류가 보일 때

`src/test/resources/fixtures/simple-spring-mvc/src/main/java/demo` 아래 파일들은 컴파일 대상 소스가 아니라 분석 입력용 fixture입니다.

이 파일들은 일부러 Spring annotation import와 미정의 외부 호출을 포함합니다.

```java
externalClient.send();
```

이 호출은 결과에서 다음처럼 표시되는지 검증하기 위한 샘플입니다.

```text
unsupported: externalClient.send()
```

따라서 Gradle 기준으로 `./gradlew test`가 통과하면 프로그램 빌드는 정상입니다.
