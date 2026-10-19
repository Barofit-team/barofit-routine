package com.barofit.routine.dto;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import tools.jackson.databind.annotation.JsonDeserialize;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;
import java.util.UUID;

public final class RoutineRequests {
    private RoutineRequests() {}

    public interface Goals {
        Integer setCount();
        Integer targetReps();
        Integer targetDurationSeconds();
    }

    @Schema(name = "CreateRoutineRequest", description = "템플릿에서 생성. exercises 생략/null/빈 배열이면 기본 구성 그대로 저장. 미지정 종목은 유지.")
    public record Create(
        @NotNull @Schema(example = "00000000-0000-0000-0000-000000000001") UUID templateId,
        @Size(max = 20) List<@NotNull @Valid CreateExercise> exercises
    ) {
        @JsonAnySetter public void unknown(String field, Object value) { reject(field); }
    }

    @Schema(name = "TemplateExerciseCustomization", description = "템플릿 상세의 exercises[].id 사용. 지정한 목표는 해당 종목의 모든 세트에 동일하게 적용. 생략/null인 목표는 유지.")
    public record CreateExercise(
        @NotNull UUID templateExerciseId,
        @JsonDeserialize(using = StrictIntegerDeserializer.class) @Min(1) @Max(20) Integer setCount,
        @JsonDeserialize(using = StrictIntegerDeserializer.class) @Positive @Schema(description = "반복 기반 운동에만 사용") Integer targetReps,
        @JsonDeserialize(using = StrictIntegerDeserializer.class) @Positive @Schema(description = "시간 기반 운동에만 사용. 초 단위") Integer targetDurationSeconds
    ) implements Goals {
        @JsonAnySetter public void unknown(String field, Object value) { reject(field); }
    }

    @Schema(name = "UpdateRoutineRequest", description = "지정한 종목의 세트 수·목표만 부분 수정. 미지정 종목은 유지. 종목 추가·삭제·교체·순서 변경 불가.")
    public record Update(
        @NotEmpty @Size(max = 20) List<@NotNull @Valid UpdateExercise> exercises
    ) {
        @JsonAnySetter public void unknown(String field, Object value) { reject(field); }
    }

    @Schema(name = "RoutineExerciseCustomization", description = "내 루틴 상세의 exercises[].id 사용. 목표는 모든 세트에 동일하게 적용. 생략/null인 값은 유지.")
    public record UpdateExercise(
        @NotNull UUID routineExerciseId,
        @JsonDeserialize(using = StrictIntegerDeserializer.class) @Min(1) @Max(20) Integer setCount,
        @JsonDeserialize(using = StrictIntegerDeserializer.class) @Positive Integer targetReps,
        @JsonDeserialize(using = StrictIntegerDeserializer.class) @Positive Integer targetDurationSeconds
    ) implements Goals {
        @JsonAnySetter public void unknown(String field, Object value) { reject(field); }
    }

    private static void reject(String field) {
        throw new IllegalArgumentException("허용하지 않는 필드입니다: " + field);
    }
}
