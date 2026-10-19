package com.barofit.routine.application;

import com.barofit.routine.dto.RoutineResponses.*;
import com.barofit.routine.repository.ExerciseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExerciseService {
    private final ExerciseRepository exercises;

    public List<ExerciseSummary> list() {
        return exercises.findByActiveTrueOrderByDisplayOrderAsc().stream().map(ExerciseSummary::from).toList();
    }

    public ExerciseDetail detail(String id) {
        return exercises.findByIdAndActiveTrue(id).map(ExerciseDetail::from)
            .orElseThrow(() -> new RoutineException(RoutineErrorCode.EXERCISE_NOT_FOUND));
    }
}
