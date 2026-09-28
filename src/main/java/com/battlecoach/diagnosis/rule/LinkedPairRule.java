package com.battlecoach.diagnosis.rule;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.battlecoach.diagnosis.domain.AnalysisContext;
import com.battlecoach.diagnosis.domain.DiagnosisRule;
import com.battlecoach.diagnosis.domain.Finding;
import com.battlecoach.diagnosis.domain.FindingType;
import com.battlecoach.diagnosis.domain.SkillUsage;
import com.battlecoach.diagnosis.statistics.JobStatistics;
import com.battlecoach.diagnosis.statistics.JobStatisticsCalculator;
import com.battlecoach.diagnosis.statistics.PairStatistic;

/**
 * 통계 진단: 랭커 대부분이 함께 쓰는 스킬 쌍(예: 스틱스 + 레디 투 다이)을 따로 쓴 경우.
 * 기록 하나로는 어떤 스킬이 짝인지 알 수 없어 랭커 통계로 판단한다.
 * 데미지로 환산할 근거가 없어 영향도 없이 참고로 낸다. 시전 수가 부족해서 짝을 못 맞췄다면 그 손해는 다른 규칙이 잰다.
 */
@Component
public class LinkedPairRule implements DiagnosisRule {

    /** 내 기록에서 함께 쓴 비율이 이보다 낮으면 따로 쓴 것으로 본다. */
    static final double SEPARATED_RATIO = 0.5;

    @Override
    public List<Finding> evaluate(AnalysisContext context) {
        Optional<JobStatistics> statistics = context.reliableStatistics();
        if (statistics.isEmpty()) {
            return List.of();
        }
        List<Finding> findings = new ArrayList<>();
        Set<String> reportedPairs = new HashSet<>();
        for (SkillUsage skill : context.skills()) {
            if (skill.castCount() == 0) {
                continue;
            }
            Optional<PairStatistic> pair = statistics.get().bestPairFor(skill.baseName());
            Optional<SkillUsage> partner = pair.flatMap(p -> context.find(p.partnerBaseName()))
                    .filter(p -> p.castCount() > 0); // 짝 스킬을 아예 안 썼으면 구성 문제다.
            if (pair.isEmpty() || partner.isEmpty() || !reportedPairs.add(pairKey(skill, partner.get()))) {
                continue;
            }
            int paired = skill.pairedCountWith(partner.get(), JobStatisticsCalculator.PAIR_WINDOW_MS);
            if ((double) paired / skill.castCount() >= SEPARATED_RATIO) {
                continue;
            }
            findings.add(Finding.unmeasured(FindingType.LINKED_PAIR_SEPARATED, skill, String.format(Locale.ROOT,
                    "두 스킬을 모두 쓴 랭커 %d명 중 %.0f%%가 이 스킬을 %s와 %.0f초 안에 함께 씁니다. 이 기록은 %d/%d회입니다.",
                    pair.get().bothUsedCount(), pair.get().pairedRate() * 100, partner.get().skillName(),
                    JobStatisticsCalculator.PAIR_WINDOW_MS / 1000.0, paired, skill.castCount())));
        }
        return findings;
    }

    private static String pairKey(SkillUsage a, SkillUsage b) {
        return a.baseName().compareTo(b.baseName()) < 0
                ? a.baseName() + "|" + b.baseName()
                : b.baseName() + "|" + a.baseName();
    }
}
