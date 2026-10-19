package com.barofit.routine.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "routine_exercise", schema = "routine",
    uniqueConstraints = @UniqueConstraint(columnNames = {"routine_id", "exercise_order"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RoutineExercise {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "routine_id", nullable = false)
    private Routine routine;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "exercise_id", nullable = false)
    private Exercise exercise;
    @Column(nullable = false)
    private int exerciseOrder;
    @OneToMany(mappedBy = "routineExercise", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("setOrder ASC")
    @org.hibernate.annotations.BatchSize(size = 100)
    private List<RoutineSet> sets = new ArrayList<>();

    public RoutineExercise(Routine routine, Exercise exercise, int exerciseOrder) {
        this.routine = routine;
        this.exercise = exercise;
        this.exerciseOrder = exerciseOrder;
    }

    public List<RoutineSet> getSets() { return Collections.unmodifiableList(sets); }
    public void addSet(RoutineSet set) { sets.add(set); }
    /** 종목 ID와 순서를 유지하며 세트 수와 목표만 변경한다. 늘어난 세트는 마지막 세트 설정을 복사한다. */
    public void customize(Integer setCount, Integer reps, Integer duration) {
        int count = setCount == null ? sets.size() : setCount;
        while (sets.size() > count) sets.remove(sets.size() - 1);
        for (RoutineSet set : sets) set.changeTarget(reps, duration);
        while (sets.size() < count) {
            RoutineSet last = sets.getLast();
            sets.add(new RoutineSet(this, sets.size() + 1, last.getTargetReps(),
                last.getTargetDurationSeconds(), last.getWeightKg(), last.getRestSeconds()));
        }
    }
}
