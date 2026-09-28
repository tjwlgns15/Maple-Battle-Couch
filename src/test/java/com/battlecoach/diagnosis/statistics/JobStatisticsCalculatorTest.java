package com.battlecoach.diagnosis.statistics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.battlecoach.diagnosis.domain.AnalysisContext;
import com.battlecoach.diagnosis.domain.SkillUsage;
import com.battlecoach.spec.domain.CharacterSpec;
import com.battlecoach.spec.domain.CooldownStats;

class JobStatisticsCalculatorTest {

    private static final CharacterSpec SPEC = CharacterSpec.of(CooldownStats.of(0, 0, 27, 0), List.of());

    private final JobStatisticsCalculator calculator = new JobStatisticsCalculator();

    @Test
    void 분위수는_선형_보간한다() {
        Quartiles quartiles = Quartiles.of(List.of(4.0, 1.0, 3.0, 2.0, 5.0));

        assertThat(quartiles.p25()).isEqualTo(2.0);
        assertThat(quartiles.p50()).isEqualTo(3.0);
        assertThat(quartiles.p75()).isEqualTo(4.0);
        assertThat(Quartiles.of(List.of(1.0, 2.0)).p50()).isEqualTo(1.5);
    }

    @Test
    void 스킬별_채택률과_분당_시전_수를_쓴_표본으로만_계산한다() {
        // 5분 전투. 판데모니움은 4명이 10·12·14·16회, 1명은 안 씀
        List<AnalysisContext> samples = new ArrayList<>();
        for (int casts : new int[]{10, 12, 14, 16}) {
            samples.add(sample(skill("판데모니움", 24_000L, 1_000L, spread(casts, 20_000))));
        }
        samples.add(sample(skill("데스 블로섬", 52_000L, 1_000L, 0)));

        JobStatistics statistics = calculator.calculate("칼리", 4, samples);

        SkillDistribution pandemonium = statistics.skill("판데모니움").orElseThrow();
        assertThat(statistics.sampleCount()).isEqualTo(5);
        assertThat(pandemonium.userCount()).isEqualTo(4);
        assertThat(pandemonium.adoptionRate()).isEqualTo(0.8);
        assertThat(pandemonium.castsPerMinute().p50()).isCloseTo(2.6, within(1e-9)); // (12+14)/2 / 5분
    }

    @Test
    void 표본_대부분이_1초_안에_함께_쓰는_쌍만_남긴다() {
        List<AnalysisContext> samples = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            samples.add(sample(
                    skill("스틱스", 52_000L, 1_000L, 1_000, 60_000, 120_000),
                    skill("레디 투 다이", 52_000L, null, 1_300, 60_200, 120_500),
                    skill("판데모니움", 24_000L, 1_000L, 10_000, 40_000, 90_000)));
        }
        samples.add(sample( // 한 명은 따로 쓴다 → 4/5 = 80%
                skill("스틱스", 52_000L, 1_000L, 1_000, 60_000, 120_000),
                skill("레디 투 다이", 52_000L, null, 30_000, 90_000, 150_000),
                skill("판데모니움", 24_000L, 1_000L, 10_000, 40_000, 90_000)));

        JobStatistics statistics = calculator.calculate("칼리", 4, samples);

        PairStatistic pair = statistics.bestPairFor("스틱스").orElseThrow();
        assertThat(pair.partnerBaseName()).isEqualTo("레디 투 다이");
        assertThat(pair.pairedRate()).isEqualTo(0.8);
        assertThat(statistics.bestPairFor("판데모니움")).isEmpty();
    }

    @Test
    void 이어진_쌍은_하나의_묶음으로_합치고_큰_묶음부터_돌려준다() {
        JobStatistics statistics = new JobStatistics("칼리", 4, 11, java.util.Map.of(), List.of(
                new PairStatistic("스틱스", "레디 투 다이", "레디 투 다이", 11, 0.91),
                new PairStatistic("레디 투 다이", "데스 블로섬", "데스 블로섬", 11, 1.0),
                new PairStatistic("레이스 오브 갓", "오블리비온", "오블리비온", 11, 1.0),
                new PairStatistic("오블리비온", "레이스 오브 갓", "레이스 오브 갓", 11, 1.0)));

        List<List<String>> groups = statistics.pairGroups();

        assertThat(groups).hasSize(2);
        assertThat(groups.get(0)).containsExactlyInAnyOrder("스틱스", "레디 투 다이", "데스 블로섬");
        assertThat(groups.get(1)).containsExactlyInAnyOrder("레이스 오브 갓", "오블리비온");
        assertThat(statistics.minPairedRate(groups.get(0))).isEqualTo(0.91);
    }

    private static AnalysisContext sample(SkillUsage... skills) {
        return AnalysisContext.single(300_000, 1_000, SPEC, List.of(skills), List.of());
    }

    private static SkillUsage skill(String name, Long cooldownMs, Long damage, long... castTimes) {
        return new SkillUsage(name, name, java.util.Arrays.stream(castTimes).boxed().toList(), damage, cooldownMs, null);
    }

    private static long[] spread(int count, long step) {
        long[] times = new long[count];
        for (int i = 0; i < count; i++) {
            times[i] = i * step;
        }
        return times;
    }
}
