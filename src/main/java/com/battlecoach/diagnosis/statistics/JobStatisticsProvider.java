package com.battlecoach.diagnosis.statistics;

import java.util.Optional;

/** 직업·기간별 비교 대상 통계를 제공한다. 표본을 모으는 방식은 구현체가 정한다. */
public interface JobStatisticsProvider {

    /**
     * @param excludeReplayId 통계에서 뺄 리플레이. 진단 대상이 풀에 들어 있으면 자기 자신과 비교하게 되므로 뺀다.
     * @return 같은 직업·기간 기록이 하나도 없으면 비어 있다
     */
    Optional<ComparisonStatistics> find(String characterClass, int periodNo, String excludeReplayId,
                                        ComparisonCriteria criteria);
}
