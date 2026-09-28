package com.battlecoach.replay.domain;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/** 연무장 측정 결과 요약 (result 응답의 최상위 필드) */
@Embeddable
public record BattleSummary(
        @Column(name = "register_date", nullable = false) LocalDate registerDate,
        @Column(name = "total_play_time_ms", nullable = false) long totalPlayTimeMs,
        @Column(name = "total_damage", nullable = false) long totalDamage,
        @Column(name = "total_dps", nullable = false) long totalDps,
        @Column(name = "end_type", nullable = false, length = 5) String endType,
        @Column(name = "like_count", nullable = false) int likeCount
) {

    public static BattleSummary of(LocalDate registerDate, long totalPlayTimeMs, long totalDamage,
                                   long totalDps, String endType, int likeCount) {
        return new BattleSummary(registerDate, totalPlayTimeMs, totalDamage, totalDps, endType, likeCount);
    }
}
