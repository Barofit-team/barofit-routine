package com.barofit.routine.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.barofit.routine.RoutineIntegrationTest;
import com.barofit.routine.domain.RoutineType;
import com.barofit.routine.repository.RoutineRepository;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@RoutineIntegrationTest
@Transactional
class TemplateRecommendationPolicyTest {
  @Autowired RoutineRepository routines;

  @Test
  void goalAndExperienceSelectThreeDistinctTemplatesWhenUnrestricted() {
    var profile = new TemplateRecommendationService.Profile("ENDURANCE", "BEGINNER", 3, Set.of());
    var result =
        TemplateRecommendationService.select(
            profile, routines.findByTypeOrderByCreatedAtAscIdAsc(RoutineType.TEMPLATE));
    assertThat(result.recommendations()).hasSize(3);
    assertThat(result.recommendations().stream().map(r -> r.template().category()).toList())
        .doesNotHaveDuplicates();
    assertThat(result.recommendations().getFirst().template().category().name())
        .isEqualTo("CARDIO");
  }

  @Test
  void discomfortDoesNotForceUnsafeExtraRecommendations() {
    var templates = routines.findByTypeOrderByCreatedAtAscIdAsc(RoutineType.TEMPLATE);
    var upper =
        TemplateRecommendationService.select(
            new TemplateRecommendationService.Profile("STRENGTH", "NOVICE", 2, Set.of("ARM_WRIST")),
            templates);
    assertThat(upper.recommendations()).hasSize(2);
    assertThat(upper.recommendations())
        .allMatch(r -> r.template().category().name().equals("LOWER_BODY"));
    var back =
        TemplateRecommendationService.select(
            new TemplateRecommendationService.Profile("POSTURE", "ADVANCED", 3, Set.of("BACK")),
            templates);
    assertThat(back.recommendations()).isEmpty();
  }
}
