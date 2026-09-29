package com.battlecoach.diagnosis.rule;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.battlecoach.diagnosis.domain.AnalysisContext;
import com.battlecoach.diagnosis.domain.BurstWindow;
import com.battlecoach.diagnosis.domain.Finding;
import com.battlecoach.diagnosis.domain.FindingType;
import com.battlecoach.diagnosis.domain.SkillUsage;
import com.battlecoach.diagnosis.sequence.BurstOrderExtractor;
import com.battlecoach.diagnosis.statistics.BurstOrderStatistics;
import com.battlecoach.diagnosis.statistics.BurstOrderStatistics.Precedence;
import com.battlecoach.diagnosis.statistics.JobStatistics;
import com.battlecoach.spec.domain.CharacterSpec;
import com.battlecoach.spec.domain.CooldownStats;

class BurstOrderRuleTest {

    private static final CharacterSpec SPEC = CharacterSpec.of(CooldownStats.of(0, 0, 0, 0), List.of());

    private final BurstOrderRule rule = new BurstOrderRule(new BurstOrderExtractor());

    @Test
    void 랭커_대부분과_반대_순서로_쓰면_합의율이_가장_높은_쌍_하나만_참고로_낸다() {
        // 칼리 B처럼 극딜마다 보이드 버스트(2초) 뒤에 스틱스(6초)를 쓴다
        AnalysisContext context = context(
                skill("보이드 버스트", 2_000, 112_000),
                skill("스틱스", 6_000, 116_000),
                skill("듄 버스트", 8_000, 118_000))
                .withStatistics(statistics(
                        new Precedence("스틱스", "보이드 버스트", 29, 1.0),
                        new Precedence("스틱스", "듄 버스트", 24, 0.88)));

        List<Finding> findings = rule.evaluate(context);

        assertThat(findings).singleElement().satisfies(finding -> {
            assertThat(finding.type()).isEqualTo(FindingType.BURST_ORDER_REVERSED);
            assertThat(finding.skillBaseName()).isEqualTo("스틱스");
            assertThat(finding.isMeasured()).isFalse();
            assertThat(finding.message()).contains("랭커 극딜 29회 중 100%").contains("2번 중 2번 보이드 버스트 뒤");
        });
    }

    @Test
    void 같은_순서거나_1초_안에_함께_썼으면_내지_않는다() {
        AnalysisContext context = context(
                skill("스틱스", 1_000, 111_000),
                skill("보이드 버스트", 1_500, 115_000))
                .withStatistics(statistics(new Precedence("스틱스", "보이드 버스트", 29, 1.0)));

        assertThat(rule.evaluate(context)).isEmpty();
    }

    private static AnalysisContext context(SkillUsage... skills) {
        return AnalysisContext.single(300_000, 1_000, SPEC, List.of(skills),
                List.of(new BurstWindow(0, 30_000), new BurstWindow(110_000, 140_000)));
    }

    private static JobStatistics statistics(Precedence... precedences) {
        return new JobStatistics("칼리", 4, 11, Map.of(), List.of(),
                new BurstOrderStatistics(33, List.of(), List.of(precedences)), null);
    }

    private static SkillUsage skill(String name, long... castTimes) {
        return new SkillUsage(name, name, Arrays.stream(castTimes).boxed().toList(), 1_000L, 60_000L, null);
    }
}
