-- 최종 서비스 23종(V10 READY)으로 루틴을 정리한다. 47종 카탈로그·READY 집합·CatalogVisibility 정책은 그대로 둔다.
-- 원칙: 서비스 범위 밖(PLANNED) 종목을 같은 분류·측정 방식·필요 기구의 READY 종목 중 동작이 가장 가까운 종목으로 교체한다.
--       같은 조건의 종목이 없으면 템플릿 목적이 유지되는 경우에만 해당 종목을 뺀다. 세트 수·휴식·무게·순서는 보존한다.
--       목적을 유지할 수 없는 템플릿(바·고난도 장비 중심: 106 고급 상체 밀기, 108 중급 등 당기기 입문, 109 고급 등 당기기)은
--       변경하지 않으며 CatalogVisibility에 의해 계속 숨겨진다.
-- 템플릿 ID·routine_exercise ID·routine_set ID는 유지하므로 운동 계획(workout_plan)과 다른 참조는 그대로다.
-- 과거 Workout 세션 snapshot·결과·Report는 다른 서비스 schema이며 변경하지 않는다.

CREATE TEMP TABLE v14_changed_routine (routine_id uuid) ON COMMIT DROP;

-- ---------------------------------------------------------------------------
-- 1. 템플릿 (type = TEMPLATE)
-- ---------------------------------------------------------------------------
-- 같은 분류·측정 방식·기구 교체: 목표·세트·휴식 유지
WITH plan(template_suffix, src, dst) AS (VALUES
    -- 102 중급 하체 다양화: 런지와 한쪽 다리 동작
    (102, 'forward-lunge', 'lunge'),
    (102, 'reverse-lunge', 'split-squat'),
    (102, 'single-leg-glute-bridge', 'glute-bridge'),
    (102, 'clamshell', 'side-lying-leg-raise'),
    -- 103 고급 하체 다양화: 한쪽 다리 균형과 하체 근력
    (103, 'single-leg-squat', 'lunge'),
    (103, 'single-leg-calf-raise', 'standing-calf-raise'),
    -- 107 초급 등 안정화: 엎드린 등 신전
    (107, 'prone-ytw-raise', 'superman'),
    -- 110 초급 코어 다양화: 몸통 굽힘
    (110, 'crunch', 'sit-up'),
    -- 111 중급 코어 다양화: 굽힘 → 굽힘, 회전 → 회전 저항, 할로우 유지 → 같은 자세의 다리 교차(시간)
    (111, 'bicycle-crunch', 'reverse-crunch'),
    (111, 'russian-twist', 'dead-bug'),
    (111, 'hollow-hold', 'flutter-kick'),
    -- 112 고급 코어 다양화: 플랭크 자세 회전 저항 → 팔다리 대각 회전 저항
    (112, 'plank-shoulder-tap', 'dead-bug'),
    -- 113 초급 심폐 활동: 저충격 제자리 동작 → 점프 없는 무릎 당기기(REPS, 아래 3에서 목표 변환)
    (113, 'march-in-place', 'mountain-climber'),
    -- 115 고급 심폐 활동: 점프 하체 → 점프 스쿼트, 무릎을 가슴으로 당기는 점프 → 무릎 당기기
    (115, 'jumping-lunge', 'jump-squat'),
    (115, 'tuck-jump', 'mountain-climber')
), changed AS (
    UPDATE routine.routine_exercise re
    SET exercise_id = p.dst
    FROM plan p
    JOIN routine.routine r ON r.id = ('00000000-0000-0000-0000-' || lpad(p.template_suffix::text, 12, '0'))::uuid
    WHERE re.routine_id = r.id
      AND r.type = 'TEMPLATE'
      AND re.exercise_id = p.src
      AND NOT EXISTS (SELECT 1 FROM routine.routine_exercise x WHERE x.routine_id = r.id AND x.exercise_id = p.dst)
    RETURNING re.routine_id
)
INSERT INTO v14_changed_routine SELECT routine_id FROM changed;

-- 2. 같은 조건의 READY 종목이 없어 빼는 종목 (세트는 FK CASCADE로 함께 삭제). 남은 종목 순서를 1부터 다시 맞춘다
--    104 초급 상체 밀기: 무릎 푸시업 → 인클라인 푸시업 → 푸시업의 단계 중 인클라인·푸시업이 남는다
--    105 중급 상체 밀기: 좁은 손 간격 푸시업 제외, 푸시업·파이크 푸시업 유지
--    107 초급 등 안정화: 시간형 등 신전 유지(프론 코브라)는 READY 대체 종목이 없어 제외(수퍼맨·버드독 유지)
--    113 초급 심폐 활동: 저충격 스텝잭은 점프 동작으로 바꾸지 않고 제외
WITH removed(template_suffix, exercise_id) AS (VALUES
    (104, 'knee-push-up'), (105, 'close-grip-push-up'), (107, 'prone-cobra'), (113, 'step-jack')
), deleted AS (
    DELETE FROM routine.routine_exercise re
    USING removed d
    WHERE re.routine_id = ('00000000-0000-0000-0000-' || lpad(d.template_suffix::text, 12, '0'))::uuid
      AND re.exercise_id = d.exercise_id
      AND EXISTS (SELECT 1 FROM routine.routine r WHERE r.id = re.routine_id AND r.type = 'TEMPLATE')
    RETURNING re.routine_id
)
INSERT INTO v14_changed_routine SELECT routine_id FROM deleted;

-- 순서 재정렬: 고유 제약(routine_id, exercise_order)과 CHECK(> 0)를 지키려고 +1000으로 옮긴 뒤 1부터 다시 매긴다
UPDATE routine.routine_exercise re SET exercise_order = re.exercise_order + 1000
WHERE re.routine_id IN (
    '00000000-0000-0000-0000-000000000104'::uuid, '00000000-0000-0000-0000-000000000105'::uuid,
    '00000000-0000-0000-0000-000000000107'::uuid, '00000000-0000-0000-0000-000000000113'::uuid);
UPDATE routine.routine_exercise re SET exercise_order = o.n
FROM (
    SELECT id, row_number() OVER (PARTITION BY routine_id ORDER BY exercise_order) AS n
    FROM routine.routine_exercise
    WHERE routine_id IN (
        '00000000-0000-0000-0000-000000000104'::uuid, '00000000-0000-0000-0000-000000000105'::uuid,
        '00000000-0000-0000-0000-000000000107'::uuid, '00000000-0000-0000-0000-000000000113'::uuid)
) o
WHERE re.id = o.id;

-- 3. 측정 방식·난이도가 달라진 교체의 목표값 (REPS/DURATION 목표를 섞지 않는다)
--    113 제자리 걷기 30초(DURATION) → 마운틴 클라이머 초급 16회(REPS, V16 기준), 115 턱 점프 10회 → 마운틴 클라이머 고급 24회
--    103 한쪽 다리 스쿼트 5회 → 런지 10회(V6 반복 종목 기본값, 같은 템플릿의 불가리안 스플릿 스쿼트와 동일)
UPDATE routine.routine_set s
SET target_reps = CASE re.routine_id
        WHEN '00000000-0000-0000-0000-000000000113'::uuid THEN 16
        WHEN '00000000-0000-0000-0000-000000000115'::uuid THEN 24 END,
    target_duration_seconds = NULL
FROM routine.routine_exercise re
WHERE s.routine_exercise_id = re.id
  AND re.exercise_id = 'mountain-climber'
  AND re.routine_id IN ('00000000-0000-0000-0000-000000000113'::uuid, '00000000-0000-0000-0000-000000000115'::uuid);

UPDATE routine.routine_set s
SET target_reps = 10
FROM routine.routine_exercise re
WHERE s.routine_exercise_id = re.id
  AND re.routine_id = '00000000-0000-0000-0000-000000000103'::uuid
  AND re.exercise_id = 'lunge'
  AND s.target_reps = 5;

-- 4. 구성이 바뀐 템플릿 설명 (빠진 종목을 언급하는 문구만)
UPDATE routine.routine SET description = '인클라인 푸시업부터 상체 밀기를 연습합니다.'
WHERE id = '00000000-0000-0000-0000-000000000104' AND type = 'TEMPLATE';
UPDATE routine.routine SET description = '상체 각도를 달리해 밀기 동작을 연습합니다.'
WHERE id = '00000000-0000-0000-0000-000000000105' AND type = 'TEMPLATE';
UPDATE routine.routine SET description = '몸통 굽힘·회전 저항·유지 동작을 연습합니다.'
WHERE id = '00000000-0000-0000-0000-000000000111' AND type = 'TEMPLATE';

-- ---------------------------------------------------------------------------
-- 5. 기존 사용자 루틴 (type = USER) 호환성
--    같은 분류·측정 방식·기구의 READY 종목으로만 교체하고 사용자의 세트·목표·휴식·순서를 그대로 둔다.
--    이미 같은 종목이 있는 루틴은 중복을 만들지 않으려고 교체하지 않는다.
--    의미상 안전한 교체가 없는 종목(무릎·좁은 손 간격 푸시업, 딥스, 핸드스탠드 푸시업, 프론 코브라, 바 당기기 3종,
--    제자리 걷기(측정 방식 다름), 스텝잭(저충격 → 점프), 턱 점프(횟수 단위가 다름))은 삭제하거나 바꾸지 않는다.
--    해당 루틴은 데이터가 그대로 보존되고 기존 CatalogVisibility 정책에 따라 숨겨진다.
-- ---------------------------------------------------------------------------
DO $$
DECLARE
    m record;
BEGIN
    FOR m IN
        SELECT * FROM (VALUES
            (1, 'forward-lunge', 'lunge'),
            (2, 'single-leg-squat', 'lunge'),
            (3, 'reverse-lunge', 'split-squat'),
            (4, 'single-leg-glute-bridge', 'glute-bridge'),
            (5, 'clamshell', 'side-lying-leg-raise'),
            (6, 'single-leg-calf-raise', 'standing-calf-raise'),
            (7, 'prone-ytw-raise', 'superman'),
            (8, 'crunch', 'sit-up'),
            (9, 'bicycle-crunch', 'reverse-crunch'),
            (10, 'russian-twist', 'dead-bug'),
            (11, 'plank-shoulder-tap', 'dead-bug'),
            (12, 'hollow-hold', 'flutter-kick'),
            (13, 'jumping-lunge', 'jump-squat')
        ) AS v(ord, src, dst)
        ORDER BY ord
    LOOP
        WITH changed AS (
            UPDATE routine.routine_exercise re
            SET exercise_id = m.dst
            FROM routine.routine r, routine.exercise es, routine.exercise ed
            WHERE r.id = re.routine_id
              AND r.type = 'USER'
              AND re.exercise_id = m.src
              AND es.id = m.src
              AND ed.id = m.dst
              AND es.category = ed.category
              AND es.measurement_type = ed.measurement_type
              AND es.equipment = ed.equipment
              AND ed.assessment_status = 'READY'
              AND ed.active
              AND NOT EXISTS (
                  SELECT 1 FROM routine.routine_exercise x
                  WHERE x.routine_id = re.routine_id AND x.exercise_id = m.dst)
            RETURNING re.routine_id
        )
        INSERT INTO v14_changed_routine SELECT routine_id FROM changed;
    END LOOP;
END $$;

-- 6. 변경된 루틴의 낙관적 잠금 버전과 수정 시각
UPDATE routine.routine r
SET version = r.version + 1, updated_at = CURRENT_TIMESTAMP
WHERE r.id IN (SELECT DISTINCT routine_id FROM v14_changed_routine);
