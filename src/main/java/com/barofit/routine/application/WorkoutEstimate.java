package com.barofit.routine.application;

import com.barofit.routine.domain.*;
import java.math.BigDecimal;

/** 계획 목표로 계산한 화면용 추정치. 실제 운동 시간·소모 열량이 아니다. */
public final class WorkoutEstimate {
    private WorkoutEstimate() {}

    public record Result(int estimatedMinutes, Integer estimatedKcal, int workSeconds, int restSeconds) {}

    public static Result of(Routine routine, BigDecimal weightKg) {
        int work = 0;
        int rest = 0;
        int totalSets = routine.getExercises().stream().mapToInt(e -> e.getSets().size()).sum();
        int seenSets = 0;
        for (RoutineExercise exercise : routine.getExercises()) {
            for (RoutineSet set : exercise.getSets()) {
                work += set.getTargetDurationSeconds() != null
                    ? set.getTargetDurationSeconds() : set.getTargetReps() * 3;
                if (++seenSets < totalSets) rest += set.getRestSeconds();
            }
        }
        // 종목 이동 시간을 종목당 10초로 표시한다.
        int transition = routine.getExercises().size() * 10;
        int minutes = (work + rest + transition + 59) / 60;
        Integer kcal = null;
        if (weightKg != null && weightKg.signum() > 0) {
            double met = routine.getCategory() == RoutineCategory.CARDIO
                && routine.getDifficulty() == Difficulty.ADVANCED ? 7.5 : 3.8;
            // 2024 Adult Compendium의 중강도 맨몸운동 3.8 MET / 고강도 7.5 MET.
            // 휴식은 1.3 MET로 가정하며 kcal/min = MET * 3.5 * kg / 200.
            double energy = (met * work + 1.3 * (rest + transition)) / 60.0
                * 3.5 * weightKg.doubleValue() / 200.0;
            kcal = (int) Math.round(energy);
        }
        return new Result(minutes, kcal, work, rest + transition);
    }
}
