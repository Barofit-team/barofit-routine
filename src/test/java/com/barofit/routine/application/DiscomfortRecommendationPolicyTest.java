package com.barofit.routine.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.barofit.routine.domain.*;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class DiscomfortRecommendationPolicyTest {
  @Test
  void countsExercisesInsteadOfSetsAndExcludesAtExactlyHalf() {
    var leg = item("LEG_ANKLE");
    var arm = item("ARM_WRIST");
    assertThat(eligible(List.of(leg, leg, arm, arm, arm), "LEG_ANKLE")).isTrue();
    assertThat(eligible(List.of(leg, leg, leg, arm, arm), "LEG_ANKLE")).isFalse();
    assertThat(eligible(List.of(leg, arm), "LEG_ANKLE")).isFalse();
    assertThat(eligible(List.of(leg), "LEG_ANKLE")).isFalse();
    assertThat(eligible(List.of(arm), "LEG_ANKLE")).isTrue();
  }

  @Test
  void multipleAreasUseUnionWithoutCountingSameExerciseTwice() {
    var both = item("ARM_WRIST", "NECK_SHOULDER");
    var leg = item("LEG_ANKLE");
    assertThat(eligible(List.of(both, leg, leg), "ARM_WRIST", "NECK_SHOULDER")).isTrue();
    assertThat(eligible(List.of(both, item("BACK"), leg), "ARM_WRIST", "BACK")).isFalse();
  }

  @Test
  void noneDoesNotFilterButUnmappedExercisesNeedReviewForDiscomfortRecommendations() {
    var unknown = item();
    assertThat(eligible(List.of(unknown), "NONE")).isTrue();
    assertThat(eligible(List.of(unknown))).isTrue();
    assertThat(eligible(List.of(unknown), "BACK")).isFalse();
    assertThat(eligible(List.of(), "BACK")).isFalse();
  }

  private boolean eligible(List<RoutineExercise> items, String... areas) {
    var routine = mock(Routine.class);
    when(routine.getExercises()).thenReturn(items);
    boolean result = DiscomfortRecommendationPolicy.eligible(routine, Set.of(areas));
    assertThat(routine.getExercises()).containsExactlyElementsOf(items);
    return result;
  }

  private RoutineExercise item(String... areas) {
    var exercise = mock(Exercise.class);
    when(exercise.getDiscomfortAreas()).thenReturn(Set.of(areas));
    return new RoutineExercise(null, exercise, 1);
  }
}
