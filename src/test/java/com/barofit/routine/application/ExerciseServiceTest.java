package com.barofit.routine.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.barofit.routine.domain.AssessmentStatus;
import com.barofit.routine.domain.Equipment;
import com.barofit.routine.domain.Exercise;
import com.barofit.routine.domain.MeasurementType;
import com.barofit.routine.domain.RoutineCategory;
import com.barofit.routine.repository.ExerciseRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ExerciseServiceTest {

  private ExerciseRepository exercises;
  private ExerciseService service;

  @BeforeEach
  void setUp() {
    exercises = mock(ExerciseRepository.class);
    service = new ExerciseService(exercises);
  }

  private Exercise exercise(String id) {
    Exercise e = mock(Exercise.class);
    when(e.getId()).thenReturn(id);
    when(e.getName()).thenReturn("운동-" + id);
    when(e.getMeasurementType()).thenReturn(MeasurementType.REPS);
    when(e.getCategory()).thenReturn(RoutineCategory.UPPER_BODY);
    when(e.getEquipment()).thenReturn(Equipment.NONE);
    when(e.getAssessmentStatus()).thenReturn(AssessmentStatus.READY);
    when(e.getDescription()).thenReturn("설명");
    when(e.getPreparation()).thenReturn("준비");
    when(e.getInstructions()).thenReturn("방법");
    when(e.getPrecautions()).thenReturn("주의");
    when(e.getIntroTtsText()).thenReturn("안내");
    return e;
  }

  @Test
  void list_mapsActiveExercisesInDisplayOrder() {
    Exercise squat = exercise("squat");
    Exercise lunge = exercise("lunge");
    when(exercises.findByActiveTrueOrderByDisplayOrderAsc()).thenReturn(List.of(squat, lunge));

    var result = service.list();

    assertThat(result).extracting("id").containsExactly("squat", "lunge");
  }

  @Test
  void detail_returnsMappedDetail_whenFound() {
    Exercise squat = exercise("squat");
    when(exercises.findByIdAndActiveTrue("squat")).thenReturn(Optional.of(squat));

    var detail = service.detail("squat");

    assertThat(detail.id()).isEqualTo("squat");
    assertThat(detail.instructions()).isEqualTo("방법");
  }

  @Test
  void detail_throwsExerciseNotFound_whenMissingOrInactive() {
    when(exercises.findByIdAndActiveTrue("missing")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.detail("missing"))
        .isInstanceOf(RoutineException.class)
        .extracting(ex -> ((RoutineException) ex).getErrorCode().code())
        .isEqualTo(RoutineErrorCode.EXERCISE_NOT_FOUND.code());
  }
}
