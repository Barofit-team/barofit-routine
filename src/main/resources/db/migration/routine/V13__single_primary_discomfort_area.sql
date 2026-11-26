-- 추천용 대표 부위를 종목당 하나로 통일한다. 사용자 불편 부위 복수 선택은 유지한다.
-- 복합 동작의 모든 사용 부위를 뜻하지 않으며 기존 종목 가이드·주의사항은 보존한다.
CREATE TEMPORARY TABLE exercise_primary_discomfort (
  exercise_id VARCHAR(50) PRIMARY KEY,
  area VARCHAR(20) NOT NULL
) ON COMMIT DROP;
INSERT INTO exercise_primary_discomfort (exercise_id, area) VALUES
  ('push-up', 'ARM_WRIST'),
  ('knee-push-up', 'ARM_WRIST'),
  ('incline-push-up', 'ARM_WRIST'),
  ('pike-push-up', 'NECK_SHOULDER'),
  ('handstand-push-up', 'NECK_SHOULDER'),
  ('close-grip-push-up', 'ARM_WRIST'),
  ('dips', 'ARM_WRIST'),
  ('pull-up', 'BACK'),
  ('chin-up', 'BACK'),
  ('inverted-row', 'BACK'),
  ('squat', 'LEG_ANKLE'),
  ('split-squat', 'LEG_ANKLE'),
  ('forward-lunge', 'LEG_ANKLE'),
  ('reverse-lunge', 'LEG_ANKLE'),
  ('single-leg-squat', 'LEG_ANKLE'),
  ('bulgarian-split-squat', 'LEG_ANKLE'),
  ('glute-bridge', 'LEG_ANKLE'),
  ('single-leg-glute-bridge', 'LEG_ANKLE'),
  ('side-lying-leg-raise', 'LEG_ANKLE'),
  ('clamshell', 'LEG_ANKLE'),
  ('standing-calf-raise', 'LEG_ANKLE'),
  ('single-leg-calf-raise', 'LEG_ANKLE'),
  ('jump-squat', 'LEG_ANKLE'),
  ('jumping-lunge', 'LEG_ANKLE'),
  ('march-in-place', 'LEG_ANKLE'),
  ('lunge', 'LEG_ANKLE'),
  ('plank', 'BACK'),
  ('side-plank', 'BACK'),
  ('plank-shoulder-tap', 'ARM_WRIST'),
  ('bird-dog', 'BACK'),
  ('prone-ytw-raise', 'NECK_SHOULDER'),
  ('prone-cobra', 'BACK'),
  ('superman', 'BACK'),
  ('crunch', 'BACK'),
  ('reverse-crunch', 'BACK'),
  ('sit-up', 'BACK'),
  ('dead-bug', 'BACK'),
  ('hollow-hold', 'BACK'),
  ('bicycle-crunch', 'BACK'),
  ('russian-twist', 'BACK'),
  ('leg-raise', 'BACK'),
  ('flutter-kick', 'BACK'),
  ('jumping-jack', 'LEG_ANKLE'),
  ('step-jack', 'LEG_ANKLE'),
  ('mountain-climber', 'LEG_ANKLE'),
  ('burpee', 'LEG_ANKLE'),
  ('tuck-jump', 'LEG_ANKLE');

-- 열린 편집 화면이 이전 매핑으로 덮어쓰지 못하도록 변경된 종목 버전도 갱신한다.
UPDATE routine.exercise e
SET version = version + 1
WHERE EXISTS (
  SELECT 1 FROM exercise_primary_discomfort p
  WHERE p.exercise_id = e.id AND (
    (SELECT count(*) FROM routine.exercise_discomfort_area a WHERE a.exercise_id = e.id) <> 1
    OR NOT EXISTS (SELECT 1 FROM routine.exercise_discomfort_area a
                   WHERE a.exercise_id = e.id AND a.area = p.area)
  )
);
DELETE FROM routine.exercise_discomfort_area a
USING exercise_primary_discomfort p
WHERE a.exercise_id = p.exercise_id AND a.area <> p.area;
INSERT INTO routine.exercise_discomfort_area (exercise_id, area)
SELECT p.exercise_id, p.area FROM exercise_primary_discomfort p
JOIN routine.exercise e ON e.id = p.exercise_id
ON CONFLICT (exercise_id, area) DO NOTHING;
ALTER TABLE routine.exercise_discomfort_area
  ADD CONSTRAINT uq_exercise_primary_discomfort UNIQUE (exercise_id);
