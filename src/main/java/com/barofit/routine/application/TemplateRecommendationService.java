package com.barofit.routine.application;

import com.barofit.routine.domain.*;
import com.barofit.routine.dto.RoutineResponses.Detail;
import com.barofit.routine.repository.RoutineRepository;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.databind.JsonNode;

@Service
public class TemplateRecommendationService {
  private final RoutineRepository routines;
  private final RestClient identity;

  public TemplateRecommendationService(
      RoutineRepository routines,
      @Value("${barofit.identity.base-url:http://localhost:3004}") String identityBaseUrl) {
    this.routines = routines;
    var requestFactory =
        new JdkClientHttpRequestFactory(
            HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build());
    requestFactory.setReadTimeout(Duration.ofSeconds(3));
    this.identity =
        RestClient.builder().baseUrl(identityBaseUrl).requestFactory(requestFactory).build();
  }

  public record Recommendation(Detail template, String reason, boolean assessmentReady) {}

  public record Result(List<Recommendation> recommendations, String notice) {}

  @Transactional(readOnly = true)
  public Result recommend(UUID userId, String accessToken) {
    JsonNode me;
    try {
      me =
          identity
              .get()
              .uri("/api/v1/users/me")
              .headers(h -> h.setBearerAuth(accessToken))
              .retrieve()
              .body(JsonNode.class);
    } catch (RestClientException e) {
      throw new RoutineException(RoutineErrorCode.PROFILE_UNAVAILABLE);
    }
    if (me == null || !userId.toString().equals(me.path("id").asText())) {
      throw new RoutineException(RoutineErrorCode.PROFILE_UNAVAILABLE);
    }
    if (!me.path("isOnboarded").asBoolean()
        || me.path("profile").isMissingNode()
        || me.path("profile").isNull()) {
      throw new RoutineException(RoutineErrorCode.ONBOARDING_REQUIRED);
    }
    Profile profile = Profile.from(me.path("profile"));
    List<Routine> candidates = routines.findByTypeOrderByCreatedAtAscIdAsc(RoutineType.TEMPLATE);
    return select(profile, candidates);
  }

  static Result select(Profile profile, List<Routine> templates) {
    if (profile.discomfortAreas.contains("BACK")) {
      return new Result(List.of(), "등·허리 불편 정보만으로 안전한 동작을 판단할 수 없습니다. 운동 가능 범위를 먼저 확인해 주세요.");
    }
    Difficulty target =
        switch (profile.experienceLevel) {
          case "NOVICE", "BEGINNER" -> Difficulty.BEGINNER;
          case "INTERMEDIATE" -> Difficulty.INTERMEDIATE;
          case "ADVANCED" ->
              profile.weeklyWorkoutDays <= 2 ? Difficulty.INTERMEDIATE : Difficulty.ADVANCED;
          default -> throw new RoutineException(RoutineErrorCode.PROFILE_UNAVAILABLE);
        };
    List<RoutineCategory> priority =
        switch (profile.goal) {
          case "WEIGHT_LOSS", "ENDURANCE" ->
              List.of(
                  RoutineCategory.CARDIO,
                  RoutineCategory.FULL_BODY,
                  RoutineCategory.LOWER_BODY,
                  RoutineCategory.CORE,
                  RoutineCategory.UPPER_BODY,
                  RoutineCategory.BACK);
          case "STRENGTH" ->
              List.of(
                  RoutineCategory.FULL_BODY,
                  RoutineCategory.LOWER_BODY,
                  RoutineCategory.UPPER_BODY,
                  RoutineCategory.BACK,
                  RoutineCategory.CORE,
                  RoutineCategory.CARDIO);
          case "POSTURE" ->
              List.of(
                  RoutineCategory.CORE,
                  RoutineCategory.BACK,
                  RoutineCategory.FULL_BODY,
                  RoutineCategory.LOWER_BODY,
                  RoutineCategory.UPPER_BODY,
                  RoutineCategory.CARDIO);
          default -> throw new RoutineException(RoutineErrorCode.PROFILE_UNAVAILABLE);
        };
    boolean upper =
        profile.discomfortAreas.contains("NECK_SHOULDER")
            || profile.discomfortAreas.contains("ARM_WRIST");
    boolean lower = profile.discomfortAreas.contains("LEG_ANKLE");
    List<Routine> eligible =
        templates.stream()
            .filter(t -> t.getCategory() != null && !t.getExercises().isEmpty())
            .filter(t ->
                t.getExercises().stream()
                    .allMatch(e -> e.getExercise().isActive() && !e.getSets().isEmpty()))
            .filter(t -> t.getDifficulty().ordinal() <= target.ordinal())
            .filter(t -> !upper || t.getCategory() == RoutineCategory.LOWER_BODY)
            .filter(t ->
                !lower
                    || Set.of(
                            RoutineCategory.UPPER_BODY, RoutineCategory.BACK, RoutineCategory.CORE)
                        .contains(t.getCategory()))
            .sorted(
                Comparator.<Routine>comparingInt(
                        t -> target.ordinal() - t.getDifficulty().ordinal())
                    .thenComparingInt(t -> priority.indexOf(t.getCategory()))
                    .thenComparing(Routine::getId))
            .toList();
    List<Routine> selected = new ArrayList<>();
    Set<RoutineCategory> seen = EnumSet.noneOf(RoutineCategory.class);
    for (Routine candidate : eligible) {
      if (seen.add(candidate.getCategory())) selected.add(candidate);
      if (selected.size() == 3) break;
    }
    if (selected.size() < 3) {
      for (Routine candidate : eligible) {
        if (!selected.contains(candidate)) selected.add(candidate);
        if (selected.size() == 3) break;
      }
    }
    List<Recommendation> results =
        selected.stream()
            .map(t ->
                new Recommendation(
                    Detail.from(t),
                    reason(t, target, priority.getFirst()),
                    t.getExercises().stream()
                        .allMatch(
                            e -> e.getExercise().getAssessmentStatus() == AssessmentStatus.READY)))
            .toList();
    String notice =
        results.size() < 2
            ? "불편 부위와 현재 운동 경력에 맞는 후보가 2개 미만입니다. 조건을 무시해 추천 수를 채우지 않습니다."
            : "추천은 목표·경력·빈도·불편 부위 기반의 일반 선택입니다. 자세 판정 준비 상태와 필요한 기구를 확인하고, 운동 중 통증이 심해지면 중단해 주세요.";
    return new Result(results, notice);
  }

  private static String reason(Routine routine, Difficulty target, RoutineCategory preferred) {
    String category = routine.getCategory() == preferred ? "운동 목표에 맞는 분류" : "운동 목표를 보완하는 분류";
    String level = routine.getDifficulty() == target ? "운동 경력에 맞는 난이도" : "운동 경력보다 낮은 난이도";
    return category + "와 " + level + "를 기준으로 선택했습니다.";
  }

  record Profile(
      String goal, String experienceLevel, int weeklyWorkoutDays, Set<String> discomfortAreas) {
    static Profile from(JsonNode node) {
      JsonNode areas = node.path("discomfortAreas");
      if (!areas.isArray()
          || !node.hasNonNull("goal")
          || !node.hasNonNull("experienceLevel")
          || !node.hasNonNull("weeklyWorkoutDays")) {
        throw new RoutineException(RoutineErrorCode.PROFILE_UNAVAILABLE);
      }
      Set<String> values = new HashSet<>();
      areas.forEach(area -> values.add(area.asText()));
      return new Profile(
          node.path("goal").asText(),
          node.path("experienceLevel").asText(),
          node.path("weeklyWorkoutDays").asInt(),
          values);
    }
  }
}
