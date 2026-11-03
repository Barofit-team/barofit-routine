package com.barofit.routine.application;

import com.barofit.routine.application.RoutineErrorCode;
import tools.jackson.databind.JsonNode;
import java.net.http.HttpClient;
import java.time.Duration;
import java.time.LocalDate;
import java.time.DayOfWeek;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Service
public class WeeklyGoalProgressService {
    private final RestClient identity;
    private final RestClient workout;

    public WeeklyGoalProgressService(
        @Value("${barofit.identity.base-url:http://localhost:3004}") String identityBaseUrl,
        @Value("${barofit.workout.base-url:http://localhost:3006}") String workoutBaseUrl) {
        var factory = new JdkClientHttpRequestFactory(
            HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build());
        factory.setReadTimeout(Duration.ofSeconds(3));
        identity = RestClient.builder().baseUrl(identityBaseUrl).requestFactory(factory).build();
        workout = RestClient.builder().baseUrl(workoutBaseUrl).requestFactory(factory).build();
    }

    public record Result(LocalDate weekStart, int weeklyGoal, int completedCount,
                         int goalProgress, List<LocalDate> completedDates) { }

    public Result get(UUID userId, LocalDate weekStart, String accessToken) {
        if (weekStart.getDayOfWeek() != DayOfWeek.MONDAY)
            throw new RoutineException(RoutineErrorCode.INVALID_PLAN);
        JsonNode me;
        try {
            me = identity.get().uri("/api/v1/users/me").headers(h -> h.setBearerAuth(accessToken))
                .retrieve().body(JsonNode.class);
        } catch (Exception e) {
            throw new RoutineException(RoutineErrorCode.PROFILE_UNAVAILABLE);
        }
        if (me == null || !userId.toString().equals(me.path("id").asText()))
            throw new RoutineException(RoutineErrorCode.PROFILE_UNAVAILABLE);
        JsonNode goal = me.path("profile").path("weeklyWorkoutDays");
        if (!me.path("isOnboarded").asBoolean() || !goal.isInt())
            throw new RoutineException(RoutineErrorCode.ONBOARDING_REQUIRED);
        JsonNode progress;
        try {
            progress = workout.get().uri(uri -> uri.path("/api/v1/workout-sessions/weekly-progress")
                .queryParam("weekStart", weekStart).build())
                .headers(h -> h.setBearerAuth(accessToken)).retrieve().body(JsonNode.class);
        } catch (Exception e) {
            throw new RoutineException(RoutineErrorCode.WORKOUT_UNAVAILABLE);
        }
        if (progress == null || !weekStart.toString().equals(progress.path("weekStart").asText())
            || !progress.path("completedDates").isArray())
            throw new RoutineException(RoutineErrorCode.WORKOUT_UNAVAILABLE);
        List<LocalDate> dates = new java.util.ArrayList<>();
        for (JsonNode date : progress.path("completedDates")) dates.add(LocalDate.parse(date.asText()));
        int completed = dates.size();
        return new Result(weekStart, goal.intValue(), completed, Math.min(goal.intValue(), completed),
            List.copyOf(dates));
    }
}
