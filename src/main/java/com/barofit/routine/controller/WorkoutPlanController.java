package com.barofit.routine.controller;

import com.barofit.routine.application.WeeklyGoalProgressService;
import com.barofit.routine.application.WorkoutPlanService;
import com.barofit.routine.auth.CurrentUserProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/workout-plans")
@RequiredArgsConstructor
@Tag(name = "Workout Plan", description = "날짜별 루틴 배정과 오늘의 운동")
@SecurityRequirement(name = "routineAuth")
public class WorkoutPlanController {
  private final WorkoutPlanService service;
  private final WeeklyGoalProgressService progress;
  private final CurrentUserProvider currentUser;

  public record AssignRequest(@NotNull UUID routineId) {}

  @GetMapping("/weekly-progress")
  @Operation(
      summary = "주간 운동 목표 진행률",
      description =
          "온보딩 주간 목표 횟수와 70% 이상 수행해 종료한 날짜를 합쳐 반환합니다. weeklyGoal 개수만큼 점을 그리고 goalProgress 개를 활성화하며 completedDates로 달력 완료 표시를 할 수 있습니다.")
  public WeeklyGoalProgressService.Result weeklyProgress(
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStart,
      @AuthenticationPrincipal Jwt jwt) {
    return progress.get(currentUser.userId(), weekStart, jwt.getTokenValue());
  }

  @GetMapping
  @Operation(
      summary = "주간 운동 계획",
      description =
          "weekStart는 월요일 날짜(YYYY-MM-DD). 비어 있는 날도 포함해 월~일 7일을 반환합니다. 날짜는 Asia/Seoul 기준입니다.")
  public WorkoutPlanService.Week week(
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStart,
      @AuthenticationPrincipal Jwt jwt) {
    return service.week(currentUser.userId(), weekStart, jwt.getTokenValue());
  }

  @GetMapping("/today")
  @Operation(
      summary = "오늘의 운동",
      description =
          "Asia/Seoul 날짜에 배정된 루틴의 종목·세트 목표, 운동/휴식 순서와 시간·칼로리 추정치를 반환합니다. 운동 시작 시 routine 전체를 Workout 세션의 routineSnapshot으로 전송합니다.")
  public WorkoutPlanService.Day today(@AuthenticationPrincipal Jwt jwt) {
    return service.today(currentUser.userId(), jwt.getTokenValue());
  }

  @GetMapping("/month")
  @Operation(summary = "월간 운동 계획", description = "로그인 사용자가 날짜별 배정한 루틴만 반환합니다.")
  public java.util.List<WorkoutPlanService.MonthlyPlan> month(
      @RequestParam int year, @RequestParam int month) {
    return service.month(currentUser.userId(), year, month);
  }

  @GetMapping("/{date}")
  @Operation(summary = "특정 날짜의 운동 계획")
  public WorkoutPlanService.Day date(
      @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
      @AuthenticationPrincipal Jwt jwt) {
    return service.date(currentUser.userId(), date, jwt.getTokenValue());
  }

  @PutMapping("/{date}")
  @Operation(
      summary = "날짜에 루틴 배정·변경",
      description = "기본 템플릿 또는 내 루틴의 ID를 지정합니다. 같은 날짜에 다시 호출하면 교체합니다.")
  public WorkoutPlanService.Day assign(
      @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
      @Valid @RequestBody AssignRequest request,
      @AuthenticationPrincipal Jwt jwt) {
    return service.assign(currentUser.userId(), date, request.routineId(), jwt.getTokenValue());
  }

  @DeleteMapping("/{date}")
  @Operation(summary = "날짜의 루틴 배정 제거", description = "빈 날짜도 204를 반환합니다. 이미 시작한 운동 세션의 스냅샷은 유지됩니다.")
  public ResponseEntity<Void> remove(
      @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
    service.remove(currentUser.userId(), date);
    return ResponseEntity.noContent().build();
  }
}
