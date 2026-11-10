package com.barofit.routine.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
    schema = "routine",
    name = "workout_plan",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uq_workout_plan_owner_date",
            columnNames = {"owner_user_id", "plan_date"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WorkoutPlan {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(nullable = false, updatable = false)
  private UUID ownerUserId;

  @Column(nullable = false, updatable = false)
  private LocalDate planDate;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "routine_id", nullable = false)
  private Routine routine;

  @Column(nullable = false, updatable = false)
  private Instant createdAt;

  @Column(nullable = false)
  private Instant updatedAt;

  public WorkoutPlan(UUID ownerUserId, LocalDate planDate, Routine routine, Instant now) {
    this.ownerUserId = ownerUserId;
    this.planDate = planDate;
    this.routine = routine;
    this.createdAt = now;
    this.updatedAt = now;
  }

  public void assign(Routine routine, Instant now) {
    this.routine = routine;
    this.updatedAt = now;
  }
}
