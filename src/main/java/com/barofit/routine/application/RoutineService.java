package com.barofit.routine.application;

import com.barofit.routine.domain.*;
import com.barofit.routine.dto.RoutineRequests.*;
import com.barofit.routine.dto.RoutineResponses.*;
import com.barofit.routine.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoutineService {
    private final RoutineRepository routines;
    private final ExerciseRepository exercises;

    public RoutinePage list(UUID userId, int page, int size) {
        var result = routines.findByOwnerUserIdAndType(userId, RoutineType.USER,
            PageRequest.of(page, size, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"))));
        return new RoutinePage(result.getContent().stream().map(Summary::from).toList(),
            page, size, result.getTotalElements(), result.getTotalPages());
    }

    public Detail detail(UUID userId, UUID id) { return Detail.from(owned(userId, id)); }

    public Detail template(UUID id) {
        return routines.findByIdAndType(id, RoutineType.TEMPLATE)
            .map(Detail::from)
            .orElseThrow(() -> new RoutineException(RoutineErrorCode.TEMPLATE_NOT_FOUND));
    }

    public List<Detail> templates(Difficulty difficulty, RoutineCategory category) {
        var templates = difficulty == null
            ? routines.findByTypeOrderByCreatedAtAscIdAsc(RoutineType.TEMPLATE)
            : routines.findByTypeAndDifficultyOrderByCreatedAtAscIdAsc(RoutineType.TEMPLATE, difficulty);
        return templates.stream().filter(t -> category == null || t.getCategory() == category)
            .map(Detail::from).toList();
    }

    @Transactional
    public Detail create(UUID userId, Create request) {
        Routine template = routines.findByIdAndType(request.templateId(), RoutineType.TEMPLATE)
            .orElseThrow(() -> new RoutineException(RoutineErrorCode.TEMPLATE_NOT_FOUND));
        if (template.getExercises().isEmpty() || template.getCategory() == null) {
            throw RoutineException.invalid("사용할 수 없는 템플릿입니다.");
        }
        Map<UUID, CreateExercise> changes = new HashMap<>();
        if (request.exercises() != null) {
            for (CreateExercise input : request.exercises()) {
                if (changes.put(input.templateExerciseId(), input) != null) {
                    throw RoutineException.invalid("같은 종목의 설정을 중복해서 전달할 수 없습니다.");
                }
            }
        }
        Routine routine = new Routine(userId, template);
        for (RoutineExercise source : template.getExercises()) {
            if (!source.getExercise().isActive() || source.getSets().isEmpty()) {
                throw RoutineException.invalid("사용할 수 없는 종목이 포함된 템플릿입니다.");
            }
            RoutineExercise item = new RoutineExercise(routine, source.getExercise(), source.getExerciseOrder());
            for (RoutineSet set : source.getSets()) {
                item.addSet(new RoutineSet(item, set.getSetOrder(), set.getTargetReps(), set.getTargetDurationSeconds(),
                    set.getWeightKg(), set.getRestSeconds()));
            }
            CreateExercise change = changes.remove(source.getId());
            if (change != null) {
                validateGoals(item, change);
                item.customize(change.setCount(), change.targetReps(), change.targetDurationSeconds(), change.restSeconds());
            }
            routine.addExercise(item);
        }
        if (!changes.isEmpty()) throw RoutineException.invalid("선택한 템플릿에 없는 종목입니다.");
        return Detail.from(routines.saveAndFlush(routine));
    }

    @Transactional
    public Detail update(UUID userId, UUID id, Update request) {
        Routine routine = owned(userId, id);
        Map<UUID, RoutineExercise> items = new HashMap<>();
        routine.getExercises().forEach(item -> items.put(item.getId(), item));
        Set<UUID> seen = new HashSet<>();
        // 전체 요청을 검증한 뒤 변경한다.
        for (UpdateExercise input : request.exercises()) {
            RoutineExercise item = items.get(input.routineExerciseId());
            if (item == null || !seen.add(input.routineExerciseId())) {
                throw RoutineException.invalid("루틴에 없는 종목이거나 중복된 설정입니다.");
            }
            validateGoals(item, input);
        }
        routine.touch();
        for (UpdateExercise input : request.exercises()) {
            items.get(input.routineExerciseId()).customize(input.setCount(), input.targetReps(),
                input.targetDurationSeconds(), input.restSeconds());
        }
        routines.flush();
        return Detail.from(routine);
    }

    public List<ExerciseSummary> replacementCandidates(UUID userId, UUID routineId, UUID routineExerciseId) {
        Routine routine = owned(userId, routineId);
        RoutineExercise current = findExercise(routine, routineExerciseId);
        Set<String> used = new HashSet<>();
        routine.getExercises().forEach(item -> used.add(item.getExercise().getId()));
        return exercises.findByActiveTrueOrderByDisplayOrderAsc().stream()
            .filter(candidate -> similar(current.getExercise(), candidate) && !used.contains(candidate.getId()))
            .map(ExerciseSummary::from).toList();
    }

    @Transactional
    public Detail replaceExercise(UUID userId, UUID routineId, UUID routineExerciseId, String exerciseId) {
        Routine routine = owned(userId, routineId);
        RoutineExercise current = findExercise(routine, routineExerciseId);
        Exercise replacement = exercises.findByIdAndActiveTrue(exerciseId)
            .orElseThrow(() -> new RoutineException(RoutineErrorCode.EXERCISE_NOT_FOUND));
        if (!similar(current.getExercise(), replacement))
            throw RoutineException.invalid("같은 분류·측정 방식·필요 기구의 종목으로만 교체할 수 있습니다.");
        if (routine.getExercises().stream().anyMatch(item -> item.getExercise().getId().equals(exerciseId)))
            throw RoutineException.invalid("이미 루틴에 포함된 종목입니다.");
        current.replaceExercise(replacement);
        routine.touch();
        routines.flush();
        return Detail.from(routine);
    }

    private RoutineExercise findExercise(Routine routine, UUID id) {
        return routine.getExercises().stream().filter(item -> item.getId().equals(id)).findFirst()
            .orElseThrow(() -> RoutineException.invalid("루틴에 없는 종목입니다."));
    }

    private boolean similar(Exercise current, Exercise candidate) {
        return current.getCategory() == candidate.getCategory()
            && current.getMeasurementType() == candidate.getMeasurementType()
            && current.getEquipment() == candidate.getEquipment();
    }

    @Transactional
    public void delete(UUID userId, UUID id) {
        routines.delete(owned(userId, id));
        routines.flush();
    }

    private Routine owned(UUID userId, UUID id) {
        return routines.findByIdAndOwnerUserIdAndType(id, userId, RoutineType.USER)
            .orElseThrow(RoutineException::notFound);
    }

    private void validateGoals(RoutineExercise item, Goals goals) {
        if (goals.setCount() == null && goals.targetReps() == null && goals.targetDurationSeconds() == null
            && goals.restSeconds() == null) {
            throw RoutineException.invalid("세트 수·목표·휴식 중 하나 이상 입력해 주세요.");
        }
        if (item.getSets().isEmpty()) throw RoutineException.invalid("세트 구성이 없는 종목입니다.");
        MeasurementType type = item.getExercise().getMeasurementType();
        if ((type == MeasurementType.REPS && goals.targetDurationSeconds() != null)
            || (type == MeasurementType.DURATION && goals.targetReps() != null)) {
            throw RoutineException.invalid("종목의 측정 방식에 맞는 목표만 입력해 주세요.");
        }
    }
}
