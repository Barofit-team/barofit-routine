package com.barofit.routine.repository;

import com.barofit.routine.domain.Difficulty;
import com.barofit.routine.domain.Routine;
import com.barofit.routine.domain.RoutineType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface RoutineRepository extends JpaRepository<Routine, UUID> {
  Optional<Routine> findByIdAndType(UUID id, RoutineType type);

  Optional<Routine> findByIdAndOwnerUserIdAndType(UUID id, UUID ownerUserId, RoutineType type);

  @Query(
      "select r from Routine r where r.ownerUserId = :ownerUserId and r.type = :type and r.exercises is not empty and not exists (select e.id from RoutineExercise e where e.routine = r and (e.exercise.active = false or e.exercise.assessmentStatus <> com.barofit.routine.domain.AssessmentStatus.READY))")
  Page<Routine> findByOwnerUserIdAndType(UUID ownerUserId, RoutineType type, Pageable pageable);

  List<Routine> findByTypeOrderByCreatedAtAscIdAsc(RoutineType type);

  List<Routine> findByTypeAndDifficultyOrderByCreatedAtAscIdAsc(
      RoutineType type, Difficulty difficulty);
}
