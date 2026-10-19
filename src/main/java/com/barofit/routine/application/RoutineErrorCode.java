package com.barofit.routine.application;
import com.barofit.common.error.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import org.springframework.http.HttpStatus;
@Getter
@Accessors(fluent = true)
@RequiredArgsConstructor
public enum RoutineErrorCode implements ErrorCode {
    TEMPLATE_NOT_FOUND(HttpStatus.NOT_FOUND, "기본 루틴 템플릿을 찾을 수 없습니다."),
    INVALID_ROUTINE(HttpStatus.BAD_REQUEST, "루틴 설정을 확인해 주세요."),
    EXERCISE_NOT_FOUND(HttpStatus.NOT_FOUND, "운동 종목을 찾을 수 없습니다."),
    ROUTINE_NOT_FOUND(HttpStatus.NOT_FOUND, "루틴을 찾을 수 없습니다."),
    AUTHENTICATION_REQUIRED(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다."),
    ROUTINE_CONFLICT(HttpStatus.CONFLICT, "루틴이 변경되었습니다. 다시 조회해 주세요.");
    private final HttpStatus status;
    private final String message;
    @Override public String code() { return name(); }
}
