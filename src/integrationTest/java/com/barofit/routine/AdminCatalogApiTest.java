package com.barofit.routine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.barofit.routine.admin.*;
import com.barofit.routine.admin.AdminCatalogDtos.*;
import com.barofit.routine.domain.*;
import com.barofit.testsupport.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

@RoutineIntegrationTest
@AutoConfigureMockMvc
@Transactional
class AdminCatalogApiTest {
  @Autowired MockMvc mvc;
  @Autowired ObjectMapper json;
  @Autowired AdminCatalogService service;
  @Autowired CatalogAuditRepository audits;
  private final UUID actor = UUID.randomUUID();

  String admin() {
    return "Bearer "
        + BarofitTestTokens.accessToken(
            BarofitJwtKeyTestSupport.KEYS.keyPair(),
            actor,
            Instant.now(),
            c -> c.claim("role", "ADMIN"));
  }

  @Test
  void requiresAdminForEveryReadAndWrite() throws Exception {
    for (String path : List.of("exercises", "templates", "history")) {
      mvc.perform(get("/api/v1/admin/routine/" + path)).andExpect(status().isUnauthorized());
      mvc.perform(
              get("/api/v1/admin/routine/" + path)
                  .header(
                      "Authorization",
                      "Bearer " + BarofitTestTokens.accessToken(actor, Instant.now())))
          .andExpect(status().isForbidden());
    }
    var exercise =
        service.exercises().stream().filter(e -> e.id().equals("squat")).findFirst().orElseThrow();
    mvc.perform(
            put("/api/v1/admin/routine/exercises/squat")
                .header(
                    "Authorization",
                    "Bearer " + BarofitTestTokens.accessToken(actor, Instant.now()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    json.writeValueAsString(
                        edit(exercise, exercise.version(), Set.of("LEG_ANKLE")))))
        .andExpect(status().isForbidden());
    assertThat(audits.count()).isZero();
  }

  ExerciseEdit edit(ExerciseView e, long version, Set<String> areas) {
    return new ExerciseEdit(
        version,
        e.name(),
        "새로운 스쿼트 설명",
        e.preparation(),
        e.instructions(),
        e.precautions(),
        e.introTtsText(),
        AssessmentStatus.PLANNED,
        true,
        areas);
  }

  @Test
  void exposesAllCatalogAndAuditsChangesWithConflictProtection() throws Exception {
    mvc.perform(get("/api/v1/admin/routine/exercises").header("Authorization", admin()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(47));
    var e =
        service.exercises().stream().filter(x -> x.id().equals("squat")).findFirst().orElseThrow();
    var input = edit(e, e.version(), Set.of("LEG_ANKLE", "BACK"));
    mvc.perform(
            put("/api/v1/admin/routine/exercises/squat")
                .header("Authorization", admin())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(input)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.visible").value(false))
        .andExpect(jsonPath("$.version").value(e.version() + 1));
    assertThat(audits.findAll()).hasSize(1);
    assertThat(audits.findAll().getFirst().getActorId()).isEqualTo(actor);
    assertThat(audits.findAll().getFirst().getBeforeState()).contains(e.description());
    assertThat(audits.findAll().getFirst().getAfterState()).contains("새로운 스쿼트 설명");
    mvc.perform(
            put("/api/v1/admin/routine/exercises/squat")
                .header("Authorization", admin())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(input)))
        .andExpect(status().isConflict());
    assertThat(audits.count()).isEqualTo(1);
    mvc.perform(get("/api/v1/exercises/squat")).andExpect(status().isNotFound());
  }

  @Test
  void rejectsInvalidMappingWithoutAudit() throws Exception {
    var e = service.exercises().getFirst();
    mvc.perform(
            put("/api/v1/admin/routine/exercises/" + e.id())
                .header("Authorization", admin())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(edit(e, e.version(), Set.of("NONE")))))
        .andExpect(status().isBadRequest());
    assertThat(audits.count()).isZero();
  }

  @Test
  void replacesTemplateCompositionPreservingIndividualSetTargetsAndAudits() throws Exception {
    var t = service.templates().getFirst();
    var sets =
        List.of(
            new SetEdit(8, null, new BigDecimal("2.50"), 45),
            new SetEdit(12, null, new BigDecimal("3.25"), 90));
    var input =
        new TemplateEdit(
            t.version(),
            "관리자 수정 루틴",
            t.detail().description(),
            Difficulty.BEGINNER,
            RoutineCategory.LOWER_BODY,
            List.of(new ItemEdit("squat", sets)));
    mvc.perform(
            put("/api/v1/admin/routine/templates/" + t.detail().id())
                .header("Authorization", admin())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(input)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.detail.exercises.length()").value(1))
        .andExpect(jsonPath("$.detail.exercises[0].sets[1].targetReps").value(12))
        .andExpect(jsonPath("$.detail.exercises[0].sets[1].weightKg").value(3.25))
        .andExpect(jsonPath("$.visible").value(true));
    assertThat(audits.count()).isEqualTo(1);
    mvc.perform(
            put("/api/v1/admin/routine/templates/" + t.detail().id())
                .header("Authorization", admin())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(input)))
        .andExpect(status().isConflict());
  }

  @Test
  void rejectsDurationForRepetitionExerciseAndLeavesOriginalComposition() throws Exception {
    var t = service.templates().getFirst();
    var input =
        new TemplateEdit(
            t.version(),
            t.detail().name(),
            t.detail().description(),
            Difficulty.BEGINNER,
            RoutineCategory.CORE,
            List.of(new ItemEdit("squat", List.of(new SetEdit(null, 30, BigDecimal.ZERO, 60)))));
    mvc.perform(
            put("/api/v1/admin/routine/templates/" + t.detail().id())
                .header("Authorization", admin())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(input)))
        .andExpect(status().isBadRequest());
    assertThat(service.templates().getFirst().detail().exercises())
        .hasSize(t.detail().exercises().size());
    assertThat(audits.count()).isZero();
  }
}
