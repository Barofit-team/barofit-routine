package com.barofit.routine.admin;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.*;

@Entity
@Table(name = "admin_catalog_audit", schema = "routine")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CatalogAudit {
  @Id private UUID id;

  @Column(nullable = false)
  private UUID actorId;

  @Column(nullable = false, length = 20)
  private String resourceType;

  @Column(nullable = false, length = 50)
  private String resourceId;

  @Column(nullable = false, columnDefinition = "text")
  private String beforeState;

  @Column(nullable = false, columnDefinition = "text")
  private String afterState;

  @Column(nullable = false)
  private Instant createdAt;

  public CatalogAudit(UUID actor, String type, String resource, String before, String after) {
    id = UUID.randomUUID();
    actorId = actor;
    resourceType = type;
    resourceId = resource;
    beforeState = before;
    afterState = after;
    createdAt = Instant.now();
  }
}
