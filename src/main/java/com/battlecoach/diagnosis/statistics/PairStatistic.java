package com.battlecoach.diagnosis.statistics;

/**
 * 연동 스킬 후보: 스킬 A 를 쓸 때 B 도 1초 안에 함께 쓰는지.
 *
 * @param bothUsedCount 두 스킬을 모두 쓴 표본 수
 * @param pairedRate    그중 A 시전의 80% 이상을 B 와 함께 쓴 표본 비율
 */
public record PairStatistic(
        String baseName,
        String partnerBaseName,
        String partnerSkillName,
        int bothUsedCount,
        double pairedRate
) {
}
