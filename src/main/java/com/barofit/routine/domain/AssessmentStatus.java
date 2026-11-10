package com.barofit.routine.domain;

import io.swagger.v3.oas.annotations.media.Schema;

/** 온디바이스 자세 판정 모듈의 1차 준비 상태. READY는 서비스 최종 검증을 뜻하지 않는다. */
@Schema(description = "자세 판정 로직의 준비 상태. READY는 판정 로직이 준비된 상태이며 앱 연동이나 실기기 검증 완료를 뜻하지 않습니다.")
public enum AssessmentStatus {
  READY,
  IN_PROGRESS,
  PLANNED
}
