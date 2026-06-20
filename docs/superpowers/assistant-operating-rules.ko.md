# Assistant Operating Rules

이 문서는 Codex가 작업 규칙을 잊었을 때 가장 먼저 다시 읽어야 하는
프로젝트 운영 규칙이다.

## 1. 플러그인 미적용처럼 보일 때

작업 시작 전에 아래를 확인한다.

- 사용자가 특정 플러그인이나 skill을 언급하면 반드시 해당 skill 문서를 먼저
  읽는다.
- Superpowers 관련 작업이면 `superpowers:using-superpowers` 규칙을 최우선으로
  다시 확인한다.
- Ponytail이 필요한 리팩토링/단순화 작업이면 최소 변경 원칙을 따른다.
- Caveman이 적용된 대화에서는 간결하게 말한다.
- 하위 agent에게 지시할 때는 `/caveman lite`로 시작한다.

플러그인이 설치되어 있는데 적용되지 않는 것처럼 보이면:

1. 현재 사용 가능한 skill 목록을 확인한다.
2. 관련 skill의 `SKILL.md`를 다시 읽는다.
3. 그래도 callable capability가 없으면 사용자에게 짧게 알리고 가능한 대체
   절차로 진행한다.

## 2. Superpowers Rule

Superpowers는 선택 사항이 아니다. 관련 가능성이 조금이라도 있으면 먼저 읽고
적용한다.

기본 규칙:

- 작업 전에 관련 skill을 읽는다.
- 구현 작업은 `superpowers:test-driven-development`를 사용한다.
- 버그/실패 분석은 `superpowers:systematic-debugging`을 사용한다.
- 계획 실행은 `superpowers:subagent-driven-development` 또는
  `superpowers:executing-plans`를 사용한다.
- 완료 주장 전에는 `superpowers:verification-before-completion`을 사용한다.
- 개발 브랜치 마무리/merge 전에는
  `superpowers:finishing-a-development-branch`를 사용한다.

사용자 규칙이 Superpowers와 충돌하면 사용자 규칙이 우선이다.

## 3. Subagent 운영 규칙

코드 구현 task는 새 작업 브랜치에서 진행한다. `main`에서 직접 구현하지 않는다.

하위 agent에게 첫 메시지를 보낼 때 반드시 아래를 포함한다.

```text
/caveman lite
Role: <Implementer | Spec Compliance Reviewer | Code Quality Reviewer | Test Verifier | Final Reviewer>
Task: <task 이름>
```

공통 규칙:

- 하위 agent는 숨은 대화 이력을 안다고 가정하지 않는다.
- task 내용, 관련 파일, 성공 기준, 검증 명령을 명시한다.
- 구현 agent는 TDD 책임 때문에 task-specific test를 직접 실행한다.
- Test Verifier는 구현자가 아닌 독립 검증 gate이다.
- 사용자가 지정한 현재 규칙상, 코드 구현 task의 마지막 구현 이후에
  Spec Reviewer, Code Quality Reviewer, Test Verifier를 실행한다.
- task가 끝나면 해당 task의 하위 agent를 모두 종료한다.
- subagent는 remote push를 하지 않는다.

## 4. Compound 과정

리뷰나 검증 과정에서 실수, 혼동, 실패, 교훈이 생기면 마지막 단계에서
compound 문서로 남긴다.

반드시 compound 해야 하는 경우:

- CI/test/build 실패 원인을 해결했을 때
- subagent 운영 실수나 역할 혼동이 있었을 때
- 검증 방식이 과하거나 부족해서 조정했을 때
- 아키텍처 경계나 정책 결정을 새로 만들었을 때
- 다음 작업자가 같은 실수를 반복할 가능성이 있을 때

작성 위치:

- 실패/오류: `docs/solutions/test-failures/`, `build-errors/` 등
- workflow 교훈: `docs/solutions/workflow-issues/`
- 아키텍처/설계 패턴: `docs/solutions/architecture-patterns/`

작성 후 `validate-frontmatter.py`로 frontmatter를 검증한다.

## 5. Task 종료와 Main Merge

모든 task가 끝나면 다음 순서로 마무리한다.

1. 작업 브랜치에서 전체 테스트를 실행한다.
2. 필요한 fixture 실행 또는 문서 검증을 수행한다.
3. 작업 tree가 깨끗한지 확인한다.
4. `main`으로 이동한다.
5. 작업 브랜치를 `main`에 fast-forward merge한다.
6. merged `main`에서 전체 테스트를 다시 실행한다.
7. 성공하면 feature branch를 삭제한다.
8. push는 사용자가 담당한다. Codex는 사용자 요청 없이 push하지 않는다.

기본 명령:

```bash
./gradlew test
git switch main
git merge --ff-only <feature-branch>
./gradlew test
git branch -d <feature-branch>
```

## 6. Git 형상관리 규칙

- main에는 사용자가 직접 push한다.
- Codex는 local task branch 생성, commit, main fast-forward merge까지만 담당한다.
- destructive 명령은 사용자 명시 승인 없이 사용하지 않는다.
- unrelated user change는 되돌리지 않는다.
- commit은 task 단위로 작게 나눈다.
- 문서/compound commit은 기능 코드 commit과 가능하면 분리한다.

## 7. 검증 규칙

완료를 말하기 전에 반드시 방금 실행한 검증 결과를 확인한다.

자주 쓰는 검증:

```bash
./gradlew test
./gradlew run --args="src/test/resources/fixtures/simple-spring-mvc -o build/dev3-result.txt"
git diff --check
```

fixture 결과에서 자주 확인할 항목:

- `UserController.createUser()`가 출력되는지
- `unsupported: externalClient.send()`가 유지되는지
- Controller noise인 `unsupported: Thread.sleep()`이 숨겨지는지
- Controller noise인 `unsupported: model.addAttribute()`가 숨겨지는지

문서만 변경한 경우에도 최소한 `git diff --check`를 실행한다.
compound 문서가 있으면 frontmatter validator도 실행한다.

## 8. 현재 프로젝트에서 특히 기억할 것

- 목표는 완벽한 Java 분석기가 아니라 레거시 Spring MVC 흐름을 빠르게 읽게
  하는 도구다.
- 가독성이 분석 정확도보다 우선인 경우가 있다.
- unsupported/unresolved는 사람이 이해할 수 있게 명시한다.
- Controller의 불필요한 unsupported noise는 숨기되, Service/Repository 내부
  unsupported 호출은 유지한다.
- Spring Data JPA inherited repository method는 향후
  `UserRepository.findAll()`처럼 흐름으로 표시할 정책이다.
- Swing UI는 분석 로직을 직접 소유하지 않고 `AnalysisRunner`를 사용한다.
- CLI와 GUI는 같은 실행 경로를 공유해야 한다.

## 9. 내가 헷갈리면 읽을 문서

우선순위:

1. `docs/superpowers/assistant-operating-rules.ko.md`
2. `docs/superpowers/subagent-roles.md`
3. `docs/superpowers/specs/2026-06-16-spring-mvc-static-analyzer-design3.ko.md`
4. `docs/solutions/workflow-issues/subagent-role-and-test-responsibility-clarity.md`
5. `docs/solutions/architecture-patterns/shared-analysis-runner-and-policy-boundary.md`
6. `docs/usage.md`
