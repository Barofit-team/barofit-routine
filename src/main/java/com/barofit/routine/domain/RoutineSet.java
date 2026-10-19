package com.barofit.routine.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "routine_set", schema = "routine",
    uniqueConstraints = @UniqueConstraint(columnNames = {"routine_exercise_id", "set_order"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RoutineSet {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "routine_exercise_id", nullable = false)
    private RoutineExercise routineExercise;
    @Column(nullable = false)
    private int setOrder;
    private Integer targetReps;
    private Integer targetDurationSeconds;
    @Column(nullable = false, precision = 6, scale = 2)
    private BigDecimal weightKg;
    @Column(nullable = false)
    private int restSeconds;

    public RoutineSet(RoutineExercise exercise, int order, Integer reps, Integer duration,
                      BigDecimal weight, int rest) {
        this.routineExercise = exercise;
        this.setOrder = order;
        this.targetReps = reps;
        this.targetDurationSeconds = duration;
        this.weightKg = weight;
        this.restSeconds = rest;
    }
    public void changeTarget(Integer reps, Integer duration) {
        if (reps != null) this.targetReps = reps;
        if (duration != null) this.targetDurationSeconds = duration;
    }
}
