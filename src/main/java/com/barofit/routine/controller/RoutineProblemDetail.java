package com.barofit.routine.controller;
import io.swagger.v3.oas.annotations.media.Schema;
import java.net.URI;
import java.util.List;
/** 공통 Problem Details 응답의 OpenAPI 표현. */
@Schema(description = "RFC 9457 Problem Details + code. 프론트는 code로 분기하고 detail을 표시합니다.")
public record RoutineProblemDetail(URI type, String title, int status, String detail, URI instance,
                                   String code, List<FieldError> errors) {
    public record FieldError(String field, String message) {}
}
