-- 플러터킥 측정 계약을 AI 분석기와 일치시킨다: DURATION → REPS (V16 마운틴 클라이머와 같은 방식).
-- AI(flutter_kicks)는 위쪽 다리가 좌→우→좌 또는 우→좌→우로 교대하면 1회로 센다. 이후에는 반대쪽·원래 쪽 교대 한 쌍마다 1회
--   (1회 = 다리별 1번). holdSeconds는 없다(null).
-- 기본 목표 횟수: 템플릿 반복 운동의 기본값(V6, 10회)을 V16처럼 "다리별 10회"로 보고, 1회 = 다리별 1번이므로 중급 10회.
--   난이도별 ±다리별 2번 = ±2회(V16과 같은 폭): BEGINNER 8 / INTERMEDIATE 10 / ADVANCED 12. 기존 30초 목표를 30회로 옮기지 않는다.
-- 세트 수·휴식·무게·세트 순서는 보존하고 목표 시간 → 목표 횟수로만 바꾼다(ck_routine_set_target 충족을 위해 한 UPDATE에서 함께 변경).
-- 대상: 템플릿(111 중급, 112 고급), V15 신규 루틴(218 초급, 220 고급)과 사용자 루틴(V17에서 hollow-hold → flutter-kick으로 교체된 루틴 포함). 루틴 난이도 기준.
-- 변환한 세트에는 V16과 같이 V14 계약의 REPS 세트 제한 시간 기본값 60초를 둔다.
-- 사용자가 설정했던 목표 시간은 수행 기록으로 변환하지 않는다. 과거 Workout 세션 snapshot·결과·Report는 다른 서비스 schema이며 변경하지 않는다.

UPDATE routine.exercise
SET measurement_type = 'REPS', updated_at = CURRENT_TIMESTAMP
WHERE id = 'flutter-kick' AND measurement_type = 'DURATION';

WITH changed AS (
    UPDATE routine.routine_set s
    SET target_reps = CASE r.difficulty WHEN 'BEGINNER' THEN 8 WHEN 'INTERMEDIATE' THEN 10 ELSE 12 END,
        target_duration_seconds = NULL,
        time_limit_seconds = 60
    FROM routine.routine_exercise re
    JOIN routine.routine r ON r.id = re.routine_id
    WHERE s.routine_exercise_id = re.id
      AND re.exercise_id = 'flutter-kick'
      AND s.target_duration_seconds IS NOT NULL
    RETURNING re.routine_id
)
UPDATE routine.routine r
SET version = r.version + 1, updated_at = CURRENT_TIMESTAMP
WHERE r.id IN (SELECT DISTINCT routine_id FROM changed);
