package com.battlecoach.nexon.dto;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * GET /maplestory/v1/battle-practice/character-info 중 쿨타임·스펙 분석에 필요한 스탯, 스킬, 헥사 코어와
 * 쿨감 출처(장비 잠재, 유니온 공격대원, 어빌리티, 유니온 아티팩트)만 읽는다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record CharacterInfoSpecResponse(
        @JsonProperty("stat_object") StatObject statObject,
        @JsonProperty("skill_object") SkillObject skillObject,
        @JsonProperty("hexa_matrix_object") HexaMatrixObject hexaMatrixObject,
        @JsonProperty("item_object") ItemObject itemObject,
        @JsonProperty("union_raider_object") UnionRaiderObject unionRaiderObject,
        @JsonProperty("ability_object") AbilityObject abilityObject,
        @JsonProperty("union_artifact_object") UnionArtifactObject unionArtifactObject
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ItemObject(@JsonProperty("item_equipment_object") ItemEquipmentObject itemEquipmentObject) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ItemEquipmentObject(@JsonProperty("item_equipment") List<ItemEquipment> itemEquipment) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ItemEquipment(
            @JsonProperty("item_equipment_slot") String slot,
            @JsonProperty("potential_option_1") String potentialOption1,
            @JsonProperty("potential_option_2") String potentialOption2,
            @JsonProperty("potential_option_3") String potentialOption3,
            @JsonProperty("additional_potential_option_1") String additionalPotentialOption1,
            @JsonProperty("additional_potential_option_2") String additionalPotentialOption2,
            @JsonProperty("additional_potential_option_3") String additionalPotentialOption3
    ) {

        public List<String> potentialOptions() {
            return nonNull(potentialOption1, potentialOption2, potentialOption3);
        }

        public List<String> additionalPotentialOptions() {
            return nonNull(additionalPotentialOption1, additionalPotentialOption2, additionalPotentialOption3);
        }

        private static List<String> nonNull(String... options) {
            return Arrays.stream(options).filter(Objects::nonNull).toList();
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record UnionRaiderObject(@JsonProperty("union_raider_stat") List<String> unionRaiderStat) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record AbilityObject(@JsonProperty("ability_info") List<AbilityInfo> abilityInfo) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record AbilityInfo(@JsonProperty("ability_value") String value) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record UnionArtifactObject(@JsonProperty("union_artifact_effect") List<UnionArtifactEffect> effects) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record UnionArtifactEffect(@JsonProperty("name") String name) {
    }

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
