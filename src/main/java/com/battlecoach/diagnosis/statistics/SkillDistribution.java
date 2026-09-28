package com.battlecoach.diagnosis.statistics;

/**
 * 랭커 표본에서 본 스킬 하나의 분포.
 *
 * @param userCount      이 스킬을 한 번이라도 쓴 표본 수
 * @param adoptionRate   userCount / 전체 표본 수
 * @param castsPerMinute 쓴 표본들의 분당 시전 수
 * @param seconds        쓴 표본들의 초 환산(스킬 데미지 ÷ 총 DPS). 데미지 항목이 없는 스킬은 null
 */
public record SkillDistribution(
        String baseName,
        String skillName,
        int userCount,
        double adoptionRate,
        Quartiles castsPerMinute,
        Quartiles seconds
) {
}
