package com.battlecoach.comparison.application;

import java.time.LocalDate;
import java.util.List;

import com.battlecoach.replay.repository.ClassCount;
import com.battlecoach.replay.repository.PeriodCount;

/**
 * 기간·직업별로 저장된 기록 목록.
 *
 * @param periods          기록이 있는 기간 (최근 순)
 * @param periodNo         고른 기간. 저장된 기록이 없으면 null
 * @param classes          고른 기간에 기록이 있는 직업 (기록 많은 순)
 * @param characterClass   고른 직업. 없으면 null
 * @param sort             "dps" 또는 "efficiency"
 * @param efficiencyReady  전투력 대비 DPS 추세선을 만들었는지 (기록 5개 이상)
 */
public record RecordBoard(
        List<PeriodCount> periods,
        Integer periodNo,
        List<ClassCount> classes,
        String characterClass,
        String sort,
        boolean efficiencyReady,
        List<Row> rows
) {

    public static final String SORT_DPS = "dps";
    public static final String SORT_EFFICIENCY = "efficiency";

    public static RecordBoard empty() {
        return new RecordBoard(List.of(), null, List.of(), null, SORT_DPS, false, List.of());
    }

    /**
     * @param rank        지금 정렬 기준의 순위
     * @param dpsRank     DPS 순위
     * @param combatPower 입장 시점 전투력. 모르면 null
     * @param efficiency  전투력 대비 DPS 추세보다 높은 비율(0.05 = 5%). 계산할 수 없으면 null
     */
    public record Row(
            int rank,
            int dpsRank,
            String replayId,
            String characterName,
            int characterLevel,
            LocalDate registerDate,
            long totalDps,
            Long combatPower,
            Double efficiency
    ) {
    }
}
