package com.battlecoach.replay.domain;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 연무장 리플레이 1건 (집계 루트).
 * 등록된 리플레이는 바뀌지 않으므로 한 번 저장하면 갱신하지 않는다.
 */
@Entity
@Table(name = "replay")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Replay {

    @Id
    @Column(name = "replay_id", length = 64)
    private String replayId;

    @Embedded
    private CharacterProfile characterProfile;

    @Embedded
    private BattleSummary battleSummary;

    @Column(name = "fetched_at", nullable = false)
    private LocalDateTime fetchedAt;

    @OneToMany(mappedBy = "replay", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<ReplaySkillStat> skillStats = new ArrayList<>();

    @OneToMany(mappedBy = "replay", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("recordedOrder ASC")
    private List<ReplayCast> casts = new ArrayList<>();

    private Replay(String replayId, CharacterProfile characterProfile, BattleSummary battleSummary,
                   LocalDateTime fetchedAt) {
        this.replayId = replayId;
        this.characterProfile = characterProfile;
        this.battleSummary = battleSummary;
        this.fetchedAt = fetchedAt;
    }

    public static Replay create(String replayId, CharacterProfile characterProfile,
                                BattleSummary battleSummary, LocalDateTime fetchedAt) {
        return new Replay(replayId, characterProfile, battleSummary, fetchedAt);
    }

    public void addSkillStat(SkillName skillName, SkillDamage damage) {
        skillStats.add(ReplaySkillStat.create(this, skillName, damage));
    }

    /**
     * @param recordedOrder API 응답에 기록된 순서 (0부터). 시퀀스 내부에서는 elapseMs 와 순서가 역전될 수 있어 따로 보존한다.
     */
    public void addCast(int recordedOrder, long elapseMs, SkillName skillName, HexaType hexaType,
                        String sequenceName, String sequenceKey) {
        casts.add(ReplayCast.create(this, recordedOrder, elapseMs, skillName, hexaType, sequenceName, sequenceKey));
    }

    public List<ReplaySkillStat> getSkillStats() {
        return Collections.unmodifiableList(skillStats);
    }

    public List<ReplayCast> getCasts() {
        return Collections.unmodifiableList(casts);
    }

    public List<SequenceSegment> getSequenceSegments() {
        return SequenceSegment.extract(casts);
    }
}
