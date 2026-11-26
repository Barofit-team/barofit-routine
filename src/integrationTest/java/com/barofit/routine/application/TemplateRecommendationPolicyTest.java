package com.barofit.routine.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.barofit.routine.RoutineIntegrationTest;
import com.barofit.routine.domain.RoutineType;
import com.barofit.routine.repository.ExerciseRepository;
import com.barofit.routine.repository.RoutineRepository;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@RoutineIntegrationTest
@Transactional
class TemplateRecommendationPolicyTest {
  @Autowired RoutineRepository routines;
  @Autowired RoutineService service;
  @Autowired ExerciseRepository exercises;

  @Test
  void everySeedExerciseHasPersistedDiscomfortMappingIncludingFutureSupport() {
    var catalog = exercises.findAll();
    assertThat(catalog).hasSize(47);
    assertThat(catalog)
        .allSatisfy(exercise ->
            assertThat(exercise.getDiscomfortAreas())
                .isNotEmpty()
                .isSubsetOf("NECK_SHOULDER", "ARM_WRIST", "BACK", "LEG_ANKLE"));
  }

  @Test
  void habitGoalRecommendsAccessibleFullBodyTemplates() {
    var result =
        TemplateRecommendationService.select(
            new TemplateRecommendationService.Profile("HABIT", "NOVICE", 3, Set.of()),
            routines.findByTypeOrderByCreatedAtAscIdAsc(RoutineType.TEMPLATE));
    assertThat(result.recommendations()).isNotEmpty();
    assertThat(result.recommendations().getFirst().template().category().name())
        .isEqualTo("FULL_BODY");
  }

  @Test
  void goalAndExperienceSelectThreeDistinctTemplatesWhenUnrestricted() {
    var profile = new TemplateRecommendationService.Profile("ENDURANCE", "BEGINNER", 3, Set.of());
    var result =
        TemplateRecommendationService.select(
            profile, routines.findByTypeOrderByCreatedAtAscIdAsc(RoutineType.TEMPLATE));
    assertThat(result.recommendations()).hasSize(3);
    assertThat(result.recommendations().stream().map(r -> r.template().category()).toList())
        .doesNotHaveDuplicates();
    // ENDURANCE 우선순위(CARDIO → FULL_BODY → LOWER_BODY)를 그대로 따른다.
    assertThat(result.recommendations().stream().map(r -> r.template().category().name()))
        .containsExactly("CARDIO", "FULL_BODY", "LOWER_BODY");
    assertThat(result.recommendations())
        .allMatch(r -> r.template().difficulty().name().equals("BEGINNER"));
    assertThat(result.recommendations())
        .allMatch(TemplateRecommendationService.Recommendation::assessmentReady);
  }

  @Test
  void discomfortUsesExerciseRatioAndDoesNotChangeCatalog() {
    var templates = routines.findByTypeOrderByCreatedAtAscIdAsc(RoutineType.TEMPLATE);
    var upper =
        TemplateRecommendationService.select(
            new TemplateRecommendationService.Profile("STRENGTH", "NOVICE", 2, Set.of("ARM_WRIST")),
            templates);
    assertThat(upper.recommendations()).isNotEmpty();
    assertThat(upper.recommendations())
        .allMatch(r -> {
          var template =
              templates.stream()
                  .filter(t -> t.getId().equals(r.template().id()))
                  .findFirst()
                  .orElseThrow();
          return DiscomfortRecommendationPolicy.eligible(template, Set.of("ARM_WRIST"));
        });
    var back =
        TemplateRecommendationService.select(
            new TemplateRecommendationService.Profile("POSTURE", "ADVANCED", 3, Set.of("BACK")),
            templates);
    assertThat(back.recommendations()).isNotEmpty();
    assertThat(back.recommendations())
        .allMatch(r -> {
          var template =
              templates.stream()
                  .filter(t -> t.getId().equals(r.template().id()))
                  .findFirst()
                  .orElseThrow();
          return DiscomfortRecommendationPolicy.eligible(template, Set.of("BACK"));
        });
    var excluded =
        templates.stream()
            .filter(CatalogVisibility::available)
            .filter(t -> !DiscomfortRecommendationPolicy.eligible(t, Set.of("BACK")))
            .toList();
    assertThat(excluded).isNotEmpty();
    assertThat(service.templates(null, null))
        .hasSize(45)
        .extracting(t -> t.id())
        .containsAll(excluded.stream().map(t -> t.getId()).toList());
    for (var template : excluded) {
      assertThat(service.template(template.getId()).exercises())
          .hasSize(template.getExercises().size());
    }
    assertThat(
            templates.stream()
                .flatMap(t -> t.getExercises().stream())
                .map(e -> e.getExercise())
                .distinct())
        .allMatch(e -> !e.getDiscomfortAreas().isEmpty());
  }
}
