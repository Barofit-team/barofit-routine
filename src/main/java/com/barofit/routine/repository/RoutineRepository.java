package com.barofit.routine.repository;

import com.barofit.routine.domain.Routine;
import com.barofit.routine.domain.Difficulty;
import com.barofit.routine.domain.RoutineType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RoutineRepository extends JpaRepository<Routine, UUID> {
    Optional<Routine> findByIdAndType(UUID id, RoutineType type);
    Optional<Routine> findByIdAndOwnerUserIdAndType(UUID id, UUID ownerUserId, RoutineType type);
    Page<Routine> findByOwnerUserIdAndType(UUID ownerUserId, RoutineType type, Pageable pageable);
    List<Routine> findByTypeOrderByCreatedAtAscIdAsc(RoutineType type);
    List<Routine> findByTypeAndDifficultyOrderByCreatedAtAscIdAsc(RoutineType type, Difficulty difficulty);
}
