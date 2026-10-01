package com.battlecoach.comparison.application;

import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.battlecoach.diagnosis.domain.AnalysisContext;
import com.battlecoach.diagnosis.statistics.ComparisonCriteria;
import com.battlecoach.diagnosis.statistics.ComparisonSampleSelector;
import com.battlecoach.diagnosis.statistics.EfficiencyModelFitter;
import com.battlecoach.diagnosis.statistics.HoldTradeoff;
import com.battlecoach.diagnosis.statistics.HoldTradeoffAnalyzer;

import lombok.RequiredArgsConstructor;

/** 비교 조건으로 고른 기록으로 "극딜까지 아끼는 것 vs 쿨마다 쓰는 것" 가설을 확인한다(분석용). */
@Service
@RequiredArgsConstructor
public class HoldTradeoffService {

    private final ComparisonPoolLoader comparisonPoolLoader;
    private final EfficiencyModelFitter efficiencyModelFitter;
    private final ComparisonSampleSelector comparisonSampleSelector;
    private final HoldTradeoffAnalyzer holdTradeoffAnalyzer;

    public Optional<HoldTradeoff> analyze(String characterClass, int periodNo, String skillBaseName,
                                          ComparisonCriteria criteria) {
        List<PoolEntry> pool = comparisonPoolLoader.load(characterClass, periodNo);
        Map<AnalysisContext, String> names = new IdentityHashMap<>();
        pool.forEach(entry -> names.put(entry.context(), entry.characterName()));
        List<AnalysisContext> contexts = pool.stream().map(PoolEntry::context).toList();

        Map<String, AnalysisContext> samples = new LinkedHashMap<>();
        comparisonSampleSelector.select(contexts, criteria, efficiencyModelFitter.fit(contexts).orElse(null))
                .forEach(context -> samples.put(names.get(context), context));
        return holdTradeoffAnalyzer.analyze(skillBaseName, samples);
    }
}
