package com.battlecoach.nexon.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * GET /maplestory/v1/battle-practice/skill-timeline
 * 시퀀스 내부에서는 elapse_time 이 기록 순서와 역전될 수 있다(수 ms 차이). 응답 순서를 그대로 보존해 둔다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record SkillTimelineResponse(
        @JsonProperty("page_no") int pageNo,
        @JsonProperty("total_page_no") int totalPageNo,
        @JsonProperty("skill_timeline") List<Event> skillTimeline
) {

    public List<Event> events() {
        return skillTimeline == null ? List.of() : skillTimeline;
    }

    public boolean hasNextPage() {
        return pageNo < totalPageNo;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Event(
            @JsonProperty("elapse_time") long elapseTime,
            @JsonProperty("skill_name") String skillName,
            @JsonProperty("hexa_skill_specificity_flag") String hexaSkillSpecificityFlag,
            @JsonProperty("sequence_name") String sequenceName,
            @JsonProperty("sequence_key") String sequenceKey
    ) {
    }
}
