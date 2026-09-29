package com.battlecoach.diagnosis.statistics;

import java.util.Optional;

import com.battlecoach.diagnosis.domain.AnalysisContext;
import com.battlecoach.spec.domain.PowerStats;

/**
 * 같은 직업·기간 랭커 표본으로 적합한 "스펙으로 예상되는 DPS" 추세선.
 * log(DPS) = intercept + combatPowerSlope × log(전투력) [+ hexaSlope × 헥사 코어 레벨 합]
 * <p>
 * 운용 효율은 실제 DPS 가 추세선보다 몇 % 높은지다. 전투력에는 헥사·5차 스킬 레벨이 빠져 있어 헥사 항을 더하지만,
 * 표본이 적으면 헥사 계수가 불안정해 전투력만 쓴다. 남는 차이에는 운용 말고도 미적용 발동 같은 운과 기록마다의 편차가 섞여 있다.
 * <p>
 * 랭커 스펙 범위 밖으로는 외삽하지 않는다. 칼리 랭커는 헥사 합이 263~420 이라 헥사 계수가 1레벨당 +0.06%로 작게 나오는데,
 * 헥사 합 137 인 기록에 적용하면 헥사 부족분이 거의 설명되지 않아 −22% 가 나왔다.
 *
 * @param hexaSlope     헥사 코어 레벨 1당 log(DPS) 증가. 헥사 항을 쓰지 않으면 null
 * @param sampleCount   적합에 쓴 표본 수
 * @param minEfficiency 표본 운용 효율의 최솟값 (0.05 = 추세보다 5% 높음)
 * @param maxEfficiency 표본 운용 효율의 최댓값
 * @param lowCutoff     표본 운용 효율의 하위 25% 경계. 이보다 낮은 표본은 분포 계산에서 가중치를 낮춘다
 * @param minPower      표본 스펙의 최솟값(전투력, 헥사 합)
 * @param maxPower      표본 스펙의 최댓값
 */
public record EfficiencyModel(
        double intercept,
        double combatPowerSlope,
        Double hexaSlope,
        int sampleCount,
        double minEfficiency,
        double maxEfficiency,
        double lowCutoff,
        PowerStats minPower,
        PowerStats maxPower
) {

    /** 운용 효율 하위 25% 표본의 가중치 */
    public static final double LOW_EFFICIENCY_WEIGHT = 0.5;

    public boolean usesHexa() {
        return hexaSlope != null;
    }

    /** 추세선을 적용할 수 있는 스펙인지. 전투력과 (헥사 항을 쓰면) 헥사 합이 표본 범위 안이어야 한다. */
    public boolean covers(PowerStats power) {
        if (!power.hasCombatPower()
                || power.combatPower() < minPower.combatPower() || power.combatPower() > maxPower.combatPower()) {
            return false;
        }
        return !usesHexa()
                || (power.hexaLevelSum() >= minPower.hexaLevelSum() && power.hexaLevelSum() <= maxPower.hexaLevelSum());
    }

    /**
     * 범위 검사 없이 추세 대비 비율을 계산한다. 적합한 표본 자신에게만 쓴다.
     *
     * @return 추세 대비 비율(0.05 = 5% 높음). 전투력이나 (헥사 항을 쓰면) 헥사 레벨을 모르면 비어 있다
     */
    Optional<Double> rawEfficiencyOf(long totalDps, PowerStats power) {
        if (totalDps <= 0 || !power.hasCombatPower() || (usesHexa() && !power.hasHexaLevels())) {
            return Optional.empty();
        }
        double predicted = intercept + combatPowerSlope * Math.log(power.combatPower())
                + (usesHexa() ? hexaSlope * power.hexaLevelSum() : 0);
        return Optional.of(Math.exp(Math.log(totalDps) - predicted) - 1);
    }

    /** @return 추세 대비 비율. 스펙을 모르거나 랭커 스펙 범위 밖이면 비어 있다 */
    public Optional<Double> efficiencyOf(AnalysisContext context) {
        PowerStats power = context.spec().powerStats();
        if (!covers(power)) {
            return Optional.empty();
        }
        return rawEfficiencyOf(context.totalDps(), power);
    }

    /** 효율을 모르는 표본은 낮추지 않는다. */
    public double weightOf(AnalysisContext context) {
        return efficiencyOf(context).filter(efficiency -> efficiency < lowCutoff)
                .map(efficiency -> LOW_EFFICIENCY_WEIGHT)
                .orElse(1.0);
    }
}
