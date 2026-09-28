package com.battlecoach.nexon.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * GET /maplestory/v1/ranking/overall (레벨·경험치 순위. DPS 순위가 아니다)
 * 한 페이지에 200명이 온다. ocid 는 오지 않아 캐릭터명으로 다시 조회해야 한다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record OverallRankingResponse(
        @JsonProperty("ranking") List<Entry> ranking
) {

    public List<Entry> entries() {
        return ranking == null ? List.of() : ranking;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Entry(
            @JsonProperty("ranking") int ranking,
            @JsonProperty("character_name") String characterName,
            @JsonProperty("character_level") int characterLevel,
            @JsonProperty("class_name") String className,
            @JsonProperty("sub_class_name") String subClassName,
            @JsonProperty("world_name") String worldName
    ) {
    }
}
