ALTER TABLE routine.routine ADD COLUMN inter_exercise_rest_seconds INTEGER NOT NULL DEFAULT 60 CHECK (inter_exercise_rest_seconds BETWEEN 0 AND 600);
ALTER TABLE routine.routine_set ADD COLUMN time_limit_seconds INTEGER CHECK (time_limit_seconds BETWEEN 10 AND 3600);
ALTER TABLE routine.workout_plan ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE routine.routine ADD COLUMN date_scoped BOOLEAN NOT NULL DEFAULT false;
