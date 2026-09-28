package com.battlecoach.spec.domain;

/** 기본 쿨과 쿨감 스탯으로 실효 쿨을 계산한다. */
public interface CooldownCalculator {

    /**
     * @param spec 쿨 표기가 있는 스킬 ({@link SkillSpec#hasCooldown()})
     * @return 실효 쿨 (ms)
     */
    long effectiveCooldownMs(SkillSpec spec, CooldownStats stats);
}
