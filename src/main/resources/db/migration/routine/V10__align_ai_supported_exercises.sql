-- AI팀 1차 검증 목록 23종목. 기존 데이터·가이드는 보존한다.
-- 이후 모델 지원이 추가되면 해당 종목을 READY로 올려 사용자에게 다시 노출한다.
UPDATE routine.exercise
SET assessment_status = CASE WHEN id IN (
    'push-up',
    'plank',
    'squat',
    'bird-dog',
    'jumping-jack',
    'lunge',
    'leg-raise',
    'side-lying-leg-raise',
    'glute-bridge',
    'dead-bug',
    'superman',
    'pike-push-up',
    'standing-calf-raise',
    'incline-push-up',
    'bulgarian-split-squat',
    'split-squat',
    'jump-squat',
    'mountain-climber',
    'reverse-crunch',
    'side-plank',
    'flutter-kick',
    'burpee',
    'sit-up'
) THEN 'READY' ELSE 'PLANNED' END,
updated_at = CURRENT_TIMESTAMP;
