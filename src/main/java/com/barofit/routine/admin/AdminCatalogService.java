package com.barofit.routine.admin;

import static com.barofit.routine.admin.AdminCatalogDtos.*;

import com.barofit.routine.application.*;
import com.barofit.routine.domain.*;
import com.barofit.routine.dto.RoutineResponses.Detail;
import com.barofit.routine.repository.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminCatalogService {
  private static final Set<String> AREAS =
      Set.of("NECK_SHOULDER", "ARM_WRIST", "BACK", "LEG_ANKLE");
  private final ExerciseRepository exercises;
  private final RoutineRepository routines;
  private final CatalogAuditRepository audits;
  private final ObjectMapper json;

  public List<ExerciseView> exercises() {
    return exercises.findAll(Sort.by("displayOrder", "id")).stream().map(this::view).toList();
  }

  private ExerciseView view(Exercise e) {
    return new ExerciseView(
        e.getId(),
        e.getName(),
        e.getCategory(),
        e.getMeasurementType(),
        e.getAssessmentStatus(),
        e.isActive(),
        CatalogVisibility.available(e),
        e.getVersion(),
        Set.copyOf(e.getDiscomfortAreas()),
        e.getDescription(),
        e.getPreparation(),
        e.getInstructions(),
        e.getPrecautions(),
        e.getIntroTtsText());
  }

  public List<TemplateView> templates() {
    return routines.findByTypeOrderByCreatedAtAscIdAsc(RoutineType.TEMPLATE).stream()
        .map(this::view)
        .toList();
  }

  private TemplateView view(Routine r) {
    var reasons = new ArrayList<String>();
    if (r.getExercises().isEmpty()) reasons.add("종목 없음");
    for (var item : r.getExercises()) {
      if (!CatalogVisibility.available(item.getExercise()))
        reasons.add(item.getExercise().getName() + ": 비활성 또는 AI 검증 대기");
      if (item.getSets().isEmpty()) reasons.add(item.getExercise().getName() + ": 세트 없음");
    }
    return new TemplateView(
        Detail.from(r), r.getVersion(), CatalogVisibility.available(r), reasons);
  }

  @Transactional
  public ExerciseView updateExercise(UUID actor, String id, ExerciseEdit edit) {
    if (!AREAS.containsAll(edit.discomfortAreas()))
      throw RoutineException.invalid("지원하지 않는 불편 부위입니다.");
    var e =
        exercises
            .findById(id)
            .orElseThrow(() -> new RoutineException(RoutineErrorCode.EXERCISE_NOT_FOUND));
    if (e.getVersion() != edit.version())
      throw new RoutineException(RoutineErrorCode.ROUTINE_CONFLICT);
    var before = view(e);
    e.manage(
        edit.name(),
        edit.description(),
        edit.preparation(),
        edit.instructions(),
        edit.precautions(),
        edit.introTtsText(),
        edit.assessmentStatus(),
        edit.active(),
        edit.discomfortAreas());
    exercises.flush();
    var after = view(e);
    audit(actor, "EXERCISE", id, before, after);
    return after;
  }

  @Transactional
  public TemplateView updateTemplate(UUID actor, UUID id, TemplateEdit edit) {
    var r =
        routines
            .findByIdAndType(id, RoutineType.TEMPLATE)
            .orElseThrow(() -> new RoutineException(RoutineErrorCode.TEMPLATE_NOT_FOUND));
    if (r.getVersion() != edit.version())
      throw new RoutineException(RoutineErrorCode.ROUTINE_CONFLICT);
    var replacements = new ArrayList<RoutineExercise>();
    Set<String> seen = new HashSet<>();
    for (var input : edit.exercises()) {
      if (!seen.add(input.exerciseId())) throw RoutineException.invalid("같은 종목을 중복 배치할 수 없습니다.");
      var e =
          exercises
              .findById(input.exerciseId())
              .orElseThrow(() -> new RoutineException(RoutineErrorCode.EXERCISE_NOT_FOUND));
      var item = new RoutineExercise(r, e, replacements.size() + 1);
      int order = 0;
      for (var set : input.sets()) {
        if (e.getMeasurementType() == MeasurementType.REPS
            ? set.targetReps() == null || set.targetDurationSeconds() != null
            : set.targetDurationSeconds() == null || set.targetReps() != null)
          throw RoutineException.invalid("종목 측정 방식에 맞는 횟수 또는 시간을 입력하세요.");
        item.addSet(
            new RoutineSet(
                item,
                ++order,
                set.targetReps(),
                set.targetDurationSeconds(),
                set.weightKg(),
                set.restSeconds()));
      }
      replacements.add(item);
    }
    var before = view(r);
    r.manage(edit.name(), edit.description(), edit.difficulty(), edit.category());
    r.clearExercises();
    routines.flush();
    replacements.forEach(r::addExercise);
    routines.flush();
    var after = view(r);
    audit(actor, "TEMPLATE", id.toString(), before, after);
    return after;
  }

  public List<CatalogAudit> history() {
    return audits
        .findAll(
            PageRequest.of(0, 50, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"))))
        .getContent();
  }

  private void audit(UUID actor, String type, String id, Object before, Object after) {
    audits.save(
        new CatalogAudit(
            actor, type, id, json.writeValueAsString(before), json.writeValueAsString(after)));
  }
}
