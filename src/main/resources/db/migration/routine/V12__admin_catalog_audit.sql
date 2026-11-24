ALTER TABLE routine.exercise ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
CREATE TABLE routine.admin_catalog_audit (
 id UUID PRIMARY KEY,
 actor_id UUID NOT NULL,
 resource_type VARCHAR(20) NOT NULL,
 resource_id VARCHAR(50) NOT NULL,
 before_state TEXT NOT NULL,
 after_state TEXT NOT NULL,
 created_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX ix_admin_catalog_audit_created ON routine.admin_catalog_audit(created_at DESC);
