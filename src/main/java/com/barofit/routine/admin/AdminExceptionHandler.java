package com.barofit.routine.admin;

import com.barofit.common.error.ProblemDetails;
import org.springframework.core.annotation.Order;
import org.springframework.http.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;

@Order(-2)
@RestControllerAdvice(assignableTypes = AdminCatalogController.class)
public class AdminExceptionHandler {
  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<ProblemDetail> forbidden(AccessDeniedException exception) {
    return ResponseEntity.status(403).body(ProblemDetails.of(AdminErrorCode.ADMIN_ACCESS_DENIED));
  }

  @ExceptionHandler(org.springframework.dao.OptimisticLockingFailureException.class)
  public ResponseEntity<ProblemDetail> conflict(Exception exception) {
    return ResponseEntity.status(409)
        .body(ProblemDetails.of(com.barofit.routine.application.RoutineErrorCode.ROUTINE_CONFLICT));
  }
}
