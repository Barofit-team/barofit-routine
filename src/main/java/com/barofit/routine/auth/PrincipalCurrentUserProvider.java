package com.barofit.routine.auth;

import com.barofit.routine.application.RoutineErrorCode;
import com.barofit.routine.application.RoutineException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PrincipalCurrentUserProvider implements CurrentUserProvider {
  private final HttpServletRequest request;

  @Override
  public UUID userId() {
    var principal = request.getUserPrincipal();
    if (principal == null) throw unauthorized();
    try {
      return UUID.fromString(principal.getName());
    } catch (IllegalArgumentException exception) {
      throw unauthorized();
    }
  }

  static RoutineException unauthorized() {
    return new RoutineException(RoutineErrorCode.AUTHENTICATION_REQUIRED);
  }
}
