package com.barofit.routine.admin;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.barofit.routine.admin.AdminCatalogDtos.ExerciseEdit;
import com.barofit.routine.application.RoutineException;
import com.barofit.routine.domain.*;
import com.barofit.routine.repository.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

class AdminCatalogServiceTest {
  @Test
  void rejectsUnknownMappingAndStaleVersionBeforeAnyMutation() {
    var exercises = mock(ExerciseRepository.class);
    var audits = mock(CatalogAuditRepository.class);
    var service =
        new AdminCatalogService(
            exercises, mock(RoutineRepository.class), audits, mock(ObjectMapper.class));
    var unknown =
        new ExerciseEdit(
            0, "이름", "설명", "준비", "방법", "주의", "안내", AssessmentStatus.READY, true, Set.of("NONE"));
    assertThatThrownBy(() -> service.updateExercise(UUID.randomUUID(), "squat", unknown))
        .isInstanceOf(RoutineException.class);
    verifyNoInteractions(exercises, audits);
    var exercise = mock(Exercise.class);
    when(exercise.getVersion()).thenReturn(2L);
    when(exercises.findById("squat")).thenReturn(Optional.of(exercise));
    var stale =
        new ExerciseEdit(
            1,
            "이름",
            "설명",
            "준비",
            "방법",
            "주의",
            "안내",
            AssessmentStatus.READY,
            true,
            Set.of("LEG_ANKLE"));
    assertThatThrownBy(() -> service.updateExercise(UUID.randomUUID(), "squat", stale))
        .isInstanceOf(RoutineException.class);
    verifyNoInteractions(audits);
    verify(exercise, never())
        .manage(any(), any(), any(), any(), any(), any(), any(), anyBoolean(), any());
  }
}
