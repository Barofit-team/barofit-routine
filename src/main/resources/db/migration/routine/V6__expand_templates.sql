ALTER TABLE routine.routine DROP CONSTRAINT IF EXISTS routine_category_check;
ALTER TABLE routine.routine ADD CONSTRAINT ck_routine_category
    CHECK (category IN ('FULL_BODY', 'LOWER_BODY', 'UPPER_BODY', 'BACK', 'CORE', 'CARDIO'));

-- 기존 9개 템플릿과 그로부터 복사된 사용자 루틴은 수정하지 않는다.
-- 새 분류별 루틴을 난이도마다 추가한다. 모든 신규 종목이 적어도 한 템플릿에 포함된다.
INSERT INTO routine.routine (id, type, name, description, difficulty, category)
VALUES
 ('00000000-0000-0000-0000-000000000101','TEMPLATE','초급 하체 다양화','기본 하체·둔근·종아리 움직임을 연습합니다.','BEGINNER','LOWER_BODY'),
 ('00000000-0000-0000-0000-000000000102','TEMPLATE','중급 하체 다양화','런지와 한쪽 다리 동작을 연습합니다.','INTERMEDIATE','LOWER_BODY'),
 ('00000000-0000-0000-0000-000000000103','TEMPLATE','고급 하체 다양화','한쪽 다리 균형과 하체 근력을 훈련합니다.','ADVANCED','LOWER_BODY'),
 ('00000000-0000-0000-0000-000000000104','TEMPLATE','초급 상체 밀기','무릎·인클라인 푸시업부터 상체 밀기를 연습합니다.','BEGINNER','UPPER_BODY'),
 ('00000000-0000-0000-0000-000000000105','TEMPLATE','중급 상체 밀기','손 간격과 상체 각도를 달리해 밀기 동작을 연습합니다.','INTERMEDIATE','UPPER_BODY'),
 ('00000000-0000-0000-0000-000000000106','TEMPLATE','고급 상체 밀기','벽과 안정적인 지지대가 필요한 고난도 동작을 포함합니다.','ADVANCED','UPPER_BODY'),
 ('00000000-0000-0000-0000-000000000107','TEMPLATE','초급 등 안정화','기구 없이 등과 몸통 안정화를 연습합니다.','BEGINNER','BACK'),
 ('00000000-0000-0000-0000-000000000108','TEMPLATE','중급 등 당기기 입문','고정된 바를 활용한 당기기와 몸통 안정화를 연습합니다.','INTERMEDIATE','BACK'),
 ('00000000-0000-0000-0000-000000000109','TEMPLATE','고급 등 당기기','철봉 당기기 동작을 포함하며 기구가 필요합니다.','ADVANCED','BACK'),
 ('00000000-0000-0000-0000-000000000110','TEMPLATE','초급 코어 다양화','몸통 굽힘과 앞·옆쪽 안정화를 연습합니다.','BEGINNER','CORE'),
 ('00000000-0000-0000-0000-000000000111','TEMPLATE','중급 코어 다양화','몸통 굽힘·회전·유지 동작을 연습합니다.','INTERMEDIATE','CORE'),
 ('00000000-0000-0000-0000-000000000112','TEMPLATE','고급 코어 다양화','팔다리 움직임 중 몸통을 안정적으로 유지합니다.','ADVANCED','CORE'),
 ('00000000-0000-0000-0000-000000000113','TEMPLATE','초급 심폐 활동','점프가 적은 동작부터 시작하는 전신 활동 루틴입니다.','BEGINNER','CARDIO'),
 ('00000000-0000-0000-0000-000000000114','TEMPLATE','중급 심폐 활동','빠른 전신 반복과 점프 동작을 포함합니다.','INTERMEDIATE','CARDIO'),
 ('00000000-0000-0000-0000-000000000115','TEMPLATE','고급 심폐 활동','점프와 버피를 포함한 고강도 전신 루틴입니다.','ADVANCED','CARDIO');

WITH plan(template_suffix, exercise_id, exercise_order) AS (VALUES
 (101,'split-squat',1),(101,'glute-bridge',2),(101,'side-lying-leg-raise',3),(101,'standing-calf-raise',4),
 (102,'forward-lunge',1),(102,'reverse-lunge',2),(102,'single-leg-glute-bridge',3),(102,'clamshell',4),
 (103,'single-leg-squat',1),(103,'bulgarian-split-squat',2),(103,'single-leg-calf-raise',3),
 (104,'knee-push-up',1),(104,'incline-push-up',2),(104,'push-up',3),
 (105,'push-up',1),(105,'close-grip-push-up',2),(105,'pike-push-up',3),
 (106,'pike-push-up',1),(106,'dips',2),(106,'handstand-push-up',3),
 (107,'prone-ytw-raise',1),(107,'bird-dog',2),(107,'prone-cobra',3),
 (108,'prone-ytw-raise',1),(108,'bird-dog',2),(108,'inverted-row',3),
 (109,'inverted-row',1),(109,'chin-up',2),(109,'pull-up',3),
 (110,'crunch',1),(110,'reverse-crunch',2),(110,'dead-bug',3),(110,'side-plank',4),
 (111,'sit-up',1),(111,'bicycle-crunch',2),(111,'russian-twist',3),(111,'hollow-hold',4),
 (112,'plank-shoulder-tap',1),(112,'leg-raise',2),(112,'flutter-kick',3),
 (113,'march-in-place',1),(113,'step-jack',2),(113,'jumping-jack',3),
 (114,'jumping-jack',1),(114,'mountain-climber',2),(114,'jump-squat',3),
 (115,'burpee',1),(115,'jumping-lunge',2),(115,'tuck-jump',3)
)
INSERT INTO routine.routine_exercise (id, routine_id, exercise_id, exercise_order)
SELECT gen_random_uuid(), ('00000000-0000-0000-0000-' || lpad(template_suffix::text, 12, '0'))::uuid,
       exercise_id, exercise_order FROM plan;

INSERT INTO routine.routine_set
    (id, routine_exercise_id, set_order, target_reps, target_duration_seconds, weight_kg, rest_seconds)
SELECT gen_random_uuid(), re.id, s.set_order,
       CASE WHEN e.measurement_type = 'REPS'
            THEN CASE WHEN e.id IN ('handstand-push-up','pull-up','chin-up','single-leg-squat','dips') THEN 5 ELSE 10 END
            ELSE NULL END,
       CASE WHEN e.measurement_type = 'DURATION' THEN 30 ELSE NULL END,
       0, CASE WHEN r.difficulty = 'ADVANCED' THEN 90 ELSE 60 END
FROM routine.routine_exercise re
JOIN routine.routine r ON r.id = re.routine_id
JOIN routine.exercise e ON e.id = re.exercise_id
CROSS JOIN LATERAL generate_series(1, CASE r.difficulty
    WHEN 'BEGINNER' THEN 2 WHEN 'INTERMEDIATE' THEN 3 ELSE 4 END) AS s(set_order)
WHERE r.id BETWEEN '00000000-0000-0000-0000-000000000101'::uuid
               AND '00000000-0000-0000-0000-000000000115'::uuid;
