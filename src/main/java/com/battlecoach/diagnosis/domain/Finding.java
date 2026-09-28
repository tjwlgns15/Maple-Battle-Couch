package com.battlecoach.diagnosis.domain;

/**
 * 진단 결과 1건.
 *
 * @param impactSeconds 손해를 평균 DPS 기준 초로 환산한 값. 데미지가 없는 버프처럼 계산할 수 없으면 null
 */
public record Finding(
        FindingType type,
        String skillBaseName,
        String skillName,
        Double impactSeconds,
        String message
) {

    public static Finding measured(FindingType type, SkillUsage skill, double impactSeconds, String message) {
        return new Finding(type, skill.baseName(), skill.skillName(), impactSeconds, message);
    }

    public static Finding unmeasured(FindingType type, SkillUsage skill, String message) {
        return new Finding(type, skill.baseName(), skill.skillName(), null, message);
    }

    public boolean isMeasured() {
        return impactSeconds != null;
    }
}
