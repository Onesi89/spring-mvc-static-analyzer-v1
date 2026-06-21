# Spring MVC Static Analyzer Dev 4 Design

## 목적

개발 4차는 Ponytail 관점으로 과한 구조를 찾고, 기존 동작을 유지하면서
삭제하거나 줄이는 리팩토링 단계이다.

개발 3차에서 이미 CLI/GUI 공통 `AnalysisRunner`와 호출 해석 정책 경계를
도입했다. 개발 4차는 이 구조를 다시 크게 바꾸는 단계가 아니다. 현재 제품에
필요한 것보다 무거운 코드, 의존성, 중복만 작게 줄인다.

## 범위

- 코드 변경 전에 Ponytail 방식으로 과한 구조를 감사한다.
- 삭제량, 의존성 감소, 간접 계층 감소 효과가 큰 후보부터 순위를 매긴다.
- 한 번에 하나의 작은 리팩토링만 진행한다.
- 공개 동작과 리포트 텍스트를 보존한다.
- CLI와 GUI는 계속 공통 `AnalysisRunner` 경로를 사용한다.
- Controller의 unsupported noise 억제는 유지한다.
- Service/Repository 내부 unsupported 호출 표시는 유지한다.
- 리팩토링이 보호 경계를 건드릴 때만 테스트를 추가하거나 보강한다.
- 반복해서 쓸 수 있는 교훈이 생기면 마지막에 compound 문서로 남긴다.

## 비목표

- 새 분석 기능 추가 없음.
- JavaSymbolSolver 기반 정밀 분석 확장 없음.
- Spring Data JPA inherited method 구현 없음.
- MyBatis XML 추적 없음.
- GUI 디자인 개편 없음.
- installer 작업 없음.
- 추측성 package 분리 없음.
- 현재 테스트로 보호되는 단순화가 필요하지 않다면 marker model 재작성 없음.

## Ponytail 감사 기준

아래 기준은 과한 구조와 복잡도에만 적용한다. 정확성 버그, 보안, 성능은 별도
리뷰에서 다룬다.

- `delete`: 현재 동작에서 쓰지 않는 코드, 의존성, 옵션, 계층 제거.
- `stdlib`: Java, Gradle, Swing, picocli가 이미 제공하는 기능으로 대체.
- `native`: 플랫폼 기본 기능으로 대체 가능한 의존성이나 wrapper 제거.
- `yagni`: 구현체가 하나뿐이거나 호출자가 하나뿐인 추측성 추상화 축소.
- `shrink`: 같은 동작을 더 적은 분기, 클래스, 반복 코드로 표현.

미래 기능을 가정해야만 의미가 있는 후보는 제외한다. 현재 동작을 테스트로
검증할 수 있는 후보만 채택한다.

## 현재 과한 구조 후보 영역, 우선순위

1. `build.gradle`에 `javaparser-symbol-solver-core`가 포함되어 있지만 현재
   main code는 `javaparser-core`만 직접 사용한다. 후보: 전체 테스트가 통과하면
   미사용 의존성을 제거한다.

2. `TextReportWriter`에는 자식 노드를 순회하는 재귀 메서드가 두 개 있고 구조가
   거의 같다. 후보: 예상 리포트 fixture를 그대로 유지하면서 하나의 helper로
   줄인다.

3. `AnalyzerGui`는 `AnalysisRunner`가 다시 수행하는 target directory 존재 검사를
   먼저 수행한다. 후보: GUI에는 빈 입력 검증만 남기거나, 더 이른 dialog가 필요한
   이유를 명확히 한다. 변경 시 사용자 오류 메시지 품질은 유지한다.

4. `CallResolutionPolicy`는 현재 규칙이 하나뿐인 클래스이다. 다만 개발 3차에서
   의도적으로 만든 정책 경계이므로 자동 삭제 대상은 아니다. 후보: 제거하면
   Controller noise 정책이 `CallGraphBuilder`로 되돌아가는지 확인하고, 그렇다면
   유지한다.

5. `Analyzer`는 pipeline dependency를 모두 inline으로 생성한다. 아직 작고 읽기
   쉽다. 후보: 테스트 주입이나 실제 중복 제거가 필요하지 않으면 아무것도 하지
   않는다. factory나 container를 새로 만들지 않는다.

6. `AnalysisExecutionResult`는 CLI와 GUI가 서로 다른 필드를 사용한다. 특정 필드가
   실제로 미사용임이 테스트와 호출부로 확인되지 않으면 유지한다.

## 작업 계획, 한 번에 하나씩

1. repo 전체 Ponytail 감사를 실행하고, task note에 우선순위 결과를 남긴다.

2. 미사용 의존성 후보를 확인 후 제거한다.
   - `build.gradle`만 수정한다.
   - `./gradlew test`를 실행한다.
   - 테스트 실패가 의존성 필요 때문이면 이 task의 변경만 되돌린다.

3. 리포트 tree 재귀를 줄인다.
   - 정확한 출력 fixture assertion을 유지하거나 보강한다.
   - `TextReportWriter`만 수정한다.
   - `./gradlew test --tests com.onesi.smsa.report.TextReportWriterTest`를 실행한다.
   - 출력 텍스트가 바뀌었으면 simple fixture 확인도 실행한다.

4. GUI validation 중복을 검토한다.
   - `AnalyzerGuiTest`와 `AnalysisRunnerTest`를 비교한다.
   - 사용자 동작이 명확하게 유지될 때만 변경한다.
   - 변경한 surface에 맞춰 GUI/runner 테스트를 실행한다.

5. 정책 경계를 검토한다.
   - 더 작은 구조가 개발 3차의 architecture lesson을 유지하지 못하면
     `CallResolutionPolicy`를 유지한다.
   - 변경 시 `CallResolutionPolicyTest`와 `CallGraphBuilderTest`를 실행한다.

6. 의미 있는 code diff마다 Ponytail review를 실행한다. diff가 작거나 문서만
   바뀐 경우 마지막에 한 번 실행해도 된다.

7. 전체 검증과 compound를 실행한다.
   - `./gradlew test`
   - 운영 규칙의 fixture command
   - `git diff --check`
   - 재사용 가능한 교훈이 생긴 경우에만 compound 문서 작성

## 검증 계획

구현 변경의 최소 검증:

```bash
./gradlew test
git diff --check
```

리포트 출력 또는 분석 흐름이 바뀌면 fixture 검증:

```bash
./gradlew run --args="src/test/resources/fixtures/simple-spring-mvc -o build/dev4-result.txt"
```

fixture 출력 확인 항목:

- `UserController.createUser()`
- `unsupported: externalClient.send()`
- `unsupported: Thread.sleep()` 없음
- `unsupported: model.addAttribute()` 없음

문서만 바꾼 경우에는 명령, 경로, 기대 동작이 바뀌지 않는 한 `git diff --check`로
충분하다.

## Subagent / 리뷰 규칙

- Subagent 첫 통신만 `/caveman lite`로 시작한다.
- Subagent 첫 통신에만 해당 subagent의 역할을 전달한다.
- 같은 subagent에게 보내는 후속 메시지에는 `/caveman lite`와 역할을 반복하지 않는다.
- Implementer subagent에는 경계가 작은 task 하나와 task-specific test를 준다.
- 구현 전 Ponytail audit를 실행한다.
- 의미 있는 code diff마다 Ponytail review를 실행하거나, 작은 묶음이면 마지막에
  한 번 실행한다.
- 마지막 code 구현 task 이후에는 spec compliance review, code quality review,
  독립 test verification을 실행한다.
- Coordinator는 코드 검정, diff review, Ponytail review, test verification,
  문서 변경을 subagent에게 맡기고 보고를 읽는다.
- 문서 생성, 수정, 변경은 subagent 작업이다.
- 긴급 조율 메모나 사용자 명시 지시가 없으면 Coordinator는 문서를 직접 편집하지 않는다.
- Coordinator가 직접 하는 검사는 조율에 필요한 최소 git 상태 확인으로 제한한다.
- Subagent는 push하지 않는다.
- 사용자가 요청하지 않으면 `main` merge 단계로 가지 않는다.

## 완료 기준

- 개발 4차 변경이 CLI 동작, GUI 동작, LF UTF-8 리포트, warning 동작, fixture
  출력을 보존한다.
- 채택된 각 리팩토링은 현재 복잡도를 삭제하거나 줄인다.
- 추측성 추상화를 추가하지 않는다.
- 필요한 테스트가 최신 실행 결과로 통과한다.
- `git diff --check`가 통과한다.
- 반복해서 쓸 수 있는 교훈은 `docs/solutions/` 아래에 남긴다.
- 최종 보고에는 status, files, verification, commit, concerns를 포함한다.

## 위험

- `javaparser-symbol-solver-core` 제거가 transitive parser 동작이나 향후 작업에
  영향을 줄 수 있다. 완화: 현재 테스트 통과 시에만 제거하고, 미래 재추가는
  feature task로 다룬다.

- 리포트 재귀 축소가 공백이나 tree glyph 위치를 바꿀 수 있다. 완화: expected
  fixture 출력으로 변경 전후를 보호한다.

- GUI 사전 검증 제거가 오류 메시지를 덜 친절하게 만들 수 있다. 완화: 빈 입력
  검증은 유지하고 기존 GUI 테스트를 먼저 비교한다.

- 정책 경계를 삭제하면 개발 3차의 architecture 개선을 되돌릴 수 있다. 완화:
  최종 구조가 더 작고 더 명확하지 않으면 `CallResolutionPolicy`는 유지한다.

- Ponytail 관점이 필요한 경계까지 과하게 자를 수 있다. 완화: 모든 삭제는 현재
  동작 보존과 현재 코드 기준의 근거가 있어야 한다.
