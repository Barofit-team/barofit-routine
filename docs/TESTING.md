# TESTING

barofit 폴리레포 공통 테스트 가이드다. **CI가 아니다** — 로컬·에이전트가 직접 실행하고 통과를 확인하는
것이 전제다. git hook으로 강제되지 않는다.

## 공통 원칙

- 신규·수정 코드에는 단위 테스트를 동반한다. 수치 커버리지 목표는 두지 않는다 — "이 변경의 핵심
  분기·예외 케이스가 검증되는가"를 기준으로 판단한다.
- 테스트가 없는 변경을 "완료"로 보고하지 않는다. 테스트를 추가하거나, 추가가 어려운 사정이 있으면 그
  사실을 명시한다.

## 서비스 레포 (identity / routine / workout / analytics / report)

### 소스셋

- `src/test`: 단위 테스트. Mockito로 의존성을 모킹한다. 빠르게 돈다 — Docker 불필요.
- `src/integrationTest`: 이 서비스 자신의 컴포넌트 테스트. `@SpringBootTest` + Testcontainers로
  실제 DB를 띄운다. Docker 필요.
- 실행: `./gradlew test`(단위), `./gradlew integrationTest`(통합).

### barofit-common 공통 test-fixtures

- Testcontainers 베이스 클래스, 픽스처 빌더는 `barofit-common`의 test-fixtures를 재사용한다.
  서비스마다 새로 만들지 않는다.

### 완료 기준 (서비스 레포 단독 변경 시)

- `./gradlew test` + `./gradlew integrationTest` + `./gradlew spotlessCheck` 통과.
- `barofit-infra`의 전체 스택 E2E는 의무 아님 (아래 "언제 infra E2E까지 필요한가" 참고).

## barofit-common

- 퍼블릭 API를 바꾸면 5개 서비스 레포 전체에 영향 간다. 하위호환이 깨지는 변경인지 먼저 검토한다.
- `./gradlew test`로 모듈 자체 테스트를 통과시키고, `./gradlew publishToMavenLocal`로 반영한 뒤
  **영향받는 서비스 레포 최소 1곳**에서 재빌드까지 확인한다.

## barofit-infra (integration-tests)

- 게이트웨이를 경유하는 HTTP 클라이언트로, minikube에 배포된 환경 전체에 대한 E2E만 다룬다. 서비스
  자신의 로직 검증(단위·통합)은 여기서 하지 않는다 — 그건 각 서비스 레포의 책임이다.
- 실행 전 전제: minikube + umbrella 차트로 전체 스택이 떠 있어야 한다 (`infra/README.md` 참고).
- 실행: `./gradlew test`(integration-tests 모듈).

## 언제 infra E2E까지 필요한가

서비스 레포 하나만 변경했다면 그 레포의 테스트까지만 의무다. 아래 경우에만 `barofit-infra`의 E2E까지
추가로 통과시킨다:

1. `barofit-infra` 자체(Helm 차트, 스크립트)를 변경했을 때
2. 여러 레포에 걸친 변경일 때 (예: barofit-common + 서비스 레포 동시 변경)
3. 사용자가 명시적으로 요청했을 때

## 기존 테스트 공백 (별도 작업으로 메울 예정)

아래는 이 가이드를 배포하는 시점(2026-11)에는 포함하지 않고, 이후 실제 테스트 코드 작성 작업에서
메울 예정이다:

- `barofit-routine`: 단위 테스트 0개
- `barofit-workout` / `barofit-analytics` / `barofit-report`: 자체 통합 테스트(컴포넌트 테스트) 없음
- `barofit-infra/integration-tests`: identity·routine만 다루고 있고, HTTP 기반 E2E로 아직 전환되지
  않음
