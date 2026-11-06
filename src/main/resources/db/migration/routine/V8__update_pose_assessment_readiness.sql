-- READY means the pose rule module exists; it does not assert app integration or final validation.
INSERT INTO routine.exercise
    (id, name, category, measurement_type, equipment, description, preparation, instructions,
     precautions, intro_tts_text, display_order, assessment_status)
VALUES
    ('lunge', '런지', 'LOWER_BODY', 'REPS', 'NONE', '런지 운동입니다.',
     '양발을 앞뒤로 벌리고 균형을 잡으세요.',
     '두 무릎을 굽혀 몸을 내렸다가 천천히 일어나세요.',
     '통증이 느껴지거나 균형이 불안정하면 중단하세요.',
     '런지를 시작할게요. 양발을 앞뒤로 벌리고 균형을 잡은 뒤 두 무릎을 굽혔다가 천천히 일어나세요.',
     200, 'READY'),
    ('superman', '수퍼맨', 'BACK', 'REPS', 'NONE', '수퍼맨 운동입니다.',
     '바닥에 엎드려 팔과 다리를 편하게 뻗으세요.',
     '팔과 다리를 가능한 범위에서 들어 올렸다가 천천히 내리세요.',
     '허리에 통증이 느껴지면 동작을 중단하세요.',
     '수퍼맨 운동을 시작할게요. 바닥에 엎드려 팔과 다리를 뻗고 가능한 범위에서 들어 올렸다가 천천히 내리세요.',
     201, 'READY')
ON CONFLICT (id) DO NOTHING;

UPDATE routine.exercise SET assessment_status = 'READY'
WHERE id IN (
    'push-up', 'plank', 'squat', 'bird-dog', 'jumping-jack', 'leg-raise',
    'glute-bridge', 'pike-push-up', 'superman', 'mountain-climber', 'lunge',
    'split-squat', 'bulgarian-split-squat', 'incline-push-up',
    'side-lying-leg-raise', 'standing-calf-raise', 'dead-bug'
);
