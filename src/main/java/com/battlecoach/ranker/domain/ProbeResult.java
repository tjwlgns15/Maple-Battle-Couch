package com.battlecoach.ranker.domain;

public enum ProbeResult {

    /** 캐릭터명으로 ocid 를 찾지 못함 (이름 변경 등) */
    NOT_FOUND,
    /** 연무장 기록이 없음 (replay-id 가 400 OPENAPI00004) */
    NO_RECORD,
    /** 기록은 있지만 대상 기간 기록이 없음 */
    OTHER_PERIOD_ONLY,
    /** 대상 기간 기록을 표본으로 저장함 */
    SAMPLED
}
