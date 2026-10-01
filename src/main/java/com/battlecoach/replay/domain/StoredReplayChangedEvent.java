package com.battlecoach.replay.domain;

/**
 * 저장된 리플레이가 새로 생기거나, 갱신되거나, 지워졌다. 저장된 기록으로 만든 캐시(비교 풀, 통계)를 비우는 데 쓴다.
 * 바꾼 트랜잭션 안에서 발행한다. 지울 때는 기간도 함께 지우므로 기간을 이벤트에 담는다.
 *
 * @param periodNo 연무장 기간. 기록 목록을 거치지 않아 모르면 null (비교 풀에 들어가지 않은 기록이다)
 */
public record StoredReplayChangedEvent(String replayId, String characterClass, Integer periodNo) {
}
