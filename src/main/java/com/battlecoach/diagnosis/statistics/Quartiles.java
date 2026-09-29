package com.battlecoach.diagnosis.statistics;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** 25·50·75 분위수. 표본 사이는 선형 보간한다. */
public record Quartiles(double p25, double p50, double p75) {

    public static Quartiles of(List<Double> values) {
        return weighted(values, values.stream().map(value -> 1.0).toList());
    }

    /**
     * 가중 분위수. 가중치가 모두 같으면 {@link #of} 와 같은 값(선형 보간, 양 끝이 최솟값·최댓값)이 되도록
     * i 번째 값의 위치를 "앞선 값들의 가중치 합 ÷ (전체 합 − 마지막 값의 가중치)"로 둔다.
     */
    public static Quartiles weighted(List<Double> values, List<Double> weights) {
        if (values.isEmpty()) {
            throw new IllegalArgumentException("분위수를 계산할 값이 없습니다.");
        }
        if (values.size() != weights.size() || weights.stream().anyMatch(weight -> weight <= 0)) {
            throw new IllegalArgumentException("가중치는 값마다 하나씩, 0보다 커야 합니다.");
        }
        List<double[]> sorted = new ArrayList<>();
        for (int i = 0; i < values.size(); i++) {
            sorted.add(new double[] {values.get(i), weights.get(i)});
        }
        sorted.sort(Comparator.comparingDouble(pair -> pair[0]));
        return new Quartiles(percentile(sorted, 0.25), percentile(sorted, 0.5), percentile(sorted, 0.75));
    }

    private static double percentile(List<double[]> sorted, double fraction) {
        if (sorted.size() == 1) {
            return sorted.get(0)[0];
        }
        double total = sorted.stream().mapToDouble(pair -> pair[1]).sum();
        double span = total - sorted.get(sorted.size() - 1)[1];
        double cumulative = 0;
        for (int i = 0; i < sorted.size() - 1; i++) {
            double position = cumulative / span;
            double next = (cumulative + sorted.get(i)[1]) / span;
            if (fraction <= next) {
                double weight = (fraction - position) / (next - position);
                return sorted.get(i)[0] * (1 - weight) + sorted.get(i + 1)[0] * weight;
            }
            cumulative += sorted.get(i)[1];
        }
        return sorted.get(sorted.size() - 1)[0];
    }
}
