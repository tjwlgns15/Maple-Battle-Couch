package com.battlecoach.diagnosis.rule;

import com.battlecoach.diagnosis.domain.AnalysisContext;
import com.battlecoach.diagnosis.domain.SkillUsage;

/** 시전 수·쿨 기반 규칙이 판단할 수 있는 스킬인지 가린다. */
final class CooldownEligibility {

    /**
     * 실효 쿨이 이보다 짧은 스킬은 제외한다. 짧은 쿨 스킬끼리는 같은 시전 시간을 두고 경쟁하므로
     * 쿨마다 쓰지 못한 것이나 시전 수가 적은 것을 손해로 보기 어렵다. (칼리: 크레센텀 4.7초, 차크람 스윕 5.0초)
     */
    static final long MIN_COOLDOWN_MS = 10_000;

    /** "이른 사용" 비율이 미적용 확률보다 이만큼 높으면 쿨이 실행 중에 바뀌는 스킬로 본다. */
    static final double EARLY_RATIO_MARGIN = 0.2;

    /** 비율을 판단하기 위한 최소 간격 수. 3회 시전 스킬에서 미적용 1회로 제외되지 않게 한다. */
    static final int MIN_INTERVALS_FOR_RATIO = 4;

    private CooldownEligibility() {
    }

    static boolean hasTrackableCooldown(SkillUsage skill) {
        return skill.hasCooldown() && skill.effectiveCooldownMs() >= MIN_COOLDOWN_MS;
    }

    static boolean hasDynamicCooldown(SkillUsage skill, AnalysisContext context) {
        return skill.castCount() - 1 >= MIN_INTERVALS_FOR_RATIO
                && skill.earlyIntervalRatio() > context.cooldownStats().resetChancePercent() / 100 + EARLY_RATIO_MARGIN;
    }

    static boolean isComparable(SkillUsage skill, AnalysisContext context) {
        return skill.castCount() > 0 && hasTrackableCooldown(skill) && !hasDynamicCooldown(skill, context);
    }
}
