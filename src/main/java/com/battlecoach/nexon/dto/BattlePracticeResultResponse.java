package com.battlecoach.nexon.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * GET /maplestory/v1/battle-practice/result
 * use_count 는 시전 횟수가 아니라 발동 단위(틱/소환 공격 등) 횟수다. 시전 횟수는 skill-timeline 에서 센다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record BattlePracticeResultResponse(
        @JsonProperty("register_date") String registerDate,
        @JsonProperty("total_play_time") long totalPlayTime,
        @JsonProperty("total_damage") long totalDamage,
        @JsonProperty("total_dps") long totalDps,
        @JsonProperty("end_type") String endType,
        @JsonProperty("like_count") int likeCount,
        @JsonProperty("skill_statistic") List<SkillStatistic> skillStatistic
) {

    public List<SkillStatistic> statistics() {
        return skillStatistic == null ? List.of() : skillStatistic;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SkillStatistic(
            @JsonProperty("skill_name") String skillName,
            @JsonProperty("damage") long damage,
            @JsonProperty("damage_percent") String damagePercent,
            @JsonProperty("dps") long dps,
            @JsonProperty("use_count") int useCount,
            @JsonProperty("damage_per_use") long damagePerUse,
            @JsonProperty("attack_count") int attackCount,
            @JsonProperty("max_damage") long maxDamage,
            @JsonProperty("min_damage") long minDamage
    ) {
    }
}
