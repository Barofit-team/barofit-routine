package com.barofit.routine.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.barofit.routine.domain.*;
import com.barofit.routine.repository.RoutineRepository;
import com.barofit.routine.repository.WorkoutPlanRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class WorkoutPlanServiceTest {

  // 포트 1번은 어떤 로컬 서버도 열 수 없어 연결이 즉시 거부된다 - identity가 응답 없을 때의 동작 검증용.
  private static final String UNREACHABLE = "http://127.0.0.1:1";
  private static final Instant NOW = Instant.parse("2026-03-10T00:00:00Z");

  private WorkoutPlanRepository plans;
  private RoutineRepository routines;
  private UUID userId;

  @BeforeEach
  void setUp() {
    plans = mock(WorkoutPlanRepository.class);
    routines = mock(RoutineRepository.class);
    userId = UUID.randomUUID();
  }

  private WorkoutPlanService service(String identityBaseUrl) {
    return new WorkoutPlanService(
        plans, routines, Clock.fixed(NOW, ZoneOffset.UTC), identityBaseUrl);
  }

  private static void setId(Object entity, UUID id) {
    ReflectionTestUtils.setField(entity, "id", id);
  }

  private Routine routineWithOneExercise(UUID ownerUserId) {
    Exercise exercise = mock(Exercise.class);
    when(exercise.getId()).thenReturn("squat");
    when(exercise.getName()).thenReturn("스쿼트");
    when(exercise.getMeasurementType()).thenReturn(MeasurementType.REPS);
    when(exercise.getCategory()).thenReturn(RoutineCategory.LOWER_BODY);
    when(exercise.getEquipment()).thenReturn(Equipment.NONE);
    when(exercise.getAssessmentStatus()).thenReturn(AssessmentStatus.READY);
    when(exercise.getPreparation()).thenReturn("준비");
    when(exercise.getInstructions()).thenReturn("방법");
    when(exercise.getPrecautions()).thenReturn("주의");
    when(exercise.getIntroTtsText()).thenReturn("안내");
    when(exercise.isActive()).thenReturn(true);

    Routine template = mock(Routine.class);
    when(template.getId()).thenReturn(UUID.randomUUID());
    when(template.getCategory()).thenReturn(RoutineCategory.LOWER_BODY);
    when(template.getName()).thenReturn("하체 템플릿");
    when(template.getDescription()).thenReturn("설명");
    when(template.getDifficulty()).thenReturn(Difficulty.BEGINNER);

    Routine routine = new Routine(ownerUserId, template);
    setId(routine, UUID.randomUUID());
    RoutineExercise item = new RoutineExercise(routine, exercise, 1);
    setId(item, UUID.randomUUID());
    RoutineSet set = new RoutineSet(item, 1, 10, null, BigDecimal.ZERO, 30);
    setId(set, UUID.randomUUID());
    item.addSet(set);
    routine.addExercise(item);
    return routine;
  }

  private WorkoutPlan planFor(UUID ownerUserId, LocalDate date, Routine routine) {
    WorkoutPlan plan = new WorkoutPlan(ownerUserId, date, routine, NOW);
    setId(plan, UUID.randomUUID());
    return plan;
  }

  @Test
  void month_rejectsInvalidYearMonth() {
    WorkoutPlanService service = service(UNREACHABLE);

    assertThatThrownBy(() -> service.month(userId, 2026, 13))
        .isInstanceOf(RoutineException.class)
        .extracting(ex -> ((RoutineException) ex).getErrorCode().code())
        .isEqualTo(RoutineErrorCode.INVALID_PLAN.code());
  }

  @Test
  void month_mapsPlansWithinRange() {
    Routine routine = routineWithOneExercise(userId);
    WorkoutPlan plan = planFor(userId, LocalDate.of(2026, 3, 5), routine);
    when(plans.findByOwnerUserIdAndPlanDateBetweenOrderByPlanDateAsc(
            userId, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31)))
        .thenReturn(List.of(plan));

    var result = service(UNREACHABLE).month(userId, 2026, 3);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).routineId()).isEqualTo(routine.getId());
  }

  @Test
  void remove_deletesExistingPlan_whenPresent() {
    Routine routine = routineWithOneExercise(userId);
    LocalDate date = LocalDate.of(2026, 3, 10);
    WorkoutPlan plan = planFor(userId, date, routine);
    when(plans.findByOwnerUserIdAndPlanDate(userId, date)).thenReturn(Optional.of(plan));

    service(UNREACHABLE).remove(userId, date);

    verify(plans).delete(plan);
  }

  @Test
  void remove_doesNothing_whenNoPlanForDate() {
    LocalDate date = LocalDate.of(2026, 3, 10);
    when(plans.findByOwnerUserIdAndPlanDate(userId, date)).thenReturn(Optional.empty());

    service(UNREACHABLE).remove(userId, date);

    verify(plans, never()).delete(any());
  }

  @Test
  void assign_throwsRoutineNotFound_whenRoutineMissing() {
    UUID routineId = UUID.randomUUID();
    when(routines.findById(routineId)).thenReturn(Optional.empty());

    assertThatThrownBy(
            () -> service(UNREACHABLE).assign(userId, LocalDate.of(2026, 3, 10), routineId, "t"))
        .isInstanceOf(RoutineException.class)
        .extracting(ex -> ((RoutineException) ex).getErrorCode().code())
        .isEqualTo(RoutineErrorCode.ROUTINE_NOT_FOUND.code());
  }

  @Test
  void assign_throwsRoutineNotFound_whenRoutineOwnedBySomeoneElse() {
    Routine routine = routineWithOneExercise(UUID.randomUUID());
    when(routines.findById(routine.getId())).thenReturn(Optional.of(routine));

    assertThatThrownBy(() ->
            service(UNREACHABLE).assign(userId, LocalDate.of(2026, 3, 10), routine.getId(), "t"))
        .isInstanceOf(RoutineException.class)
        .extracting(ex -> ((RoutineException) ex).getErrorCode().code())
        .isEqualTo(RoutineErrorCode.ROUTINE_NOT_FOUND.code());
  }

  @Test
  void assign_createsNewPlan_andGracefullyIgnoresUnreachableIdentity() {
    Routine routine = routineWithOneExercise(userId);
    when(routines.findById(routine.getId())).thenReturn(Optional.of(routine));
    LocalDate date = LocalDate.of(2026, 3, 10);
    when(plans.findByOwnerUserIdAndPlanDate(userId, date)).thenReturn(Optional.empty());
    when(plans.saveAndFlush(any(WorkoutPlan.class))).thenAnswer(inv -> inv.getArgument(0));

    var day = service(UNREACHABLE).assign(userId, date, routine.getId(), "token");

    assertThat(day.routine().exercises()).hasSize(1);
    assertThat(day.estimatedMinutes()).isPositive();
    // identity가 응답하지 않아도 예외 없이 체중 정보 없는 결과를 반환한다.
    assertThat(day.estimatedKcal()).isNull();
  }

  @Test
  void week_rejectsNonMondayStart() {
    assertThatThrownBy(() -> service(UNREACHABLE).week(userId, LocalDate.of(2026, 3, 10), "t"))
        .isInstanceOf(RoutineException.class)
        .extracting(ex -> ((RoutineException) ex).getErrorCode().code())
        .isEqualTo(RoutineErrorCode.INVALID_PLAN.code());
  }

  @Test
  void week_returnsSevenDays_withEmptyDaysWhenNoPlan() {
    LocalDate monday = LocalDate.of(2026, 3, 9);
    when(plans.findByOwnerUserIdAndPlanDateBetweenOrderByPlanDateAsc(
            userId, monday, monday.plusDays(6)))
        .thenReturn(List.of());

    var week = service(UNREACHABLE).week(userId, monday, "token");

    assertThat(week.days()).hasSize(7);
    assertThat(week.days()).allSatisfy(day -> assertThat(day.planId()).isNull());
  }

  @Test
  void today_returnsEmptyDay_whenNoPlanAssigned() {
    Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    WorkoutPlanService service = new WorkoutPlanService(plans, routines, clock, UNREACHABLE);
    // NOW(UTC)는 Asia/Seoul 기준으로도 같은 날(3/10)이다.
    when(plans.findByOwnerUserIdAndPlanDate(userId, LocalDate.of(2026, 3, 10)))
        .thenReturn(Optional.empty());

    var day = service.today(userId, "token");

    assertThat(day.planId()).isNull();
    assertThat(day.steps()).isEmpty();
  }
}
