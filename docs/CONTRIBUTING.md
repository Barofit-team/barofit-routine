# CONTRIBUTING

barofit 폴리레포 7개(서비스 5개 + barofit-common + barofit-infra) 공통 개발 규칙이다.
[CLAUDE.md](../CLAUDE.md)가 요약해서 가리키는 세부 내용이 여기 있다.

## 코드 스타일

### 포맷

- Spotless(`com.diffplug.spotless`) + google-java-format으로 자동 포맷한다.
- 코드를 수정했으면 커밋 전에 `./gradlew spotlessApply`를 돌린다. 수동으로 포맷을 맞추지 않는다.
- `./gradlew spotlessCheck`가 실패하면 "완료"가 아니다.

### 네이밍

- 클래스는 PascalCase, 메서드·필드는 camelCase, 상수는 UPPER_SNAKE_CASE.
- 컨트롤러는 `*Controller`, 서비스는 `*Service`, 리포지토리는 `*Repository`, DTO는 요청·응답을
  구분해 `*Request`/`*Response`로 끝낸다.
- 예외는 `BusinessException`을 throw하고, 레포마다 자체 `ErrorCode` enum을 두되 `barofit-common`의
  `ErrorCode` 인터페이스를 구현한다 (공통 처리는 common의 `GlobalExceptionHandler`가 담당).

### 레이어드 아키텍처 (서비스 레포)

- `controller` → `service` → `repository` 한 방향으로만 호출한다. `controller`가 `repository`를
  직접 호출하지 않는다.
- `controller`는 요청·응답 변환과 검증만 한다. 비즈니스 로직은 `service`에 둔다.
- 트랜잭션 경계는 `service`에만 둔다.

## barofit-common 사용 규칙

- `ErrorCode`/`BusinessException`/`GlobalExceptionHandler`(공통 에러 처리)와 `SecurityConfig`/
  `ClockConfig`(런타임 설정)는 `barofit-common`의 것을 그대로 쓴다. 레포마다 새로 만들거나 재정의하지
  않는다.
- `barofit-common`을 수정했으면 `./gradlew publishToMavenLocal`로 반영한 뒤 의존하는 레포에서 다시
  빌드해 확인한다.

## 커밋 규칙

형식:

```
<type>(#<이슈번호>): <한 줄 설명>
```

이슈가 없으면 `<type>: <한 줄 설명>`.

| type | 의미 |
|---|---|
| `feat` | 새로운 기능 개발 |
| `fix` | 버그 등의 수정 |
| `chore` | 프로젝트 기본 구조 및 설정 관련 |
| `docs` | 문서 수정 |
| `refactor` | 기능 수정 없는 코드 리팩토링 |

- 설명은 한글로, 무엇을 했는지 한 줄로 쓴다.
- 여러 성격이 섞여 있으면 비중이 더 큰 쪽의 type을 쓴다.
- 작업의 큰 범주(기능 단위)마다 이슈를 먼저 만들고, 그 이슈에 연결된 브랜치로 작업한다.

## PR 규칙

- `.github/PULL_REQUEST_TEMPLATE.md`를 그대로 쓴다. 템플릿 내용을 CLAUDE.md나 다른 문서에 중복해서
  적지 않는다.
- PR 올리기 전에 [docs/TESTING.md](TESTING.md)의 "완료 기준"을 통과했는지 확인한다.
