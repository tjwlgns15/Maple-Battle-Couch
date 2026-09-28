package com.battlecoach.spec.domain;

import java.util.OptionalLong;

import com.battlecoach.replay.domain.SkillName;

/**
 * character-info 의 스킬 1개에서 읽은 스펙.
 *
 * @param baseCooldownMs     효과 텍스트의 기본 쿨. 표기가 없으면 null
 * @param durationMs         효과 텍스트의 첫 "N초 동안". 버프 지속시간 증가는 반영하지 않은 값. 없으면 null
 * @param cooldownReducible  재사용 대기시간 감소(초·%)를 받는지
 * @param cooldownResettable 재사용 대기시간 초기화(미적용 포함)를 받는지. 미검증: 미적용이 초기화와 같은 취급인지
 * @param reactivatable      지속 중 다시 눌러 효과를 바꾸는 스킬인지. 다시 누른 것도 시전으로 기록된다.
 */
public record SkillSpec(
        String skillName,
        String baseName,
        int level,
        Long baseCooldownMs,
        Long durationMs,
        boolean cooldownReducible,
        boolean cooldownResettable,
        boolean reactivatable
) {

    public static SkillSpec of(String skillName, int level, SkillText text) {
        OptionalLong cooldown = text.cooldownMs();
        OptionalLong duration = text.durationMs();
        return new SkillSpec(
                skillName,
                SkillName.of(skillName).baseName(),
                level,
                cooldown.isPresent() ? cooldown.getAsLong() : null,
                duration.isPresent() ? duration.getAsLong() : null,
                !text.refusesCooldownReduction(),
                !text.refusesCooldownReset(),
                text.isReactivatable());
    }

    public boolean hasCooldown() {
        return baseCooldownMs != null;
    }

    /** "데스 블로섬 VI" 처럼 헥사 강화된 항목인지 */
    public boolean isHexaEnhanced() {
        return !skillName.equals(baseName);
    }
}
