package com.battlecoach.replay.application.dto;

import java.util.List;

import com.battlecoach.diagnosis.domain.BurstWindow;
import com.battlecoach.diagnosis.domain.DiagnosisResult;
import com.battlecoach.diagnosis.sequence.AlignedPair;

/**
 * 내 기록(base)을 기준 기록(target)과 비교한 결과.
 *
 * @param playTimeScale 기준 기록의 시전 수·초 환산을 내 전투 시간에 맞춘 배율
 * @param burstOrder    첫 극딜의 스킬 순서 정렬 (left = 내 기록, right = 기준 기록)
 */
public record ReplayComparison(
        ReplayDetail base,
        ReplayDetail target,
        double playTimeScale,
        DiagnosisResult diagnosis,
        List<SkillRow> skills,
        List<AlignedPair> burstOrder,
        List<BurstWindow> baseBursts,
        List<BurstWindow> targetBursts
) {

    /**
     * 초 환산 차이를 시전 수 효과와 1회 효율 효과로 나눈다. 두 효과의 합 = 내 초 환산 − 기준 초 환산(보정).
     * <ul>
     *   <li>castEffect = (내 시전 수 − 기준 시전 수(보정)) × 기준 1회 초 환산</li>
     *   <li>efficiencyEffect = (내 1회 초 환산 − 기준 1회 초 환산) × 내 시전 수</li>
     * </ul>
     * 초 환산 합은 전투 시간과 같아서 한 스킬이 오르면 다른 스킬이 내려간다. 차이 자체를 손해로 읽으면 안 된다.
     * 시전 기록이 없는 패시브·연동 스킬은 분해하지 않는다(null).
     */
    public record SkillRow(
            String skillName,
            Integer baseLevel,
            Integer targetLevel,
            int baseCasts,
            int targetCasts,
            double baseSeconds,
            double targetSecondsScaled,
            Double basePerCastSeconds,
            Double targetPerCastSeconds,
            Double castEffectSeconds,
            Double efficiencyEffectSeconds
    ) {

        public double differenceSeconds() {
            return baseSeconds - targetSecondsScaled;
        }
    }
}
