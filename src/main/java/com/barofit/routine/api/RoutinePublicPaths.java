package com.barofit.routine.api;
/** 인증 없이 조회할 수 있는 GET 경로. */
public final class RoutinePublicPaths {
    public static final String[] GET = {
        "/api/v1/exercises", "/api/v1/exercises/*", "/api/v1/routine-templates"
    };
    private RoutinePublicPaths() {}
}
