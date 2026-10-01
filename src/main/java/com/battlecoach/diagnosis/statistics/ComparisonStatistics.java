package com.battlecoach.diagnosis.statistics;

import java.util.Optional;

/**
 * 비교 조건으로 고른 기록들의 통계.
 *
 * @param statistics 조건에 맞는 기록이 하나도 없으면 null (전투력 대비 기준인데 풀이 5개 미만이라 추세선이 없을 때 등)
 * @param poolSize   같은 직업·기간으로 저장된 기록 수(진단 대상 제외). 이 중 조건에 맞는 것이 statistics.sampleCount()
 */
public record ComparisonStatistics(JobStatistics statistics, ComparisonCriteria criteria, int poolSize) {

    public Optional<JobStatistics> jobStatistics() {
        return Optional.ofNullable(statistics);
    }
}
