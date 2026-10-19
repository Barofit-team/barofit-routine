package com.barofit.routine.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.Instant;

@Entity
@org.hibernate.annotations.BatchSize(size = 50)
@Table(name = "exercise", schema = "routine")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Exercise {
    @Id
    @Column(length = 50)
    private String id;
    @Column(nullable = false, length = 100)
    private String name;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MeasurementType measurementType;
    @Column(nullable = false, columnDefinition = "text")
    private String description;
    @Column(nullable = false, columnDefinition = "text")
    private String preparation;
    @Column(nullable = false, columnDefinition = "text")
    private String instructions;
    @Column(nullable = false, columnDefinition = "text")
    private String precautions;
    @Column(nullable = false, columnDefinition = "text")
    private String introTtsText;
    @Column(nullable = false)
    private int displayOrder;
    @Column(nullable = false)
    private boolean active;
    @Column(nullable = false)
    private Instant createdAt;
    @Column(nullable = false)
    private Instant updatedAt;
}
