package com.barofit.routine.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.barofit.routine.domain.Difficulty;
import com.barofit.routine.domain.Routine;
import com.barofit.routine.domain.RoutineCategory;
import com.barofit.routine.domain.RoutineExercise;
import com.barofit.routine.domain.RoutineSet;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class WorkoutEstimateTest {

  private Routine routineWith(
      RoutineCategory category, Difficulty difficulty, RoutineExercise... items) {
    Routine routine = mock(Routine.class);
    when(routine.getCategory()).thenReturn(category);
    when(routine.getDifficulty()).thenReturn(difficulty);
    when(routine.getExercises()).thenReturn(List.of(items));
    return routine;
  }

  @Test
  void of_countsRepBasedSetsAsThreeSecondsEach_withoutWeight() {
    RoutineExercise item = mock(RoutineExercise.class);
    RoutineSet repsSet = new RoutineSet(null, 1, 10, null, BigDecimal.ZERO, 30);
    when(item.getSets()).thenReturn(List.of(repsSet));
    Routine routine = routineWith(RoutineCategory.UPPER_BODY, Difficulty.BEGINNER, item);

    var result = WorkoutEstimate.of(routine, null);

    // work = 10 reps * 3s = 30s, 종목 1개 전환 10s, 휴식은 마지막 세트라 0s
    assertThat(result.workSeconds()).isEqualTo(30);
    assertThat(result.restSeconds()).isEqualTo(10);
    assertThat(result.estimatedMinutes()).isEqualTo(1);
    assertThat(result.estimatedKcal()).isNull();
  }

  @Test
  void of_usesDurationDirectly_whenSetIsDurationBased() {
    RoutineExercise item = mock(RoutineExercise.class);
    RoutineSet durationSet = new RoutineSet(null, 1, null, 40, BigDecimal.ZERO, 20);
    when(item.getSets()).thenReturn(List.of(durationSet));
    Routine routine = routineWith(RoutineCategory.CORE, Difficulty.BEGINNER, item);

    var result = WorkoutEstimate.of(routine, null);

    assertThat(result.workSeconds()).isEqualTo(40);
  }

  @Test
  void of_estimatesKcal_whenWeightProvided() {
    RoutineExercise item = mock(RoutineExercise.class);
    RoutineSet set = new RoutineSet(null, 1, 10, null, BigDecimal.ZERO, 30);
    when(item.getSets()).thenReturn(List.of(set));
    Routine routine = routineWith(RoutineCategory.UPPER_BODY, Difficulty.BEGINNER, item);

    var result = WorkoutEstimate.of(routine, BigDecimal.valueOf(70));

    assertThat(result.estimatedKcal()).isNotNull().isPositive();
  }

  @Test
  void of_ignoresNonPositiveWeight() {
    RoutineExercise item = mock(RoutineExercise.class);
    RoutineSet set = new RoutineSet(null, 1, 10, null, BigDecimal.ZERO, 30);
    when(item.getSets()).thenReturn(List.of(set));
    Routine routine = routineWith(RoutineCategory.UPPER_BODY, Difficulty.BEGINNER, item);

    var result = WorkoutEstimate.of(routine, BigDecimal.ZERO);

    assertThat(result.estimatedKcal()).isNull();
  }

  @Test
  void of_usesHigherMetForAdvancedCardio() {
    RoutineExercise item = mock(RoutineExercise.class);
    RoutineSet set = new RoutineSet(null, 1, 10, null, BigDecimal.ZERO, 30);
    when(item.getSets()).thenReturn(List.of(set));
    BigDecimal weight = BigDecimal.valueOf(70);

    var cardioAdvanced =
        WorkoutEstimate.of(routineWith(RoutineCategory.CARDIO, Difficulty.ADVANCED, item), weight);
    var upperBeginner =
        WorkoutEstimate.of(
            routineWith(RoutineCategory.UPPER_BODY, Difficulty.BEGINNER, item), weight);

    assertThat(cardioAdvanced.estimatedKcal()).isGreaterThan(upperBeginner.estimatedKcal());
  }

  @Test
  void of_doesNotCountRestAfterLastSet() {
    RoutineExercise item = mock(RoutineExercise.class);
    RoutineSet only = new RoutineSet(null, 1, 10, null, BigDecimal.ZERO, 99);
    when(item.getSets()).thenReturn(List.of(only));
    Routine routine = routineWith(RoutineCategory.UPPER_BODY, Difficulty.BEGINNER, item);

    var result = WorkoutEstimate.of(routine, null);

    // 유일한 세트이자 마지막 세트이므로 99초 휴식은 세지 않는다. 종목 전환 10초만 남는다.
    assertThat(result.restSeconds()).isEqualTo(10);
  }
}
