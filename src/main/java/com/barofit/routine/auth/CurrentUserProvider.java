package com.barofit.routine.auth;

import java.util.UUID;

/** 인증 모듈이 제공하는 내부 사용자 ID와 Routine 도메인의 연결 지점. */
public interface CurrentUserProvider {
    UUID userId();
}
