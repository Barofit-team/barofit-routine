package com.barofit.routine.admin;

import com.barofit.routine.domain.*;
import com.barofit.routine.dto.RoutineResponses.Detail;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.*;

public final class AdminCatalogDtos {
  private AdminCatalogDtos() {}

  public record ExerciseView(
      String id,
      String name,
      RoutineCategory category,
      MeasurementType measurementType,
      AssessmentStatus assessmentStatus,
      boolean active,
      boolean visible,
      long version,
      Set<String> discomfortAreas,
      String description,
      String preparation,
      String instructions,
      String precautions,
      String introTtsText) {}

  public record ExerciseEdit(
      @PositiveOrZero long version,
      @NotBlank @Size(max = 100) String name,
      @NotBlank @Size(max = 10000) String description,
      @NotBlank @Size(max = 10000) String preparation,
      @NotBlank @Size(max = 10000) String instructions,
      @NotBlank @Size(max = 10000) String precautions,
      @NotBlank @Size(max = 10000) String introTtsText,
      @NotNull AssessmentStatus assessmentStatus,
      @NotNull Boolean active,
      @NotEmpty @Size(min = 1, max = 1) Set<@NotBlank String> discomfortAreas) {}

  public record TemplateView(
      Detail detail, long version, boolean visible, List<String> hiddenReasons) {}

  public record TemplateEdit(
      @PositiveOrZero long version,
      @NotBlank @Size(max = 100) String name,
      @NotNull @Size(max = 10000) String description,
      @NotNull Difficulty difficulty,
      @NotNull RoutineCategory category,
      @NotEmpty @Size(max = 30) List<@Valid ItemEdit> exercises) {}

  public record ItemEdit(
      @NotBlank String exerciseId, @NotEmpty @Size(max = 10) List<@Valid SetEdit> sets) {}

  public record SetEdit(
      @Min(1) @Max(1000) Integer targetReps,
      @Min(1) @Max(3600) Integer targetDurationSeconds,
      @NotNull @DecimalMin("0") @DecimalMax("9999.99") @Digits(integer = 4, fraction = 2)
          java.math.BigDecimal weightKg,
      @Min(0) @Max(600) int restSeconds) {}
}
