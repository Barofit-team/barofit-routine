package com.barofit.routine.application;

import com.barofit.routine.domain.Routine;
import java.util.Set;

/** 오늘의 추천 전용 정책. 카탈로그 및 다른 루틴 목록의 공개 여부와는 독립적이다. */
final class DiscomfortRecommendationPolicy {
  private DiscomfortRecommendationPolicy() {}

  static boolean eligible(Routine routine, Set<String> selectedAreas) {
    var areas = new java.util.HashSet<>(selectedAreas);
    areas.remove("NONE");
    if (areas.isEmpty()) return true;
    var exercises = routine.getExercises();
    if (exercises.isEmpty()) return false;
    // 세트 수와 관계없이 루틴의 종목 하나를 한 번 센다. 여러 선택 부위와 겹쳐도 한 번이다.
    // 매핑 없는 신규 종목은 확인 전까지 불편 부위가 있는 사용자의 추천에서 제외한다.
    if (exercises.stream().anyMatch(e -> e.getExercise().getDiscomfortAreas().isEmpty()))
      return false;
    long affected =
        exercises.stream()
            .filter(e -> e.getExercise().getDiscomfortAreas().stream().anyMatch(areas::contains))
            .count();
    return affected * 2 < exercises.size();
  }
}
