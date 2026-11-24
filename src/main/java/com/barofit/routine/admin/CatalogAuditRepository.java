package com.barofit.routine.admin;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CatalogAuditRepository extends JpaRepository<CatalogAudit, UUID> {}
