package com.battlecoach.nexon.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * GET /maplestory/v1/character/basic 중 직업만 읽는다.
 * character_class 표기는 연무장 character-info 의 직업명과 같다(예: "칼리", "아크메이지(썬,콜)").
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record CharacterBasicResponse(
        @JsonProperty("character_name") String characterName,
        @JsonProperty("character_class") String characterClass
) {
}
