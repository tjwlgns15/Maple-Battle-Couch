package com.battlecoach.diagnosis.statistics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.battlecoach.diagnosis.domain.AnalysisContext;
import com.battlecoach.diagnosis.domain.BurstWindow;
import com.battlecoach.diagnosis.domain.SkillUsage;
import com.battlecoach.spec.domain.CharacterSpec;
import com.battlecoach.spec.domain.CooldownStats;

class HoldTradeoffAnalyzerTest {

    private static final CharacterSpec SPEC = CharacterSpec.of(CooldownStats.of(0, 0, 0, 0), List.of());

    private final HoldTradeoffAnalyzer analyzer = new HoldTradeoffAnalyzer();

    @Test
    void 스피어만_상관은_같은_값에_평균_순위를_주고_계산할_수_없으면_비어_있다() {
        assertThat(Correlation.spearman(List.of(1.0, 2.0, 3.0), List.of(10.0, 20.0, 30.0))).hasValue(1.0);
        assertThat(Correlation.spearman(List.of(1.0, 2.0, 3.0), List.of(30.0, 20.0, 10.0))).hasValue(-1.0);
        assertThat(Correlation.ranks(List.of(5.0, 1.0, 5.0))).containsExactly(2.5, 1.0, 2.5);
        assertThat(Correlation.spearman(List.of(1.0, 2.0), List.of(1.0, 2.0))).isEmpty();
        assertThat(Correlation.spearman(List.of(1.0, 1.0, 1.0), List.of(1.0, 2.0, 3.0))).isEmpty();
    }

    @Test
    void 표본마다_극딜_대기_시간과_시전_수와_초_환산을_모아_상관을_낸다() {
        // 극딜(45~75초) 전에 쿨이 돌았는데 극딜 때 쓴 표본일수록 대기가 길고 시전 수와 데미지가 적다
        Map<String, AnalysisContext> samples = new LinkedHashMap<>();
        samples.put("쿨마다", sample(10_000L, 0, 20_000, 40_000, 60_000, 80_000));   // 대기 0초, 5회
        samples.put("조금 아낌", sample(8_000L, 0, 20_000, 45_000, 65_000, 85_000)); // 40초에 쿨 → 45초 사용: 5초, 5회
        samples.put("많이 아낌", sample(6_000L, 0, 50_000, 70_000, 90_000));        // 20초에 쿨 → 50초 사용: 30초, 4회

        HoldTradeoff result = analyzer.analyze("판데모니움", samples).orElseThrow();

        assertThat(result.points()).extracting(HoldTradeoff.Point::label)
                .containsExactly("쿨마다", "조금 아낌", "많이 아낌");
        assertThat(result.points()).extracting(HoldTradeoff.Point::heldForBurstSeconds).containsExactly(0.0, 5.0, 30.0);
        assertThat(result.heldVsSeconds()).isCloseTo(-1.0, within(1e-9));
        assertThat(result.heldVsRate()).isNegative(); // 시전 수 5·5·4 동률이 있어 -1 은 아니다
    }

    @Test
    void 이_스킬을_쓴_표본이_없으면_비어_있다() {
        assertThat(analyzer.analyze("없는 스킬", Map.of("a", sample(1_000L, 0)))).isEmpty();
    }

    private static AnalysisContext sample(Long damage, long... castTimes) {
        SkillUsage skill = new SkillUsage("판데모니움", "판데모니움",
                Arrays.stream(castTimes).boxed().toList(), damage, 20_000L, null);
        return AnalysisContext.single(100_000, 1_000, SPEC, List.of(skill),
                List.of(new BurstWindow(45_000, 75_000)));
    }
}
