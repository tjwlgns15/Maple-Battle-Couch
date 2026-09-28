package com.battlecoach.ranker.domain;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 통계에 쓰는 랭커 기록. 리플레이 본문은 replay 테이블에 있고 여기서는 분류 정보만 둔다.
 * 직업은 랭킹 파라미터가 아니라 리플레이의 직업명(예: "칼리")으로 묶는다. 진단 대상 기록도 같은 값을 갖기 때문이다.
 */
@Entity
@Table(name = "ranker_sample", indexes = @Index(name = "idx_ranker_sample_class_period",
        columnList = "character_class, period_no"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RankerSample {

    @Id
    @Column(name = "replay_id", length = 64)
    private String replayId;

    @Column(name = "character_class", nullable = false, length = 30)
    private String characterClass;

    @Column(name = "period_no", nullable = false)
    private int periodNo;

    @Column(name = "character_name", nullable = false, length = 30)
    private String characterName;

    @Column(name = "collected_at", nullable = false)
    private LocalDateTime collectedAt;

    private RankerSample(String replayId, String characterClass, int periodNo, String characterName,
                         LocalDateTime collectedAt) {
        this.replayId = replayId;
        this.characterClass = characterClass;
        this.periodNo = periodNo;
        this.characterName = characterName;
        this.collectedAt = collectedAt;
    }

    public static RankerSample of(String replayId, String characterClass, int periodNo, String characterName,
                                  LocalDateTime collectedAt) {
        return new RankerSample(replayId, characterClass, periodNo, characterName, collectedAt);
    }
}
