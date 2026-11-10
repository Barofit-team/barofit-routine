# CLAUDE.md

이 파일은 Claude Code(및 다른 AI 코딩 에이전트)가 이 레포에서 작업할 때 따라야 할 규칙을 정의한다.
세부 내용은 [docs/CONTRIBUTING.md](docs/CONTRIBUTING.md)(코드 스타일·커밋 규칙)와
[docs/TESTING.md](docs/TESTING.md)(테스트 작성·실행 가이드)를 참고한다.

## 이 레포는 폴리레포의 일부다

`barofit-identity`, `barofit-routine`, `barofit-workout`, `barofit-analytics`, `barofit-report`(서비스 5개)
+ `barofit-common`(공통 라이브러리) + `barofit-infra`(Helm 차트·통합 테스트)로 분리된 7개 레포 중 하나다.
각 레포는 독립적으로 빌드·배포되지만, 아래 규칙은 7개 레포 공통으로 적용된다.

## 작업 시작 전 선행조건

- `barofit-common`을 수정했다면: 그 레포에서 `./gradlew publishToMavenLocal`을 먼저 실행해 mavenLocal에
  반영한 뒤 이 레포 작업을 시작한다.
- `.env`, `keys/jwt-*.pem`, `keys/jwt-jwks.json`이 필요한 작업(인증·시크릿 관련)이라면 로컬에 준비돼
  있는지 먼저 확인한다.

## 코드 스타일

- 레이어드 아키텍처, 네이밍 컨벤션은 docs/CONTRIBUTING.md를 따른다.
- 포맷은 Spotless(google-java-format)로 강제한다. 수동으로 들여쓰기·import 순서를 맞추지 않는다 —
  `./gradlew spotlessApply`로 교정한다.

## barofit-common 사용 규칙

- `ErrorCode`/`BusinessException`/`GlobalExceptionHandler`는 재사용한다. 레포마다 새로 만들지 않는다.
- `SecurityConfig`를 이 레포에서 재정의하지 않는다 — 필요하면 `barofit-common`에서 확장 지점을 추가한다.

## 테스트와 완료 기준

작업을 "완료"로 보고하기 전에 반드시 아래를 통과시킨다:

- 이 레포의 `./gradlew test`(단위 테스트) + `./gradlew integrationTest`(해당 레포에 있다면)
- `./gradlew spotlessCheck`
- `barofit-infra`의 전체 스택 E2E(minikube)는 **다음 경우에만** 추가로 의무:
  - `barofit-infra` 자체(Helm 차트·스크립트)를 변경했을 때
  - 여러 레포에 걸친 변경일 때
  - 사용자가 명시적으로 요청했을 때
- 신규·수정 코드에는 단위 테스트를 동반한다 (수치 커버리지 목표는 없음 — docs/TESTING.md의 질적 기준을
  따른다).

위 테스트는 CI가 아니라 로컬·에이전트가 직접 실행하는 것이다. git hook으로 강제되지 않으므로, 에이전트가
스스로 책임지고 실행·확인한다.

## 커밋 / PR

- 커밋 메시지, 이슈 연결 규칙은 docs/CONTRIBUTING.md를 따른다.
- PR은 `.github/PULL_REQUEST_TEMPLATE.md`를 그대로 사용한다 (중복 서술 안 함).

---
## 이 레포(서비스) 추가 규칙

- 레이어: `controller` → `service` → `repository` 순서로만 호출한다. `controller`에서 `repository`를
  직접 호출하지 않는다.
- 통합 테스트는 이 레포 자신에 둔다 (`src/integrationTest`, Testcontainers 기반). `barofit-infra`의
  통합 테스트와는 역할이 다르다 — 거긴 배포된 환경에 대한 전체 E2E만 다룬다.
- `barofit-common`의 test-fixtures(Testcontainers 베이스 클래스)를 재사용한다. 직접 Testcontainers
  설정을 새로 만들지 않는다.
