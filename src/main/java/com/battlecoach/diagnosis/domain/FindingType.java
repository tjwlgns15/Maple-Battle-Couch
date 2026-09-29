package com.battlecoach.diagnosis.domain;

public enum FindingType {

    MISSED_CAST("놓친 시전", FindingCategory.OPERATION),
    EXCLUDED_DYNAMIC_COOLDOWN("진단 제외: 쿨 변동", FindingCategory.OPERATION),
    CAST_COUNT_GAP("시전 수 부족", FindingCategory.OPERATION),
    UNUSED_SKILL("미사용", FindingCategory.OPERATION),
    MISSING_SKILL("미보유·미해금", FindingCategory.LOADOUT),
    CAST_RATE_BELOW_RANKERS("랭커보다 적은 시전", FindingCategory.OPERATION),
    LINKED_PAIR_SEPARATED("연동 스킬 따로 사용", FindingCategory.OPERATION),
    BURST_ORDER_REVERSED("극딜 순서 다름", FindingCategory.OPERATION);

    private final String label;
    private final FindingCategory category;

    FindingType(String label, FindingCategory category) {
        this.label = label;
        this.category = category;
    }

    public String label() {
        return label;
    }

    public FindingCategory category() {
        return category;
    }
}
