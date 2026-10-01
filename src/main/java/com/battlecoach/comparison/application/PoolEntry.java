package com.battlecoach.comparison.application;

import java.time.LocalDate;

import com.battlecoach.diagnosis.domain.AnalysisContext;

/** 비교 풀의 기록 1건. 기록 목록 화면에 필요한 값과 분석 컨텍스트를 함께 둔다. */
public record PoolEntry(
        String replayId,
        String characterName,
        int characterLevel,
        LocalDate registerDate,
        AnalysisContext context
) {
}
