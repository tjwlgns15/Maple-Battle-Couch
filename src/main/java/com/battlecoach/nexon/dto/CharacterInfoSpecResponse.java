package com.battlecoach.nexon.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/** GET /maplestory/v1/battle-practice/character-info 중 쿨타임·스펙 분석에 필요한 스탯, 스킬, 헥사 코어만 읽는다. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record CharacterInfoSpecResponse(
        @JsonProperty("stat_object") StatObject statObject,
        @JsonProperty("skill_object") SkillObject skillObject,
        @JsonProperty("hexa_matrix_object") HexaMatrixObject hexaMatrixObject
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record HexaMatrixObject(@JsonProperty("hexa_core_object") HexaCoreObject hexaCoreObject) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record HexaCoreObject(@JsonProperty("character_hexa_core_equipment") List<HexaCore> equipment) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record HexaCore(
            @JsonProperty("hexa_core_name") String name,
            @JsonProperty("hexa_core_type") String type,
            @JsonProperty("hexa_core_level") int level
    ) {
    }

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
