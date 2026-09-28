package com.battlecoach.diagnosis.statistics;

import java.util.Optional;

/** 직업·기간별 랭커 통계를 제공한다. 수집·저장 방식은 구현체가 정한다. */
public interface JobStatisticsProvider {

    /**
     * @param excludeReplayId 통계에서 뺄 리플레이. 진단 대상이 표본에 들어 있으면 자기 자신과 비교하게 되므로 뺀다.
     */
    Optional<JobStatistics> find(String characterClass, int periodNo, String excludeReplayId);
}
