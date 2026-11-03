package com.barofit.routine.application;

import com.barofit.routine.domain.*;
import com.barofit.routine.dto.RoutineResponses.Detail;
import com.barofit.routine.dto.RoutineResponses.ExerciseItem;
import com.barofit.routine.repository.*;
import tools.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.net.http.HttpClient;
import java.time.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Service
public class WorkoutPlanService {
    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
    private final WorkoutPlanRepository plans;
    private final RoutineRepository routines;
    private final RestClient identity;
    private final Clock clock;

    public WorkoutPlanService(WorkoutPlanRepository plans, RoutineRepository routines, Clock clock,
        @Value("${barofit.identity.base-url:http://localhost:3004}") String identityBaseUrl) {
        this.plans = plans;
        this.routines = routines;
        this.clock = clock;
        var factory = new JdkClientHttpRequestFactory(
            HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build());
        factory.setReadTimeout(Duration.ofSeconds(3));
        this.identity = RestClient.builder().baseUrl(identityBaseUrl).requestFactory(factory).build();
    }

    public enum StepType { EXERCISE, REST }
    public record Step(StepType type, ExerciseItem exercise, Integer restSeconds) { }
    public record Day(LocalDate date, UUID planId, Detail routine, List<Step> steps,
                      Integer estimatedMinutes, Integer estimatedKcal) { }
    public record Week(LocalDate weekStart, List<Day> days) { }
    public record MonthlyPlan(LocalDate date, UUID planId, UUID routineId, String routineName) { }

    @Transactional(readOnly = true)
    public List<MonthlyPlan> month(UUID userId, int year, int month) {
        YearMonth period;
        try { period = YearMonth.of(year, month); }
        catch (DateTimeException ex) { throw new RoutineException(RoutineErrorCode.INVALID_PLAN); }
        return plans.findByOwnerUserIdAndPlanDateBetweenOrderByPlanDateAsc(
            userId, period.atDay(1), period.atEndOfMonth()).stream()
            .map(plan -> new MonthlyPlan(plan.getPlanDate(), plan.getId(),
                plan.getRoutine().getId(), plan.getRoutine().getName())).toList();
    }

    @Transactional
    public Day assign(UUID userId, LocalDate date, UUID routineId, String accessToken) {
        Routine routine = routines.findById(routineId)
            .filter(r -> r.getType() == RoutineType.TEMPLATE || userId.equals(r.getOwnerUserId()))
            .orElseThrow(() -> new RoutineException(RoutineErrorCode.ROUTINE_NOT_FOUND));
        if (routine.getExercises().isEmpty() || routine.getExercises().stream()
            .anyMatch(e -> !e.getExercise().isActive() || e.getSets().isEmpty())) {
            throw new RoutineException(RoutineErrorCode.INVALID_PLAN);
        }
        Instant now = clock.instant();
        WorkoutPlan plan = plans.findByOwnerUserIdAndPlanDate(userId, date)
            .orElseGet(() -> new WorkoutPlan(userId, date, routine, now));
        plan.assign(routine, now);
        try {
            plans.saveAndFlush(plan);
        } catch (DataIntegrityViolationException e) {
            throw new RoutineException(RoutineErrorCode.PLAN_CONFLICT);
        }
        return day(date, plan, weight(userId, accessToken));
    }

    @Transactional
    public void remove(UUID userId, LocalDate date) {
        plans.findByOwnerUserIdAndPlanDate(userId, date).ifPresent(plans::delete);
    }

    @Transactional(readOnly = true)
    public Week week(UUID userId, LocalDate weekStart, String accessToken) {
        if (weekStart.getDayOfWeek() != DayOfWeek.MONDAY) {
            throw new RoutineException(RoutineErrorCode.INVALID_PLAN);
        }
        Map<LocalDate, WorkoutPlan> byDate = new HashMap<>();
        plans.findByOwnerUserIdAndPlanDateBetweenOrderByPlanDateAsc(userId, weekStart, weekStart.plusDays(6))
            .forEach(plan -> byDate.put(plan.getPlanDate(), plan));
        BigDecimal weight = weight(userId, accessToken);
        List<Day> days = new ArrayList<>(7);
        for (int i = 0; i < 7; i++) {
            LocalDate date = weekStart.plusDays(i);
            days.add(day(date, byDate.get(date), weight));
        }
        return new Week(weekStart, List.copyOf(days));
    }

    @Transactional(readOnly = true)
    public Day today(UUID userId, String accessToken) {
        LocalDate date = LocalDate.now(clock.withZone(SEOUL));
        return day(date, plans.findByOwnerUserIdAndPlanDate(userId, date).orElse(null),
            weight(userId, accessToken));
    }

    @Transactional(readOnly = true)
    public Day date(UUID userId, LocalDate date, String accessToken) {
        return day(date, plans.findByOwnerUserIdAndPlanDate(userId, date).orElse(null),
            weight(userId, accessToken));
    }

    private Day day(LocalDate date, WorkoutPlan plan, BigDecimal weightKg) {
        if (plan == null) return new Day(date, null, null, List.of(), null, null);
        Routine routine = plan.getRoutine();
        List<Step> steps = new ArrayList<>();
        Detail detail = Detail.from(routine);
        List<ExerciseItem> items = detail.exercises();
        for (int i = 0; i < items.size(); i++) {
            ExerciseItem item = items.get(i);
            steps.add(new Step(StepType.EXERCISE, item, null));
            if (i + 1 < items.size()) {
                steps.add(new Step(StepType.REST, null, item.sets().getLast().restSeconds()));
            }
        }
        WorkoutEstimate.Result estimate = WorkoutEstimate.of(routine, weightKg);
        return new Day(date, plan.getId(), detail, List.copyOf(steps),
            estimate.estimatedMinutes(), estimate.estimatedKcal());
    }

    private BigDecimal weight(UUID userId, String accessToken) {
        try {
            JsonNode me = identity.get().uri("/api/v1/users/me")
                .headers(h -> h.setBearerAuth(accessToken)).retrieve().body(JsonNode.class);
            if (me == null || !userId.toString().equals(me.path("id").asText())
                || !me.path("profile").path("weightKg").isNumber()) return null;
            return me.path("profile").path("weightKg").decimalValue();
        } catch (RestClientException e) {
            // identity가 잠시 불가해도 계획과 예상 시간은 조회할 수 있다.
            return null;
        }
    }
}
