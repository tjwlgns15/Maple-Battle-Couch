package com.battlecoach.comparison.application;

import java.util.List;
import java.util.Optional;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.battlecoach.diagnosis.domain.AnalysisContext;
import com.battlecoach.diagnosis.statistics.ComparisonCriteria;
import com.battlecoach.diagnosis.statistics.ComparisonSampleSelector;
import com.battlecoach.diagnosis.statistics.ComparisonStatistics;
import com.battlecoach.diagnosis.statistics.EfficiencyModel;
import com.battlecoach.diagnosis.statistics.EfficiencyModelFitter;
import com.battlecoach.diagnosis.statistics.JobStatisticsCalculator;
import com.battlecoach.diagnosis.statistics.JobStatisticsProvider;
import com.battlecoach.global.config.CacheNames;

import lombok.RequiredArgsConstructor;

/**
 * 같은 직업·기간으로 저장된 기록(비교 풀)에서 조건에 맞는 기록을 골라 통계를 계산한다. API 를 부르지 않는다.
 * 전투력 대비 DPS 추세선은 고른 표본이 아니라 풀 전체로 적합한다. 검색이 쌓이면 풀이 커지므로 짧은 TTL 로 캐시한다.
 */
@Service
@RequiredArgsConstructor
public class JobStatisticsService implements JobStatisticsProvider {

    private final ComparisonPoolLoader comparisonPoolLoader;
    private final EfficiencyModelFitter efficiencyModelFitter;
    private final ComparisonSampleSelector comparisonSampleSelector;
    private final JobStatisticsCalculator jobStatisticsCalculator;

    @Override
    @Cacheable(cacheNames = CacheNames.JOB_STATISTICS,
            key = "#characterClass + '|' + #periodNo + '|' + #excludeReplayId + '|' + #criteria",
            unless = "#result == null")
    public Optional<ComparisonStatistics> find(String characterClass, int periodNo, String excludeReplayId,
                                               ComparisonCriteria criteria) {
        List<AnalysisContext> pool = comparisonPoolLoader.load(characterClass, periodNo).stream()
                .filter(entry -> !entry.replayId().equals(excludeReplayId))
                .map(PoolEntry::context)
                .toList();
        if (pool.isEmpty()) {
            return Optional.empty();
        }
        EfficiencyModel efficiency = efficiencyModelFitter.fit(pool).orElse(null);
        List<AnalysisContext> samples = comparisonSampleSelector.select(pool, criteria, efficiency);
        if (samples.isEmpty()) {
            return Optional.of(new ComparisonStatistics(null, criteria, pool.size()));
        }
        return Optional.of(new ComparisonStatistics(
                jobStatisticsCalculator.calculate(characterClass, periodNo, samples, efficiency),
                criteria, pool.size()));
    }
}
