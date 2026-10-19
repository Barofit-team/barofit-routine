package com.barofit.routine.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "routine", schema = "routine")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Routine {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    private UUID ownerUserId;
    private UUID templateId;
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private RoutineCategory category;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RoutineType type;
    @Column(nullable = false, length = 100)
    private String name;
    @Column(columnDefinition = "text")
    private String description;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Difficulty difficulty;
    @Column(nullable = false, updatable = false)
    private Instant createdAt;
    @Column(nullable = false)
    private Instant updatedAt;
    @Version
    private long version;
    @OneToMany(mappedBy = "routine", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("exerciseOrder ASC")
    @org.hibernate.annotations.BatchSize(size = 100)
    private List<RoutineExercise> exercises = new ArrayList<>();

    public Routine(UUID ownerUserId, Routine template) {
        this.ownerUserId = ownerUserId;
        this.type = RoutineType.USER;
        this.templateId = template.getId();
        this.category = template.getCategory();
        this.name = template.getName();
        this.description = template.getDescription();
        this.difficulty = template.getDifficulty();
        this.createdAt = Instant.now();
        this.updatedAt = createdAt;
    }

    public List<RoutineExercise> getExercises() {
        return Collections.unmodifiableList(exercises);
    }

    public void touch() { this.updatedAt = Instant.now(); }
    public void addExercise(RoutineExercise exercise) { exercises.add(exercise); }
}
