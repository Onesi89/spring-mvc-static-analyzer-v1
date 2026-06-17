# Spring MVC 정적 분석기 1 개발 2차 설계

## 개발 2차 정의

개발 2차는 MVP CLI 분석기를 Windows 사용자가 실제로 내려받아 실행할 수 있는 배포물로 다듬고, 실사용 과정에서 드러난 리포트 노이즈와 실행 편의성 문제를 개선하는 단계이다.

이 단계의 중심 목표는 분석 정확도를 무리하게 확장하는 것이 아니다. 사용자가 Windows 환경에서 Java 설치나 복잡한 명령어 없이 분석기를 실행하고, 결과와 실행 상태를 더 쉽게 확인하도록 만드는 것이다.

## 목표

- GitHub Actions에서 Windows 사용자를 위한 빌드 산출물을 제공한다.
- Java를 별도로 설치하지 않아도 실행 가능한 runtime 포함 ZIP을 제공한다.
- OS 차이 때문에 테스트와 리포트가 흔들리지 않도록 출력 포맷을 안정화한다.
- Controller 내부의 프레임워크/유틸리티 호출 노이즈를 줄인다.
- `spring-mvc-static-analyzer-v1.exe`를 더블 클릭하면 Swing UI가 뜨도록 한다.
- 기존 CLI 사용법은 유지한다.

## 범위

### 포함

- GitHub Actions artifact 업로드
- Windows `.bat` 포함 Gradle distribution ZIP
- `jpackage` 기반 Windows runtime 포함 ZIP
- runtime ZIP 검증 단계 간소화
- 텍스트 리포트 줄바꿈을 LF로 고정
- Controller의 unsupported 노이즈 호출 억제
- Swing 기반 GUI 추가
- 인자 없음 실행은 GUI, 인자 있음 실행은 CLI
- 사용 문서 보강
- 문제 해결 과정 compound 문서화

### 제외

- JavaFX 기반 UI
- Electron 또는 웹 기반 UI
- 설치형 Windows installer
- 코드 서명
- 아이콘/브랜딩 고도화
- GitHub URL 직접 clone 기능
- async/lambda/anonymous class 정밀 분석
- Service/Repository가 없는 Controller에 대한 별도 요약 문구

## 아키텍처 변경

### 실행 모드 분리

기존 `Main`은 항상 Picocli CLI를 실행했다. 개발 2차에서는 `AppLauncher`를 추가하여 실행 모드를 분리한다.

규칙:

- 인자가 없으면 GUI 모드
- 인자가 있으면 기존 CLI 모드

```text
spring-mvc-static-analyzer-v1.exe
→ Swing UI 실행

spring-mvc-static-analyzer-v1.exe C:\workspace\legacy-project -o result.txt
→ 기존 CLI 분석 실행
```

이 방식은 Windows 사용자의 더블 클릭 실행을 지원하면서도 자동화 스크립트와 기존 CLI 사용자를 깨지 않기 위한 절충안이다.

### Swing UI

Swing UI는 새 외부 라이브러리를 추가하지 않고 Java 17 표준 라이브러리만 사용한다.

주요 컴포넌트:

- `AnalyzerGui`
  - 대상 프로젝트 폴더 선택
  - 결과 txt 파일 선택
  - 분석 실행 버튼
  - 실행 로그 영역
  - 실행 중 버튼 비활성화
  - 오류 메시지 표시
- `AppLauncher`
  - GUI/CLI 실행 모드 선택
- 기존 `Analyzer`
  - 분석 로직 재사용
- 기존 `TextReportWriter`
  - 결과 파일 생성 재사용

GUI는 분석 로직을 새로 구현하지 않는다. 기존 core 분석 파이프라인을 호출하고 결과만 화면 로그와 파일로 연결한다.

## Windows 배포 설계

### `.bat` distribution ZIP

Gradle `application` 플러그인의 `distZip` 산출물을 GitHub Actions artifact로 업로드한다.

특징:

- `.bat` 실행 파일 포함
- 사용자 PC에 Java 17 필요
- 구조가 단순하고 기존 Gradle distribution과 호환됨

artifact:

```text
spring-mvc-static-analyzer-windows-bat-zip
```

### runtime 포함 Windows ZIP

Windows 사용자가 Java를 따로 설치하지 않도록 `jpackage` app image를 ZIP으로 묶는다.

Gradle task:

- `windowsRuntimeImage`
- `windowsRuntimeZip`

artifact:

```text
spring-mvc-static-analyzer-windows-runtime-zip
```

사용자는 ZIP 압축 해제 후 `.exe`를 실행한다.

```text
spring-mvc-static-analyzer-v1.exe
```

## GitHub Actions 설계

`Build` workflow는 두 종류의 Windows 산출물을 만든다.

### Windows .bat distribution job

runner:

```text
ubuntu-latest
```

역할:

- 테스트 실행
- `distZip` 생성
- ZIP 내부 `.bat` 존재 확인
- `.bat` distribution artifact 업로드
- 샘플 분석 결과 artifact 업로드

### Windows runtime distribution job

runner:

```text
windows-latest
```

역할:

- 테스트 실행
- `windowsRuntimeZip` 생성
- ZIP 파일 존재 확인
- ZIP 내부 application `.exe` 존재 확인
- runtime 포함 ZIP artifact 업로드

검증은 의도적으로 간소화한다. `jpackage` 내부 runtime 파일 경로, 예를 들어 `runtime/bin/java.exe`, 는 검증하지 않는다. 사용자가 의존하는 표면은 ZIP과 application `.exe`이기 때문이다.

## 리포트 출력 안정화

Windows runner에서 `System.lineSeparator()` 때문에 테스트가 실패했다.

원인:

- Linux는 LF
- Windows는 CRLF
- Java text block 기반 기대값은 LF
- 리포트 문자열 비교가 OS에 따라 달라짐

설계 결정:

- 리포트 출력 줄바꿈은 LF `\n`로 고정한다.
- persisted txt 리포트는 OS와 무관하게 동일한 형식을 갖는다.

적용 대상:

- `AnalysisWarning.format()`
- `TextReportWriter`

## Controller 노이즈 호출 억제

실제 Spring MVC 예제인 `spring-mvc-showcase`를 돌리면서 Controller 내부의 프레임워크 호출이 `unsupported`로 과도하게 출력되는 문제가 확인되었다.

예:

```text
├─ unsupported: Thread.sleep()
├─ unsupported: model.addAttribute()
└─ unsupported: model.addAttribute()
```

개발 2차의 판단:

- Controller에서 Service/Repository로 이어지지 않는 호출은 호출 흐름 이해에 도움이 적다.
- 특히 MVC showcase, async `Callable`, `Model` 조작, logging, framework utility 호출은 리포트 노이즈가 되기 쉽다.
- Controller에서 해석되지 않은 `unsupported` 호출은 출력하지 않는다.
- 단, Service/Repository 내부의 unsupported 호출은 유지한다.

이 결정으로 `CallableController.callableWithView()`처럼 downstream application layer가 없는 경우 출력은 Controller method에서 끝난다.

## GUI UX 설계

초기 GUI는 기능을 과하게 늘리지 않고 실행 편의성에 집중한다.

화면 구성:

- 대상 프로젝트 경로
- 대상 프로젝트 선택 버튼
- 결과 파일 경로
- 결과 파일 선택 버튼
- 분석 실행 버튼
- 실행 로그 영역

로그 예:

```text
[21:30:10] Ready.
[21:30:21] Target project selected: C:\workspace\legacy-project
[21:30:30] Result file selected: C:\workspace\result.txt
[21:30:35] Analysis started.
[21:30:35] Target project: C:\workspace\legacy-project
[21:30:35] Result file: C:\workspace\result.txt
[21:30:37] Analysis completed.
[21:30:37] Report written: C:\workspace\result.txt
```

오류 처리:

- 대상 폴더가 선택되지 않으면 dialog와 로그에 오류 표시
- 결과 파일이 선택되지 않으면 dialog와 로그에 오류 표시
- 대상 경로가 디렉터리가 아니면 오류 표시
- Java 파일이 없거나 input-error warning이 있으면 분석 중단 로그 표시
- 예외 발생 시 로그에 `ERROR` 표시

## 테스트 전략

### 기존 테스트 유지

개발 2차는 기존 CLI 분석 기능을 깨지 않아야 한다.

필수 검증:

```bash
./gradlew test
```

### 실행 모드 테스트

`AppLauncherTest`로 다음을 검증한다.

- 인자 없음 실행은 GUI launcher를 호출한다.
- 인자 있음 실행은 CLI로 전달된다.
- CLI 모드에서는 GUI가 실행되지 않는다.

Swing 화면 자체는 headless CI에서 직접 표시하지 않는다. 대신 launcher 분기와 core 분석 재사용을 테스트한다.

### 배포 검증

로컬에서 가능한 검증:

```bash
./gradlew distZip
./gradlew run --args="src/test/resources/fixtures/simple-spring-mvc -o build/result.txt"
```

Windows runtime ZIP은 `windows-latest` GitHub Actions에서 최종 검증한다.

## 오류 처리 및 사용자 안내

개발 2차 이후 사용자는 두 방식 중 하나로 실행한다.

GUI:

```text
spring-mvc-static-analyzer-v1.exe 더블 클릭
```

CLI:

```bat
spring-mvc-static-analyzer-v1.exe C:\workspace\legacy-project -o C:\workspace\result.txt
```

GitHub URL 직접 입력은 아직 지원하지 않는다. 사용자는 먼저 repository를 clone한 뒤 로컬 경로를 선택해야 한다.

```bat
git clone https://github.com/spring-attic/spring-mvc-showcase.git C:\workspace\spring-mvc-showcase
```

## 문서화 및 지식 축적

개발 2차에서는 CI와 Windows 배포 과정에서 반복 가능한 교훈이 생겼다. 이를 compound 문서로 남긴다.

작성된 문서:

- `docs/solutions/test-failures/windows-runner-report-line-ending-test-failure.md`
- `docs/solutions/workflow-issues/simplify-ci-artifact-verification.md`

핵심 교훈:

- OS별 줄바꿈 차이는 golden text 테스트를 깨뜨릴 수 있다.
- persisted report 출력은 canonical line ending을 정하는 편이 안전하다.
- CI artifact 검증은 사용자-facing contract 중심으로 간소화해야 한다.
- packaging tool 내부 파일 구조를 과검증하면 false negative가 생긴다.

## 향후 확장 후보

- GitHub URL 입력 시 clone 안내 또는 자동 clone 지원
- Controller entry point를 request mapping annotation 기준으로 제한
- Service/Repository 호출이 없는 Controller에 `downstream call 없음` 표시
- Swing UI에서 결과 파일 열기 버튼 제공
- Swing UI에서 최근 입력 경로 기억
- Windows installer 생성
- application icon 추가
- UI 로그를 별도 파일로 저장
- async/lambda/anonymous class 미지원 패턴을 더 명시적으로 요약
