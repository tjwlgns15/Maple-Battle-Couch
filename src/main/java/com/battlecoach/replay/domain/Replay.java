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
 * 등록된 리플레이 본문(데미지, 시전 기록)은 바뀌지 않는다. Nexon Open API 고지("크롤링한 데이터는 30일 이내에 갱신")에 따라
 * 주기적으로 API 에 다시 확인하고, 그때 캐릭터 기본 정보만 새 값으로 바꾼다({@link #refresh}).
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

    /** 마지막으로 API 에 다시 확인한 시각. 처음 받은 뒤 한 번도 확인하지 않았으면 null */
    @Column(name = "refreshed_at")
    private LocalDateTime refreshedAt;

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

    /** API 에서 다시 받은 캐릭터 기본 정보로 바꾸고 확인 시각을 남긴다. */
    public void refresh(CharacterProfile latestProfile, LocalDateTime refreshedAt) {
        this.characterProfile = latestProfile;
        this.refreshedAt = refreshedAt;
    }

    /** API 와 마지막으로 맞춘 시각 */
    public LocalDateTime lastSyncedAt() {
        return refreshedAt != null ? refreshedAt : fetchedAt;
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
