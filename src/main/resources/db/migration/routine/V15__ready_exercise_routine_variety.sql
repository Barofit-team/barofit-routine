-- 기존 템플릿과 사용자 루틴은 유지하고 AI READY 23종목만 조합한 24개를 추가한다.
CREATE TEMP TABLE ready_routine_seed (
  suffix INTEGER PRIMARY KEY, name TEXT, description TEXT, difficulty TEXT, category TEXT
) ON COMMIT DROP;
INSERT INTO ready_routine_seed VALUES
 (201,'초급 전신 기본기','스쿼트와 밀기, 몸통 지지 동작을 균형 있게 연습합니다.','BEGINNER','FULL_BODY'),
 (202,'초급 전신 습관 만들기','세 가지 맨몸 동작을 짧게 반복하며 운동을 시작합니다.','BEGINNER','FULL_BODY'),
 (203,'중급 전신 근력 밸런스','상체 밀기와 런지, 등과 코어 동작을 함께 진행합니다.','INTERMEDIATE','FULL_BODY'),
 (204,'고급 전신 챌린지','버피와 한쪽 다리 하체 운동, 상체 밀기와 코어를 조합합니다.','ADVANCED','FULL_BODY'),
 (205,'초급 하체 기초 다지기','스쿼트와 브리지, 종아리 운동을 기본 세트로 진행합니다.','BEGINNER','LOWER_BODY'),
 (206,'초급 둔근과 하체 균형','브리지와 옆으로 다리 들기, 스플릿 스쿼트를 연습합니다.','BEGINNER','LOWER_BODY'),
 (207,'중급 런지와 하체 근력','런지와 스플릿 스쿼트에 브리지와 카프레이즈를 더합니다.','INTERMEDIATE','LOWER_BODY'),
 (208,'고급 하체 파워','지지대를 사용하는 스플릿 스쿼트와 점프 스쿼트를 포함합니다.','ADVANCED','LOWER_BODY'),
 (209,'초급 상체 밀기 입문','안정적인 지지대의 인클라인 푸시업과 몸통 지지를 연습합니다.','BEGINNER','UPPER_BODY'),
 (210,'초급 상체와 몸통 연결','인클라인 푸시업에 슈퍼맨과 데드버그를 조합합니다.','BEGINNER','UPPER_BODY'),
 (211,'중급 푸시업 집중','두 가지 푸시업과 사이드 플랭크로 상체 지지력을 연습합니다.','INTERMEDIATE','UPPER_BODY'),
 (212,'고급 상체 밀기 챌린지','파이크와 기본, 인클라인 푸시업을 플랭크와 함께 진행합니다.','ADVANCED','UPPER_BODY'),
 (213,'초급 등과 몸통 기본기','버드독과 슈퍼맨, 브리지로 몸통 지지 동작을 연습합니다.','BEGINNER','BACK'),
 (214,'초급 매트 등 균형','매트에서 슈퍼맨과 데드버그, 옆으로 다리 들기를 진행합니다.','BEGINNER','BACK'),
 (215,'중급 등과 코어 협응','버드독과 슈퍼맨에 옆쪽 몸통 지지와 브리지를 더합니다.','INTERMEDIATE','BACK'),
 (216,'고급 몸통 지지 챌린지','등과 복부 동작을 연결하고 사이드 플랭크 유지 시간을 늘립니다.','ADVANCED','BACK'),
 (217,'초급 코어 지지 입문','데드버그와 버드독, 짧은 플랭크부터 시작합니다.','BEGINNER','CORE'),
 (218,'초급 매트 코어 리듬','브리지와 리버스 크런치, 짧은 플러터킥을 조합합니다.','BEGINNER','CORE'),
 (219,'중급 복부와 옆구리','싯업과 레그 레이즈, 사이드 플랭크와 데드버그를 진행합니다.','INTERMEDIATE','CORE'),
 (220,'고급 코어 컨트롤','다리 들기와 골반 말아 올리기, 코어 유지 동작을 연결합니다.','ADVANCED','CORE'),
 (221,'초급 전신 리듬 입문','점프 없이 스쿼트와 카프레이즈, 마운틴 클라이머를 진행합니다.','BEGINNER','CARDIO'),
 (222,'초급 점핑잭 시작','가벼운 횟수의 점핑잭에 스쿼트와 브리지를 더합니다.','BEGINNER','CARDIO'),
 (223,'중급 심폐 템포','점핑잭과 마운틴 클라이머, 점프 스쿼트를 반복합니다.','INTERMEDIATE','CARDIO'),
 (224,'고급 전신 인터벌','버피와 점핑잭, 마운틴 클라이머와 점프 스쿼트를 진행합니다.','ADVANCED','CARDIO');

CREATE TEMP TABLE ready_routine_exercise_seed (
  suffix INTEGER, exercise_id TEXT, exercise_order INTEGER, target INTEGER,
  PRIMARY KEY (suffix, exercise_order), UNIQUE (suffix, exercise_id)
) ON COMMIT DROP;
INSERT INTO ready_routine_exercise_seed VALUES
 (201,'squat',1,8),
 (201,'incline-push-up',2,8),
 (201,'bird-dog',3,8),
 (201,'plank',4,20),
 (202,'glute-bridge',1,10),
 (202,'standing-calf-raise',2,12),
 (202,'dead-bug',3,8),
 (203,'push-up',1,10),
 (203,'lunge',2,10),
 (203,'superman',3,12),
 (203,'plank',4,35),
 (204,'burpee',1,10),
 (204,'bulgarian-split-squat',2,10),
 (204,'pike-push-up',3,8),
 (204,'mountain-climber',4,20),
 (204,'side-plank',5,35),
 (205,'squat',1,8),
 (205,'glute-bridge',2,12),
 (205,'standing-calf-raise',3,15),
 (206,'glute-bridge',1,12),
 (206,'side-lying-leg-raise',2,12),
 (206,'split-squat',3,8),
 (207,'lunge',1,10),
 (207,'split-squat',2,10),
 (207,'glute-bridge',3,15),
 (207,'standing-calf-raise',4,18),
 (208,'bulgarian-split-squat',1,10),
 (208,'jump-squat',2,10),
 (208,'lunge',3,12),
 (208,'standing-calf-raise',4,20),
 (209,'incline-push-up',1,8),
 (209,'bird-dog',2,8),
 (209,'plank',3,20),
 (210,'incline-push-up',1,10),
 (210,'superman',2,10),
 (210,'dead-bug',3,10),
 (211,'push-up',1,10),
 (211,'incline-push-up',2,12),
 (211,'side-plank',3,30),
 (212,'pike-push-up',1,8),
 (212,'push-up',2,12),
 (212,'incline-push-up',3,15),
 (212,'plank',4,45),
 (213,'bird-dog',1,8),
 (213,'superman',2,8),
 (213,'glute-bridge',3,10),
 (214,'superman',1,10),
 (214,'dead-bug',2,8),
 (214,'side-lying-leg-raise',3,10),
 (215,'bird-dog',1,12),
 (215,'superman',2,12),
 (215,'side-plank',3,30),
 (215,'glute-bridge',4,15),
 (216,'superman',1,15),
 (216,'bird-dog',2,15),
 (216,'dead-bug',3,14),
 (216,'side-plank',4,45),
 (217,'dead-bug',1,8),
 (217,'bird-dog',2,8),
 (217,'plank',3,20),
 (218,'glute-bridge',1,10),
 (218,'reverse-crunch',2,8),
 (218,'flutter-kick',3,30),
 (219,'sit-up',1,12),
 (219,'leg-raise',2,10),
 (219,'side-plank',3,30),
 (219,'dead-bug',4,12),
 (220,'leg-raise',1,15),
 (220,'reverse-crunch',2,15),
 (220,'flutter-kick',3,40),
 (220,'plank',4,45),
 (221,'squat',1,8),
 (221,'standing-calf-raise',2,15),
 (221,'mountain-climber',3,12),
 (222,'jumping-jack',1,15),
 (222,'squat',2,8),
 (222,'glute-bridge',3,10),
 (223,'jumping-jack',1,25),
 (223,'mountain-climber',2,20),
 (223,'jump-squat',3,10),
 (224,'burpee',1,12),
 (224,'jumping-jack',2,30),
 (224,'mountain-climber',3,30),
 (224,'jump-squat',4,12);

INSERT INTO routine.routine (id, type, name, description, difficulty, category, inter_exercise_rest_seconds)
SELECT ('00000000-0000-0000-0000-' || lpad(suffix::text, 12, '0'))::uuid,
       'TEMPLATE', name, description, difficulty, category,
       CASE difficulty WHEN 'BEGINNER' THEN 45 WHEN 'INTERMEDIATE' THEN 60 ELSE 75 END
FROM ready_routine_seed;

INSERT INTO routine.routine_exercise (id, routine_id, exercise_id, exercise_order)
SELECT gen_random_uuid(), ('00000000-0000-0000-0000-' || lpad(suffix::text, 12, '0'))::uuid,
       exercise_id, exercise_order
FROM ready_routine_exercise_seed;

INSERT INTO routine.routine_set
 (id, routine_exercise_id, set_order, target_reps, target_duration_seconds, weight_kg, rest_seconds, time_limit_seconds)
SELECT gen_random_uuid(), re.id, s.set_order,
       CASE WHEN e.measurement_type = 'REPS' THEN p.target ELSE NULL END,
       CASE WHEN e.measurement_type = 'DURATION' THEN p.target ELSE NULL END,
       0, CASE r.difficulty WHEN 'BEGINNER' THEN 30 WHEN 'INTERMEDIATE' THEN 45 ELSE 60 END,
       CASE WHEN e.measurement_type = 'REPS' THEN 60 ELSE NULL END
FROM ready_routine_exercise_seed p
JOIN routine.routine r ON r.id = ('00000000-0000-0000-0000-' || lpad(p.suffix::text, 12, '0'))::uuid
JOIN routine.routine_exercise re ON re.routine_id = r.id AND re.exercise_id = p.exercise_id
JOIN routine.exercise e ON e.id = re.exercise_id
CROSS JOIN LATERAL generate_series(1, CASE r.difficulty
  WHEN 'BEGINNER' THEN 2 WHEN 'INTERMEDIATE' THEN 3 ELSE 4 END) AS s(set_order);
