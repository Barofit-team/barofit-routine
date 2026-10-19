package com.barofit.routine.application;
import com.barofit.common.error.BusinessException;
public class RoutineException extends BusinessException {
    public RoutineException(RoutineErrorCode code) { super(code); }
    public RoutineException(RoutineErrorCode code, String detail) { super(code, detail); }
    public static RoutineException invalid(String detail) {
        return new RoutineException(RoutineErrorCode.INVALID_ROUTINE, detail);
    }
    public static RoutineException notFound() {
        return new RoutineException(RoutineErrorCode.ROUTINE_NOT_FOUND);
    }
}
