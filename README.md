# barofit-routine

**barofit** 백엔드의 루틴 서비스 레포입니다. 원래 `backend_baro` 모노레포의
`services/routine` 모듈을 폴리레포 전환 과정에서 분리했습니다(커밋 이력 보존).

운동 카탈로그·가이드/TTS 원문, 난이도별 템플릿, 템플릿 기반 사용자 루틴 CRUD, 온보딩 기반 추천,
날짜별 주간 운동 계획을 제공합니다.

## 기술 스택

- Java 21 / Gradle (단일 모듈, 독립 레포)
- Spring Boot 4.1 (Web MVC, Data JPA)
- PostgreSQL `routine` 스키마, Flyway

## 의존성

[barofit-common](https://github.com/Barofit-team/barofit-common)의 `barofit-runtime`을 참조합니다.
먼저 그 레포에서 `./gradlew publishToMavenLocal`을 실행해둬야 이 레포가 빌드됩니다.

인증은 [barofit-identity](https://github.com/Barofit-team/barofit-identity)가 발급한 JWT를
공개키(JWKS)로 검증하며, identity 서비스와 직접 통신하지 않습니다. `IDENTITY_BASE_URL`,
`WORKOUT_BASE_URL` 환경변수로 다른 서비스 호출(루틴 추천/주간 계획 산정 등)을 합니다.

## 로컬 실행

```bash
cp .env.example .env   # 레포에 .env.example이 없다면 barofit-infra의 것을 참고
./gradlew bootRun       # 기본 포트 3005
```

## 테스트 / 빌드

```bash
./gradlew test
./gradlew build   # build/libs/app.jar
```

## Docker 이미지

```bash
DOCKERHUB_USER=<hub-user> ./scripts/build-and-push.sh
```

## 배포

로컬 minikube 배포는 [barofit-infra](https://github.com/Barofit-team/barofit-infra)의
`infra/charts/routine` Helm 차트로 합니다.

## API 요약

- `GET /api/v1/exercises`, `/api/v1/exercises/{id}` — 운동 카탈로그(공개)
- `GET /api/v1/routine-templates` — 난이도별 템플릿(공개)
- `GET/POST/PATCH/DELETE /api/v1/routines` — 사용자 루틴 CRUD (JWT 필요)
- `GET/POST /api/v1/workout-plans/*` — 주간 운동 계획 (JWT 필요)
- Swagger UI: `/swagger-ui.html`, OpenAPI: `/v3/api-docs`

## 관련 레포

- [barofit-common](https://github.com/Barofit-team/barofit-common) — 공유 라이브러리(이 레포가 의존)
- [barofit-identity](https://github.com/Barofit-team/barofit-identity) · [barofit-workout](https://github.com/Barofit-team/barofit-workout) · [barofit-analytics](https://github.com/Barofit-team/barofit-analytics) · [barofit-report](https://github.com/Barofit-team/barofit-report)
- [barofit-infra](https://github.com/Barofit-team/barofit-infra) — Helm 차트, `integration-tests`(이 서비스를 참조), 로컬 검증 스크립트
- [backend_baro](https://github.com/Barofit-team/backend_baro) — 분리 이전 모노레포(참고용)
