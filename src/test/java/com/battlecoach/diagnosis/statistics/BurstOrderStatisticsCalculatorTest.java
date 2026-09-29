package com.battlecoach.diagnosis.statistics;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.battlecoach.diagnosis.domain.AnalysisContext;
import com.battlecoach.diagnosis.domain.BurstWindow;
import com.battlecoach.diagnosis.domain.SkillUsage;
import com.battlecoach.diagnosis.sequence.BurstOrderExtractor;
import com.battlecoach.spec.domain.CharacterSpec;
import com.battlecoach.spec.domain.CooldownStats;

class BurstOrderStatisticsCalculatorTest {

    private static final CharacterSpec SPEC = CharacterSpec.of(CooldownStats.of(0, 0, 0, 0), List.of());

    private final BurstOrderStatisticsCalculator calculator =
            new BurstOrderStatisticsCalculator(new BurstOrderExtractor());

    @Test
    void 모든_극딜에서_스킬별_채택률과_상대_위치를_계산하고_표준_순서를_만든다() {
        // 표본 2개 × 극딜 2회. 레이스(0초) → 스틱스(3초) → 보이드 버스트(6초). 에르다 노바는 한 번만 끝에 나온다.
        List<AnalysisContext> samples = List.of(
                sample(List.of(burst(0), burst(110_000)),
                        skill("레이스 오브 갓", 0, 110_000),
                        skill("스틱스", 3_000, 113_000),
                        skill("보이드 버스트", 6_000, 116_000)),
                sample(List.of(burst(0), burst(110_000)),
                        skill("레이스 오브 갓", 0, 110_000),
                        skill("스틱스", 3_000, 113_000),
                        skill("보이드 버스트", 6_000, 116_000),
                        skill("에르다 노바", 8_000)));

        BurstOrderStatistics statistics = calculator.calculate(samples);

        assertThat(statistics.burstCount()).isEqualTo(4);
        assertThat(statistics.entry("에르다 노바").orElseThrow().adoptionRate()).isEqualTo(0.25);
        assertThat(statistics.entry("레이스 오브 갓").orElseThrow().medianPosition()).isEqualTo(0.0);
        assertThat(statistics.standardOrder()).extracting(BurstOrderStatistics.Entry::baseName)
                .containsExactly("레이스 오브 갓", "스틱스", "보이드 버스트");
    }

    @Test
    void 대부분의_극딜에서_먼저_나온_쌍만_남기고_1초_안에_함께_나간_쌍은_순서로_보지_않는다() {
        List<AnalysisContext> samples = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            // 레이스와 오블리비온은 0.3초 차이(같은 매크로), 스틱스는 3초 뒤
            samples.add(sample(List.of(burst(0)),
                    skill("레이스 오브 갓", 0),
                    skill("오블리비온", 300),
                    skill("스틱스", 3_000)));
        }

        BurstOrderStatistics statistics = calculator.calculate(samples);

        assertThat(statistics.precedences())
                .extracting(BurstOrderStatistics.Precedence::before, BurstOrderStatistics.Precedence::after)
                .containsExactlyInAnyOrder(
                        org.assertj.core.groups.Tuple.tuple("레이스 오브 갓", "스틱스"),
                        org.assertj.core.groups.Tuple.tuple("오블리비온", "스틱스"));
    }

    @Test
    void 극딜이_없으면_빈_통계다() {
        BurstOrderStatistics statistics = calculator.calculate(List.of(sample(List.of(), skill("스틱스", 0))));

        assertThat(statistics.burstCount()).isZero();
        assertThat(statistics.standardOrder()).isEmpty();
    }

    private static BurstWindow burst(long startMs) {
        return new BurstWindow(startMs, startMs + 30_000);
    }

    private static AnalysisContext sample(List<BurstWindow> bursts, SkillUsage... skills) {
        return AnalysisContext.single(300_000, 1_000, SPEC, List.of(skills), bursts);
    }

    private static SkillUsage skill(String name, long... castTimes) {
        return new SkillUsage(name, name, Arrays.stream(castTimes).boxed().toList(), 1_000L, 60_000L, null);
    }
}
