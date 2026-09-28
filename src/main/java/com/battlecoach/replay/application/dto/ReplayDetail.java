package com.battlecoach.replay.application.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.battlecoach.replay.domain.BattleSummary;
import com.battlecoach.replay.domain.CharacterProfile;
import com.battlecoach.replay.domain.HexaType;
import com.battlecoach.replay.domain.Replay;
import com.battlecoach.replay.domain.ReplayCast;
import com.battlecoach.replay.domain.ReplaySkillStat;
import com.battlecoach.replay.domain.SequenceSegment;
import com.battlecoach.replay.domain.SkillDamage;

/** 리플레이 상세 (결과 요약 + 스킬별 데미지 + 스킬 사용 내역 + 시퀀스 구간) */
public record ReplayDetail(
        String replayId,
        String characterName,
        String characterClass,
        int characterLevel,
        LocalDate registerDate,
        long totalPlayTimeMs,
        long totalDamage,
        long totalDps,
        String endType,
        List<SkillStatView> skillStats,
        List<CastView> casts,
        List<SequenceSegment> sequenceSegments
) {

    public static ReplayDetail from(Replay replay) {
        CharacterProfile profile = replay.getCharacterProfile();
        BattleSummary summary = replay.getBattleSummary();
        return new ReplayDetail(
                replay.getReplayId(),
                profile.characterName(),
                profile.characterClass(),
                profile.characterLevel(),
                summary.registerDate(),
                summary.totalPlayTimeMs(),
                summary.totalDamage(),
                summary.totalDps(),
                summary.endType(),
                replay.getSkillStats().stream().map(SkillStatView::from).toList(),
                replay.getCasts().stream().map(CastView::from).toList(),
                replay.getSequenceSegments());
    }

    public record SkillStatView(
            String skillName,
            String baseName,
            long damage,
            BigDecimal damagePercent,
            long dps,
            int useCount
    ) {

        static SkillStatView from(ReplaySkillStat stat) {
            SkillDamage damage = stat.getDamage();
            return new SkillStatView(
                    stat.getSkillName(),
                    stat.getBaseName(),
                    damage.damage(),
                    damage.damagePercent(),
                    damage.dps(),
                    damage.useCount());
        }
    }

    public record CastView(
            int recordedOrder,
            long elapseMs,
            String skillName,
            String baseName,
            HexaType hexaType,
            String sequenceName,
            String sequenceKey
    ) {

        static CastView from(ReplayCast cast) {
            return new CastView(
                    cast.getRecordedOrder(),
                    cast.getElapseMs(),
                    cast.getSkillName(),
                    cast.getBaseName(),
                    cast.getHexaType(),
                    cast.getSequenceName(),
                    cast.getSequenceKey());
        }
    }
}
