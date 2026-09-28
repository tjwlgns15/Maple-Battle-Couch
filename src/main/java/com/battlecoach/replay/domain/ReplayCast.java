package com.battlecoach.replay.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** skill-timeline 의 시전 기록 1건 */
@Entity
@Table(name = "replay_cast", indexes = @Index(name = "idx_cast_replay_order", columnList = "replay_id, recorded_order"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReplayCast {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "replay_id", nullable = false)
    private Replay replay;

    @Column(name = "recorded_order", nullable = false)
    private int recordedOrder;

    @Column(name = "elapse_ms", nullable = false)
    private long elapseMs;

    @Column(name = "skill_name", nullable = false, length = 100)
    private String skillName;

    @Column(name = "base_name", nullable = false, length = 100)
    private String baseName;

    @Enumerated(EnumType.STRING)
    @Column(name = "hexa_type", nullable = false, length = 10)
    private HexaType hexaType;

    /** 유저가 직접 붙인 시퀀스 이름 (예: "극", "극딜"). 직업/유저마다 다르므로 판단 기준으로 쓰지 않는다. */
    @Column(name = "sequence_name", length = 50)
    private String sequenceName;

    @Column(name = "sequence_key", length = 10)
    private String sequenceKey;

    private ReplayCast(Replay replay, int recordedOrder, long elapseMs, SkillName skillName, HexaType hexaType,
                       String sequenceName, String sequenceKey) {
        this.replay = replay;
        this.recordedOrder = recordedOrder;
        this.elapseMs = elapseMs;
        this.skillName = skillName.value();
        this.baseName = skillName.baseName();
        this.hexaType = hexaType;
        this.sequenceName = sequenceName;
        this.sequenceKey = sequenceKey;
    }

    static ReplayCast create(Replay replay, int recordedOrder, long elapseMs, SkillName skillName,
                             HexaType hexaType, String sequenceName, String sequenceKey) {
        return new ReplayCast(replay, recordedOrder, elapseMs, skillName, hexaType, sequenceName, sequenceKey);
    }

    public boolean isInSequence() {
        return sequenceKey != null;
    }
}
