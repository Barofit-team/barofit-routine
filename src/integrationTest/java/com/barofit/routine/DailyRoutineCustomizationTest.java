package com.barofit.routine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.barofit.testsupport.BarofitTestTokens;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@RoutineIntegrationTest
@AutoConfigureMockMvc
@Transactional
@TestPropertySource(properties = "barofit.identity.base-url=http://127.0.0.1:1")
class DailyRoutineCustomizationTest {
  @Autowired MockMvc mvc;
  @Autowired ObjectMapper json;
  final UUID user = UUID.randomUUID();

  String auth(UUID id) {
    return "Bearer " + BarofitTestTokens.accessToken(id, java.time.Instant.now());
  }

  JsonNode source() throws Exception {
    return json.readTree(
        mvc.perform(
                get("/api/v1/routine-templates/00000000-0000-0000-0000-000000000001")
                    .header("Authorization", auth(user)))
            .andReturn()
            .getResponse()
            .getContentAsString());
  }

  Map<String, Object> input(JsonNode source, Object expected) {
    var edits = new ArrayList<Map<String, Object>>();
    for (int i = source.path("exercises").size() - 1; i >= 1; i--) {
      var e = source.path("exercises").get(i);
      var target = new HashMap<String, Object>();
      if (e.path("measurementType").asText().equals("REPS")) {
        target.put("targetReps", 12);
        target.put("timeLimitSeconds", 90);
      } else target.put("targetDurationSeconds", 45);
      target.put("weightKg", 0);
      target.put("restSeconds", 20);
      edits.add(Map.of("sourceRoutineExerciseId", e.path("id").asText(), "sets", List.of(target)));
    }
    var request = new HashMap<String, Object>();
    request.put("sourceRoutineId", source.path("id").asText());
    request.put("sourceUpdatedAt", source.path("updatedAt").asText());
    request.put("expectedRoutineId", expected);
    request.put("interExerciseRestSeconds", 80);
    request.put("exercises", edits);
    return request;
  }

  @Test
  void copiesOnlySelectedDatePreservingOriginalAndOtherDates() throws Exception {
    var original = source();
    String template = original.path("id").asText();
    for (String date : List.of("2026-11-25", "2026-11-26"))
      mvc.perform(
              put("/api/v1/workout-plans/" + date)
                  .header("Authorization", auth(user))
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(json.writeValueAsString(Map.of("routineId", template))))
          .andExpect(status().isOk());
    var body = input(original, template);
    String result =
        mvc.perform(
                put("/api/v1/workout-plans/2026-11-25/customization")
                    .header("Authorization", auth(user))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json.writeValueAsString(body)))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    var copy = json.readTree(result).path("routine");
    assertThat(copy.path("id").asText()).isNotEqualTo(template);
    assertThat(copy.path("interExerciseRestSeconds").asInt()).isEqualTo(80);
    assertThat(copy.path("exercises").size()).isEqualTo(original.path("exercises").size() - 1);
    assertThat(copy.path("exercises").get(0).path("exerciseId").asText())
        .isEqualTo(
            original
                .path("exercises")
                .get(original.path("exercises").size() - 1)
                .path("exerciseId")
                .asText());
    assertThat(source()).isEqualTo(original);
    mvc.perform(get("/api/v1/routines").header("Authorization", auth(user)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.length()").value(0));
    mvc.perform(get("/api/v1/workout-plans/2026-11-26").header("Authorization", auth(user)))
        .andExpect(jsonPath("$.routine.id").value(template));
    mvc.perform(
            put("/api/v1/workout-plans/2026-11-25/customization")
                .header("Authorization", auth(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(body)))
        .andExpect(status().isConflict());
    var foreign = input(copy, null);
    mvc.perform(
            put("/api/v1/workout-plans/2026-11-25/customization")
                .header("Authorization", auth(UUID.randomUUID()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(foreign)))
        .andExpect(status().isNotFound());
  }

  @Test
  void rejectsEmptyCompositionDuplicatesAndStaleSource() throws Exception {
    var source = source();
    var body = input(source, null);
    body.put("exercises", List.of());
    mvc.perform(
            put("/api/v1/workout-plans/2026-11-25/customization")
                .header("Authorization", auth(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(body)))
        .andExpect(status().isBadRequest());
    body = input(source, null);
    var edits = (List<?>) body.get("exercises");
    body.put("exercises", List.of(edits.get(0), edits.get(0)));
    mvc.perform(
            put("/api/v1/workout-plans/2026-11-25/customization")
                .header("Authorization", auth(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(body)))
        .andExpect(status().isBadRequest());
    body = input(source, null);
    body.put("interExerciseRestSeconds", 1.5);
    mvc.perform(
            put("/api/v1/workout-plans/2026-11-25/customization")
                .header("Authorization", auth(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(body)))
        .andExpect(status().isBadRequest());
    body = input(source, null);
    body.put("sourceUpdatedAt", "2000-01-01T00:00:00Z");
    mvc.perform(
            put("/api/v1/workout-plans/2026-11-25/customization")
                .header("Authorization", auth(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(body)))
        .andExpect(status().isConflict());
  }
}
