package com.battlecoach.replay.application.dto;

import java.util.List;

import com.battlecoach.diagnosis.statistics.Quartiles;

/**
 * 같은 직업·기간 랭커 표본과 비교한 내 위치. 통계를 쓸 수 없으면 {@code unavailableReason} 만 채운다.
 *
 * @param sampleCount 진단 대상 기록을 뺀 표본 수
 */
public record RankerStanding(
        Integer periodNo,
        int sampleCount,
        boolean reliable,
        String unavailableReason,
        List<Row> rows,
        List<GroupRow> groups
) {

    public static RankerStanding unavailable(Integer periodNo, int sampleCount, String reason) {
        return new RankerStanding(periodNo, sampleCount, false, reason, List.of(), List.of());
    }

    public boolean isAvailable() {
        return unavailableReason == null;
    }

    /**
     * @param myCastsPerMinute 내가 안 쓴 스킬이면 0
     * @param mySeconds        데미지 항목이 없으면 null
     * @param belowP25         분당 시전 수가 랭커 하위 25%보다 낮은지
     */
    public record Row(
            String skillName,
            double adoptionRate,
            int userCount,
            double myCastsPerMinute,
            Quartiles castsPerMinute,
            Double mySeconds,
            Quartiles seconds,
            boolean belowP25
    ) {
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
}
