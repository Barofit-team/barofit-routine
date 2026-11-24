package com.barofit.routine.admin;

import static com.barofit.routine.admin.AdminCatalogDtos.*;

import jakarta.validation.Valid;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/routine")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminCatalogController {
  private final AdminCatalogService service;

  @GetMapping("/exercises")
  public List<ExerciseView> exercises() {
    return service.exercises();
  }

  @GetMapping("/templates")
  public List<TemplateView> templates() {
    return service.templates();
  }

  @GetMapping("/history")
  public List<CatalogAudit> history() {
    return service.history();
  }

  @PutMapping("/exercises/{id}")
  public ExerciseView updateExercise(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable String id,
      @Valid @RequestBody ExerciseEdit edit) {
    return service.updateExercise(UUID.fromString(jwt.getSubject()), id, edit);
  }

  @PutMapping("/templates/{id}")
  public TemplateView updateTemplate(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable UUID id,
      @Valid @RequestBody TemplateEdit edit) {
    return service.updateTemplate(UUID.fromString(jwt.getSubject()), id, edit);
  }
}
