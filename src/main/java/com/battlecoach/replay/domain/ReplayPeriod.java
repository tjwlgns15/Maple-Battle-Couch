package com.battlecoach.replay.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 리플레이가 등록된 연무장 기간. result·character-info 응답에는 기간이 없고 replay-id 목록에만 있어,
 * 목록을 받을 때마다 저장해 둔다. 통계를 기간별로 나눌 때 쓴다(패치·밸런스가 기간마다 다르다).
 * Replay 본문 저장과는 독립적이라 본문을 받기 전에도 기록된다.
 */
@Entity
@Table(name = "replay_period", indexes = @Index(name = "idx_replay_period_no", columnList = "period_no"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReplayPeriod {

    @Id
    @Column(name = "replay_id", length = 64)
    private String replayId;

    @Column(name = "period_no", nullable = false)
    private int periodNo;

    private ReplayPeriod(String replayId, int periodNo) {
        this.replayId = replayId;
        this.periodNo = periodNo;
    }

    public static ReplayPeriod of(String replayId, int periodNo) {
        return new ReplayPeriod(replayId, periodNo);
    }
}
