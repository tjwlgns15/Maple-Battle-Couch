package com.battlecoach.spec.domain;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.stream.Collectors;

/** 연무장 입장 시점의 쿨타임 스탯(과 그 출처), 스펙 크기, 스킬 스펙. 리플레이마다 고정이라 캐시해도 된다. */
public record CharacterSpec(CooldownStats cooldownStats, CooldownSources cooldownSources, PowerStats powerStats,
                            Map<String, SkillSpec> skillsByBaseName) {

    /** 강화 코어 항목의 접미사 ("헥스 : 판데모니움 강화") */
    static final String ENHANCEMENT_SUFFIX = " 강화";

    /**
     * 헥사 스킬은 "데스 블로섬"(Lv1)과 "데스 블로섬 VI"(Lv30)가 따로 온다.
     * 쿨 표기가 있는 쪽 → 헥사 강화된 쪽 → 레벨이 높은 쪽 순으로 하나를 고른다.
     */
    private static final Comparator<SkillSpec> PREFERENCE = Comparator
            .comparing(SkillSpec::hasCooldown)
            .thenComparing(SkillSpec::isHexaEnhanced)
            .thenComparingInt(SkillSpec::level);

    public CharacterSpec {
        cooldownSources = cooldownSources == null ? CooldownSources.none() : cooldownSources;
        powerStats = powerStats == null ? PowerStats.unknown() : powerStats;
        skillsByBaseName = Map.copyOf(skillsByBaseName);
    }

    public static CharacterSpec of(CooldownStats cooldownStats, List<SkillSpec> skills) {
        return of(cooldownStats, PowerStats.unknown(), skills);
    }

    public static CharacterSpec of(CooldownStats cooldownStats, PowerStats powerStats, List<SkillSpec> skills) {
        return of(cooldownStats, CooldownSources.none(), powerStats, skills);
    }

    public static CharacterSpec of(CooldownStats cooldownStats, CooldownSources cooldownSources, PowerStats powerStats,
                                   List<SkillSpec> skills) {
        Map<String, SkillSpec> byBaseName = skills.stream()
                .collect(Collectors.toMap(
                        SkillSpec::baseName,
                        Function.identity(),
                        BinaryOperator.maxBy(PREFERENCE)));
        return new CharacterSpec(cooldownStats, cooldownSources, powerStats, byBaseName);
    }

    public Optional<SkillSpec> find(String baseName) {
        return Optional.ofNullable(skillsByBaseName.get(baseName));
    }

    /** 스킬 레벨과 강화 코어 레벨. 스킬 목록에 없으면 비어 있다. */
    public Optional<SkillLevel> levelOf(String baseName) {
        return find(baseName).map(skill -> new SkillLevel(skill.level(),
                find(baseName + ENHANCEMENT_SUFFIX).map(SkillSpec::level).orElse(null)));
    }
}
