package com.battlecoach.diagnosis.statistics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.battlecoach.diagnosis.domain.AnalysisContext;
import com.battlecoach.spec.domain.CharacterSpec;
import com.battlecoach.spec.domain.CooldownStats;
import com.battlecoach.spec.domain.PowerStats;

class EfficiencyModelFitterTest {

    private final EfficiencyModelFitter fitter = new EfficiencyModelFitter();

    @Test
    void 전투력에_비례하는_표본은_효율이_0이고_추세보다_낮은_표본은_음수다() {
        List<AnalysisContext> samples = new ArrayList<>();
        for (long power = 400; power <= 800; power += 100) {
            samples.add(sample(power * 10_000 * 1_000_000L / 100, power * 1_000_000, 0));
        }
        AnalysisContext weak = sample(500 * 10_000 * 1_000_000L / 100 * 9 / 10, 500_000_000, 0);
        samples.add(weak);

        EfficiencyModel model = fitter.fit(samples).orElseThrow();

        assertThat(model.usesHexa()).isFalse();
        assertThat(model.efficiencyOf(weak).orElseThrow()).isNegative();
        assertThat(model.efficiencyOf(samples.get(4)).orElseThrow()).isPositive();
        assertThat(model.minEfficiency()).isLessThan(0).isEqualTo(model.efficiencyOf(weak).orElseThrow(), within(1e-9));
        assertThat(model.weightOf(weak)).isEqualTo(EfficiencyModel.LOW_EFFICIENCY_WEIGHT);
        AnalysisContext best = samples.stream()
                .max(java.util.Comparator.comparingDouble(s -> model.efficiencyOf(s).orElseThrow()))
                .orElseThrow();
        assertThat(model.weightOf(best)).isEqualTo(1.0);
    }

    @Test
    void 표본이_8개_이상이고_헥사_레벨이_다르면_헥사_항을_넣는다() {
        List<AnalysisContext> samples = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            long power = 400_000_000L + i * 50_000_000L;
            int hexa = 300 + (i % 3) * 40;
            // DPS = 전투력 × 1.001^헥사 → 헥사 1레벨당 약 0.1%
            long dps = Math.round(power * 10_000 * Math.pow(1.001, hexa));
            samples.add(sample(dps, power, hexa));
        }

        EfficiencyModel model = fitter.fit(samples).orElseThrow();

        assertThat(model.usesHexa()).isTrue();
        assertThat(model.hexaSlope()).isCloseTo(Math.log(1.001), within(1e-6));
        assertThat(model.combatPowerSlope()).isCloseTo(1.0, within(1e-6));
        assertThat(model.efficiencyOf(samples.get(0)).orElseThrow()).isCloseTo(0, within(1e-6));
    }

    @Test
    void 랭커_스펙_범위_밖은_외삽하지_않는다() {
        List<AnalysisContext> samples = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            long power = 400_000_000L + i * 50_000_000L;
            samples.add(sample(power * 10_000, power, 300 + (i % 3) * 40));
        }
        EfficiencyModel model = fitter.fit(samples).orElseThrow();

        // 헥사 합이 랭커 최소(300)보다 낮다
        assertThat(model.efficiencyOf(sample(450_000_000L * 10_000, 450_000_000L, 137))).isEmpty();
        // 전투력이 랭커 최소(4억)보다 낮다
        assertThat(model.efficiencyOf(sample(300_000_000L * 10_000, 300_000_000L, 340))).isEmpty();
        assertThat(model.efficiencyOf(sample(450_000_000L * 10_000, 450_000_000L, 340))).isPresent();
    }

    @Test
    void 전투력을_모르는_표본은_빼고_5개_미만이면_만들지_않는다() {
        List<AnalysisContext> samples = List.of(
                sample(1_000, 100, 0), sample(2_000, 200, 0), sample(3_000, 300, 0), sample(4_000, 400, 0),
                sample(5_000, 0, 0));

        assertThat(fitter.fit(samples)).isEmpty();
    }

    @Test
    void 가중_분위수는_가중치가_같으면_보통_분위수와_같고_낮은_가중치는_중앙값을_밀어낸다() {
        List<Double> values = List.of(1.0, 2.0, 3.0, 4.0, 5.0);

        assertThat(Quartiles.weighted(values, List.of(1.0, 1.0, 1.0, 1.0, 1.0))).isEqualTo(Quartiles.of(values));
        assertThat(Quartiles.weighted(values, List.of(0.5, 1.0, 1.0, 1.0, 1.0)).p50()).isGreaterThan(3.0);
    }

    private static AnalysisContext sample(long totalDps, long combatPower, int hexaLevelSum) {
        CharacterSpec spec = CharacterSpec.of(CooldownStats.of(0, 0, 0, 0), PowerStats.of(combatPower, hexaLevelSum), List.of());
        return AnalysisContext.single(300_000, totalDps, spec, List.of(), List.of());
    }
}
