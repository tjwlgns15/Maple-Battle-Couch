package com.battlecoach.diagnosis.statistics;

import java.util.List;

/**
 * "극딜까지 아끼는 것 vs 쿨마다 쓰는 것" 가설 검증 결과. 스킬 하나를 표본끼리 비교한다.
 * 상관은 인과가 아니다. 초 환산에는 스킬 레벨, 다른 스킬 레벨, 버프 레벨이 함께 섞여 있다.
 *
 * @param heldVsSeconds 극딜 대기 시간과 초 환산의 스피어만 상관. 계산할 수 없으면 null
 * @param heldVsRate    극딜 대기 시간과 분당 시전 수의 스피어만 상관. 계산할 수 없으면 null
 */
public record HoldTradeoff(
        String baseName,
        String skillName,
        List<Point> points,
        Double heldVsSeconds,
        Double heldVsRate
) {

    public HoldTradeoff {
        points = List.copyOf(points);
    }

    /**
     * 표본 하나
     *
     * @param seconds 스킬 초 환산. 데미지 항목이 없으면 null
     */
    public record Point(
            String label,
            double heldForBurstSeconds,
            double unusedSeconds,
            int casts,
            double castsPerMinute,
            Double seconds
    ) {
    }
}
