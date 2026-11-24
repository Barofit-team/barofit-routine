package com.barofit.routine.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class HabitGoalTest {
  @Test
  void habitWithNoEligibleTemplatesReturnsNoticeRatherThanProfileError() {
    var result =
        TemplateRecommendationService.select(
            new TemplateRecommendationService.Profile("HABIT", "NOVICE", 3, Set.of()), List.of());
    assertThat(result.recommendations()).isEmpty();
    assertThat(result.notice()).isNotBlank();
  }

  @Test
  void unknownGoalStillRejectsTheProfile() {
    assertThatThrownBy(() ->
            TemplateRecommendationService.select(
                new TemplateRecommendationService.Profile("UNKNOWN", "NOVICE", 3, Set.of()),
                List.of()))
        .isInstanceOf(RuntimeException.class);
  }
}
