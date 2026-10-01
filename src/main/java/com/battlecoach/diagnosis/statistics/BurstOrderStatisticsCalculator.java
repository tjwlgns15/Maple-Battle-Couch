package com.battlecoach.diagnosis.statistics;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.battlecoach.diagnosis.domain.AnalysisContext;
import com.battlecoach.diagnosis.sequence.BurstOrderExtractor;
import com.battlecoach.diagnosis.sequence.BurstOrderExtractor.BurstCast;

import lombok.RequiredArgsConstructor;

/** 비교 대상 표본의 모든 극딜 구간에서 순서 통계를 만든다. */
@Component
@RequiredArgsConstructor
public class BurstOrderStatisticsCalculator {

    /** 두 시전이 이 안에 있으면 같은 매크로에서 함께 나간 것으로 보고 순서를 따지지 않는다. */
    public static final long SIMULTANEOUS_MS = 1_000;
    /** 이 비율 이상의 극딜에서 A 가 먼저면 합의된 순서로 본다. */
    public static final double MIN_PRECEDENCE_RATE = 0.8;

    private final BurstOrderExtractor burstOrderExtractor;

    public BurstOrderStatistics calculate(List<AnalysisContext> samples) {
        List<List<BurstCast>> bursts = samples.stream()
                .flatMap(sample -> burstOrderExtractor.burstCasts(sample).stream())
                .toList();
        if (bursts.isEmpty()) {
            return BurstOrderStatistics.empty();
        }
        return new BurstOrderStatistics(bursts.size(), entries(bursts), precedences(bursts));
    }

    private static List<BurstOrderStatistics.Entry> entries(List<List<BurstCast>> bursts) {
        Map<String, List<Double>> positions = new LinkedHashMap<>();
        for (List<BurstCast> burst : bursts) {
            for (int i = 0; i < burst.size(); i++) {
                double position = burst.size() == 1 ? 0 : (double) i / (burst.size() - 1);
                positions.computeIfAbsent(burst.get(i).baseName(), key -> new ArrayList<>()).add(position);
            }
        }
        List<BurstOrderStatistics.Entry> entries = new ArrayList<>();
        positions.forEach((baseName, values) -> entries.add(new BurstOrderStatistics.Entry(
                baseName, (double) values.size() / bursts.size(), Quartiles.of(values).p50())));
        return entries;
    }

    private static List<BurstOrderStatistics.Precedence> precedences(List<List<BurstCast>> bursts) {
        Map<String, int[]> counts = new LinkedHashMap<>(); // key "a|b" (a < b 사전순) → [a 먼저, b 먼저]
        for (List<BurstCast> burst : bursts) {
            for (int i = 0; i < burst.size(); i++) {
                for (int j = i + 1; j < burst.size(); j++) {
                    BurstCast earlier = burst.get(i);
                    BurstCast later = burst.get(j);
                    if (later.timeMs() - earlier.timeMs() <= SIMULTANEOUS_MS) {
                        continue;
                    }
                    boolean earlierIsFirstKey = earlier.baseName().compareTo(later.baseName()) < 0;
                    String key = earlierIsFirstKey
                            ? earlier.baseName() + "|" + later.baseName()
                            : later.baseName() + "|" + earlier.baseName();
                    counts.computeIfAbsent(key, k -> new int[2])[earlierIsFirstKey ? 0 : 1]++;
                }
            }
        }
        int minOrdered = Math.max(3, (int) Math.ceil(bursts.size() / 2.0));
        List<BurstOrderStatistics.Precedence> precedences = new ArrayList<>();
        counts.forEach((key, count) -> {
            String[] names = key.split("\\|", 2);
            int ordered = count[0] + count[1];
            if (ordered < minOrdered) {
                return;
            }
            double firstRate = (double) count[0] / ordered;
            if (firstRate >= MIN_PRECEDENCE_RATE) {
                precedences.add(new BurstOrderStatistics.Precedence(names[0], names[1], ordered, firstRate));
            } else if (1 - firstRate >= MIN_PRECEDENCE_RATE) {
                precedences.add(new BurstOrderStatistics.Precedence(names[1], names[0], ordered, 1 - firstRate));
            }
        });
        return precedences;
    }
}
