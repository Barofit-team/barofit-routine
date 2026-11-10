package com.barofit.routine.controller;

import com.barofit.routine.application.RoutineService;
import com.barofit.routine.application.WorkoutPlanService;
import com.barofit.routine.auth.CurrentUserProvider;
import com.barofit.routine.dto.RoutineRequests.*;
import com.barofit.routine.dto.RoutineResponses.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/routines")
@RequiredArgsConstructor
@Tag(name = "Routine", description = "로그인 사용자의 루틴 관리")
@SecurityRequirement(name = "routineAuth")
@ApiResponse(
    responseCode = "401",
    description = "인증 정보 없음 또는 잘못된 내부 사용자 ID",
    content = @Content(schema = @Schema(implementation = RoutineProblemDetail.class)))
@ApiResponse(
    responseCode = "400",
    description = "요청 형식 또는 입력값 오류",
    content = @Content(schema = @Schema(implementation = RoutineProblemDetail.class)))
public class RoutineController {
  private final RoutineService service;
  private final WorkoutPlanService workoutPlans;
  private final CurrentUserProvider currentUser;

  @GetMapping("/today")
  @Operation(
      summary = "오늘 계획된 운동",
      description = "GET /api/v1/workout-plans/today와 동일합니다. Asia/Seoul 날짜 기준.")
  public WorkoutPlanService.Day today(
      @org.springframework.security.core.annotation.AuthenticationPrincipal
          org.springframework.security.oauth2.jwt.Jwt jwt) {
    return workoutPlans.today(currentUser.userId(), jwt.getTokenValue());
  }

  @GetMapping
  @Operation(summary = "내 루틴 목록", description = "최신 생성순. 종목 수와 전체 세트 수를 포함합니다.")
  @ApiResponse(responseCode = "200", description = "조회 성공", useReturnTypeSchema = true)
  public RoutinePage list(
      @RequestParam(defaultValue = "0") @Min(0) @Parameter(description = "0부터 시작") int page,
      @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
    return service.list(currentUser.userId(), page, size);
  }

  @PostMapping
  @Operation(
      summary = "템플릿에서 내 루틴 생성",
      description = "종목·순서·이름·난이도·무게는 템플릿에서 복사합니다. 세트 수·반복 횟수·유지 시간·휴식 시간을 조절할 수 있습니다.")
  @io.swagger.v3.oas.annotations.parameters.RequestBody(
      content =
          @Content(
              schema = @Schema(implementation = Create.class),
              examples = {
                @ExampleObject(
                    name = "기본값 그대로 저장",
                    value = "{\"templateId\":\"00000000-0000-0000-0000-000000000001\"}"),
                @ExampleObject(
                    name = "스쿼트만 조절",
                    value =
                        """
                        {"templateId":"00000000-0000-0000-0000-000000000001","exercises":[
                          {"templateExerciseId":"00000000-0000-0000-0001-000000000002","setCount":3,"targetReps":12}]}
                        """)
              }))
  @ApiResponse(
      responseCode = "404",
      description = "템플릿 없음",
      content = @Content(schema = @Schema(implementation = RoutineProblemDetail.class)))
  @ApiResponse(
      responseCode = "201",
      description = "생성 성공. Location 헤더에 상세 경로 반환",
      useReturnTypeSchema = true)
  public ResponseEntity<Detail> create(@Valid @RequestBody Create request) {
    Detail result = service.create(currentUser.userId(), request);
    return ResponseEntity.created(URI.create("/api/v1/routines/" + result.id())).body(result);
  }

  @GetMapping("/{routineId}")
  @Operation(summary = "내 루틴 상세", description = "종목별 가이드·TTS 원문과 세트 목표를 포함합니다.")
  @ApiResponse(responseCode = "200", description = "조회 성공", useReturnTypeSchema = true)
  @ApiResponse(
      responseCode = "404",
      description = "본인 소유의 사용자 루틴이 없음",
      content = @Content(schema = @Schema(implementation = RoutineProblemDetail.class)))
  public Detail detail(@PathVariable UUID routineId) {
    return service.detail(currentUser.userId(), routineId);
  }

  @PatchMapping("/{routineId}")
  @Operation(
      summary = "종목별 세트 수·목표·휴식 수정",
      description =
          "내 루틴 상세의 exercises[].id를 사용합니다. 미지정 종목/값은 유지. 지정 목표와 휴식 시간은 해당 종목의 모든 세트에 적용합니다.")
  @io.swagger.v3.oas.annotations.parameters.RequestBody(
      content =
          @Content(
              schema = @Schema(implementation = Update.class),
              examples =
                  @ExampleObject(
                      name = "내 루틴의 종목 설정 조절",
                      value =
                          """
                          {"exercises":[{"routineExerciseId":"11111111-1111-1111-1111-111111111111","setCount":3,"targetReps":12}]}
                          """)))
  @ApiResponse(responseCode = "200", description = "수정 성공", useReturnTypeSchema = true)
  @ApiResponse(
      responseCode = "404",
      description = "본인 소유의 사용자 루틴이 없음",
      content = @Content(schema = @Schema(implementation = RoutineProblemDetail.class)))
  @ApiResponse(
      responseCode = "409",
      description = "동시 변경 충돌. 다시 조회 후 재시도",
      content = @Content(schema = @Schema(implementation = RoutineProblemDetail.class)))
  public Detail update(@PathVariable UUID routineId, @Valid @RequestBody Update request) {
    return service.update(currentUser.userId(), routineId, request);
  }

  @GetMapping("/{routineId}/exercises/{routineExerciseId}/replacements")
  @Operation(
      summary = "유사 종목 후보 조회",
      description = "같은 운동 분류·측정 방식·필요 기구의 활성 종목 중 현재 루틴에 없는 종목을 반환합니다.")
  public List<ExerciseSummary> replacements(
      @PathVariable UUID routineId, @PathVariable UUID routineExerciseId) {
    return service.replacementCandidates(currentUser.userId(), routineId, routineExerciseId);
  }

  @PutMapping("/{routineId}/exercises/{routineExerciseId}/replacement")
  @Operation(
      summary = "내 루틴의 종목 교체",
      description = "유사 종목으로 교체합니다. 종목 순서·세트 수·목표·휴식을 유지하고 종목 가이드와 TTS는 새 종목의 것으로 바뀝니다.")
  public Detail replaceExercise(
      @PathVariable UUID routineId,
      @PathVariable UUID routineExerciseId,
      @Valid @RequestBody ReplaceExercise request) {
    return service.replaceExercise(
        currentUser.userId(), routineId, routineExerciseId, request.exerciseId());
  }

  @DeleteMapping("/{routineId}")
  @Operation(summary = "루틴 삭제", description = "루틴과 소속 종목·세트를 함께 삭제합니다.")
  @ApiResponse(responseCode = "204", description = "삭제 성공", content = @Content)
  @ApiResponse(
      responseCode = "404",
      description = "본인 소유의 사용자 루틴이 없음",
      content = @Content(schema = @Schema(implementation = RoutineProblemDetail.class)))
  @ApiResponse(
      responseCode = "409",
      description = "동시 변경 충돌",
      content = @Content(schema = @Schema(implementation = RoutineProblemDetail.class)))
  public ResponseEntity<Void> delete(@PathVariable UUID routineId) {
    service.delete(currentUser.userId(), routineId);
    return ResponseEntity.noContent().build();
  }
}
