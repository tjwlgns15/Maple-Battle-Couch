-- 랭커 수집기를 없앴다(2026-10-01). 비교 대상은 같은 직업·기간으로 저장된 모든 기록(replay + replay_period)에서 고른다.
-- 수집한 리플레이 본문은 replay 에 그대로 남아 비교 풀에 들어간다. 지우는 것은 수집 표시와 랭커 확인 이력뿐이다.
DROP TABLE IF EXISTS `ranker_sample`;
DROP TABLE IF EXISTS `ranker_probe`;
