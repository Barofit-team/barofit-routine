package com.barofit.routine.repository;

import com.barofit.routine.domain.WorkoutPlan;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkoutPlanRepository extends JpaRepository<WorkoutPlan, UUID> {
  Optional<WorkoutPlan> findByOwnerUserIdAndPlanDate(UUID ownerUserId, LocalDate date);

  List<WorkoutPlan> findByOwnerUserIdAndPlanDateBetweenOrderByPlanDateAsc(
      UUID ownerUserId, LocalDate start, LocalDate end);
}
