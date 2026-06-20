# Spring MVC 정적 분석기 1 개발 3차 설계

## 개발 3차 정의

개발 3차는 1차 MVP 구현과 2차 Windows 배포/UI 개선 이후, 현재 코드베이스를 유지보수하기 쉬운 구조로 정리하는 리팩토링 단계이다.

이번 단계는 새 분석 기능을 크게 늘리는 것이 목적이 아니다. Ponytail 플러그인을 설치한 뒤, 현재 구현된 코드를 더 잘 읽히고 더 안전하게 바꾸기 위한 계획을 세우는 것이 목적이다.

핵심 기준:

- 기존 동작을 깨지 않는다.
- 사람이 이해하기 쉬운 호출 흐름 리포트라는 철학을 유지한다.
- 리팩토링은 테스트로 보호한다.
- 기능 추가와 구조 개선을 섞지 않는다.
- Ponytail은 계획/작업 추적을 돕는 도구로 사용한다.

## 참조 문서

개발 3차는 아래 설계를 전제로 한다.

- `docs/superpowers/specs/2026-06-16-spring-mvc-static-analyzer-design.ko.md`
- `docs/superpowers/specs/2026-06-16-spring-mvc-static-analyzer-design2.ko.md`

1차 문서는 MVP 분석 파이프라인의 기본 구조를 정의한다.

```text
Java file scan
→ parse
→ class model extract
→ layer classify
→ injection resolve
→ call graph build
→ call tree build
→ text report write
```

2차 문서는 Windows 배포, runtime 포함 ZIP, Swing UI, Controller 노이즈 억제, LF 리포트 출력 고정을 정의한다.

3차 문서는 이 구현들이 누적된 현재 코드에서 책임 경계를 다시 정리한다.

## 현재 코드의 주요 문제

### 1. 분석 orchestration이 한 클래스에 집중됨

`Analyzer`는 전체 파이프라인을 직접 조립한다.

현재 역할:

- Java 파일 스캔
- 파싱
- 클래스 모델 추출
- 주입 해석
- 호출 그래프 생성
- 호출 트리 생성
- warning 누적

아직 크지는 않지만, 다음 기능이 들어오면 빠르게 비대해질 수 있다.

- Spring Data JPA repository method 추정
- request mapping 기준 entry point 필터링
- GitHub URL 입력 처리
- 분석 로그 이벤트
- UI 진행 상태 표시

### 2. 호출 해석 정책이 `CallGraphBuilder`에 섞임

`CallGraphBuilder`는 graph 생성뿐 아니라 호출 해석 정책도 가진다.

현재 포함된 정책:

- 같은 클래스 메서드 호출 해석
- 주입된 dependency method 호출 해석
- unresolved marker 생성
- unsupported marker 생성
- Controller unsupported noise 억제

이 정책이 늘어나면 `CallGraphBuilder`가 graph builder가 아니라 분석 규칙 모음이 된다.

### 3. GUI가 분석 실행 세부사항을 직접 처리함

`AnalyzerGui`는 UI 구성 외에 아래 일을 직접 한다.

- 입력 검증
- 분석 실행
- warning 출력 형식 변환
- 결과 파일 parent directory 생성
- 결과 파일 write
- 성공/실패 로그 생성

Swing UI가 얇은 adapter가 되려면 분석 실행 use case를 별도 클래스로 분리하는 편이 좋다.

### 4. CLI와 GUI의 실행 경로가 부분적으로 중복됨

CLI는 `AnalyzeCommand`에서 분석하고 파일을 쓴다.

GUI는 `AnalyzerGui`에서 분석하고 파일을 쓴다.

중복되는 개념:

- target path 검증
- output path 처리
- `Analyzer.analyze()`
- `TextReportWriter.write()`
- `Files.writeString()`
- input-error warning 처리

이 중복은 나중에 둘 중 하나만 수정되는 버그를 만들 수 있다.

### 5. 리포트 marker 문자열이 구조화되어 있지 않음

현재 호출 edge는 marker text를 문자열로 가진다.

예:

```text
unsupported: externalClient.send()
unresolved: userRepository.findAll()
circular: UserService.process()
```

문자열은 리포트에는 편하지만, 정책 판단에는 약하다.

예:

- Controller unsupported noise 억제
- unresolved repository method 처리
- warning summary 생성
- UI에서 marker 종류별 색상/필터 적용

이런 기능이 늘어나면 marker type을 구조화할 필요가 있다.

## 개발 3차 목표

### 구조 목표

- 분석 실행 use case를 CLI/GUI에서 공통으로 사용한다.
- 호출 해석 정책을 graph builder에서 분리한다.
- marker를 가능한 범위에서 구조화한다.
- GUI는 화면과 사용자 이벤트 처리에 집중한다.
- 기존 public behavior는 유지한다.

### 기능 보존 목표

아래 동작은 리팩토링 후에도 유지해야 한다.

- CLI 인자 실행
- GUI 더블 클릭 실행
- UTF-8 txt 리포트 생성
- LF 줄바꿈 고정
- Controller unsupported noise 억제
- Service/Repository 내부 unsupported 표시 유지
- GitHub Actions Windows runtime ZIP 빌드

## 리팩토링 범위

### 포함

- 분석 실행 application service 추가
- CLI/GUI 공통 실행 결과 모델 추가
- 호출 해석 정책 분리
- Controller noise filter를 명시적 policy로 이동
- 테스트 재배치 또는 보강
- 문서 업데이트

### 제외

- 분석 알고리즘 대규모 교체
- JavaSymbolSolver 기반 정밀 타입 해석 확장
- MyBatis XML 추적
- Spring Data JPA method 완전 해석
- UI 디자인 고도화
- installer 생성
- GitHub URL 자동 clone

## 제안 아키텍처

### 1. `analysis` use case 계층 추가

새 package 후보:

```text
com.onesi.smsa.app
```

주요 클래스:

- `AnalysisRequest`
- `AnalysisExecutionResult`
- `AnalysisRunner`
- `AnalysisLogSink`

역할:

```text
CLI / GUI
→ AnalysisRunner
→ Analyzer
→ TextReportWriter
→ output file
```

`AnalysisRunner`는 분석 실행과 결과 파일 저장을 담당한다.

CLI와 GUI는 `AnalysisRunner`를 호출하고, 결과만 표현한다.

### 2. 실행 결과 모델

`AnalysisExecutionResult`는 CLI/GUI 공통 결과를 담는다.

필드 후보:

```java
public record AnalysisExecutionResult(
        int exitCode,
        Path targetPath,
        Path outputPath,
        boolean reportWritten,
        List<AnalysisWarning> warnings,
        String message
) {
}
```

의도:

- CLI는 `exitCode`를 그대로 반환한다.
- GUI는 `message`, `warnings`, `reportWritten`을 로그로 보여준다.
- 테스트는 파일 생성 여부와 exit code를 검증한다.

### 3. 로그 sink

GUI는 실행 로그가 필요하고 CLI는 기본적으로 조용해야 한다.

공통 interface:

```java
public interface AnalysisLogSink {
    void info(String message);
    void warn(String message);
    void error(String message);
}
```

구현 후보:

- `NoOpAnalysisLogSink`
- `SwingAnalysisLogSink`
- 필요 시 `ConsoleAnalysisLogSink`

1차 리팩토링에서는 `NoOp`과 GUI adapter만 두어도 충분하다.

### 4. 호출 해석 policy 분리

새 package 후보:

```text
com.onesi.smsa.graph.policy
```

주요 클래스 후보:

- `CallResolutionPolicy`
- `ControllerNoiseFilter`
- `RepositoryMethodPolicy`

개발 3차에서 우선 분리할 정책:

- Controller layer에서 unsupported marker를 숨기는 규칙

현재:

```java
if (owner.layer() == Layer.CONTROLLER
        && !edge.resolved()
        && edge.markerText().startsWith("unsupported: ")) {
    continue;
}
```

목표:

```java
if (callResolutionPolicy.shouldSuppress(owner, edge)) {
    continue;
}
```

이렇게 바꾸면 다음 개발에서 Spring Data JPA method 추정도 같은 정책 계층에 넣을 수 있다.

### 5. marker 구조화 후보

현재 `CallEdge.marker(String)`은 문자열만 가진다.

개선 후보:

```java
public enum CallMarkerType {
    UNSUPPORTED,
    UNRESOLVED,
    CIRCULAR,
    MAX_DEPTH
}
```

`CallEdge` 후보:

```java
public record CallEdge(
        MethodRef target,
        CallMarkerType markerType,
        String markerText
) {
}
```

단, 이 변경은 영향 범위가 넓다. 개발 3차에서는 바로 적용하지 않고, 먼저 policy 분리를 끝낸 뒤 별도 task로 검토한다.

## Ponytail 사용 계획

Ponytail은 개발 3차 리팩토링의 작업 단위 추적에 사용한다.

사용 목적:

- refactor task를 작게 나눈다.
- 각 task의 의도, 변경 파일, 검증 결과를 남긴다.
- 1차/2차에서 생긴 회고 교훈을 반복하지 않는다.

권장 task 단위:

1. `AnalysisRunner` 설계 및 테스트
2. CLI를 `AnalysisRunner`로 이전
3. GUI를 `AnalysisRunner`로 이전
4. Controller noise policy 분리
5. marker 구조화 필요성 재검토
6. docs/usage 업데이트
7. 전체 검증 및 compound

각 task 완료 기준:

- 관련 테스트 추가 또는 수정
- `./gradlew test` 통과
- 필요 시 fixture 실행
- 변경 파일 목록 기록
- 다음 task 전 짧은 리뷰

## 세부 작업 계획

### Task 1. `AnalysisRunner` 추가

목표:

- CLI/GUI 공통 분석 실행 class를 만든다.
- 아직 CLI/GUI를 바꾸지 않고 새 class 테스트만 작성한다.

파일:

- `src/main/java/com/onesi/smsa/app/AnalysisRequest.java`
- `src/main/java/com/onesi/smsa/app/AnalysisExecutionResult.java`
- `src/main/java/com/onesi/smsa/app/AnalysisRunner.java`
- `src/test/java/com/onesi/smsa/app/AnalysisRunnerTest.java`

검증:

- 유효한 fixture 입력이면 결과 파일 생성
- 없는 target path면 exit code `1`
- Java 파일 없는 directory면 exit code `1`
- write 실패는 exit code `2`

### Task 2. CLI를 `AnalysisRunner`로 이전

목표:

- `AnalyzeCommand`에서 직접 `Analyzer`와 `TextReportWriter`를 호출하지 않는다.
- exit code 의미는 유지한다.

파일:

- `src/main/java/com/onesi/smsa/cli/AnalyzeCommand.java`
- `src/test/java/com/onesi/smsa/cli/AnalyzeCommandTest.java`

검증:

- 기존 CLI 테스트 통과
- fixture CLI 실행 결과 동일

### Task 3. GUI를 `AnalysisRunner`로 이전

목표:

- `AnalyzerGui`는 화면 이벤트와 로그 출력에 집중한다.
- 분석 실행과 파일 저장은 `AnalysisRunner`에 맡긴다.

파일:

- `src/main/java/com/onesi/smsa/gui/AnalyzerGui.java`
- 필요 시 `src/main/java/com/onesi/smsa/app/AnalysisLogSink.java`

검증:

- `AppLauncherTest` 통과
- 전체 테스트 통과
- headless CI에서 UI 창 직접 테스트는 하지 않는다.

### Task 4. Controller noise policy 분리

목표:

- `CallGraphBuilder`의 controller-specific 조건문을 policy class로 옮긴다.

파일:

- `src/main/java/com/onesi/smsa/graph/CallGraphBuilder.java`
- `src/main/java/com/onesi/smsa/graph/policy/CallResolutionPolicy.java`
- `src/test/java/com/onesi/smsa/graph/policy/CallResolutionPolicyTest.java`

검증:

- Controller의 `Thread.sleep()`, `model.addAttribute()`는 출력되지 않는다.
- Service의 `externalClient.send()`는 계속 출력된다.

### Task 5. Spring Data JPA unresolved 정책 검토

목표:

- `userRepository.findAll()` 같은 상속 repository method를 어떻게 표시할지 결정한다.
- 개발 3차에서는 구현보다 설계 결정까지가 목표다.

선택지:

1. Repository/DAO/Mapper 대상이면 선언 없는 method도 resolved처럼 표시
2. `[assumed]` suffix 추가
3. `unresolved` 유지하되 warning에 Spring Data JPA 가능성 안내

추천:

```text
UserRepository.findAll()
```

이유:

- 레거시 흐름 파악에는 repository 호출 사실이 더 중요하다.
- Spring Data JPA 기본 method는 소스에 안 보이는 것이 정상이다.
- `unresolved`는 사용자에게 오류처럼 보인다.

### Task 6. 문서 업데이트

목표:

- 리팩토링 후 구조를 usage/design 문서에 반영한다.
- 개발 3차 결과를 compound로 남긴다.

파일:

- `docs/usage.md`
- `docs/superpowers/specs/2026-06-16-spring-mvc-static-analyzer-design3.ko.md`
- `docs/solutions/...`

## 테스트 전략

### 필수 테스트

```bash
./gradlew test
```

### fixture 실행

```bash
./gradlew run --args="src/test/resources/fixtures/simple-spring-mvc -o build/dev3-result.txt"
```

검증:

- 기존 expected report와 핵심 흐름 동일
- `unsupported: externalClient.send()` 유지
- Controller noise suppression 유지

### 배포 확인

```bash
./gradlew distZip
```

Windows runtime ZIP은 GitHub Actions에서 확인한다.

## 리스크

### CLI/GUI 동작 차이

공통 runner를 만들 때 CLI와 GUI의 메시지 방식이 달라질 수 있다.

대응:

- exit code 테스트 유지
- GUI는 message/log만 다르게 표현
- core result는 공유

### 리팩토링 중 behavior 변경

CallGraphBuilder를 건드리면 리포트가 바뀔 수 있다.

대응:

- golden file 유지
- Controller noise test 유지
- fixture integration test 유지

### marker 구조화 영향 범위

`CallEdge` 구조를 바꾸면 tree/report 테스트가 많이 흔들릴 수 있다.

대응:

- 개발 3차에서는 policy 분리 우선
- marker 구조화는 별도 task로 분리

## 완료 기준

개발 3차 리팩토링은 아래 조건을 만족하면 완료로 본다.

- CLI와 GUI가 공통 `AnalysisRunner`를 사용한다.
- Controller noise suppression이 policy class로 분리된다.
- 기존 테스트가 모두 통과한다.
- fixture 실행 결과가 기존 핵심 흐름을 유지한다.
- Windows 배포 workflow에 불필요한 변경이 없다.
- 리팩토링 과정에서 얻은 교훈이 compound 문서로 남는다.

## 향후 개발 4차 후보

- Spring Data JPA repository method 추정 처리
- request mapping annotation 기준 Controller entry point 필터링
- GitHub URL 입력 시 clone 안내 또는 자동 clone
- Swing UI 결과 파일 열기 버튼
- 최근 입력 경로 저장
- marker type 구조화
- MyBatis Mapper XML 추적
