package com.barofit.routine.controller;

import com.barofit.routine.application.RoutineService;
import com.barofit.routine.domain.Difficulty;
import com.barofit.routine.domain.RoutineCategory;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import com.barofit.routine.dto.RoutineResponses.Detail;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/routine-templates")
@RequiredArgsConstructor
@Tag(name = "Routine Template", description = "기본 루틴 조회")
public class RoutineTemplateController {
    private final RoutineService service;

    @GetMapping
    @Operation(summary = "기본 루틴 목록", description = "전신·하체·코어 각각 초급·중급·고급 템플릿을 제공합니다. category와 difficulty로 필터링합니다. 생략한 조건은 전체 조회. 종목·세트·가이드 포함. 인증 불필요.")
    @ApiResponse(responseCode = "200", description = "조회 성공", useReturnTypeSchema = true)
    @ApiResponse(responseCode = "400", description = "지원하지 않는 난이도",
        content = @Content(schema = @Schema(implementation = RoutineProblemDetail.class)))
    public List<Detail> list(
        @RequestParam(required = false) @Parameter(description = "BEGINNER(초급), INTERMEDIATE(중급), ADVANCED(고급)") Difficulty difficulty,
        @RequestParam(required = false) @Parameter(description = "FULL_BODY(전신), LOWER_BODY(하체), CORE(코어)") RoutineCategory category) {
        return service.templates(difficulty, category);
    }
}
