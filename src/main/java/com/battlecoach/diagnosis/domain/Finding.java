package com.battlecoach.diagnosis.domain;

/**
 * 진단 결과 1건.
 *
 * @param impactSeconds 손해를 평균 DPS 기준 초로 환산한 값. 데미지가 없는 버프처럼 계산할 수 없으면 null
 * @param message       근거: 무엇을 보고 이렇게 판단했는지
 * @param advice        처방: 무엇을 하면 되는지. 운용으로 줄일 수 없거나 정할 수 없으면 null
 */
public record Finding(
        FindingType type,
        String skillBaseName,
        String skillName,
        Double impactSeconds,
        String message,
        String advice
) {

    public Finding(FindingType type, String skillBaseName, String skillName, Double impactSeconds, String message) {
        this(type, skillBaseName, skillName, impactSeconds, message, null);
    }

    public static Finding measured(FindingType type, SkillUsage skill, double impactSeconds, String message) {
        return new Finding(type, skill.baseName(), skill.skillName(), impactSeconds, message);
    }

    public static Finding unmeasured(FindingType type, SkillUsage skill, String message) {
        return new Finding(type, skill.baseName(), skill.skillName(), null, message);
    }

    public Finding withAdvice(String advice) {
        return new Finding(type, skillBaseName, skillName, impactSeconds, message, advice);
    }

    public boolean isMeasured() {
        return impactSeconds != null;
    }

    public boolean hasAdvice() {
        return advice != null && !advice.isBlank();
    }
}
