package com.barofit.routine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.barofit.routine.application.WorkoutEstimate;
import com.barofit.routine.repository.RoutineRepository;
import com.barofit.testsupport.BarofitTestTokens;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@RoutineIntegrationTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "barofit.identity.base-url=http://127.0.0.1:1")
class WorkoutPlanApiTests {
  private static final UUID USER = UUID.fromString("11111111-1111-1111-1111-111111111111");
  private static final UUID OTHER = UUID.fromString("22222222-2222-2222-2222-222222222222");
  private static final String TEMPLATE = "00000000-0000-0000-0000-000000000001";
  private static final String LOWER = "00000000-0000-0000-0000-000000000004";
  @Autowired MockMvc mvc;
  @Autowired JdbcTemplate jdbc;
  @Autowired RoutineRepository routines;

  @AfterEach
  void cleanup() {
    jdbc.update("DELETE FROM routine.workout_plan WHERE owner_user_id IN (?,?)", USER, OTHER);
  }

  @Test
  void assignReplaceReadWeekAndDeleteAreScopedToOwner() throws Exception {
    mvc.perform(get("/api/v1/workout-plans/today")).andExpect(status().isUnauthorized());
    mvc.perform(
            put("/api/v1/workout-plans/2026-10-19")
                .header("Authorization", bearer(USER))
                .contentType(APPLICATION_JSON)
                .content("{\"routineId\":\"" + TEMPLATE + "\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.routine.id").value(TEMPLATE))
        .andExpect(
            jsonPath(
                "$.steps[*].type", contains("EXERCISE", "REST", "EXERCISE", "REST", "EXERCISE")))
        .andExpect(jsonPath("$.estimatedMinutes", greaterThan(0)))
        .andExpect(jsonPath("$.estimatedKcal").isEmpty());
    mvc.perform(
            get("/api/v1/workout-plans")
                .param("weekStart", "2026-10-19")
                .header("Authorization", bearer(USER)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.days", hasSize(7)))
        .andExpect(jsonPath("$.days[0].routine.id").value(TEMPLATE))
        .andExpect(jsonPath("$.days[1].routine").isEmpty());
    mvc.perform(get("/api/v1/workout-plans/2026-10-19").header("Authorization", bearer(USER)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.routine.id").value(TEMPLATE));
    mvc.perform(get("/api/v1/workout-plans/2026-10-19").header("Authorization", bearer(OTHER)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.routine").isEmpty());
    mvc.perform(
            put("/api/v1/workout-plans/2026-10-19")
                .header("Authorization", bearer(USER))
                .contentType(APPLICATION_JSON)
                .content("{\"routineId\":\"" + LOWER + "\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.routine.id").value(LOWER));
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM routine.workout_plan WHERE owner_user_id=?",
                Integer.class,
                USER))
        .isEqualTo(1);
    mvc.perform(delete("/api/v1/workout-plans/2026-10-19").header("Authorization", bearer(USER)))
        .andExpect(status().isNoContent());
    mvc.perform(get("/api/v1/workout-plans/2026-10-19").header("Authorization", bearer(USER)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.routine").isEmpty());
  }

  @Test
  void invalidDateAndForeignRoutineAreRejected() throws Exception {
    mvc.perform(
            get("/api/v1/workout-plans")
                .param("weekStart", "2026-10-20")
                .header("Authorization", bearer(USER)))
        .andExpect(status().isBadRequest());
    mvc.perform(
            put("/api/v1/workout-plans/2026-10-19")
                .header("Authorization", bearer(USER))
                .contentType(APPLICATION_JSON)
                .content("{\"routineId\":\"" + UUID.randomUUID() + "\"}"))
        .andExpect(status().isNotFound());
  }

  @Test
  void todayAliasesReturnTheSameAssignedRoutine() throws Exception {
    String date = LocalDate.now(ZoneId.of("Asia/Seoul")).toString();
    mvc.perform(
            put("/api/v1/workout-plans/" + date)
                .header("Authorization", bearer(USER))
                .contentType(APPLICATION_JSON)
                .content("{\"routineId\":\"" + TEMPLATE + "\"}"))
        .andExpect(status().isOk());
    mvc.perform(get("/api/v1/workout-plans/today").header("Authorization", bearer(USER)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.routine.id").value(TEMPLATE));
    mvc.perform(get("/api/v1/routines/today").header("Authorization", bearer(USER)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.routine.id").value(TEMPLATE));
  }

  @Test
  @Transactional
  void estimateUsesConfiguredSetTargetsAndWeight() {
    var routine = routines.findById(UUID.fromString(TEMPLATE)).orElseThrow();
    var estimate = WorkoutEstimate.of(routine, new BigDecimal("84"));
    assertThat(estimate.estimatedMinutes()).isPositive();
    assertThat(estimate.estimatedKcal()).isPositive();
    assertThat(estimate.workSeconds()).isEqualTo(210);
    assertThat(WorkoutEstimate.of(routine, null).estimatedKcal()).isNull();
  }

  private String bearer(UUID user) {
    return "Bearer " + BarofitTestTokens.accessToken(user, Instant.now());
  }
}
