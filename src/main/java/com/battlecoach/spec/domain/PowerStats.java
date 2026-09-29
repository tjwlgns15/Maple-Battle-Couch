package com.battlecoach.spec.domain;

/**
 * 스펙 크기를 가늠하는 값. 운용 효율(스펙 대비 DPS)을 계산할 때만 쓴다.
 * 전투력에는 헥사·5차 스킬 레벨이 반영되지 않고 직업마다 산식이 달라, 같은 직업 안에서 헥사 코어 레벨 합과 함께 쓴다.
 *
 * @param combatPower  final_stat 의 "전투력". 없으면 0
 * @param hexaLevelSum 장착한 헥사 코어 레벨 합(스킬·마스터리·강화·공용). 없으면 0
 */
public record PowerStats(long combatPower, int hexaLevelSum) {

    public static PowerStats of(long combatPower, int hexaLevelSum) {
        return new PowerStats(combatPower, hexaLevelSum);
    }

    public static PowerStats unknown() {
        return new PowerStats(0, 0);
    }

    public boolean hasCombatPower() {
        return combatPower > 0;
    }

    public boolean hasHexaLevels() {
        return hexaLevelSum > 0;
    }
}
