ALTER TABLE routine.exercise ADD COLUMN category varchar(20);
ALTER TABLE routine.exercise ADD COLUMN equipment varchar(20) NOT NULL DEFAULT 'NONE';
ALTER TABLE routine.exercise ADD COLUMN assessment_status varchar(20) NOT NULL DEFAULT 'PLANNED';
ALTER TABLE routine.exercise ADD CONSTRAINT ck_exercise_category
    CHECK (category IN ('LOWER_BODY', 'UPPER_BODY', 'BACK', 'CORE', 'CARDIO'));
ALTER TABLE routine.exercise ADD CONSTRAINT ck_exercise_equipment
    CHECK (equipment IN ('NONE', 'SUPPORT', 'BAR', 'WALL'));
ALTER TABLE routine.exercise ADD CONSTRAINT ck_exercise_assessment_status
    CHECK (assessment_status IN ('READY', 'IN_PROGRESS', 'PLANNED'));
UPDATE routine.exercise SET category = 'LOWER_BODY', assessment_status = 'READY' WHERE id = 'squat';
UPDATE routine.exercise SET category = 'UPPER_BODY', assessment_status = 'READY' WHERE id = 'push-up';
UPDATE routine.exercise SET category = 'CORE', assessment_status = 'READY' WHERE id = 'plank';
ALTER TABLE routine.exercise ALTER COLUMN category SET NOT NULL;

-- 자세 판정 준비 상태는 운동 카탈로그와 분리해서 갱신한다. PLANNED는 판정 검증 완료를 뜻하지 않는다.
-- 안내 문구는 일반적인 동작 설명이며, 출시 전에 종목별 코칭 문구를 검토한다.
WITH catalog(id, name, category, measure, equipment, preparation, instructions) AS (VALUES
 ('split-squat','스플릿 스쿼트','LOWER_BODY','REPS','NONE','한 발을 앞에 두고 양발을 앞뒤로 벌려 서세요.','몸통을 세운 채 두 무릎을 굽혔다가 앞발로 바닥을 밀어 일어나세요.'),
 ('forward-lunge','포워드 런지','LOWER_BODY','REPS','NONE','발을 골반너비로 벌리고 똑바로 서세요.','한 발을 앞으로 내디뎌 무릎을 굽힌 뒤 원래 자리로 돌아오세요.'),
 ('reverse-lunge','리버스 런지','LOWER_BODY','REPS','NONE','발을 골반너비로 벌리고 똑바로 서세요.','한 발을 뒤로 내디뎌 양쪽 무릎을 굽힌 뒤 앞발로 밀어 돌아오세요.'),
 ('single-leg-squat','싱글레그 스쿼트','LOWER_BODY','REPS','NONE','한 발로 서고 필요하면 벽 가까이에서 균형을 잡으세요.','지지하는 다리의 무릎을 굽혔다가 천천히 펴세요.'),
 ('bulgarian-split-squat','불가리안 스플릿 스쿼트','LOWER_BODY','REPS','SUPPORT','뒤쪽 발을 안정적인 낮은 지지대에 올리세요.','앞쪽 다리의 무릎을 굽혀 내려갔다가 앞발로 밀어 올라오세요.'),
 ('glute-bridge','글루트 브리지','LOWER_BODY','REPS','NONE','바로 누워 무릎을 굽히고 발바닥을 바닥에 놓으세요.','엉덩이를 들어 몸통과 허벅지를 연결한 뒤 천천히 내리세요.'),
 ('single-leg-glute-bridge','싱글레그 글루트 브리지','LOWER_BODY','REPS','NONE','바로 누워 한쪽 발을 바닥에 놓고 반대쪽 다리를 드세요.','지지하는 발로 바닥을 밀며 골반을 들어 올렸다가 내리세요.'),
 ('side-lying-leg-raise','옆으로 누워 다리 들기','LOWER_BODY','REPS','NONE','옆으로 누워 아래쪽 몸통을 안정적으로 지지하세요.','위쪽 다리를 천천히 들어 올렸다가 제자리로 내리세요.'),
 ('clamshell','클램셸','LOWER_BODY','REPS','NONE','옆으로 누워 무릎을 굽히고 양발을 모으세요.','발을 붙인 채 위쪽 무릎을 열었다가 천천히 닫으세요.'),
 ('standing-calf-raise','스탠딩 카프레이즈','LOWER_BODY','REPS','NONE','발을 골반너비로 벌리고 서세요.','발뒤꿈치를 들어 발끝으로 섰다가 천천히 내리세요.'),
 ('single-leg-calf-raise','싱글레그 카프레이즈','LOWER_BODY','REPS','NONE','한 발로 서고 필요하면 벽을 가볍게 짚으세요.','지지하는 발의 뒤꿈치를 들어 올렸다가 천천히 내리세요.'),
 ('knee-push-up','무릎 푸시업','UPPER_BODY','REPS','NONE','무릎과 손을 바닥에 두고 몸통을 곧게 유지하세요.','팔꿈치를 굽혀 몸을 내렸다가 바닥을 밀어 올라오세요.'),
 ('incline-push-up','인클라인 푸시업','UPPER_BODY','REPS','SUPPORT','움직이지 않는 높은 지지대에 두 손을 올리세요.','몸통을 곧게 유지하며 팔꿈치를 굽혔다가 밀어 올리세요.'),
 ('pike-push-up','파이크 푸시업','UPPER_BODY','REPS','NONE','손과 발을 바닥에 두고 엉덩이를 높이 올리세요.','팔꿈치를 굽혀 상체를 내렸다가 바닥을 밀어 올리세요.'),
 ('handstand-push-up','핸드스탠드 푸시업','UPPER_BODY','REPS','WALL','안전한 벽 앞에서 손을 짚고 거꾸로 선 자세를 준비하세요.','균형을 유지하며 팔꿈치를 굽혔다가 펴세요.'),
 ('close-grip-push-up','좁은 손 간격 푸시업','UPPER_BODY','REPS','NONE','손을 어깨너비보다 좁게 짚고 몸통을 곧게 유지하세요.','팔꿈치를 몸 가까이에 두며 내려갔다가 밀어 올리세요.'),
 ('dips','딥스','UPPER_BODY','REPS','SUPPORT','흔들리지 않는 평행 지지대를 잡고 몸을 지탱하세요.','팔꿈치를 굽혀 몸을 내렸다가 팔을 펴며 올라오세요.'),
 ('pull-up','풀업','BACK','REPS','BAR','고정된 철봉을 손바닥이 앞을 향하도록 잡으세요.','몸을 철봉 쪽으로 당겼다가 천천히 내려오세요.'),
 ('chin-up','친업','BACK','REPS','BAR','고정된 철봉을 손바닥이 몸을 향하도록 잡으세요.','몸을 철봉 쪽으로 당겼다가 천천히 내려오세요.'),
 ('inverted-row','인버티드 로우','BACK','REPS','BAR','고정된 낮은 바를 잡고 몸을 기울여 곧게 유지하세요.','가슴을 바 쪽으로 당겼다가 팔을 펴며 돌아오세요.'),
 ('prone-ytw-raise','엎드린 Y–T–W 레이즈','BACK','REPS','NONE','엎드려 이마를 편하게 두고 팔을 뻗으세요.','팔을 Y, T, W 모양으로 차례로 들었다가 내리세요.'),
 ('prone-cobra','프론 코브라','BACK','DURATION','NONE','엎드려 팔을 몸통 옆에 두고 다리를 편하게 뻗으세요.','가슴과 팔을 살짝 들어 어깨뼈를 모으며 자세를 유지하세요.'),
 ('bird-dog','버드독','BACK','REPS','NONE','손과 무릎을 바닥에 놓고 네발기기 자세를 만드세요.','반대쪽 팔과 다리를 뻗었다가 몸통을 유지하며 돌아오세요.'),
 ('crunch','크런치','CORE','REPS','NONE','바로 누워 무릎을 굽히고 발을 바닥에 놓으세요.','복부에 힘을 주며 어깨를 조금 들어 올렸다가 내리세요.'),
 ('reverse-crunch','리버스 크런치','CORE','REPS','NONE','바로 누워 무릎을 굽히고 팔을 몸 옆에 두세요.','골반을 천천히 말아 올렸다가 바닥으로 내리세요.'),
 ('sit-up','싯업','CORE','REPS','NONE','바로 누워 무릎을 굽히고 발을 안정적으로 놓으세요.','몸통을 일으켜 앉았다가 천천히 내려오세요.'),
 ('dead-bug','데드버그','CORE','REPS','NONE','바로 누워 팔을 들고 무릎을 굽혀 다리를 들어 올리세요.','반대쪽 팔과 다리를 뻗었다가 몸통을 유지하며 돌아오세요.'),
 ('hollow-hold','홀로우 홀드','CORE','DURATION','NONE','바로 누워 팔과 다리를 뻗을 준비를 하세요.','허리가 과하게 뜨지 않도록 하며 팔과 다리를 들어 유지하세요.'),
 ('side-plank','사이드 플랭크','CORE','DURATION','NONE','옆으로 누워 팔꿈치를 어깨 아래에 놓으세요.','골반을 들어 몸을 일직선으로 유지하세요.'),
 ('plank-shoulder-tap','플랭크 숄더탭','CORE','REPS','NONE','손을 바닥에 짚고 높은 플랭크 자세를 만드세요.','몸통 회전을 줄이며 한 손씩 반대쪽 어깨를 터치하세요.'),
 ('bicycle-crunch','바이시클 크런치','CORE','REPS','NONE','바로 누워 손을 머리 옆에 두고 다리를 드세요.','몸통을 돌리며 반대쪽 무릎과 팔꿈치를 번갈아 가까이 가져오세요.'),
 ('russian-twist','맨몸 러시안 트위스트','CORE','REPS','NONE','앉아서 무릎을 굽히고 몸통을 약간 뒤로 기울이세요.','몸통을 좌우로 천천히 돌리며 균형을 유지하세요.'),
 ('leg-raise','레그레이즈','CORE','REPS','NONE','바로 누워 다리를 펴고 팔을 몸 옆에 두세요.','다리를 들어 올렸다가 허리가 과하게 뜨지 않게 내리세요.'),
 ('flutter-kick','플러터킥','CORE','DURATION','NONE','바로 누워 다리를 뻗고 복부에 힘을 주세요.','다리를 번갈아 작게 들고 내리며 몸통을 유지하세요.'),
 ('jumping-jack','점핑잭','CARDIO','REPS','NONE','양발을 모으고 팔을 몸 옆에 두고 서세요.','가볍게 뛰며 다리를 벌리고 팔을 올렸다가 원위치로 돌아오세요.'),
 ('mountain-climber','마운틴 클라이머','CARDIO','DURATION','NONE','손을 바닥에 짚고 높은 플랭크 자세를 만드세요.','무릎을 번갈아 가슴 쪽으로 당기며 몸통을 유지하세요.'),
 ('burpee','버피','CARDIO','REPS','NONE','발을 골반너비로 벌리고 설 공간을 확보하세요.','쪼그려 손을 짚고 다리를 뒤로 보낸 뒤 다시 일어나세요.'),
 ('jump-squat','점프 스쿼트','CARDIO','REPS','NONE','발을 어깨너비로 벌리고 서세요.','무릎을 굽혔다가 위로 뛰고 부드럽게 착지하세요.'),
 ('jumping-lunge','점핑 런지','CARDIO','REPS','NONE','양발을 앞뒤로 벌리고 서세요.','가볍게 뛰며 앞뒤 발을 바꾸고 부드럽게 착지하세요.'),
 ('tuck-jump','턱 점프','CARDIO','REPS','NONE','발을 골반너비로 벌리고 착지할 공간을 확보하세요.','위로 뛰며 무릎을 끌어올렸다가 부드럽게 착지하세요.'),
 ('step-jack','스텝잭','CARDIO','REPS','NONE','양발을 모으고 서세요.','한 발씩 옆으로 내디디며 팔을 올렸다가 돌아오세요.'),
 ('march-in-place','제자리 걷기','CARDIO','DURATION','NONE','편하게 설 공간을 확보하고 자세를 세우세요.','양발을 번갈아 들어 올리며 일정하게 제자리에서 걸으세요.')
)
INSERT INTO routine.exercise
    (id, name, category, measurement_type, equipment, description, preparation, instructions,
     precautions, intro_tts_text, display_order, assessment_status)
SELECT id, name, category, measure, equipment,
       name || ' 운동입니다.', preparation, instructions,
       '통증이 느껴지거나 균형이 불안정하면 중단하세요. 필요한 지지대가 있다면 먼저 안전하게 고정하세요.',
       name || ' 운동을 시작할게요. ' || preparation || ' ' || instructions,
       3 + row_number() OVER (ORDER BY category, id),
       CASE WHEN id = 'glute-bridge' THEN 'IN_PROGRESS' ELSE 'PLANNED' END
FROM catalog;
