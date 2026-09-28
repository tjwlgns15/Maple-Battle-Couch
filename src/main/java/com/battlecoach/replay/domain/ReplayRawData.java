package com.battlecoach.replay.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Basic;
import jakarta.persistence.Table;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Nexon API 응답 원문.
 * 스킬 파서나 진단 규칙이 바뀌었을 때 API 재호출 없이 재분석하기 위해 보관한다.
 * 조회 화면에서는 쓰지 않으므로 Replay 집계와 분리해 기본 조회 비용을 줄인다.
 */
@Entity
@Table(name = "replay_raw_data")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReplayRawData {

    @Id
    @Column(name = "replay_id", length = 64)
    private String replayId;

    @Basic(fetch = FetchType.LAZY)
    @Column(name = "result_json", nullable = false, columnDefinition = "json")
    private String resultJson;

    /** 페이지별 응답을 JSON 배열로 묶어 저장한다. 예: [page1, page2] */
    @Basic(fetch = FetchType.LAZY)
    @Column(name = "timeline_json", nullable = false, columnDefinition = "json")
    private String timelineJson;

    @Basic(fetch = FetchType.LAZY)
    @Column(name = "character_info_json", nullable = false, columnDefinition = "json")
    private String characterInfoJson;

    private ReplayRawData(String replayId, String resultJson, String timelineJson, String characterInfoJson) {
        this.replayId = replayId;
        this.resultJson = resultJson;
        this.timelineJson = timelineJson;
        this.characterInfoJson = characterInfoJson;
    }

    public static ReplayRawData of(String replayId, String resultJson, String timelineJson, String characterInfoJson) {
        return new ReplayRawData(replayId, resultJson, timelineJson, characterInfoJson);
    }
}
