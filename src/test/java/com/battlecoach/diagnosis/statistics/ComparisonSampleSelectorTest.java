package com.battlecoach.diagnosis.statistics;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.battlecoach.diagnosis.domain.AnalysisContext;
import com.battlecoach.spec.domain.CharacterSpec;
import com.battlecoach.spec.domain.CooldownStats;
import com.battlecoach.spec.domain.PowerStats;

class ComparisonSampleSelectorTest {

    private final ComparisonSampleSelector selector = new ComparisonSampleSelector();
    private final EfficiencyModelFitter fitter = new EfficiencyModelFitter();

    @Test
    void DPS_상위_퍼센트는_개수를_올림해서_고른다() {
        List<AnalysisContext> pool = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            pool.add(sample(i * 1_000L, i * 100L));
        }

        List<AnalysisContext> top25 = selector.select(pool, ComparisonCriteria.of(ComparisonBasis.DPS, 25), null);

        assertThat(top25).extracting(AnalysisContext::totalDps).containsExactly(10_000L, 9_000L, 8_000L);
    }

    @Test
    void 전체는_모든_기록을_고른다() {
        List<AnalysisContext> pool = List.of(sample(1_000, 100), sample(2_000, 200));

        assertThat(selector.select(pool, ComparisonCriteria.of(ComparisonBasis.DPS, 100), null)).hasSize(2);
    }

    @Test
    void 전투력_대비_DPS는_스펙이_낮아도_추세보다_잘_친_기록을_위에_둔다() {
        List<AnalysisContext> pool = new ArrayList<>();
        for (long power = 400; power <= 800; power += 100) {
            pool.add(sample(power * 1_000, power));
        }
        AnalysisContext lowSpecGood = sample(450 * 1_000 * 12 / 10, 450);
        pool.add(lowSpecGood);
        EfficiencyModel model = fitter.fit(pool).orElseThrow();

        List<AnalysisContext> byDps = selector.select(pool, ComparisonCriteria.of(ComparisonBasis.DPS, 10), model);
        List<AnalysisContext> byEfficiency = selector.select(pool, ComparisonCriteria.of(ComparisonBasis.EFFICIENCY, 10), model);

        assertThat(byDps).extracting(AnalysisContext::totalDps).containsExactly(800_000L);
        assertThat(byEfficiency).containsExactly(lowSpecGood);
    }

    @Test
    void 추세선이_없으면_전투력_대비_DPS로는_고르지_않는다() {
        List<AnalysisContext> pool = List.of(sample(1_000, 100), sample(2_000, 200));

        assertThat(selector.select(pool, ComparisonCriteria.of(ComparisonBasis.EFFICIENCY, 100), null)).isEmpty();
    }

    @Test
    void 상위_개수는_최소_1개다() {
        assertThat(ComparisonSampleSelector.topCount(3, 10)).isEqualTo(1);
        assertThat(ComparisonSampleSelector.topCount(0, 10)).isZero();
        assertThat(ComparisonSampleSelector.topCount(11, 50)).isEqualTo(6);
    }

    @Test
    void 화면_파라미터가_틀리면_기본값을_쓴다() {
        assertThat(ComparisonCriteria.parse("efficiency", 25))
                .isEqualTo(ComparisonCriteria.of(ComparisonBasis.EFFICIENCY, 25));
        assertThat(ComparisonCriteria.parse("unknown", 33)).isEqualTo(ComparisonCriteria.defaults());
        assertThat(ComparisonCriteria.parse(null, null)).isEqualTo(ComparisonCriteria.defaults());
        assertThat(ComparisonCriteria.of(ComparisonBasis.EFFICIENCY, 10).label()).isEqualTo("전투력 대비 DPS 상위 10%");
        assertThat(ComparisonCriteria.of(ComparisonBasis.DPS, 100).label()).isEqualTo("전체");
    }

    private static AnalysisContext sample(long totalDps, long combatPower) {
        CharacterSpec spec = CharacterSpec.of(CooldownStats.of(0, 0, 0, 0), PowerStats.of(combatPower, 0), List.of());
        return AnalysisContext.single(300_000, totalDps, spec, List.of(), List.of());
    }
}
