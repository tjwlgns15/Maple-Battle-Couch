package com.battlecoach.nexon.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * GET /maplestory/v1/battle-practice/replay-id
 * 기간(period_no)마다 기록이 최대 1개씩 오며, 등록하지 않은 기간은 목록에 없다.
 * register_date 는 날짜 단위(예: 2026-08-06T00:00+09:00)라 문자열로 받아 도메인에서 KST 기준으로 해석한다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ReplayIdListResponse(
        @JsonProperty("replay_list") List<Entry> replayList
) {

    public List<Entry> entries() {
        return replayList == null ? List.of() : replayList;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Entry(
            @JsonProperty("period_no") int periodNo,
            @JsonProperty("register_date") String registerDate,
            @JsonProperty("replay_id") String replayId
    ) {
    }
}
