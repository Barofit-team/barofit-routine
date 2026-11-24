package com.barofit.routine.repository;

import com.barofit.routine.domain.Exercise;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ExerciseRepository extends JpaRepository<Exercise, String> {
  @Query(
      "select e from Exercise e where e.active = true and e.assessmentStatus = com.barofit.routine.domain.AssessmentStatus.READY order by e.displayOrder")
  List<Exercise> findByActiveTrueOrderByDisplayOrderAsc();

  @Query(
      "select e from Exercise e where e.id = :id and e.active = true and e.assessmentStatus = com.barofit.routine.domain.AssessmentStatus.READY")
  Optional<Exercise> findByIdAndActiveTrue(String id);
}
