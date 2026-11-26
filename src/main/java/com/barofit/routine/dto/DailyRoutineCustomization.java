package com.barofit.routine.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record DailyRoutineCustomization(
    @NotNull UUID sourceRoutineId,
    @NotNull Instant sourceUpdatedAt,
    UUID expectedRoutineId,
    @tools.jackson.databind.annotation.JsonDeserialize(using = StrictIntegerDeserializer.class)
        @NotNull
        @Min(0)
        @Max(600)
        Integer interExerciseRestSeconds,
    @NotEmpty @Size(max = 20) List<@NotNull @Valid ExerciseEdit> exercises) {
  public record ExerciseEdit(
      @NotNull UUID sourceRoutineExerciseId,
      @NotEmpty @Size(max = 20) List<@NotNull @Valid SetEdit> sets) {}

  public record SetEdit(
      @tools.jackson.databind.annotation.JsonDeserialize(using = StrictIntegerDeserializer.class)
          @Min(1)
          @Max(1000)
          Integer targetReps,
      @tools.jackson.databind.annotation.JsonDeserialize(using = StrictIntegerDeserializer.class)
          @Min(10)
          @Max(3600)
          Integer targetDurationSeconds,
      @tools.jackson.databind.annotation.JsonDeserialize(using = StrictIntegerDeserializer.class)
          @Min(10)
          @Max(3600)
          Integer timeLimitSeconds,
      @NotNull @DecimalMin("0") @DecimalMax("9999.99") BigDecimal weightKg,
      @tools.jackson.databind.annotation.JsonDeserialize(using = StrictIntegerDeserializer.class)
          @NotNull
          @Min(0)
          @Max(600)
          Integer restSeconds) {}
}
