package com.battlecoach.ranker.application;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** 수집 실행 상태 스냅샷 */
public record CollectStatus(
        boolean running,
        String jobClass,
        Integer periodNo,
        LocalDate rankingDate,
        int callsUsed,
        int maxCalls,
        int rankersChecked,
        int rankersSkipped,
        int sampled,
        int noRecord,
        int otherPeriodOnly,
        int notFound,
        String stopReason,
        LocalDateTime startedAt,
        LocalDateTime finishedAt
) {

    public static CollectStatus idle() {
        return new CollectStatus(false, null, null, null, 0, 0, 0, 0, 0, 0, 0, 0, null, null, null);
    }
}
