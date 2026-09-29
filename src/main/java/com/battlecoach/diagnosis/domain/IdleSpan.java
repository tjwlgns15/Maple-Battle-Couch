package com.battlecoach.diagnosis.domain;

/**
 * 스킬 하나가 쿨이 돈 뒤 쓰이지 않은 구간 1개.
 *
 * @param startMs 쿨이 돈 시각
 * @param endMs   다음 시전 시각. 전투 종료 전 구간이면 전투 끝
 */
public record IdleSpan(long startMs, long endMs, Kind kind) {

    public enum Kind {
        /** 극딜 구간 안에서 끝났고 극딜 전에 쿨이 돌았다. 의도한 대기일 수 있다. */
        HELD_FOR_BURST,
        /** 극딜 대기가 아닌 공백 */
        UNUSED,
        /** 마지막으로 쿨이 돈 뒤 전투 끝까지 */
        TAIL
    }

    public long durationMs() {
        return endMs - startMs;
    }
}
