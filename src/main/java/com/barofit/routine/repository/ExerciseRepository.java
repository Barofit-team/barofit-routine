package com.barofit.routine.repository;

import com.barofit.routine.domain.Exercise;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExerciseRepository extends JpaRepository<Exercise, String> {
  List<Exercise> findByActiveTrueOrderByDisplayOrderAsc();

  Optional<Exercise> findByIdAndActiveTrue(String id);
}
