package com.battlecoach.diagnosis.statistics;

import java.util.List;

/**
 * 같은 직업·기간 기록 중 어떤 기록과 비교할지.
 *
 * @param topPercent 기준 순위 상위 몇 %까지 비교 대상으로 삼을지. {@link #PERCENT_OPTIONS} 중 하나
 */
public record ComparisonCriteria(ComparisonBasis basis, int topPercent) {

    public static final List<Integer> PERCENT_OPTIONS = List.of(10, 25, 50, 100);
    static final int DEFAULT_PERCENT = 50;

    public ComparisonCriteria {
        if (basis == null) {
            throw new IllegalArgumentException("비교 기준이 없습니다.");
        }
        if (!PERCENT_OPTIONS.contains(topPercent)) {
            throw new IllegalArgumentException("상위 비율은 " + PERCENT_OPTIONS + " 중 하나여야 합니다: " + topPercent);
        }
    }

    public static ComparisonCriteria of(ComparisonBasis basis, int topPercent) {
        return new ComparisonCriteria(basis, topPercent);
    }

    /** DPS 상위 50%. 표본이 적은 기간 초반에도 5명을 넘기기 쉬운 값이다. */
    public static ComparisonCriteria defaults() {
        return new ComparisonCriteria(ComparisonBasis.DPS, DEFAULT_PERCENT);
    }

    /** 화면 쿼리 파라미터에서 만든다. 값이 없거나 틀리면 기본값을 쓴다. */
    public static ComparisonCriteria parse(String basis, Integer topPercent) {
        ComparisonBasis parsedBasis = parseBasis(basis);
        int percent = topPercent != null && PERCENT_OPTIONS.contains(topPercent) ? topPercent : DEFAULT_PERCENT;
        return new ComparisonCriteria(parsedBasis, percent);
    }

    private static ComparisonBasis parseBasis(String basis) {
        if (basis == null) {
            return ComparisonBasis.DPS;
        }
        try {
            return ComparisonBasis.valueOf(basis.strip().toUpperCase());
        } catch (IllegalArgumentException e) {
            return ComparisonBasis.DPS;
        }
    }

    public boolean isAll() {
        return topPercent == 100;
    }

    /** "DPS 상위 25%", "전투력 대비 DPS 상위 10%", "전체" */
    public String label() {
        return isAll() ? "전체" : basis.label() + " 상위 " + topPercent + "%";
    }
}
