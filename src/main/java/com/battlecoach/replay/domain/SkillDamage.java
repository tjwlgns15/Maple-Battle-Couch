package com.battlecoach.replay.domain;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * result 응답의 스킬별 데미지 통계.
 * useCount 는 시전 횟수가 아니라 발동 단위 횟수다.
 */
@Embeddable
public record SkillDamage(
        @Column(name = "damage", nullable = false) long damage,
        @Column(name = "damage_percent", nullable = false, precision = 6, scale = 2) BigDecimal damagePercent,
        @Column(name = "dps", nullable = false) long dps,
        @Column(name = "use_count", nullable = false) int useCount,
        @Column(name = "damage_per_use", nullable = false) long damagePerUse,
        @Column(name = "attack_count", nullable = false) int attackCount,
        @Column(name = "max_damage", nullable = false) long maxDamage,
        @Column(name = "min_damage", nullable = false) long minDamage
) {

    public static SkillDamage of(long damage, BigDecimal damagePercent, long dps, int useCount,
                                 long damagePerUse, int attackCount, long maxDamage, long minDamage) {
        return new SkillDamage(damage, damagePercent, dps, useCount, damagePerUse, attackCount, maxDamage, minDamage);
    }
}
