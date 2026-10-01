package com.battlecoach.diagnosis.statistics;

import java.util.List;
import java.util.Optional;

import com.battlecoach.spec.domain.SkillLevel;

/**
 * 비교 대상 표본에서 본 스킬 하나의 분포. 분위수는 운용 효율 하위 25% 표본의 가중치를 낮춰 계산한다({@link EfficiencyModel}).
 *
 * @param userCount      이 스킬을 한 번이라도 쓴 표본 수
 * @param adoptionRate   userCount / 전체 표본 수
 * @param castsPerMinute 쓴 표본들의 분당 시전 수
 * @param seconds        쓴 표본들의 초 환산(스킬 데미지 ÷ 총 DPS). 데미지 항목이 없는 스킬은 null
 * @param secondsSamples 표본별 초 환산과 스킬 레벨. 같은 레벨끼리 비교할 때 쓴다
 */
public record SkillDistribution(
        String baseName,
        String skillName,
        int userCount,
        double adoptionRate,
        Quartiles castsPerMinute,
        Quartiles seconds,
        List<SecondsSample> secondsSamples
) {

    /** 같은 레벨 표본이 이보다 적으면 레벨과 무관한 전체 분포로 비교한다. */
    public static final int MIN_LEVEL_MATCHED = JobStatistics.MIN_SAMPLES;

    public SkillDistribution {
        secondsSamples = secondsSamples == null ? List.of() : List.copyOf(secondsSamples);
    }

    /**
     * 초 환산은 그 스킬의 레벨(과 강화 코어 레벨)에 크게 좌우되므로 레벨이 같은 비교 대상과 비교한다.
     * 다른 스킬·버프 레벨 차이는 여전히 섞여 있다(초 환산은 합이 전투 시간인 상대 지표).
     *
     * @param myLevel 내 스킬 레벨. 모르면 null
     * @return 데미지 항목이 없는 스킬이면 비어 있다
     */
    public Optional<SecondsBasis> secondsFor(SkillLevel myLevel) {
        if (seconds == null) {
            return Optional.empty();
        }
        if (myLevel != null) {
            List<SecondsSample> matched = secondsSamples.stream()
                    .filter(sample -> myLevel.equals(sample.level()))
                    .toList();
            if (matched.size() >= MIN_LEVEL_MATCHED) {
                return Optional.of(new SecondsBasis(Quartiles.weighted(
                        matched.stream().map(SecondsSample::seconds).toList(),
                        matched.stream().map(SecondsSample::weight).toList()), matched.size(), true));
            }
        }
        return Optional.of(new SecondsBasis(seconds, secondsSamples.size(), false));
    }

    /** @param level 표본의 스킬 레벨. 스킬 목록에 없으면 null */
    public record SecondsSample(double seconds, SkillLevel level, double weight) {
    }

    /** @param levelMatched 레벨이 같은 비교 대상만으로 계산했는지 */
    public record SecondsBasis(Quartiles quartiles, int sampleCount, boolean levelMatched) {
    }
}
