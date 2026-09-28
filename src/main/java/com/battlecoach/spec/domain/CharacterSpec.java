package com.battlecoach.spec.domain;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.stream.Collectors;

/** 연무장 입장 시점의 쿨타임 스탯과 스킬 스펙. 리플레이마다 고정이라 캐시해도 된다. */
public record CharacterSpec(CooldownStats cooldownStats, Map<String, SkillSpec> skillsByBaseName) {

    /**
     * 헥사 스킬은 "데스 블로섬"(Lv1)과 "데스 블로섬 VI"(Lv30)가 따로 온다.
     * 쿨 표기가 있는 쪽 → 헥사 강화된 쪽 → 레벨이 높은 쪽 순으로 하나를 고른다.
     */
    private static final Comparator<SkillSpec> PREFERENCE = Comparator
            .comparing(SkillSpec::hasCooldown)
            .thenComparing(SkillSpec::isHexaEnhanced)
            .thenComparingInt(SkillSpec::level);

    public CharacterSpec {
        skillsByBaseName = Map.copyOf(skillsByBaseName);
    }

    public static CharacterSpec of(CooldownStats cooldownStats, List<SkillSpec> skills) {
        Map<String, SkillSpec> byBaseName = skills.stream()
                .collect(Collectors.toMap(
                        SkillSpec::baseName,
                        Function.identity(),
                        BinaryOperator.maxBy(PREFERENCE)));
        return new CharacterSpec(cooldownStats, byBaseName);
    }

    public Optional<SkillSpec> find(String baseName) {
        return Optional.ofNullable(skillsByBaseName.get(baseName));
    }
}
