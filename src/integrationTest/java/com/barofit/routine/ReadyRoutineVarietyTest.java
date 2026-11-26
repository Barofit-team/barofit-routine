package com.barofit.routine;

import static org.assertj.core.api.Assertions.assertThat;

import com.barofit.routine.application.CatalogVisibility;
import com.barofit.routine.domain.*;
import com.barofit.routine.repository.RoutineRepository;
import java.util.*;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@RoutineIntegrationTest
@Transactional
class ReadyRoutineVarietyTest {
  @Autowired RoutineRepository routines;

  List<Routine> additions() {
    return routines.findByTypeOrderByCreatedAtAscIdAsc(RoutineType.TEMPLATE).stream()
        .filter(r ->
            r.getId().compareTo(UUID.fromString("00000000-0000-0000-0000-000000000201")) >= 0
                && r.getId().compareTo(UUID.fromString("00000000-0000-0000-0000-000000000224"))
                    <= 0)
        .toList();
  }

  @Test
  void twentyFourDistinctVisibleTemplatesCoverAllReadyExercisesAndCategories() {
    var added = additions();
    assertThat(added).hasSize(24).allMatch(CatalogVisibility::available);
    assertThat(added).extracting(Routine::getName).doesNotHaveDuplicates();
    assertThat(
            added.stream()
                .map(r ->
                    r.getExercises().stream().map(e -> e.getExercise().getId()).sorted().toList())
                .toList())
        .doesNotHaveDuplicates();
    assertThat(
            added.stream()
                .flatMap(r -> r.getExercises().stream())
                .map(e -> e.getExercise().getId())
                .collect(java.util.stream.Collectors.toSet()))
        .containsExactlyInAnyOrder(
            "push-up",
            "plank",
            "squat",
            "bird-dog",
            "jumping-jack",
            "lunge",
            "leg-raise",
            "side-lying-leg-raise",
            "glute-bridge",
            "dead-bug",
            "superman",
            "pike-push-up",
            "standing-calf-raise",
            "incline-push-up",
            "bulgarian-split-squat",
            "split-squat",
            "jump-squat",
            "mountain-climber",
            "reverse-crunch",
            "side-plank",
            "flutter-kick",
            "burpee",
            "sit-up");
    for (var category : RoutineCategory.values()) {
      var group = added.stream().filter(r -> r.getCategory() == category).toList();
      assertThat(group).hasSize(4);
      assertThat(group.stream().map(Routine::getDifficulty).toList())
          .containsExactlyInAnyOrder(
              Difficulty.BEGINNER,
              Difficulty.BEGINNER,
              Difficulty.INTERMEDIATE,
              Difficulty.ADVANCED);
    }
  }

  @Test
  void targetsMatchMeasurementTypeAndDifficultyWithoutChangingLegacyTemplates() {
    for (var routine : additions()) {
      assertThat(routine.getExercises()).hasSizeBetween(3, 5);
      assertThat(routine.getExercises().stream().map(RoutineExercise::getExerciseOrder).toList())
          .containsExactlyElementsOf(
              IntStream.rangeClosed(1, routine.getExercises().size()).boxed().toList());
      int count =
          switch (routine.getDifficulty()) {
            case BEGINNER -> 2;
            case INTERMEDIATE -> 3;
            case ADVANCED -> 4;
          };
      assertThat(routine.getInterExerciseRestSeconds()).isBetween(0, 600);
      for (var exercise : routine.getExercises()) {
        assertThat(exercise.getSets()).hasSize(count);
        assertThat(exercise.getSets().stream().map(RoutineSet::getSetOrder).toList())
            .containsExactlyElementsOf(IntStream.rangeClosed(1, count).boxed().toList());
        for (var set : exercise.getSets()) {
          assertThat(set.getWeightKg()).isZero();
          assertThat(set.getRestSeconds()).isBetween(0, 600);
          if (exercise.getExercise().getMeasurementType() == MeasurementType.REPS) {
            assertThat(set.getTargetReps()).isBetween(1, 1000);
            assertThat(set.getTargetDurationSeconds()).isNull();
            assertThat(set.getTimeLimitSeconds()).isEqualTo(60);
          } else {
            assertThat(set.getTargetDurationSeconds()).isBetween(10, 3600);
            assertThat(set.getTargetReps()).isNull();
            assertThat(set.getTimeLimitSeconds()).isNull();
          }
        }
      }
    }
    var legacy =
        routines.findById(UUID.fromString("00000000-0000-0000-0000-000000000001")).orElseThrow();
    assertThat(legacy.getName()).isEqualTo("초급 전신 루틴");
    assertThat(legacy.getExercises()).hasSize(3);
    assertThat(legacy.getExercises().getFirst().getSets()).hasSize(2);
  }
}
