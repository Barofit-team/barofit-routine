CREATE TABLE routine.exercise (
    id VARCHAR(50) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    measurement_type VARCHAR(20) NOT NULL CHECK (measurement_type IN ('REPS', 'DURATION')),
    description TEXT NOT NULL,
    preparation TEXT NOT NULL,
    instructions TEXT NOT NULL,
    precautions TEXT NOT NULL,
    intro_tts_text TEXT NOT NULL,
    display_order INTEGER NOT NULL CHECK (display_order > 0),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE routine.routine (
    id UUID PRIMARY KEY,
    owner_user_id UUID,
    type VARCHAR(20) NOT NULL CHECK (type IN ('USER', 'TEMPLATE')),
    name VARCHAR(100) NOT NULL CHECK (LENGTH(TRIM(name)) > 0),
    description TEXT,
    difficulty VARCHAR(20) NOT NULL CHECK (difficulty IN ('BEGINNER', 'INTERMEDIATE', 'ADVANCED')),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT ck_routine_owner CHECK (
        (type = 'USER' AND owner_user_id IS NOT NULL)
        OR (type = 'TEMPLATE' AND owner_user_id IS NULL)
    )
);

CREATE INDEX ix_routine_owner_created ON routine.routine (owner_user_id, type, created_at DESC, id DESC);
CREATE INDEX ix_routine_template ON routine.routine (type, created_at, id);

CREATE TABLE routine.routine_exercise (
    id UUID PRIMARY KEY,
    routine_id UUID NOT NULL REFERENCES routine.routine(id) ON DELETE CASCADE,
    exercise_id VARCHAR(50) NOT NULL REFERENCES routine.exercise(id),
    exercise_order INTEGER NOT NULL CHECK (exercise_order > 0),
    CONSTRAINT uq_routine_exercise_order UNIQUE (routine_id, exercise_order)
);

CREATE INDEX ix_routine_exercise_catalog ON routine.routine_exercise (exercise_id);

CREATE TABLE routine.routine_set (
    id UUID PRIMARY KEY,
    routine_exercise_id UUID NOT NULL REFERENCES routine.routine_exercise(id) ON DELETE CASCADE,
    set_order INTEGER NOT NULL CHECK (set_order > 0),
    target_reps INTEGER CHECK (target_reps > 0),
    target_duration_seconds INTEGER CHECK (target_duration_seconds > 0),
    weight_kg NUMERIC(6,2) NOT NULL DEFAULT 0 CHECK (weight_kg >= 0),
    rest_seconds INTEGER NOT NULL CHECK (rest_seconds >= 0),
    CONSTRAINT uq_routine_set_order UNIQUE (routine_exercise_id, set_order),
    CONSTRAINT ck_routine_set_target CHECK (
        (target_reps IS NOT NULL AND target_duration_seconds IS NULL)
        OR (target_reps IS NULL AND target_duration_seconds IS NOT NULL)
    )
);
