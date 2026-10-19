package com.barofit.routine.controller;

import com.barofit.routine.application.ExerciseService;
import com.barofit.routine.dto.RoutineResponses.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/exercises")
@RequiredArgsConstructor
@Tag(name = "Exercise", description = "운동 종목 및 화면·TTS 가이드")
public class ExerciseController {
    private final ExerciseService service;

    @GetMapping
    @Operation(summary = "운동 종목 목록", description = "활성 종목을 표시 순서대로 반환합니다. 인증 불필요.")
    public List<ExerciseSummary> list() { return service.list(); }

    @GetMapping("/{exerciseId}")
    @Operation(summary = "운동 상세 및 가이드", description = "준비 자세·수행 방법·주의사항과 TTS 원문을 반환합니다. 인증 불필요.")
    @ApiResponse(responseCode = "200", description = "조회 성공", useReturnTypeSchema = true)
    @ApiResponse(responseCode = "404", description = "존재하지 않거나 비활성인 종목",
        content = @Content(schema = @Schema(implementation = RoutineProblemDetail.class)))
    public ExerciseDetail detail(@PathVariable String exerciseId) { return service.detail(exerciseId); }
}
