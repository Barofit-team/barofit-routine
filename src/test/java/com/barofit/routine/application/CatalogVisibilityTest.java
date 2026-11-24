package com.barofit.routine.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.barofit.routine.domain.*;
import java.util.List;
import org.junit.jupiter.api.Test;

class CatalogVisibilityTest {
  @Test
  void onlyActiveReadyExercisesAreVisibleAndNewSupportRestoresVisibility() {
    Exercise exercise = mock(Exercise.class);
    when(exercise.isActive()).thenReturn(true);
    for (AssessmentStatus status : AssessmentStatus.values()) {
      when(exercise.getAssessmentStatus()).thenReturn(status);
      assertThat(CatalogVisibility.available(exercise)).isEqualTo(status == AssessmentStatus.READY);
    }
    when(exercise.getAssessmentStatus()).thenReturn(AssessmentStatus.READY);
    assertThat(CatalogVisibility.available(exercise)).isTrue();
    when(exercise.isActive()).thenReturn(false);
    assertThat(CatalogVisibility.available(exercise)).isFalse();
  }

  @Test
  void oneUnsupportedExerciseHidesWholeRoutineWithoutChangingComposition() {
    Routine routine = mock(Routine.class);
    when(routine.getCategory()).thenReturn(RoutineCategory.CORE);
    Exercise supported = mock(Exercise.class);
    when(supported.isActive()).thenReturn(true);
    when(supported.getAssessmentStatus()).thenReturn(AssessmentStatus.READY);
    Exercise pending = mock(Exercise.class);
    when(pending.isActive()).thenReturn(true);
    when(pending.getAssessmentStatus()).thenReturn(AssessmentStatus.PLANNED);
    RoutineExercise first = mock(RoutineExercise.class);
    RoutineExercise second = mock(RoutineExercise.class);
    when(first.getExercise()).thenReturn(supported);
    when(second.getExercise()).thenReturn(pending);
    when(first.getSets()).thenReturn(List.of(mock(RoutineSet.class)));
    when(second.getSets()).thenReturn(List.of(mock(RoutineSet.class)));
    when(routine.getExercises()).thenReturn(List.of(first, second));
    assertThat(CatalogVisibility.available(routine)).isFalse();
    when(pending.getAssessmentStatus()).thenReturn(AssessmentStatus.READY);
    assertThat(CatalogVisibility.available(routine)).isTrue();
    assertThat(routine.getExercises()).containsExactly(first, second);
    when(routine.getCategory()).thenReturn(null);
    assertThat(CatalogVisibility.available(routine)).isTrue();
  }
}
