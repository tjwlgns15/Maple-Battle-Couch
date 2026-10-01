package com.battlecoach.diagnosis.statistics;

/** 비교 대상을 고르는 순위 기준 */
public enum ComparisonBasis {

    /** 실제 DPS 순. 인게임 연무장 순위와 같은 기준이라 스펙이 높은 기록이 위로 온다. */
    DPS("DPS"),

    /** 전투력(과 헥사 레벨)으로 예상한 DPS 보다 얼마나 높은지 순. 스펙에 비해 잘 친 기록이 위로 온다. */
    EFFICIENCY("전투력 대비 DPS");

    private final String label;

    ComparisonBasis(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
