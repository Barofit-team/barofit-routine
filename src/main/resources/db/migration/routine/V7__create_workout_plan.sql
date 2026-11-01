CREATE TABLE routine.workout_plan (
    id uuid PRIMARY KEY,
    owner_user_id uuid NOT NULL,
    plan_date date NOT NULL,
    routine_id uuid NOT NULL REFERENCES routine.routine(id) ON DELETE CASCADE,
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL,
    CONSTRAINT uq_workout_plan_owner_date UNIQUE (owner_user_id, plan_date)
);
