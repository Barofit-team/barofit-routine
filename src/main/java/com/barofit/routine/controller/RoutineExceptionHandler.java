package com.barofit.routine.controller;
import com.barofit.common.error.ProblemDetails;
import com.barofit.routine.application.RoutineErrorCode;
import org.springframework.core.annotation.Order;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
/** 동시 수정 오류만 처리하고 나머지는 공통 핸들러에 위임한다. */
@Order(-1)
@RestControllerAdvice(basePackageClasses = RoutineController.class)
public class RoutineExceptionHandler {
    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ProblemDetail> conflict(OptimisticLockingFailureException exception) {
        return ResponseEntity.status(409).body(ProblemDetails.of(RoutineErrorCode.ROUTINE_CONFLICT));
    }
}
