package com.barofit.routine.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.barofit.routine.domain.*;
import com.barofit.routine.dto.RoutineRequests;
import com.barofit.routine.dto.RoutineRequests.CreateExercise;
import com.barofit.routine.dto.RoutineRequests.Update;
import com.barofit.routine.dto.RoutineRequests.UpdateExercise;
import com.barofit.routine.repository.ExerciseRepository;
import com.barofit.routine.repository.RoutineRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class RoutineServiceTest {

  private RoutineRepository routines;
  private ExerciseRepository exercises;
  private RoutineService service;
  private UUID userId;

  @BeforeEach
  void setUp() {
    routines = mock(RoutineRepository.class);
    exercises = mock(ExerciseRepository.class);
    service = new RoutineService(routines, exercises);
    userId = UUID.randomUUID();
    when(routines.saveAndFlush(any(Routine.class))).thenAnswer(inv -> inv.getArgument(0));
  }

  private Exercise exercise(String id, MeasurementType type, boolean active) {
    Exercise e = mock(Exercise.class);
    when(e.getId()).thenReturn(id);
    when(e.getName()).thenReturn("운동-" + id);
    when(e.getMeasurementType()).thenReturn(type);
    when(e.getCategory()).thenReturn(RoutineCategory.UPPER_BODY);
    when(e.getEquipment()).thenReturn(Equipment.NONE);
    when(e.getAssessmentStatus()).thenReturn(AssessmentStatus.READY);
    when(e.isActive()).thenReturn(active);
    return e;
  }

  /** JPA가 persist 시점에 채우는 @GeneratedValue id를, 메모리로만 만든 엔티티에 수동으로 넣어준다. */
  private static void setId(Object entity, UUID id) {
    ReflectionTestUtils.setField(entity, "id", id);
  }

  /** 템플릿: 종목 하나(세트 2개)를 가진 mock Routine. 공개 생성자가 복사용뿐이라 mock으로 만든다. */
  private Routine template(Exercise exercise, int setCount) {
    Routine t = mock(Routine.class);
    UUID templateId = UUID.randomUUID();
    when(t.getId()).thenReturn(templateId);
    when(t.getCategory()).thenReturn(RoutineCategory.UPPER_BODY);
    when(t.getName()).thenReturn("템플릿");
    when(t.getDescription()).thenReturn("설명");
    when(t.getDifficulty()).thenReturn(Difficulty.BEGINNER);

    RoutineExercise item = new RoutineExercise(t, exercise, 1);
    setId(item, UUID.randomUUID());
    for (int i = 1; i <= setCount; i++) {
      RoutineSet set = new RoutineSet(item, i, 10, null, BigDecimal.ZERO, 30);
      setId(set, UUID.randomUUID());
      item.addSet(set);
    }
    when(t.getExercises()).thenReturn(List.of(item));
    return t;
  }

  @Test
  void create_throwsTemplateNotFound_whenTemplateMissing() {
    UUID templateId = UUID.randomUUID();
    when(routines.findByIdAndType(templateId, RoutineType.TEMPLATE)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.create(userId, new RoutineRequests.Create(templateId, null)))
        .isInstanceOf(RoutineException.class)
        .extracting(ex -> ((RoutineException) ex).getErrorCode().code())
        .isEqualTo(RoutineErrorCode.TEMPLATE_NOT_FOUND.code());
  }

  @Test
  void create_throwsInvalid_whenTemplateHasNoExercises() {
    Routine empty = mock(Routine.class);
    UUID templateId = UUID.randomUUID();
    when(empty.getCategory()).thenReturn(RoutineCategory.UPPER_BODY);
    when(empty.getExercises()).thenReturn(List.of());
    when(routines.findByIdAndType(templateId, RoutineType.TEMPLATE)).thenReturn(Optional.of(empty));

    assertThatThrownBy(() -> service.create(userId, new RoutineRequests.Create(templateId, null)))
        .isInstanceOf(RoutineException.class)
        .hasMessageContaining("사용할 수 없는 템플릿");
  }

  @Test
  void create_throwsInvalid_whenCustomizationDuplicatesSameExercise() {
    Exercise ex = exercise("squat", MeasurementType.REPS, true);
    Routine t = template(ex, 2);
    UUID templateExerciseId = t.getExercises().get(0).getId();
    when(routines.findByIdAndType(t.getId(), RoutineType.TEMPLATE)).thenReturn(Optional.of(t));

    var change = new CreateExercise(templateExerciseId, 3, null, null, null);
    assertThatThrownBy(() ->
            service.create(userId, new RoutineRequests.Create(t.getId(), List.of(change, change))))
        .isInstanceOf(RoutineException.class)
        .hasMessageContaining("중복");
  }

  @Test
  void create_throwsInvalid_whenTemplateExerciseInactive() {
    Exercise inactive = exercise("inactive", MeasurementType.REPS, false);
    Routine t = template(inactive, 1);
    when(routines.findByIdAndType(t.getId(), RoutineType.TEMPLATE)).thenReturn(Optional.of(t));

    assertThatThrownBy(() -> service.create(userId, new RoutineRequests.Create(t.getId(), null)))
        .isInstanceOf(RoutineException.class)
        .hasMessageContaining("사용할 수 없는 종목");
  }

  @Test
  void create_copiesTemplateExercisesAndSets_whenNoCustomization() {
    Exercise ex = exercise("squat", MeasurementType.REPS, true);
    Routine t = template(ex, 2);
    when(routines.findByIdAndType(t.getId(), RoutineType.TEMPLATE)).thenReturn(Optional.of(t));

    var detail = service.create(userId, new RoutineRequests.Create(t.getId(), null));

    assertThat(detail.exercises()).hasSize(1);
    assertThat(detail.exercises().get(0).sets()).hasSize(2);
    verify(routines).saveAndFlush(any(Routine.class));
  }

  @Test
  void create_appliesCustomization_whenGoalsValid() {
    Exercise ex = exercise("squat", MeasurementType.REPS, true);
    Routine t = template(ex, 2);
    UUID templateExerciseId = t.getExercises().get(0).getId();
    when(routines.findByIdAndType(t.getId(), RoutineType.TEMPLATE)).thenReturn(Optional.of(t));

    var change = new CreateExercise(templateExerciseId, 4, 12, null, 45);
    var detail = service.create(userId, new RoutineRequests.Create(t.getId(), List.of(change)));

    assertThat(detail.exercises()).hasSize(1);
    assertThat(detail.exercises().get(0).sets()).hasSize(4);
    assertThat(detail.exercises().get(0).sets())
        .allSatisfy(set -> {
          assertThat(set.targetReps()).isEqualTo(12);
          assertThat(set.restSeconds()).isEqualTo(45);
        });
  }

  @Test
  void create_throwsInvalid_whenCustomizationReferencesUnknownExercise() {
    Exercise ex = exercise("squat", MeasurementType.REPS, true);
    Routine t = template(ex, 1);
    when(routines.findByIdAndType(t.getId(), RoutineType.TEMPLATE)).thenReturn(Optional.of(t));

    var change = new CreateExercise(UUID.randomUUID(), 1, null, null, null);
    assertThatThrownBy(
            () -> service.create(userId, new RoutineRequests.Create(t.getId(), List.of(change))))
        .isInstanceOf(RoutineException.class)
        .hasMessageContaining("선택한 템플릿에 없는 종목");
  }

  @Test
  void create_throwsInvalid_whenGoalsAreAllNull() {
    Exercise ex = exercise("squat", MeasurementType.REPS, true);
    Routine t = template(ex, 1);
    UUID templateExerciseId = t.getExercises().get(0).getId();
    when(routines.findByIdAndType(t.getId(), RoutineType.TEMPLATE)).thenReturn(Optional.of(t));

    var change = new CreateExercise(templateExerciseId, null, null, null, null);
    assertThatThrownBy(
            () -> service.create(userId, new RoutineRequests.Create(t.getId(), List.of(change))))
        .isInstanceOf(RoutineException.class)
        .hasMessageContaining("세트 수");
  }

  @Test
  void create_throwsInvalid_whenGoalDoesNotMatchMeasurementType() {
    Exercise durationExercise = exercise("plank", MeasurementType.DURATION, true);
    Routine t = template(durationExercise, 1);
    UUID templateExerciseId = t.getExercises().get(0).getId();
    when(routines.findByIdAndType(t.getId(), RoutineType.TEMPLATE)).thenReturn(Optional.of(t));

    // DURATION 종목에 targetReps을 주면 안 된다
    var change = new CreateExercise(templateExerciseId, null, 10, null, null);
    assertThatThrownBy(
            () -> service.create(userId, new RoutineRequests.Create(t.getId(), List.of(change))))
        .isInstanceOf(RoutineException.class)
        .hasMessageContaining("측정 방식");
  }

  /** owned() 경로에서 쓸, 실제 템플릿 복사로 만든 USER 루틴. */
  private Routine ownedRoutine(Exercise exercise, int setCount) {
    Routine t = template(exercise, setCount);
    Routine owned = new Routine(userId, t);
    setId(owned, UUID.randomUUID());
    for (RoutineExercise source : t.getExercises()) {
      RoutineExercise item =
          new RoutineExercise(owned, source.getExercise(), source.getExerciseOrder());
      setId(item, UUID.randomUUID());
      for (RoutineSet set : source.getSets()) {
        RoutineSet copy =
            new RoutineSet(
                item,
                set.getSetOrder(),
                set.getTargetReps(),
                set.getTargetDurationSeconds(),
                set.getWeightKg(),
                set.getRestSeconds());
        setId(copy, UUID.randomUUID());
        item.addSet(copy);
      }
      owned.addExercise(item);
    }
    return owned;
  }

  @Test
  void update_appliesGoalsToExistingSets() {
    Exercise ex = exercise("squat", MeasurementType.REPS, true);
    Routine owned = ownedRoutine(ex, 2);
    UUID routineId = owned.getId();
    UUID routineExerciseId = owned.getExercises().get(0).getId();
    when(routines.findByIdAndOwnerUserIdAndType(routineId, userId, RoutineType.USER))
        .thenReturn(Optional.of(owned));

    var update = new Update(List.of(new UpdateExercise(routineExerciseId, 3, 15, null, 20)));
    var detail = service.update(userId, routineId, update);

    assertThat(detail.exercises().get(0).sets()).hasSize(3);
    assertThat(detail.exercises().get(0).sets())
        .allSatisfy(set -> assertThat(set.targetReps()).isEqualTo(15));
    verify(routines).flush();
  }

  @Test
  void update_throwsInvalid_whenExerciseNotInRoutine() {
    Exercise ex = exercise("squat", MeasurementType.REPS, true);
    Routine owned = ownedRoutine(ex, 1);
    when(routines.findByIdAndOwnerUserIdAndType(owned.getId(), userId, RoutineType.USER))
        .thenReturn(Optional.of(owned));

    var update = new Update(List.of(new UpdateExercise(UUID.randomUUID(), 1, null, null, null)));
    assertThatThrownBy(() -> service.update(userId, owned.getId(), update))
        .isInstanceOf(RoutineException.class)
        .hasMessageContaining("루틴에 없는 종목");
  }

  @Test
  void update_throwsInvalid_whenDuplicateRoutineExerciseId() {
    Exercise ex = exercise("squat", MeasurementType.REPS, true);
    Routine owned = ownedRoutine(ex, 1);
    UUID routineExerciseId = owned.getExercises().get(0).getId();
    when(routines.findByIdAndOwnerUserIdAndType(owned.getId(), userId, RoutineType.USER))
        .thenReturn(Optional.of(owned));

    var one = new UpdateExercise(routineExerciseId, 1, null, null, null);
    var update = new Update(List.of(one, one));
    assertThatThrownBy(() -> service.update(userId, owned.getId(), update))
        .isInstanceOf(RoutineException.class)
        .hasMessageContaining("중복");
  }

  @Test
  void replaceExercise_replacesWithSimilarExercise() {
    Exercise original = exercise("squat", MeasurementType.REPS, true);
    Routine owned = ownedRoutine(original, 1);
    UUID routineExerciseId = owned.getExercises().get(0).getId();
    when(routines.findByIdAndOwnerUserIdAndType(owned.getId(), userId, RoutineType.USER))
        .thenReturn(Optional.of(owned));
    Exercise replacement = exercise("lunge", MeasurementType.REPS, true);
    when(exercises.findByIdAndActiveTrue("lunge")).thenReturn(Optional.of(replacement));

    var detail = service.replaceExercise(userId, owned.getId(), routineExerciseId, "lunge");

    assertThat(detail.exercises().get(0).exerciseId()).isEqualTo("lunge");
  }

  @Test
  void replaceExercise_throwsInvalid_whenNotSimilar() {
    Exercise original = exercise("squat", MeasurementType.REPS, true);
    Routine owned = ownedRoutine(original, 1);
    UUID routineExerciseId = owned.getExercises().get(0).getId();
    when(routines.findByIdAndOwnerUserIdAndType(owned.getId(), userId, RoutineType.USER))
        .thenReturn(Optional.of(owned));
    Exercise dissimilar = exercise("plank", MeasurementType.DURATION, true);
    when(exercises.findByIdAndActiveTrue("plank")).thenReturn(Optional.of(dissimilar));

    assertThatThrownBy(
            () -> service.replaceExercise(userId, owned.getId(), routineExerciseId, "plank"))
        .isInstanceOf(RoutineException.class)
        .hasMessageContaining("같은 분류");
  }

  @Test
  void replaceExercise_throwsInvalid_whenReplacementAlreadyInRoutine() {
    Exercise first = exercise("squat", MeasurementType.REPS, true);
    Routine owned = ownedRoutine(first, 1);
    Exercise second = exercise("lunge", MeasurementType.REPS, true);
    RoutineExercise secondItem = new RoutineExercise(owned, second, 2);
    setId(secondItem, UUID.randomUUID());
    secondItem.addSet(new RoutineSet(secondItem, 1, 10, null, BigDecimal.ZERO, 30));
    owned.addExercise(secondItem);
    UUID firstItemId = owned.getExercises().get(0).getId();
    when(routines.findByIdAndOwnerUserIdAndType(owned.getId(), userId, RoutineType.USER))
        .thenReturn(Optional.of(owned));
    when(exercises.findByIdAndActiveTrue("lunge")).thenReturn(Optional.of(second));

    assertThatThrownBy(() -> service.replaceExercise(userId, owned.getId(), firstItemId, "lunge"))
        .isInstanceOf(RoutineException.class)
        .hasMessageContaining("이미 루틴에 포함된 종목");
  }

  @Test
  void replacementCandidates_excludesUsedAndDissimilarExercises() {
    Exercise current = exercise("squat", MeasurementType.REPS, true);
    Routine owned = ownedRoutine(current, 1);
    UUID routineExerciseId = owned.getExercises().get(0).getId();
    when(routines.findByIdAndOwnerUserIdAndType(owned.getId(), userId, RoutineType.USER))
        .thenReturn(Optional.of(owned));

    Exercise similarUnused = exercise("lunge", MeasurementType.REPS, true);
    Exercise dissimilar = exercise("plank", MeasurementType.DURATION, true);
    when(exercises.findByActiveTrueOrderByDisplayOrderAsc())
        .thenReturn(List.of(current, similarUnused, dissimilar));

    var candidates = service.replacementCandidates(userId, owned.getId(), routineExerciseId);

    assertThat(candidates).extracting("id").containsExactly("lunge");
  }

  @Test
  void delete_deletesOwnedRoutineAndFlushes() {
    Exercise ex = exercise("squat", MeasurementType.REPS, true);
    Routine owned = ownedRoutine(ex, 1);
    when(routines.findByIdAndOwnerUserIdAndType(owned.getId(), userId, RoutineType.USER))
        .thenReturn(Optional.of(owned));

    service.delete(userId, owned.getId());

    verify(routines).delete(owned);
    verify(routines).flush();
  }

  @Test
  void detail_throwsNotFound_whenNotOwned() {
    UUID id = UUID.randomUUID();
    when(routines.findByIdAndOwnerUserIdAndType(id, userId, RoutineType.USER))
        .thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.detail(userId, id))
        .isInstanceOf(RoutineException.class)
        .extracting(ex -> ((RoutineException) ex).getErrorCode().code())
        .isEqualTo(RoutineErrorCode.ROUTINE_NOT_FOUND.code());
  }

  @Test
  void templates_filtersByCategoryWhenGiven() {
    Exercise ex = exercise("squat", MeasurementType.REPS, true);
    Routine upperBody = template(ex, 1);
    when(routines.findByTypeOrderByCreatedAtAscIdAsc(RoutineType.TEMPLATE))
        .thenReturn(List.of(upperBody));

    var matches = service.templates(null, RoutineCategory.UPPER_BODY);
    var misses = service.templates(null, RoutineCategory.CARDIO);

    assertThat(matches).hasSize(1);
    assertThat(misses).isEmpty();
  }
}
