package com.battlecoach.diagnosis.statistics;

import java.util.List;

/** 25·50·75 분위수. 표본 사이는 선형 보간한다. */
public record Quartiles(double p25, double p50, double p75) {

    public static Quartiles of(List<Double> values) {
        if (values.isEmpty()) {
            throw new IllegalArgumentException("분위수를 계산할 값이 없습니다.");
        }
        List<Double> sorted = values.stream().sorted().toList();
        return new Quartiles(percentile(sorted, 0.25), percentile(sorted, 0.5), percentile(sorted, 0.75));
    }

    private static double percentile(List<Double> sorted, double fraction) {
        double position = fraction * (sorted.size() - 1);
        int lower = (int) Math.floor(position);
        int upper = (int) Math.ceil(position);
        double weight = position - lower;
        return sorted.get(lower) * (1 - weight) + sorted.get(upper) * weight;
    }
}
