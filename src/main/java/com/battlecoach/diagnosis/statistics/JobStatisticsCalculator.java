package com.battlecoach.diagnosis.statistics;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.stereotype.Component;

import com.battlecoach.diagnosis.domain.AnalysisContext;
import com.battlecoach.diagnosis.domain.SkillUsage;

/** 랭커 표본(기록별 분석 컨텍스트)에서 직업 통계를 계산한다. */
@Component
public class JobStatisticsCalculator {

    /** 두 시전이 이 안에 있으면 함께 썼다고 본다. */
    public static final long PAIR_WINDOW_MS = 1_000;
    /** 한 표본에서 A 시전의 이 비율 이상을 B 와 함께 썼으면 그 표본은 둘을 짝으로 쓴 것이다. */
    public static final double PAIRED_CAST_RATIO = 0.8;
    /** 표본의 이 비율 이상이 짝으로 썼으면 연동 쌍 후보로 남긴다. */
    static final double MIN_PAIRED_RATE = 0.8;
    /** 짝 판정에 쓰는 최소 쿨. 계속 쓰는 짧은 쿨 스킬은 우연히 겹친다. */
    static final long MIN_PAIR_COOLDOWN_MS = 10_000;

    public JobStatistics calculate(String characterClass, int periodNo, List<AnalysisContext> samples) {
        return new JobStatistics(characterClass, periodNo, samples.size(), distributions(samples), pairs(samples));
    }

    private static Map<String, SkillDistribution> distributions(List<AnalysisContext> samples) {
        Map<String, List<Observation>> byBaseName = new LinkedHashMap<>();
        for (AnalysisContext sample : samples) {
            double minutes = sample.playTimeMs() / 60_000.0;
            for (SkillUsage skill : sample.skills()) {
                if (skill.castCount() == 0 || minutes <= 0) {
                    continue;
                }
                byBaseName.computeIfAbsent(skill.baseName(), key -> new ArrayList<>()).add(new Observation(
                        skill.skillName(),
                        skill.castCount() / minutes,
                        skill.damage() == null ? null : sample.toSeconds(skill.damage())));
            }
        }

        Map<String, SkillDistribution> distributions = new LinkedHashMap<>();
        byBaseName.forEach((baseName, observations) -> {
            List<Double> seconds = observations.stream().map(Observation::seconds).filter(Objects::nonNull).toList();
            distributions.put(baseName, new SkillDistribution(
                    baseName,
                    observations.get(0).skillName(),
                    observations.size(),
                    (double) observations.size() / samples.size(),
                    Quartiles.of(observations.stream().map(Observation::castsPerMinute).toList()),
                    seconds.isEmpty() ? null : Quartiles.of(seconds)));
        });
        return distributions;
    }

    /**
     * 시전 수가 비슷한(±1) 쿨 긴 스킬끼리 A → B 방향으로 센다.
     * 극딜에서는 여러 스킬이 함께 나가므로 한 스킬에 짝 후보가 여럿일 수 있다.
     */
    private static List<PairStatistic> pairs(List<AnalysisContext> samples) {
        Map<String, PairCounter> counters = new LinkedHashMap<>();
        for (AnalysisContext sample : samples) {
            List<SkillUsage> candidates = sample.skills().stream()
                    .filter(skill -> skill.castCount() > 0)
                    .filter(skill -> skill.hasCooldown() && skill.effectiveCooldownMs() >= MIN_PAIR_COOLDOWN_MS)
                    .toList();
            for (SkillUsage skill : candidates) {
                for (SkillUsage partner : candidates) {
                    if (skill == partner || Math.abs(skill.castCount() - partner.castCount()) > 1) {
                        continue;
                    }
                    PairCounter counter = counters.computeIfAbsent(skill.baseName() + "|" + partner.baseName(),
                            key -> new PairCounter(skill.baseName(), partner.baseName(), partner.skillName()));
                    counter.bothUsed++;
                    if (skill.pairedCountWith(partner, PAIR_WINDOW_MS) >= skill.castCount() * PAIRED_CAST_RATIO) {
                        counter.paired++;
                    }
                }
            }
        }
        int minBothUsed = Math.max(3, (int) Math.ceil(samples.size() / 2.0));
        return counters.values().stream()
                .filter(counter -> counter.bothUsed >= minBothUsed)
                .map(counter -> new PairStatistic(counter.baseName, counter.partnerBaseName, counter.partnerSkillName,
                        counter.bothUsed, (double) counter.paired / counter.bothUsed))
                .filter(pair -> pair.pairedRate() >= MIN_PAIRED_RATE)
                .toList();
    }

    private record Observation(String skillName, double castsPerMinute, Double seconds) {
    }

    private static final class PairCounter {

        private final String baseName;
        private final String partnerBaseName;
        private final String partnerSkillName;
        private int bothUsed;
        private int paired;

        private PairCounter(String baseName, String partnerBaseName, String partnerSkillName) {
            this.baseName = baseName;
            this.partnerBaseName = partnerBaseName;
            this.partnerSkillName = partnerSkillName;
        }
    }
}
