# Assistant 운영 규칙

Codex가 규칙을 잊었을 때 이 문서를 먼저 읽는다.

## 1. 작업 시작 전

- 관련 plugin/skill을 먼저 확인하고 읽는다.
- Superpowers 가능성이 있으면 `superpowers:using-superpowers`부터 적용한다.
- 구현 작업은 새 작업 브랜치에서 한다. `main`에서 직접 구현하지 않는다.
- 사용자가 요청하지 않으면 remote push는 하지 않는다.

## 2. Plugin / Skill

- Superpowers: 계획, TDD, 디버깅, 검증, 브랜치 마무리에 필수 적용.
- Ponytail: 리팩토링/단순화 작업에서 최소 변경 원칙 적용.
- Caveman: 사용자 또는 규칙이 요청하면 간결하게 말한다.
- 플러그인이 미적용처럼 보이면 skill 목록 확인 → `SKILL.md` 읽기 → 안 되면 사용자에게 알림.

## 3. Subagent

첫 통신에만 반드시 아래 형식으로 시작한다.

```text
/caveman lite
Role: <Implementer | Documentation Implementer | Spec Compliance Reviewer | Code Quality Reviewer | Combined Spec/Quality Reviewer | Test Verifier | Final Reviewer>
Task: <task 이름>
```

규칙:

- 이후 같은 subagent에게 추가 지시할 때는 `/caveman lite`와 역할을 반복하지 않는다.
- task 내용, 관련 파일, 성공 기준, 검증 명령을 명시한다.
- 구현 agent는 TDD 때문에 task-specific test를 실행한다.
- Test Verifier는 독립 검증 담당이다.
- task가 끝나면 관련 subagent를 모두 종료한다.
- subagent는 push하지 않는다.

Coordinator 규칙:

- main assistant/coordinator 역할은 subagent에게 지시를 보내고, 보고를 받고, 사용자에게 보고하는 것뿐이다.
- 각 task 시작 시 필요한 최소 subagent 수를 계산하고 subagent 수를 최적화한다.
- 코드 검정, diff review, Ponytail review, test verification, 문서 변경은 직접 반복하지 않고 subagent에게 지시한다.
- Coordinator는 subagent 보고를 읽는다.
- 기본적으로 필요한 경우가 아니면 subagent를 만들지 않는다. 단, 문서 생성, 수정, 삭제는 항상 최소 1명의 documentation subagent가 맡는다.
- subagent는 코드 구현, 문서 작업, 독립 검증, 명시된 review gate, compound 문서, 사용자 요청에만 만든다.
- 같은 목적의 중복 agent를 만들지 않는다.
- 이미 해당 task를 맡은 subagent가 있으면 다른 agent를 만들기 전에 그 subagent에게 수정을 요청한다.
- reviewer/test verifier 추가 호출은 보고서에 구체적인 미해결 위험이 있을 때만 한다.
- 문서 생성, 수정, 변경은 subagent에게 맡긴다.
- Coordinator는 문서를 직접 편집하지 않는다.
- Coordinator가 직접 하는 일은 작업 지시, 보고 검토, 사용자 보고, 최소 git 상태 확인으로 제한한다.

최소 subagent matrix:

- 사소한 문서 typo/path wording: documentation subagent 1명.
- 작은 code diff: Implementer 1명; risk가 없으면 최종 branch Test Verifier만 추가.
- report output, CLI, GUI, analyzer policy 변경: Implementer + Combined Spec/Quality Reviewer + Test Verifier.
- 여러 파일 behavior 또는 보호된 boundary: bounded task마다 Implementer 1명 + 필요한 reviewer + 최종 Test Verifier.
- branch 완료: Final Reviewer + Test Verifier.
- Compound: 재사용 가능한 교훈이 있을 때만 documentation subagent 1명.

## 4. Compound

리뷰/검증/실패에서 배운 내용은 마지막 단계에 subagent에게 맡겨 compound 문서로 남긴다.

대상:

- CI/test/build 실패 해결
- subagent 역할 혼동
- 검증 방식 변경
- 아키텍처 경계 또는 정책 결정
- 반복될 수 있는 실수

위치:

- workflow: `docs/solutions/workflow-issues/`
- test/build: `docs/solutions/test-failures/`, `docs/solutions/build-errors/`
- architecture: `docs/solutions/architecture-patterns/`

compound frontmatter 검증은 subagent에게 맡긴다.

## 5. Task 완료 후 Main Merge

모든 task가 끝나면:

```bash
./gradlew test
git switch main
git merge --ff-only <feature-branch>
./gradlew test
git branch -d <feature-branch>
```

규칙:

- merge는 local main까지만 한다.
- push는 사용자가 한다.
- merge 후 main에서 다시 테스트한다.

## 6. 검증

완료를 말하기 전에 직접 실행 결과를 확인한다.

기본:

```bash
./gradlew test
git diff --check
```

fixture 필요 시:

```bash
./gradlew run --args="src/test/resources/fixtures/simple-spring-mvc -o build/dev3-result.txt"
```

확인 항목:

- `UserController.createUser()` 있음
- `unsupported: externalClient.send()` 있음
- `unsupported: Thread.sleep()` 없음
- `unsupported: model.addAttribute()` 없음

## 7. 프로젝트 핵심 정책

- 목표는 완벽한 Java 분석기가 아니라 읽기 쉬운 Spring MVC 흐름 리포트다.
- 가독성이 분석 정확도보다 우선일 수 있다.
- Controller noise는 숨긴다.
- Service/Repository 내부 unsupported 호출은 유지한다.
- Spring Data JPA inherited repository method는 향후 `UserRepository.findAll()`처럼 표시한다.
- CLI와 GUI는 `AnalysisRunner` 실행 경로를 공유한다.

## 8. 같이 읽을 문서

1. `docs/superpowers/subagent-roles.md`
2. `docs/superpowers/specs/2026-06-16-spring-mvc-static-analyzer-design3.ko.md`
3. `docs/solutions/workflow-issues/subagent-role-and-test-responsibility-clarity.md`
4. `docs/solutions/architecture-patterns/shared-analysis-runner-and-policy-boundary.md`
5. `docs/usage.md`
