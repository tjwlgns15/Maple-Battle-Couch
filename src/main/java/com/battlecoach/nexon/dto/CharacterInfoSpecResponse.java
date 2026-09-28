package com.battlecoach.nexon.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/** GET /maplestory/v1/battle-practice/character-info 중 쿨타임 분석에 필요한 스탯과 스킬만 읽는다. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record CharacterInfoSpecResponse(
        @JsonProperty("stat_object") StatObject statObject,
        @JsonProperty("skill_object") SkillObject skillObject
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record StatObject(@JsonProperty("basic_stat_object") BasicStatObject basicStatObject) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record BasicStatObject(@JsonProperty("final_stat") List<FinalStat> finalStat) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record FinalStat(
            @JsonProperty("stat_name") String statName,
            @JsonProperty("stat_value") String statValue
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SkillObject(@JsonProperty("character_skill") List<CharacterSkill> characterSkill) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CharacterSkill(
            @JsonProperty("skill_name") String skillName,
            @JsonProperty("skill_level") int skillLevel,
            @JsonProperty("skill_effect") String skillEffect,
            @JsonProperty("skill_description") String skillDescription
    ) {
    }
}
