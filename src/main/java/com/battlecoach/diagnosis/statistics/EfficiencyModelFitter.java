package com.battlecoach.diagnosis.statistics;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.battlecoach.diagnosis.domain.AnalysisContext;
import com.battlecoach.spec.domain.PowerStats;

/**
 * 최소제곱으로 {@link EfficiencyModel} 을 적합한다.
 * 칼리 4기 11명: log DPS ~ log 전투력 R² 0.985(잔차 −6 ~ +6%), 헥사 레벨 합을 더하면 0.989.
 */
@Component
public class EfficiencyModelFitter {

    /** 이보다 적으면 추세선을 만들지 않는다. */
    static final int MIN_SAMPLES = JobStatistics.MIN_SAMPLES;
    /** 이 이상이어야 헥사 항을 넣는다. 전투력과 함께 오르는 값이라 표본이 적으면 계수가 흔들린다. */
    static final int MIN_SAMPLES_FOR_HEXA = 8;

    public Optional<EfficiencyModel> fit(List<AnalysisContext> samples) {
        List<AnalysisContext> usable = samples.stream()
                .filter(sample -> sample.totalDps() > 0 && sample.spec().powerStats().hasCombatPower())
                .toList();
        if (usable.size() < MIN_SAMPLES) {
            return Optional.empty();
        }
        boolean withHexa = usable.size() >= MIN_SAMPLES_FOR_HEXA
                && usable.stream().allMatch(sample -> sample.spec().powerStats().hasHexaLevels())
                && usable.stream().map(sample -> sample.spec().powerStats().hexaLevelSum()).distinct().count() > 1;

        double[][] x = usable.stream().map(sample -> features(sample.spec().powerStats(), withHexa)).toArray(double[][]::new);
        double[] y = usable.stream().mapToDouble(sample -> Math.log(sample.totalDps())).toArray();
        Optional<double[]> beta = leastSquares(x, y);
        if (beta.isEmpty()) {
            return Optional.empty();
        }
        double[] b = beta.get();
        Double hexaSlope = withHexa ? b[2] : null;
        List<PowerStats> powers = usable.stream().map(sample -> sample.spec().powerStats()).toList();
        PowerStats minPower = PowerStats.of(
                powers.stream().mapToLong(PowerStats::combatPower).min().orElseThrow(),
                powers.stream().mapToInt(PowerStats::hexaLevelSum).min().orElseThrow());
        PowerStats maxPower = PowerStats.of(
                powers.stream().mapToLong(PowerStats::combatPower).max().orElseThrow(),
                powers.stream().mapToInt(PowerStats::hexaLevelSum).max().orElseThrow());

        EfficiencyModel draft = new EfficiencyModel(b[0], b[1], hexaSlope, usable.size(), 0, 0, 0, minPower, maxPower);
        List<Double> efficiencies = usable.stream()
                .map(sample -> draft.rawEfficiencyOf(sample.totalDps(), sample.spec().powerStats()))
                .flatMap(Optional::stream)
                .toList();
        return Optional.of(new EfficiencyModel(b[0], b[1], hexaSlope, usable.size(),
                efficiencies.stream().mapToDouble(Double::doubleValue).min().orElse(0),
                efficiencies.stream().mapToDouble(Double::doubleValue).max().orElse(0),
                Quartiles.of(efficiencies).p25(),
                minPower, maxPower));
    }

    private static double[] features(PowerStats power, boolean withHexa) {
        double logPower = Math.log(power.combatPower());
        return withHexa ? new double[] {1, logPower, power.hexaLevelSum()} : new double[] {1, logPower};
    }

    /** 정규방정식을 부분 피벗 가우스 소거로 푼다. 설명 변수가 모두 같은 값이라 풀 수 없으면 비어 있다. */
    static Optional<double[]> leastSquares(double[][] x, double[] y) {
        int k = x[0].length;
        double[][] m = new double[k][k + 1];
        for (int row = 0; row < x.length; row++) {
            for (int a = 0; a < k; a++) {
                for (int b = 0; b < k; b++) {
                    m[a][b] += x[row][a] * x[row][b];
                }
                m[a][k] += x[row][a] * y[row];
            }
        }
        for (int col = 0; col < k; col++) {
            int pivot = col;
            for (int row = col + 1; row < k; row++) {
                if (Math.abs(m[row][col]) > Math.abs(m[pivot][col])) {
                    pivot = row;
                }
            }
            double[] swap = m[col];
            m[col] = m[pivot];
            m[pivot] = swap;
            if (Math.abs(m[col][col]) < 1e-9) {
                return Optional.empty();
            }
            for (int row = 0; row < k; row++) {
                if (row == col) {
                    continue;
                }
                double factor = m[row][col] / m[col][col];
                for (int c = col; c <= k; c++) {
                    m[row][c] -= factor * m[col][c];
                }
            }
        }
        double[] beta = new double[k];
        for (int a = 0; a < k; a++) {
            beta[a] = m[a][k] / m[a][a];
        }
        return Optional.of(beta);
    }
}
