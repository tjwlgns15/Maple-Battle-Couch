package com.battlecoach.nexon.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * GET /maplestory/v1/battle-practice/character-info 중 basic_object 만 읽는다.
 * 스탯·스킬 등 나머지는 원문 JSON으로 저장해 두고 분석 모듈(SkillSpecParser 등)에서 해석한다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record CharacterInfoBasicResponse(
        @JsonProperty("basic_object") Basic basicObject
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Basic(
            @JsonProperty("character_name") String characterName,
            @JsonProperty("character_level") int characterLevel,
            @JsonProperty("character_class") String characterClass,
            @JsonProperty("character_class_level") String characterClassLevel
    ) {
    }
}
