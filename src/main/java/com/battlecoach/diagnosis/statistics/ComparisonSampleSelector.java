package com.battlecoach.diagnosis.statistics;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.battlecoach.diagnosis.domain.AnalysisContext;

/**
 * 같은 직업·기간 기록(풀)에서 비교 조건에 맞는 기록을 고른다.
 * 상위 N% 는 순위를 매길 수 있는 기록 수 × N% 를 올림한 개수다(최소 1개).
 * 전투력 대비 DPS 기준은 전투력이 없는 기록과, 추세선을 만들 수 없을 때(기록 5개 미만) 모든 기록을 순위에서 뺀다.
 */
@Component
public class ComparisonSampleSelector {

    /**
     * @param efficiency 풀 전체로 적합한 추세선. 없으면 null
     * @return 기준 순위 상위부터
     */
    public List<AnalysisContext> select(List<AnalysisContext> pool, ComparisonCriteria criteria, EfficiencyModel efficiency) {
        List<Ranked> ranked = switch (criteria.basis()) {
            case DPS -> pool.stream().map(context -> new Ranked(context, context.totalDps())).toList();
            case EFFICIENCY -> efficiency == null ? List.of() : pool.stream()
                    .map(context -> efficiency.sampleEfficiencyOf(context).map(value -> new Ranked(context, value)))
                    .flatMap(Optional::stream)
                    .toList();
        };
        int count = topCount(ranked.size(), criteria.topPercent());
        return ranked.stream()
                .sorted(Comparator.comparingDouble(Ranked::score).reversed())
                .limit(count)
                .map(Ranked::context)
                .toList();
    }

    static int topCount(int size, int topPercent) {
        if (size == 0) {
            return 0;
        }
        return Math.max(1, (int) Math.ceil(size * topPercent / 100.0));
    }

    private record Ranked(AnalysisContext context, double score) {
    }
}
