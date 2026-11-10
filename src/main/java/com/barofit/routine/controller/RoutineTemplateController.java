package com.barofit.routine.controller;

import com.barofit.routine.application.RoutineService;
import com.barofit.routine.application.TemplateRecommendationService;
import com.barofit.routine.domain.Difficulty;
import com.barofit.routine.domain.RoutineCategory;
import com.barofit.routine.dto.RoutineResponses.Detail;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/routine-templates")
@RequiredArgsConstructor
@Tag(name = "Routine Template", description = "기본 루틴 조회")
public class RoutineTemplateController {
  private final RoutineService service;
  private final TemplateRecommendationService recommendations;

  @GetMapping
  @Operation(
      summary = "기본 루틴 목록",
      description =
          "전신·하체·상체·등·코어·심폐 각각 초급·중급·고급 템플릿을 제공합니다. category와 difficulty로 필터링합니다. 생략한 조건은 전체 조회. 종목·세트·가이드 포함. 인증 불필요.")
  @ApiResponse(responseCode = "200", description = "조회 성공", useReturnTypeSchema = true)
  @ApiResponse(
      responseCode = "400",
      description = "지원하지 않는 난이도",
      content = @Content(schema = @Schema(implementation = RoutineProblemDetail.class)))
  public List<Detail> list(
      @RequestParam(required = false)
          @Parameter(description = "BEGINNER(초급), INTERMEDIATE(중급), ADVANCED(고급)")
          Difficulty difficulty,
      @RequestParam(required = false)
          @Parameter(
              description =
                  "FULL_BODY(전신), LOWER_BODY(하체), UPPER_BODY(상체), BACK(등), CORE(코어), CARDIO(전신·심폐)")
          RoutineCategory category) {
    return service.templates(difficulty, category);
  }

  @GetMapping("/{templateId}")
  @Operation(summary = "기본 루틴 상세", description = "운동 시작 시 사용할 원본 템플릿과 종목별 세트 목표를 조회합니다.")
  @ApiResponse(responseCode = "200", description = "조회 성공", useReturnTypeSchema = true)
  @ApiResponse(responseCode = "404", description = "템플릿 없음")
  public Detail detail(@PathVariable UUID templateId) {
    return service.template(templateId);
  }

  @GetMapping("/recommendations")
  @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "routineAuth")
  @Operation(
      summary = "내 프로필 기반 루틴 템플릿 추천",
      description =
          "identity 서비스에 저장된 온보딩 프로필을 조회하여 기존 템플릿에서 2~3개를 고릅니다. 불편 부위 등으로 후보가 부족하면 더 적게 반환합니다. 성별·나이·키·몸무게·코칭 스타일은 임의 점수화하지 않습니다.")
  @ApiResponse(responseCode = "200", description = "추천 결과", useReturnTypeSchema = true)
  @ApiResponse(responseCode = "409", description = "온보딩 미완료")
  @ApiResponse(responseCode = "503", description = "identity 프로필 조회 실패")
  public TemplateRecommendationService.Result recommend(
      @org.springframework.security.core.annotation.AuthenticationPrincipal
          org.springframework.security.oauth2.jwt.Jwt jwt) {
    return recommendations.recommend(
        java.util.UUID.fromString(jwt.getSubject()), jwt.getTokenValue());
  }
}
