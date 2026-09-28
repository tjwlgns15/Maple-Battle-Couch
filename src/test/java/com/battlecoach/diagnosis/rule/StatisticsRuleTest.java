package com.battlecoach.diagnosis.rule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.battlecoach.diagnosis.domain.AnalysisContext;
import com.battlecoach.diagnosis.domain.Finding;
import com.battlecoach.diagnosis.domain.FindingType;
import com.battlecoach.diagnosis.domain.SkillUsage;
import com.battlecoach.diagnosis.statistics.JobStatistics;
import com.battlecoach.diagnosis.statistics.PairStatistic;
import com.battlecoach.diagnosis.statistics.Quartiles;
import com.battlecoach.diagnosis.statistics.SkillDistribution;
import com.battlecoach.spec.domain.CharacterSpec;
import com.battlecoach.spec.domain.CooldownStats;

/** 통계 규칙(CastRateRule, LinkedPairRule) */
class StatisticsRuleTest {

    private static final CharacterSpec SPEC = CharacterSpec.of(CooldownStats.of(4, 6, 27, 0), List.of());
    private static final long FIVE_MINUTES = 300_000;

    private final CastRateRule castRateRule = new CastRateRule();
    private final LinkedPairRule linkedPairRule = new LinkedPairRule();

    @Test
    void 분당_시전_수가_랭커_하위_25퍼센트보다_낮으면_중앙값까지_모자란_시전으로_영향도를_낸다() {
        // 5분에 스틱스 3회(0.6회/분), 랭커 중앙값 1.2회/분 → 6회까지 3회 모자람, 1회 1초
        AnalysisContext context = context(skill("스틱스", 52_000L, 3_000L, 0, 110_000, 220_000))
                .withStatistics(statistics(8, distribution("스틱스", 8, 1.1, 1.2, 1.3), List.of()));

        List<Finding> findings = castRateRule.evaluate(context);

        assertThat(findings).singleElement().satisfies(finding -> {
            assertThat(finding.type()).isEqualTo(FindingType.CAST_RATE_BELOW_RANKERS);
            assertThat(finding.impactSeconds()).isCloseTo(3.0, within(1e-9));
            assertThat(finding.message()).contains("랭커 8명").contains("약 3회");
        });
    }

    @Test
    void 표본이_5명_미만이면_통계_규칙은_아무것도_내지_않는다() {
        AnalysisContext context = context(skill("스틱스", 52_000L, 3_000L, 0, 110_000, 220_000))
                .withStatistics(statistics(4, distribution("스틱스", 4, 1.1, 1.2, 1.3),
                        List.of(new PairStatistic("스틱스", "레디 투 다이", "레디 투 다이", 4, 1.0))));

        assertThat(castRateRule.evaluate(context)).isEmpty();
        assertThat(linkedPairRule.evaluate(context)).isEmpty();
    }

    @Test
    void 랭커_다수가_함께_쓰는_쌍을_따로_쓰면_참고로_알리고_같은_쌍은_한_번만_낸다() {
        AnalysisContext context = context(
                skill("스틱스", 52_000L, 3_000L, 60_000, 170_000, 280_000),
                skill("레디 투 다이", 52_000L, null, 1_000, 55_000, 110_000, 165_000, 220_000, 275_000))
                .withStatistics(statistics(8, distribution("스틱스", 8, 0.5, 0.6, 0.7), List.of(
                        new PairStatistic("스틱스", "레디 투 다이", "레디 투 다이", 8, 1.0),
                        new PairStatistic("레디 투 다이", "스틱스", "스틱스", 8, 1.0))));

        List<Finding> findings = linkedPairRule.evaluate(context);

        assertThat(findings).singleElement().satisfies(finding -> {
            assertThat(finding.type()).isEqualTo(FindingType.LINKED_PAIR_SEPARATED);
            assertThat(finding.isMeasured()).isFalse();
            assertThat(finding.message()).contains("랭커 8명 중 100%").contains("0/3회");
        });
    }

    private static AnalysisContext context(SkillUsage... skills) {
        return AnalysisContext.single(FIVE_MINUTES, 1_000, SPEC, List.of(skills), List.of());
    }

    private static JobStatistics statistics(int samples, SkillDistribution distribution, List<PairStatistic> pairs) {
        return new JobStatistics("칼리", 4, samples, Map.of(distribution.baseName(), distribution), pairs);
    }

    private static SkillDistribution distribution(String name, int users, double p25, double p50, double p75) {
        return new SkillDistribution(name, name, users, 1.0, new Quartiles(p25, p50, p75), null);
    }

    private static SkillUsage skill(String name, Long cooldownMs, Long damage, long... castTimes) {
        return new SkillUsage(name, name, Arrays.stream(castTimes).boxed().toList(), damage, cooldownMs, null);
    }
}
