package com.barofit.routine.application;

import com.barofit.routine.domain.AssessmentStatus;
import com.barofit.routine.domain.Exercise;
import com.barofit.routine.domain.Routine;

/** AI 지원 여부에 따른 사용자 노출 정책. 불편 부위 추천 정책과는 별개다. */
public final class CatalogVisibility {
  private CatalogVisibility() {}

  public static boolean available(Exercise exercise) {
    return exercise.isActive() && exercise.getAssessmentStatus() == AssessmentStatus.READY;
  }

  public static boolean available(Routine routine) {
    return !routine.getExercises().isEmpty()
        && routine.getExercises().stream()
            .allMatch(item -> available(item.getExercise()) && !item.getSets().isEmpty());
  }
}
