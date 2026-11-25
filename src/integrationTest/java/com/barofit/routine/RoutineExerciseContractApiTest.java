package com.barofit.routine;

import static org.hamcrest.Matchers.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.barofit.testsupport.BarofitTestTokens;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/** 23종 측정 계약과 V14 템플릿 정리 후 공개 API 회귀 검증. 응답 구조는 바꾸지 않는다 */
@RoutineIntegrationTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "barofit.identity.base-url=http://127.0.0.1:1")
class RoutineExerciseContractApiTest {
  private static final UUID USER = UUID.fromString("33333333-3333-3333-3333-333333333333");
  private static final String LOWER_INTERMEDIATE = "00000000-0000-0000-0000-000000000102";
  private static final String BACK_BEGINNER = "00000000-0000-0000-0000-000000000107";
  private static final String CARDIO_BEGINNER = "00000000-0000-0000-0000-000000000113";
  private static final String CORE_INTERMEDIATE = "00000000-0000-0000-0000-000000000111";
  private static final String CORE_ADVANCED = "00000000-0000-0000-0000-000000000112";
  @Autowired MockMvc mvc;
  @Autowired JdbcTemplate jdbc;

  @AfterEach
  void cleanup() {
    jdbc.update("DELETE FROM routine.workout_plan WHERE owner_user_id = ?", USER);
    jdbc.update("DELETE FROM routine.routine WHERE owner_user_id = ?", USER);
  }

  @Test
  void measurementTypesMatchTheFinalContract() throws Exception {
    mvc.perform(get("/api/v1/exercises/mountain-climber"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.measurementType").value("REPS"))
        .andExpect(jsonPath("$.assessmentStatus").value("READY"));
    mvc.perform(get("/api/v1/exercises/glute-bridge"))
        .andExpect(jsonPath("$.measurementType").value("REPS"));
    // V15: 플러터킥도 AI 분석기(flutter_kicks, count)와 같은 REPS
    mvc.perform(get("/api/v1/exercises/flutter-kick"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.measurementType").value("REPS"))
        .andExpect(jsonPath("$.assessmentStatus").value("READY"));
    mvc.perform(get("/api/v1/exercises"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(23)))
        .andExpect(
            jsonPath(
                "$[?(@.measurementType == 'DURATION')].id",
                containsInAnyOrder("plank", "side-plank")));
  }

  @Test
  void realignedTemplatesAreVisibleWithTargetsMatchingMeasurement() throws Exception {
    mvc.perform(templateDetail(LOWER_INTERMEDIATE))
        .andExpect(status().isOk())
        .andExpect(
            jsonPath(
                "$.exercises[*].exerciseId",
                contains("lunge", "split-squat", "glute-bridge", "side-lying-leg-raise")));
    mvc.perform(templateDetail(BACK_BEGINNER))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.exercises[*].exerciseId", contains("superman", "bird-dog")));
    mvc.perform(templateDetail(CARDIO_BEGINNER))
        .andExpect(status().isOk())
        .andExpect(
            jsonPath("$.exercises[*].exerciseId", contains("mountain-climber", "jumping-jack")))
        .andExpect(jsonPath("$.exercises[0].sets[*].targetReps", everyItem(is(16))))
        .andExpect(jsonPath("$.exercises[0].sets[0].targetDurationSeconds").isEmpty());
    for (String hidden : new String[] {"106", "108", "109"}) {
      mvc.perform(templateDetail("00000000-0000-0000-0000-000000000" + hidden))
          .andExpect(status().isNotFound());
    }
  }

  @Test
  void realignedTemplateCopiesIntoUserRoutineAndTodayPlan() throws Exception {
    String location =
        mvc.perform(
                post("/api/v1/routines")
                    .header("Authorization", bearer())
                    .contentType(APPLICATION_JSON)
                    .content("{\"templateId\":\"" + CARDIO_BEGINNER + "\"}"))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getHeader("Location");
    mvc.perform(get(location).header("Authorization", bearer()))
        .andExpect(status().isOk())
        .andExpect(
            jsonPath("$.exercises[*].exerciseId", contains("mountain-climber", "jumping-jack")))
        .andExpect(jsonPath("$.exercises[0].sets[0].targetReps").value(16));
    String routineId = location.substring(location.lastIndexOf('/') + 1);
    mvc.perform(
            put("/api/v1/workout-plans/2026-12-01")
                .header("Authorization", bearer())
                .contentType(APPLICATION_JSON)
                .content("{\"routineId\":\"" + routineId + "\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.routine.id").value(routineId))
        .andExpect(jsonPath("$.steps[*].type", hasItem("EXERCISE")));
  }

  @Test
  void flutterKickTemplatesCopyAndPlanWithRepetitionTargets() throws Exception {
    mvc.perform(templateDetail(CORE_INTERMEDIATE))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.exercises[3].exerciseId").value("flutter-kick"))
        .andExpect(jsonPath("$.exercises[3].sets[*].targetReps", everyItem(is(10))))
        .andExpect(jsonPath("$.exercises[3].sets[0].targetDurationSeconds").isEmpty());
    String location =
        mvc.perform(
                post("/api/v1/routines")
                    .header("Authorization", bearer())
                    .contentType(APPLICATION_JSON)
                    .content("{\"templateId\":\"" + CORE_ADVANCED + "\"}"))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getHeader("Location");
    mvc.perform(get(location).header("Authorization", bearer()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.exercises[2].exerciseId").value("flutter-kick"))
        .andExpect(jsonPath("$.exercises[2].sets[*].targetReps", everyItem(is(12))))
        .andExpect(jsonPath("$.exercises[2].sets[0].targetDurationSeconds").isEmpty());
    String routineId = location.substring(location.lastIndexOf('/') + 1);
    mvc.perform(
            put("/api/v1/workout-plans/2026-12-02")
                .header("Authorization", bearer())
                .contentType(APPLICATION_JSON)
                .content("{\"routineId\":\"" + routineId + "\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.routine.id").value(routineId));
  }

  // 템플릿 상세는 기존 계약대로 인증이 필요하다 (목록만 공개)
  private org.springframework.test.web.servlet.RequestBuilder templateDetail(String id) {
    return get("/api/v1/routine-templates/" + id).header("Authorization", bearer());
  }

  private String bearer() {
    return "Bearer " + BarofitTestTokens.accessToken(USER, Instant.now());
  }
}
