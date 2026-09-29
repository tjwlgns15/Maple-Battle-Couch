package com.battlecoach.replay.application.dto;

import java.util.List;

import com.battlecoach.diagnosis.statistics.Quartiles;
import com.battlecoach.spec.domain.PowerStats;

/**
 * 같은 직업·기간 랭커 표본과 비교한 내 위치. 통계를 쓸 수 없으면 {@code unavailableReason} 만 채운다.
 *
 * @param sampleCount 진단 대상 기록을 뺀 표본 수
 * @param efficiency  스펙 대비 DPS(운용 효율). 추세선을 만들 수 없으면 null
 */
public record RankerStanding(
        Integer periodNo,
        int sampleCount,
        boolean reliable,
        String unavailableReason,
        List<Row> rows,
        List<GroupRow> groups,
        int burstCount,
        List<OrderRow> burstOrder,
        Efficiency efficiency
) {

    public static RankerStanding unavailable(Integer periodNo, int sampleCount, String reason) {
        return new RankerStanding(periodNo, sampleCount, false, reason, List.of(), List.of(), 0, List.of(), null);
    }

    public boolean isAvailable() {
        return unavailableReason == null;
    }

    /**
     * @param myCastsPerMinute 내가 안 쓴 스킬이면 0
     * @param mySeconds        데미지 항목이 없으면 null
     * @param belowP25         분당 시전 수가 랭커 하위 25%보다 낮은지
     * @param myLevel          내 스킬 레벨 표기("Lv30 · 강화 30"). 모르면 null
     * @param seconds          초 환산 분위수. 레벨이 같은 랭커가 충분하면 그들만으로 계산한다
     * @param secondsSampleCount   초 환산 분위수를 계산한 랭커 수
     * @param secondsLevelMatched  초 환산을 레벨이 같은 랭커끼리 비교했는지
     */
    public record Row(
            String skillName,
            double adoptionRate,
            int userCount,
            double myCastsPerMinute,
            Quartiles castsPerMinute,
            Double mySeconds,
            Quartiles seconds,
            boolean belowP25,
            String myLevel,
            int secondsSampleCount,
            boolean secondsLevelMatched
    ) {
    }

    /**
     * 같은 직업 랭커의 전투력(과 헥사 코어 레벨 합)으로 그린 DPS 추세선 대비 위치.
     *
     * @param mine      내 기록이 추세보다 높은 비율(0.03 = 3%). 전투력을 모르거나 랭커 스펙 범위 밖이면 null
     * @param rankerMin 랭커 표본의 최솟값
     * @param rankerMax 랭커 표본의 최댓값
     * @param usesHexa  추세선에 헥사 코어 레벨 합을 넣었는지
     * @param lowWeight 추세 하위 25% 랭커의 분포 가중치
     * @param myPower   내 스펙(전투력, 헥사 합)
     * @param minPower  랭커 스펙 범위의 아래 끝
     * @param maxPower  랭커 스펙 범위의 위 끝
     */
    public record Efficiency(Double mine, double rankerMin, double rankerMax, boolean usesHexa, int sampleCount,
                             double lowWeight, PowerStats myPower, PowerStats minPower, PowerStats maxPower) {

        /** 전투력은 있지만 랭커 스펙 범위 밖이라 계산하지 않았는지 */
        public boolean isOutOfRange() {
            return mine == null && myPower.hasCombatPower();
        }
    }

    /**
     * 랭커 다수가 1초 안에 함께 쓰는 스킬 묶음
     *
     * @param minPairedRate 묶음 안 쌍 중 가장 낮은 "함께 쓴 랭커 비율"
     */
    public record GroupRow(double minPairedRate, List<Member> members) {
    }

    /**
     * @param myTogether 내 시전 중 묶음의 다른 스킬과 1초 안에 함께 쓴 횟수
     */
    public record Member(String skillName, int myTogether, int myCasts) {

        public boolean isSeparated() {
            return myCasts > 0 && myTogether * 2 < myCasts;
        }
    }

    /**
     * 내 첫 극딜 순서와 랭커 표준 극딜 순서를 서열 정렬한 한 줄. 한쪽에만 있으면 다른 쪽은 null 이다.
     *
     * @param standardAdoption 표준 쪽 스킬이 랭커 극딜에 나온 비율
     */
    public record OrderRow(String mine, String standard, Double standardAdoption) {

        public boolean isMatch() {
            return mine != null && mine.equals(standard);
        }
    }
}
