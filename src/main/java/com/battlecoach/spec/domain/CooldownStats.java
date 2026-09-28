package com.battlecoach.spec.domain;

/**
 * character-info 의 final_stat 에 합산돼 있는 쿨타임 관련 스탯.
 *
 * @param reductionSeconds    재사용 대기시간 감소 (초)
 * @param reductionPercent    재사용 대기시간 감소 (%)
 * @param resetChancePercent  재사용 대기시간 미적용 (%). 발동하면 그 시전은 쿨이 돌지 않는다.
 * @param buffDurationPercent 버프 지속시간 증가 (%)
 */
public record CooldownStats(
        double reductionSeconds,
        double reductionPercent,
        double resetChancePercent,
        double buffDurationPercent
) {

    public static CooldownStats of(double reductionSeconds, double reductionPercent,
                                   double resetChancePercent, double buffDurationPercent) {
        return new CooldownStats(reductionSeconds, reductionPercent, resetChancePercent, buffDurationPercent);
    }
}
