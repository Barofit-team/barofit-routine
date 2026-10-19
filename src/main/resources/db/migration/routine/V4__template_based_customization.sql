-- 기존 자유 생성 루틴은 원본 템플릿을 추측하지 않고 NULL로 보존한다.
ALTER TABLE routine.routine ADD COLUMN template_id UUID REFERENCES routine.routine(id);
ALTER TABLE routine.routine ADD COLUMN category VARCHAR(20)
    CHECK (category IN ('FULL_BODY', 'LOWER_BODY', 'CORE'));
ALTER TABLE routine.routine ADD CONSTRAINT ck_template_source
    CHECK (template_id IS NULL OR (type = 'USER' AND template_id <> id));
CREATE INDEX ix_routine_template_id ON routine.routine(template_id);

UPDATE routine.routine SET category = 'FULL_BODY' WHERE type = 'TEMPLATE' AND id IN (
    '00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000003');
UPDATE routine.routine SET category = 'LOWER_BODY' WHERE type = 'TEMPLATE' AND id IN (
    '00000000-0000-0000-0000-000000000004', '00000000-0000-0000-0000-000000000005', '00000000-0000-0000-0000-000000000006');
UPDATE routine.routine SET category = 'CORE' WHERE type = 'TEMPLATE' AND id IN (
    '00000000-0000-0000-0000-000000000007', '00000000-0000-0000-0000-000000000008', '00000000-0000-0000-0000-000000000009');
