package com.battlecoach.diagnosis.rule;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.battlecoach.diagnosis.domain.AnalysisContext;
import com.battlecoach.diagnosis.domain.DiagnosisRule;
import com.battlecoach.diagnosis.domain.Finding;
import com.battlecoach.diagnosis.domain.FindingType;
import com.battlecoach.diagnosis.domain.SkillUsage;
import com.battlecoach.diagnosis.statistics.JobStatistics;
import com.battlecoach.diagnosis.statistics.JobStatisticsCalculator;
import com.battlecoach.diagnosis.statistics.SkillDistribution;

/**
 * 통계 진단: 분당 시전 수가 비교 대상 하위 25%보다 낮은 스킬.
 * 영향도는 "비교 대상 중앙값까지 모자란 시전 수 × 내 1회 초 환산"이다(내 DPS 기준).
 * 대상 스킬 기준은 {@link CooldownEligibility} 를 따른다.
 */
@Component
public class CastRateRule implements DiagnosisRule {

    @Override
    public List<Finding> evaluate(AnalysisContext context) {
        Optional<JobStatistics> statistics = context.reliableStatistics();
        if (statistics.isEmpty() || context.playTimeMinutes() <= 0) {
            return List.of();
        }
        List<Finding> findings = new ArrayList<>();
        for (SkillUsage skill : context.skills()) {
            if (!CooldownEligibility.isComparable(skill, context)) {
                continue;
            }
            Optional<SkillDistribution> distribution = statistics.get().skill(skill.baseName())
                    .filter(d -> d.userCount() >= JobStatistics.MIN_SAMPLES);
            if (distribution.isEmpty()) {
                continue;
            }
            double myRate = skill.castCount() / context.playTimeMinutes();
            double p25 = distribution.get().castsPerMinute().p25();
            double p50 = distribution.get().castsPerMinute().p50();
            int missing = (int) Math.floor((p50 - myRate) * context.playTimeMinutes());
            if (myRate >= p25 || missing <= 0) {
                continue;
            }
            String message = String.format(Locale.ROOT,
                    "이 스킬을 쓴 비교 대상 %d명의 분당 시전 수는 중앙값 %.2f회(하위 25%% %.2f회)인데 이 기록은 %.2f회입니다. 중앙값보다 약 %d회 적게 썼습니다.",
                    distribution.get().userCount(), p50, p25, myRate, missing);
            String advice = MissedCastAdvisor.adviseShortfall(skill, context,
                    separatedPartner(skill, context, statistics.get()), "비교 대상 대부분").orElse(null);
            if (skill.damage() == null || skill.damage() <= 0) {
                findings.add(Finding.unmeasured(FindingType.CAST_RATE_BELOW_PEERS, skill,
                        message + " 데미지가 없는 스킬(버프 등)이라 영향도는 계산하지 않았습니다.").withAdvice(advice));
                continue;
            }
            double lostDamage = (double) skill.damage() / skill.castCount() * missing;
            findings.add(Finding.measured(FindingType.CAST_RATE_BELOW_PEERS, skill, context.toSeconds(lostDamage), message)
                    .withAdvice(advice));
        }
        return findings;
    }

    /** 비교 대상 대부분이 함께 쓰는 짝을 이 기록은 절반 넘게 따로 썼으면 그 짝의 이름 */
    private static String separatedPartner(SkillUsage skill, AnalysisContext context, JobStatistics statistics) {
        return statistics.bestPairFor(skill.baseName())
                .flatMap(pair -> context.find(pair.partnerBaseName()))
                .filter(partner -> partner.castCount() > 0)
                .filter(partner -> skill.pairedCountWith(partner, JobStatisticsCalculator.PAIR_WINDOW_MS)
                        < skill.castCount() * LinkedPairRule.SEPARATED_RATIO)
                .map(SkillUsage::skillName)
                .orElse(null);
    }
}
