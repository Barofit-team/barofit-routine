-- 마운틴 클라이머 측정 계약을 AI 분석기와 일치시킨다: DURATION → REPS.
-- AI(mountain_climber)는 한쪽 무릎을 앞으로 당겼다가 복귀하면 1회로 센다(좌우 한 쌍 = 2회). 쌍을 1회로 환산하지 않는다.
-- 기본 목표 횟수: 템플릿 반복 운동의 기본값(V6, 10회)을 "다리별 10회"로 본 중급 20회를 기준으로 난이도별 ±4회(다리별 ±2회).
--   BEGINNER 16 / INTERMEDIATE 20 / ADVANCED 24. 기존 30초 목표를 30회로 옮기지 않는다.
-- 세트 수·휴식·무게·세트 순서는 보존하고 목표 시간 → 목표 횟수로만 바꾼다(ck_routine_set_target 충족을 위해 한 UPDATE에서 함께 변경).
-- V14(일일 루틴 커스터마이징) 계약: REPS 세트는 세트 제한 시간을 갖고 DURATION 세트는 갖지 않는다. 변환한 세트에는 V15 신규 루틴의
--   REPS 기본값과 같은 60초를 둔다(앱의 미설정 기본값도 60초). V15 신규 루틴(204·221·223·224)의 마운틴 클라이머도 이 변환 대상이다.
-- 사용자가 설정했던 목표 시간은 수행 기록으로 변환하지 않는다. 과거 Workout 세션 snapshot·결과·Report는 다른 서비스 schema이며 변경하지 않는다.

UPDATE routine.exercise
SET measurement_type = 'REPS', updated_at = CURRENT_TIMESTAMP
WHERE id = 'mountain-climber' AND measurement_type = 'DURATION';

WITH changed AS (
    UPDATE routine.routine_set s
    SET target_reps = CASE r.difficulty WHEN 'BEGINNER' THEN 16 WHEN 'INTERMEDIATE' THEN 20 ELSE 24 END,
        target_duration_seconds = NULL,
        time_limit_seconds = 60
    FROM routine.routine_exercise re
    JOIN routine.routine r ON r.id = re.routine_id
    WHERE s.routine_exercise_id = re.id
      AND re.exercise_id = 'mountain-climber'
      AND s.target_duration_seconds IS NOT NULL
    RETURNING re.routine_id
)
UPDATE routine.routine r
SET version = r.version + 1, updated_at = CURRENT_TIMESTAMP
WHERE r.id IN (SELECT DISTINCT routine_id FROM changed);
