package com.battlecoach.diagnosis.statistics;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.OptionalDouble;
import java.util.stream.IntStream;

/** 순위 상관. 표본이 작고 이상값이 있어 피어슨 대신 스피어만을 쓴다. */
public final class Correlation {

    private Correlation() {
    }

    /**
     * 스피어만 순위 상관계수. 같은 값은 평균 순위를 준다.
     *
     * @return 값이 3개 미만이거나 한쪽이 모두 같은 값이면 비어 있다
     */
    public static OptionalDouble spearman(List<Double> x, List<Double> y) {
        if (x.size() != y.size()) {
            throw new IllegalArgumentException("두 목록의 길이가 다릅니다: " + x.size() + ", " + y.size());
        }
        if (x.size() < 3) {
            return OptionalDouble.empty();
        }
        return pearson(ranks(x), ranks(y));
    }

    static double[] ranks(List<Double> values) {
        int n = values.size();
        Integer[] order = IntStream.range(0, n).boxed().sorted(Comparator.comparingDouble(values::get)).toArray(Integer[]::new);
        double[] ranks = new double[n];
        int i = 0;
        while (i < n) {
            int j = i;
            while (j + 1 < n && values.get(order[j + 1]).equals(values.get(order[i]))) {
                j++;
            }
            double averageRank = (i + j) / 2.0 + 1;
            for (int k = i; k <= j; k++) {
                ranks[order[k]] = averageRank;
            }
            i = j + 1;
        }
        return ranks;
    }

    private static OptionalDouble pearson(double[] x, double[] y) {
        double meanX = Arrays.stream(x).average().orElse(0);
        double meanY = Arrays.stream(y).average().orElse(0);
        double covariance = 0;
        double varianceX = 0;
        double varianceY = 0;
        for (int i = 0; i < x.length; i++) {
            covariance += (x[i] - meanX) * (y[i] - meanY);
            varianceX += (x[i] - meanX) * (x[i] - meanX);
            varianceY += (y[i] - meanY) * (y[i] - meanY);
        }
        if (varianceX == 0 || varianceY == 0) {
            return OptionalDouble.empty();
        }
        return OptionalDouble.of(covariance / Math.sqrt(varianceX * varianceY));
    }
}
