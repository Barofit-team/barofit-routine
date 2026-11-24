package com.barofit.routine.admin;

import com.barofit.common.error.ErrorCode;
import org.springframework.http.HttpStatus;

public enum AdminErrorCode implements ErrorCode {
  ADMIN_ACCESS_DENIED;

  public String code() {
    return name();
  }

  public String message() {
    return "활성 관리자 계정만 접근할 수 있습니다.";
  }

  public HttpStatus status() {
    return HttpStatus.FORBIDDEN;
  }
}
