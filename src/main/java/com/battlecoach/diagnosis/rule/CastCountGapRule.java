package com.battlecoach.diagnosis.rule;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.battlecoach.diagnosis.domain.AnalysisContext;
import com.battlecoach.diagnosis.domain.DiagnosisRule;
import com.battlecoach.diagnosis.domain.Finding;
import com.battlecoach.diagnosis.domain.FindingType;
import com.battlecoach.diagnosis.domain.SkillUsage;

/**
 * 비교 진단: 기준 기록보다 적게 쓴 스킬.
 * 초 환산 합은 항상 전투 시간과 같아 스킬별 초 환산 차이는 손해가 아니다. 그래서 시전 수 차이만 떼어
 * "부족한 시전 수 × 내 1회 초 환산"을 영향도로 쓴다. 내 DPS 기준이라 스펙이 달라도 의미가 있다.
 * 기준 기록에서 이 스킬과 늘 같이 쓴 스킬(연동 스킬)이 있으면 함께 쓴 비율을 힌트로 붙인다.
 */
@Component
public class CastCountGapRule implements DiagnosisRule {

    /** 두 시전이 이 안에 있으면 함께 썼다고 본다. */
    static final long PAIR_WINDOW_MS = 1_000;

    /** 기준 기록에서 이 비율 이상 함께 쓴 스킬만 연동 스킬로 본다. */
    static final double PAIR_MIN_RATIO = 0.8;

    @Override
    public List<Finding> evaluate(AnalysisContext context) {
        Optional<AnalysisContext> reference = context.referenceContext();
        if (reference.isEmpty()) {
            return List.of();
        }
        AnalysisContext ref = reference.get();
        double scale = context.playTimeScaleTo(ref);

        List<Finding> findings = new ArrayList<>();
        for (SkillUsage refSkill : ref.skills()) {
            Optional<SkillUsage> mine = context.find(refSkill.baseName());
            if (mine.isEmpty() || !CooldownEligibility.isComparable(mine.get(), context)
                    || !CooldownEligibility.isComparable(refSkill, ref)) {
                continue;
            }
            SkillUsage skill = mine.get();
            double expected = refSkill.castCount() * scale;
            int missing = (int) Math.floor(expected - skill.castCount());
            if (missing <= 0) {
                continue;
            }
            Optional<PairHint> hint = pairHint(skill, context, refSkill, ref);
            String message = String.format(Locale.ROOT,
                    "기준 기록은 %d회(전투 시간 보정 %.1f회) 썼는데 이 기록은 %d회라 약 %d회 부족합니다.%s",
                    refSkill.castCount(), expected, skill.castCount(), missing,
                    hint.map(PairHint::sentence).orElse(""));
            String advice = MissedCastAdvisor.adviseShortfall(skill, context,
                    hint.map(PairHint::partnerName).orElse(null), "기준 기록").orElse(null);
            if (skill.damage() == null || skill.damage() <= 0) {
                findings.add(Finding.unmeasured(FindingType.CAST_COUNT_GAP, skill,
                        message + " 데미지가 없는 스킬(버프 등)이라 영향도는 계산하지 않았습니다.").withAdvice(advice));
                continue;
            }
            double lostDamage = (double) skill.damage() / skill.castCount() * missing;
            findings.add(Finding.measured(FindingType.CAST_COUNT_GAP, skill, context.toSeconds(lostDamage), message)
                    .withAdvice(advice));
        }
        return findings;
    }

    /** 기준 기록의 연동 스킬을 찾아, 이 기록에서 함께 쓴 비율이 더 낮으면 알려준다. */
    private static Optional<PairHint> pairHint(SkillUsage skill, AnalysisContext context, SkillUsage refSkill, AnalysisContext ref) {
        return ref.skills().stream()
                .filter(other -> !other.baseName().equals(refSkill.baseName()))
                .filter(other -> other.castCount() == refSkill.castCount())
                .map(other -> new Pair(other, pairedCount(refSkill, other)))
                .filter(pair -> pair.count() >= refSkill.castCount() * PAIR_MIN_RATIO)
                // 극딜 때는 여러 스킬이 함께 나가므로 같은 횟수면 시전 간격이 가장 가까운 스킬을 고른다.
                .max(Comparator.comparingInt(Pair::count)
                        .thenComparing(pair -> -meanDistanceMs(refSkill, pair.partner())))
                .flatMap(pair -> context.find(pair.partner().baseName())
                        .map(myPartner -> new Pair(myPartner, pairedCount(skill, myPartner)))
                        .or(() -> Optional.of(new Pair(pair.partner(), 0)))
                        .filter(mine -> (double) mine.count() / skill.castCount()
                                < (double) pair.count() / refSkill.castCount())
                        .map(mine -> new PairHint(pair.partner().skillName(), String.format(Locale.ROOT,
                                " 기준 기록은 이 스킬을 %s %.0f초 안에 %d/%d회 함께 썼고, 이 기록은 %d/%d회입니다.",
                                KoreanJosa.withAnd(pair.partner().skillName()), PAIR_WINDOW_MS / 1000.0,
                                pair.count(), refSkill.castCount(), mine.count(), skill.castCount()))));
    }

    private static int pairedCount(SkillUsage skill, SkillUsage partner) {
        return skill.pairedCountWith(partner, PAIR_WINDOW_MS);
    }

    /** 각 시전에서 가장 가까운 짝 시전까지의 평균 거리 */
    private static double meanDistanceMs(SkillUsage skill, SkillUsage partner) {
        return skill.castTimesMs().stream()
                .mapToLong(time -> partner.castTimesMs().stream().mapToLong(other -> Math.abs(other - time)).min().orElse(Long.MAX_VALUE))
                .average()
                .orElse(Double.MAX_VALUE);
    }

    private record Pair(SkillUsage partner, int count) {
    }

    private record PairHint(String partnerName, String sentence) {
    }
}
