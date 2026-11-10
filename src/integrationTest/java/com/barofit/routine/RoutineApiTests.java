package com.barofit.routine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.barofit.testsupport.BarofitTestTokens;
import com.jayway.jsonpath.JsonPath;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@RoutineIntegrationTest
@org.springframework.test.context.TestPropertySource(
    properties = "barofit.cors.allowed-origins=http://localhost:8081")
@AutoConfigureMockMvc
class RoutineApiTests {
  private static final UUID USER = UUID.fromString("11111111-1111-1111-1111-111111111111");
  private static final UUID OTHER = UUID.fromString("22222222-2222-2222-2222-222222222222");
  private static final String TEMPLATE = "00000000-0000-0000-0000-000000000001";
  private static final String SQUAT = "00000000-0000-0000-0001-000000000002";
  private static final String PLANK = "00000000-0000-0000-0001-000000000003";
  private static final String CREATE = "{\"templateId\":\"" + TEMPLATE + "\"}";
  @Autowired MockMvc mvc;
  @Autowired JdbcTemplate jdbc;

  @BeforeEach
  void cleanup() {
    jdbc.update("DELETE FROM routine.routine WHERE type = 'USER'");
  }

  @Test
  void corsAllowsConfiguredOriginAndStillRequiresAuthentication() throws Exception {
    mvc.perform(
            options("/api/v1/routines")
                .header("Origin", "http://localhost:8081")
                .header("Access-Control-Request-Method", "POST")
                .header("Access-Control-Request-Headers", "authorization,content-type"))
        .andExpect(status().isOk())
        .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:8081"));
    mvc.perform(
            options("/api/v1/workout-plans/2026-10-19")
                .header("Origin", "http://localhost:8081")
                .header("Access-Control-Request-Method", "PUT")
                .header("Access-Control-Request-Headers", "authorization,content-type"))
        .andExpect(status().isOk())
        .andExpect(header().string("Access-Control-Allow-Methods", containsString("PUT")));
    mvc.perform(
            options("/api/v1/routines")
                .header("Origin", "https://untrusted.example")
                .header("Access-Control-Request-Method", "POST"))
        .andExpect(status().isForbidden());
    mvc.perform(get("/api/v1/routines").header("Origin", "http://localhost:8081"))
        .andExpect(status().isUnauthorized())
        .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:8081"));
  }

  @Test
  void expandedCatalogAndTemplatesExposeCategoryGuidesAndGoals() throws Exception {
    mvc.perform(get("/api/v1/exercises"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(47)))
        .andExpect(
            jsonPath("$[*].id", hasItems("push-up", "squat", "plank", "bird-dog", "tuck-jump")));
    mvc.perform(get("/api/v1/exercises/plank"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.measurementType").value("DURATION"))
        .andExpect(jsonPath("$.introTtsText", not(emptyString())))
        .andExpect(jsonPath("$.assessmentStatus").value("READY"));
    mvc.perform(get("/api/v1/exercises/glute-bridge"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.assessmentStatus").value("READY"));
    mvc.perform(get("/api/v1/routine-templates"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(24)))
        .andExpect(jsonPath("$[0].category").value("FULL_BODY"));
    for (String category :
        new String[] {"FULL_BODY", "LOWER_BODY", "UPPER_BODY", "BACK", "CORE", "CARDIO"}) {
      int count = category.equals("LOWER_BODY") || category.equals("CORE") ? 6 : 3;
      mvc.perform(get("/api/v1/routine-templates").param("category", category))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$", hasSize(count)))
          .andExpect(jsonPath("$[*].category", everyItem(is(category))));
      for (String level : new String[] {"BEGINNER", "INTERMEDIATE", "ADVANCED"}) {
        mvc.perform(
                get("/api/v1/routine-templates")
                    .param("category", category)
                    .param("difficulty", level))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(count / 3)))
            .andExpect(jsonPath("$[0].difficulty").value(level));
      }
    }
    mvc.perform(get("/api/v1/routine-templates").param("difficulty", "BEGINNER"))
        .andExpect(jsonPath("$", hasSize(8)));
    mvc.perform(get("/api/v1/routine-templates").param("category", "UNKNOWN"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
  }

  @Test
  void defaultCreationCopiesWholeTemplateAndListsOnlyOwner() throws Exception {
    String location = createRoutine();
    ownGet(location)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.templateId").value(TEMPLATE))
        .andExpect(jsonPath("$.category").value("FULL_BODY"))
        .andExpect(jsonPath("$.name").value("초급 전신 루틴"))
        .andExpect(jsonPath("$.difficulty").value("BEGINNER"))
        .andExpect(jsonPath("$.exercises[*].exerciseId", contains("push-up", "squat", "plank")))
        .andExpect(jsonPath("$.exercises[*].order", contains(1, 2, 3)))
        .andExpect(jsonPath("$.exercises[0].sets", hasSize(2)))
        .andExpect(jsonPath("$.exercises[0].sets[0].targetReps").value(10))
        .andExpect(jsonPath("$.exercises[1].sets[0].targetReps").value(15))
        .andExpect(jsonPath("$.exercises[2].sets[0].targetDurationSeconds").value(30))
        .andExpect(jsonPath("$.exercises[*].introTtsText", everyItem(not(emptyString()))));
    mvc.perform(get("/api/v1/routines").header("Authorization", bearer(USER)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].totalSetCount").value(6))
        .andExpect(jsonPath("$.content[0].templateId").value(TEMPLATE));
    mvc.perform(get("/api/v1/routines").header("Authorization", bearer(OTHER)))
        .andExpect(jsonPath("$.content", hasSize(0)));
    mvc.perform(
            get("/api/v1/routines")
                .header("Authorization", bearer(USER))
                .param("page", "1")
                .param("size", "1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(0)))
        .andExpect(jsonPath("$.totalElements").value(1));
  }

  @Test
  void createCustomizesOnlySpecifiedTemplateItemsAndKeepsOrder() throws Exception {
    create(
            """
            {"templateId":"%s","exercises":[
              {"templateExerciseId":"%s","setCount":1,"targetDurationSeconds":20},
              {"templateExerciseId":"%s","setCount":3,"targetReps":12}]}
            """
                .formatted(TEMPLATE, PLANK, SQUAT))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.exercises[*].exerciseId", contains("push-up", "squat", "plank")))
        .andExpect(jsonPath("$.exercises[0].sets", hasSize(2)))
        .andExpect(jsonPath("$.exercises[1].sets", hasSize(3)))
        .andExpect(jsonPath("$.exercises[1].sets[*].targetReps", everyItem(is(12))))
        .andExpect(jsonPath("$.exercises[2].sets", hasSize(1)))
        .andExpect(jsonPath("$.exercises[2].sets[0].targetDurationSeconds").value(20))
        .andExpect(jsonPath("$.exercises[*].sets[*].restSeconds", everyItem(is(60))))
        .andExpect(jsonPath("$.exercises[*].sets[*].weightKg", everyItem(is(0.0))))
        .andExpect(jsonPath("$.difficulty").value("BEGINNER"));
    mvc.perform(
            get("/api/v1/routine-templates")
                .param("category", "FULL_BODY")
                .param("difficulty", "BEGINNER"))
        .andExpect(jsonPath("$[0].exercises[1].sets", hasSize(2)))
        .andExpect(jsonPath("$[0].exercises[1].sets[0].targetReps").value(15));
  }

  @Test
  void updateKeepsItemIdsAndOtherItemsWhileResizingAndChangingTargets() throws Exception {
    String location = createRoutine();
    String item = itemId(location, 1);
    ownPatch(location, patchItem(item, "\"setCount\":1,\"targetReps\":8"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.exercises", hasSize(3)))
        .andExpect(jsonPath("$.exercises[1].id").value(item))
        .andExpect(jsonPath("$.exercises[1].sets", hasSize(1)))
        .andExpect(jsonPath("$.exercises[0].sets", hasSize(2)));
    ownPatch(location, patchItem(item, "\"setCount\":4"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.exercises[1].sets", hasSize(4)))
        .andExpect(jsonPath("$.exercises[1].sets[*].order", contains(1, 2, 3, 4)))
        .andExpect(jsonPath("$.exercises[1].sets[*].targetReps", everyItem(is(8))))
        .andExpect(jsonPath("$.exercises[1].sets[*].restSeconds", everyItem(is(60))));
    String plank = itemId(location, 2);
    ownPatch(location, patchItem(plank, "\"targetDurationSeconds\":25"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.exercises[2].sets", hasSize(2)))
        .andExpect(jsonPath("$.exercises[2].sets[*].targetDurationSeconds", everyItem(is(25))));
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM routine.routine_set WHERE routine_exercise_id = ?",
                Integer.class,
                UUID.fromString(item)))
        .isEqualTo(4);
  }

  @Test
  void restCanChangeAndSimilarExerciseCanReplaceInPlace() throws Exception {
    String location = createRoutine();
    String item = itemId(location, 1);
    ownPatch(location, patchItem(item, "\"restSeconds\":90"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.exercises[1].sets[*].restSeconds", everyItem(is(90))));
    mvc.perform(
            get(location + "/exercises/" + item + "/replacements")
                .header("Authorization", bearer(USER)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[*].id", hasItem("forward-lunge")))
        .andExpect(jsonPath("$[*].id", not(hasItem("push-up"))));
    mvc.perform(
            put(location + "/exercises/" + item + "/replacement")
                .header("Authorization", bearer(USER))
                .contentType(APPLICATION_JSON)
                .content("{\"exerciseId\":\"forward-lunge\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.exercises[1].id").value(item))
        .andExpect(jsonPath("$.exercises[1].exerciseId").value("forward-lunge"))
        .andExpect(jsonPath("$.exercises[1].sets[*].targetReps", everyItem(is(15))))
        .andExpect(jsonPath("$.exercises[1].sets[*].restSeconds", everyItem(is(90))));
    mvc.perform(
            put(location + "/exercises/" + item + "/replacement")
                .header("Authorization", bearer(USER))
                .contentType(APPLICATION_JSON)
                .content("{\"exerciseId\":\"push-up\"}"))
        .andExpect(status().isBadRequest());
    mvc.perform(
            put(location + "/exercises/" + item + "/replacement")
                .header("Authorization", bearer(OTHER))
                .contentType(APPLICATION_JSON)
                .content("{\"exerciseId\":\"squat\"}"))
        .andExpect(status().isNotFound());
  }

  @Test
  void existingRoutineIsIndependentOfLaterTemplateChanges() throws Exception {
    String location = createRoutine();
    jdbc.update(
        "UPDATE routine.routine SET name = '수정된 템플릿' WHERE id = ?", UUID.fromString(TEMPLATE));
    jdbc.update(
        "UPDATE routine.routine_set SET target_reps = 99, rest_seconds = 90 WHERE routine_exercise_id = ?",
        UUID.fromString(SQUAT));
    try {
      ownGet(location)
          .andExpect(jsonPath("$.name").value("초급 전신 루틴"))
          .andExpect(jsonPath("$.exercises[1].sets[0].targetReps").value(15));
      ownPatch(location, patchItem(itemId(location, 1), "\"setCount\":3"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.exercises[1].sets[*].targetReps", everyItem(is(15))))
          .andExpect(jsonPath("$.exercises[1].sets[*].restSeconds", everyItem(is(60))));
    } finally {
      jdbc.update(
          "UPDATE routine.routine SET name = '초급 전신 루틴' WHERE id = ?", UUID.fromString(TEMPLATE));
      jdbc.update(
          "UPDATE routine.routine_set SET target_reps = 15, rest_seconds = 60 WHERE routine_exercise_id = ?",
          UUID.fromString(SQUAT));
    }
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "{}",
        "{\"name\":\"자유 생성\",\"difficulty\":\"BEGINNER\",\"exercises\":[]}",
        "{\"templateId\":null}",
        "{\"templateId\":\"invalid\"}"
      })
  void rejectsCreationWithoutValidTemplateId(String body) throws Exception {
    create(body).andExpect(status().isBadRequest());
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "\"name\":\"변경\"",
        "\"difficulty\":\"ADVANCED\"",
        "\"category\":\"CORE\"",
        "\"ownerUserId\":\"11111111-1111-1111-1111-111111111111\"",
        "\"weightKg\":10",
        "\"restSeconds\":0"
      })
  void rejectsForbiddenTopLevelFieldsOnCreateAndUpdate(String field) throws Exception {
    create("{\"templateId\":\"" + TEMPLATE + "\"," + field + "}")
        .andExpect(status().isBadRequest());
    String location = createRoutine();
    ownPatch(
            location,
            "{\"exercises\":[{\"routineExerciseId\":\""
                + itemId(location, 1)
                + "\",\"setCount\":3}],"
                + field
                + "}")
        .andExpect(status().isBadRequest());
  }

  @ParameterizedTest
  @ValueSource(
      strings = {"\"exerciseId\":\"push-up\"", "\"order\":1", "\"weightKg\":10", "\"sets\":[]"})
  void rejectsForbiddenNestedFieldsOnCreateAndUpdate(String field) throws Exception {
    create(
            "{\"templateId\":\""
                + TEMPLATE
                + "\",\"exercises\":[{\"templateExerciseId\":\""
                + SQUAT
                + "\",\"setCount\":3,"
                + field
                + "}]}")
        .andExpect(status().isBadRequest());
    String location = createRoutine();
    ownPatch(location, patchItem(itemId(location, 1), "\"setCount\":3," + field))
        .andExpect(status().isBadRequest());
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "\"setCount\":0",
        "\"setCount\":21",
        "\"targetReps\":0",
        "\"targetReps\":-1",
        "\"targetReps\":1.5",
        "\"setCount\":2.5",
        "\"setCount\":\"3\"",
        "\"targetDurationSeconds\":30",
        "\"targetReps\":10,\"targetDurationSeconds\":30",
        "\"setCount\":null,\"targetReps\":null",
        "\"restSeconds\":-1",
        "\"restSeconds\":601"
      })
  void rejectsInvalidGoalsInBothRequests(String fields) throws Exception {
    create(
            "{\"templateId\":\""
                + TEMPLATE
                + "\",\"exercises\":[{\"templateExerciseId\":\""
                + SQUAT
                + "\","
                + fields
                + "}]}")
        .andExpect(status().isBadRequest());
    String location = createRoutine();
    ownPatch(location, patchItem(itemId(location, 1), fields)).andExpect(status().isBadRequest());
  }

  @Test
  void rejectsForeignItemsDuplicatesAndMissingChangesWithoutPartialUpdates() throws Exception {
    String bad = "{\"templateExerciseId\":\"00000000-0000-0000-0001-000000000010\",\"setCount\":3}";
    create("{\"templateId\":\"" + TEMPLATE + "\",\"exercises\":[" + bad + "]}")
        .andExpect(status().isBadRequest());
    String change = "{\"templateExerciseId\":\"" + SQUAT + "\",\"setCount\":3}";
    create("{\"templateId\":\"" + TEMPLATE + "\",\"exercises\":[" + change + "," + change + "]}")
        .andExpect(status().isBadRequest());
    String location = createRoutine();
    String item = itemId(location, 1);
    change = "{\"routineExerciseId\":\"" + item + "\",\"setCount\":3}";
    ownPatch(location, "{\"exercises\":[" + change + "," + change + "]}")
        .andExpect(status().isBadRequest());
    ownPatch(
            location,
            "{\"exercises\":["
                + change
                + ",{\"routineExerciseId\":\""
                + SQUAT
                + "\",\"setCount\":3}]}")
        .andExpect(status().isBadRequest());
    String otherLocation = createRoutine();
    ownPatch(location, patchItem(itemId(otherLocation, 1), "\"setCount\":3"))
        .andExpect(status().isBadRequest());
    ownPatch(location, patchItem(itemId(location, 2), "\"targetReps\":10"))
        .andExpect(status().isBadRequest());
    for (String body :
        new String[] {
          "{}",
          "{\"exercises\":[]}",
          "{\"exercises\":null}",
          "{\"exercises\":[null]}",
          "{\"exercises\":[{\"routineExerciseId\":\"" + item + "\"}]}"
        }) {
      ownPatch(location, body).andExpect(status().isBadRequest());
    }
    ownGet(location).andExpect(jsonPath("$.exercises[1].sets", hasSize(2)));
  }

  @Test
  void rejectsMissingUserAndInactiveTemplates() throws Exception {
    create("{\"templateId\":\"" + UUID.randomUUID() + "\"}")
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("TEMPLATE_NOT_FOUND"));
    String location = createRoutine();
    create("{\"templateId\":\"" + id(location) + "\"}").andExpect(status().isNotFound());
    jdbc.update("UPDATE routine.exercise SET active = false WHERE id = 'squat'");
    try {
      create(CREATE).andExpect(status().isBadRequest());
    } finally {
      jdbc.update("UPDATE routine.exercise SET active = true WHERE id = 'squat'");
    }
  }

  @Test
  void optionalOverridesAndSetCountBoundsKeepCompleteComposition() throws Exception {
    for (String suffix : new String[] {"", ",\"exercises\":null", ",\"exercises\":[]"}) {
      create("{\"templateId\":\"" + TEMPLATE + "\"" + suffix + "}")
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.exercises", hasSize(3)));
    }
    create(
            "{\"templateId\":\""
                + TEMPLATE
                + "\",\"exercises\":[{\"templateExerciseId\":\""
                + SQUAT
                + "\",\"setCount\":20}]}")
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.exercises[1].sets", hasSize(20)))
        .andExpect(jsonPath("$.exercises[1].sets[*].targetReps", everyItem(is(15))));
    create("{\"templateId\":\"" + TEMPLATE + "\",\"exercises\":[null]}")
        .andExpect(status().isBadRequest());
    String item = "{\"templateExerciseId\":\"" + SQUAT + "\",\"setCount\":2}";
    create(
            "{\"templateId\":\""
                + TEMPLATE
                + "\",\"exercises\":["
                + String.join(",", java.util.Collections.nCopies(21, item))
                + "]}")
        .andExpect(status().isBadRequest());
    for (String value : new String[] {"0", "-1", "1.5"}) {
      create(
              "{\"templateId\":\""
                  + TEMPLATE
                  + "\",\"exercises\":[{\"templateExerciseId\":\""
                  + PLANK
                  + "\",\"targetDurationSeconds\":"
                  + value
                  + "}]}")
          .andExpect(status().isBadRequest());
    }
  }

  @Test
  void legacyRoutineWithNoSourceCanStillAdjustGoalsWithoutChangingComposition() throws Exception {
    String location = createRoutine();
    jdbc.update(
        "UPDATE routine.routine SET template_id = NULL, category = NULL WHERE id = ?",
        id(location));
    String item = itemId(location, 1);
    ownPatch(location, patchItem(item, "\"setCount\":3,\"targetReps\":8"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.templateId").isEmpty())
        .andExpect(jsonPath("$.exercises[*].exerciseId", contains("push-up", "squat", "plank")))
        .andExpect(jsonPath("$.exercises[1].sets", hasSize(3)));
  }

  @Test
  void databaseStillRejectsInvalidTargetsDuplicateOrdersAndMissingOwners() throws Exception {
    String location = createRoutine();
    UUID item = UUID.fromString(itemId(location, 1));
    org.assertj.core.api.Assertions.assertThatThrownBy(() ->
            jdbc.update(
                """
                INSERT INTO routine.routine_set (id, routine_exercise_id, set_order, target_reps, target_duration_seconds, weight_kg, rest_seconds)
                VALUES (?, ?, 3, 10, 30, 0, 60)
                """,
                UUID.randomUUID(),
                item))
        .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    org.assertj.core.api.Assertions.assertThatThrownBy(() ->
            jdbc.update(
                """
                INSERT INTO routine.routine_exercise (id, routine_id, exercise_id, exercise_order)
                VALUES (?, ?, 'plank', 1)
                """,
                UUID.randomUUID(),
                id(location)))
        .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    org.assertj.core.api.Assertions.assertThatThrownBy(() ->
            jdbc.update(
                """
                INSERT INTO routine.routine (id, type, name, difficulty)
                VALUES (?, 'USER', '소유자 없음', 'BEGINNER')
                """,
                UUID.randomUUID()))
        .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
  }

  @Test
  void ownershipAndDeletionRemainProtected() throws Exception {
    String location = createRoutine();
    String item = itemId(location, 0);
    for (String path : new String[] {location, "/api/v1/routines/" + TEMPLATE}) {
      mvc.perform(get(path).header("Authorization", bearer(OTHER)))
          .andExpect(status().isNotFound());
      mvc.perform(
              patch(path)
                  .header("Authorization", bearer(OTHER))
                  .contentType(APPLICATION_JSON)
                  .content(patchItem(item, "\"setCount\":3")))
          .andExpect(status().isNotFound());
      mvc.perform(delete(path).header("Authorization", bearer(OTHER)))
          .andExpect(status().isNotFound());
    }
    mvc.perform(delete(location).header("Authorization", bearer(USER)))
        .andExpect(status().isNoContent());
    ownGet(location).andExpect(status().isNotFound());
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM routine.routine_exercise WHERE routine_id = ?",
                Integer.class,
                id(location)))
        .isZero();
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM routine.routine_set WHERE routine_exercise_id = ?",
                Integer.class,
                UUID.fromString(item)))
        .isZero();
  }

  @Test
  void jwtAndProblemDetailContractsRemainIntact() throws Exception {
    mvc.perform(get("/api/v1/routine-templates/recommendations"))
        .andExpect(status().isUnauthorized());
    mvc.perform(get("/api/v1/routines").header("X-Local-User-Id", USER))
        .andExpect(status().isUnauthorized());
    String expired = BarofitTestTokens.accessToken(USER, Instant.now().minusSeconds(3600));
    mvc.perform(get("/api/v1/routines").header("Authorization", "Bearer " + expired))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("AUTH_TOKEN_EXPIRED"));
    mvc.perform(get("/api/v1/routines").header("Authorization", "Bearer invalid"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("AUTH_INVALID_TOKEN"));
    mvc.perform(get("/api/v1/routine-templates").header("Authorization", "Bearer " + expired))
        .andExpect(status().isOk());
    create("{}")
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("INVALID_INPUT"))
        .andExpect(jsonPath("$.detail", not(emptyString())))
        .andExpect(jsonPath("$.errors").isArray());
    mvc.perform(get("/api/v1/routines").header("Authorization", bearer(USER)).param("size", "101"))
        .andExpect(status().isBadRequest());
    mvc.perform(get("/api/v1/routines/not-a-uuid").header("Authorization", bearer(USER)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void swaggerDescribesOnlyAllowedCustomizationFields() throws Exception {
    mvc.perform(get("/v3/api-docs"))
        .andExpect(status().isOk())
        .andExpect(
            jsonPath("$.components.schemas.CreateRoutineRequest.properties.templateId").exists())
        .andExpect(
            jsonPath("$.components.schemas.CreateRoutineRequest.properties.difficulty")
                .doesNotExist())
        .andExpect(
            jsonPath("$.components.schemas.UpdateRoutineRequest.properties.name").doesNotExist())
        .andExpect(
            jsonPath("$.components.schemas.TemplateExerciseCustomization.properties.setCount")
                .exists())
        .andExpect(
            jsonPath(
                    "$.components.schemas.RoutineExerciseCustomization.properties.routineExerciseId")
                .exists())
        .andExpect(jsonPath("$.components.securitySchemes.routineAuth.scheme").value("bearer"))
        .andExpect(jsonPath("$.paths['/api/v1/workout-plans'].get").exists())
        .andExpect(jsonPath("$.paths['/api/v1/workout-plans/{date}'].put").exists())
        .andExpect(jsonPath("$.paths['/api/v1/workout-plans/today'].get").exists());
    mvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk());
  }

  @Test
  // 폴리레포 분리 전에는 identity와 한 DB를 공유해서 "독립된 마이그레이션 히스토리"까지 같이
  // 검증했다. 지금은 레포가 분리돼 이 컨텍스트엔 identity 스키마가 존재하지 않으므로, 이 레포
  // 자신의 마이그레이션 개수만 확인한다.
  void migrationHistoryIncludesTemplateCustomization() {
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM routine.flyway_schema_history WHERE success AND version IS NOT NULL",
                Integer.class))
        .isEqualTo(8);
  }

  private ResultActions create(String body) throws Exception {
    return mvc.perform(
        post("/api/v1/routines")
            .header("Authorization", bearer(USER))
            .contentType(APPLICATION_JSON)
            .content(body));
  }

  private String createRoutine() throws Exception {
    return create(CREATE)
        .andExpect(status().isCreated())
        .andReturn()
        .getResponse()
        .getHeader("Location");
  }

  private ResultActions ownGet(String path) throws Exception {
    return mvc.perform(get(path).header("Authorization", bearer(USER)));
  }

  private ResultActions ownPatch(String path, String body) throws Exception {
    return mvc.perform(
        patch(path)
            .header("Authorization", bearer(USER))
            .contentType(APPLICATION_JSON)
            .content(body));
  }

  private String itemId(String path, int index) throws Exception {
    return JsonPath.read(
        ownGet(path).andExpect(status().isOk()).andReturn().getResponse().getContentAsString(),
        "$.exercises[" + index + "].id");
  }

  private String patchItem(String item, String fields) {
    return "{\"exercises\":[{\"routineExerciseId\":\"" + item + "\"," + fields + "}]}";
  }

  private String bearer(UUID user) {
    return "Bearer " + BarofitTestTokens.accessToken(user, Instant.now());
  }

  private UUID id(String location) {
    return UUID.fromString(location.substring(location.lastIndexOf('/') + 1));
  }
}
