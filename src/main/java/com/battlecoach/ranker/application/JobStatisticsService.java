package com.battlecoach.ranker.application;

import java.util.List;
import java.util.Optional;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.battlecoach.diagnosis.domain.AnalysisContext;
import com.battlecoach.diagnosis.statistics.JobStatistics;
import com.battlecoach.diagnosis.statistics.JobStatisticsCalculator;
import com.battlecoach.diagnosis.statistics.JobStatisticsProvider;
import com.battlecoach.global.config.CacheNames;

import lombok.RequiredArgsConstructor;

/**
 * 저장된 랭커 표본으로 통계를 계산한다. API 를 부르지 않는다.
 * 수집 중에는 표본이 늘어나므로 짧은 TTL 로 캐시한다.
 */
@Service
@RequiredArgsConstructor
public class JobStatisticsService implements JobStatisticsProvider {

    private final RankerSampleLoader rankerSampleLoader;
    private final JobStatisticsCalculator jobStatisticsCalculator;

    @Override
    @Cacheable(cacheNames = CacheNames.JOB_STATISTICS,
            key = "#characterClass + '|' + #periodNo + '|' + #excludeReplayId",
            unless = "#result == null")
    public Optional<JobStatistics> find(String characterClass, int periodNo, String excludeReplayId) {
        List<AnalysisContext> samples = List.copyOf(
                rankerSampleLoader.load(characterClass, periodNo, excludeReplayId).values());
        if (samples.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(jobStatisticsCalculator.calculate(characterClass, periodNo, samples));
    }
}
