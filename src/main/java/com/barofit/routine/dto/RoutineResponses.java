package com.barofit.routine.dto;

import com.barofit.routine.domain.*;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class RoutineResponses {
    private RoutineResponses() {}

    @Schema(name = "ExerciseSummary")
    public record ExerciseSummary(String id, String name, MeasurementType measurementType, String description) {
        public static ExerciseSummary from(Exercise e) {
            return new ExerciseSummary(e.getId(), e.getName(), e.getMeasurementType(), e.getDescription());
        }
    }

    @Schema(name = "ExerciseDetail")
    public record ExerciseDetail(String id, String name, MeasurementType measurementType, String description,
                                 String preparation, String instructions, String precautions,
                                 @Schema(description = "운동 시작 전 TTS로 읽을 공통 안내 원문") String introTtsText) {
        public static ExerciseDetail from(Exercise e) {
            return new ExerciseDetail(e.getId(), e.getName(), e.getMeasurementType(), e.getDescription(),
                e.getPreparation(), e.getInstructions(), e.getPrecautions(), e.getIntroTtsText());
        }
    }

    @Schema(name = "RoutineDetail")
    public record Detail(UUID id, String name, String description, Difficulty difficulty, RoutineType type, UUID templateId, RoutineCategory category,
                         List<ExerciseItem> exercises, Instant createdAt, Instant updatedAt) {
        public static Detail from(Routine r) {
            return new Detail(r.getId(), r.getName(), r.getDescription(), r.getDifficulty(), r.getType(), r.getTemplateId(), r.getCategory(),
                r.getExercises().stream().map(ExerciseItem::from).toList(), r.getCreatedAt(), r.getUpdatedAt());
        }
    }

    @Schema(name = "RoutineExerciseDetail")
    public record ExerciseItem(UUID id, String exerciseId, String name, MeasurementType measurementType,
                               int order, String preparation, String instructions, String precautions,
                               String introTtsText, List<SetItem> sets) {
        public static ExerciseItem from(RoutineExercise r) {
            Exercise e = r.getExercise();
            return new ExerciseItem(r.getId(), e.getId(), e.getName(), e.getMeasurementType(), r.getExerciseOrder(),
                e.getPreparation(), e.getInstructions(), e.getPrecautions(), e.getIntroTtsText(),
                r.getSets().stream().map(SetItem::from).toList());
        }
    }

    @Schema(name = "RoutineSetDetail")
    public record SetItem(UUID id, int order, Integer targetReps, Integer targetDurationSeconds,
                          BigDecimal weightKg, int restSeconds) {
        public static SetItem from(RoutineSet s) {
            return new SetItem(s.getId(), s.getSetOrder(), s.getTargetReps(), s.getTargetDurationSeconds(),
                s.getWeightKg(), s.getRestSeconds());
        }
    }

    @Schema(name = "RoutineSummary")
    public record Summary(UUID id, String name, Difficulty difficulty, UUID templateId, RoutineCategory category, int exerciseCount, int totalSetCount,
                          Instant createdAt, Instant updatedAt) {
        public static Summary from(Routine r) {
            return new Summary(r.getId(), r.getName(), r.getDifficulty(), r.getTemplateId(), r.getCategory(), r.getExercises().size(),
                r.getExercises().stream().mapToInt(e -> e.getSets().size()).sum(), r.getCreatedAt(), r.getUpdatedAt());
        }
    }

    @Schema(name = "RoutinePage")
    public record RoutinePage(List<Summary> content, int page, int size, long totalElements, int totalPages) {}
}
