package com.battlecoach.diagnosis.rule;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.battlecoach.diagnosis.domain.AnalysisContext;
import com.battlecoach.diagnosis.domain.DiagnosisRule;
import com.battlecoach.diagnosis.domain.Finding;
import com.battlecoach.diagnosis.domain.FindingType;
import com.battlecoach.diagnosis.domain.SkillUsage;
import com.battlecoach.diagnosis.sequence.BurstOrderExtractor;
import com.battlecoach.diagnosis.sequence.BurstOrderExtractor.BurstCast;
import com.battlecoach.diagnosis.statistics.BurstOrderStatistics.Precedence;
import com.battlecoach.diagnosis.statistics.BurstOrderStatisticsCalculator;
import com.battlecoach.diagnosis.statistics.JobStatistics;

import lombok.RequiredArgsConstructor;

/**
 * 통계 진단: 랭커 대부분이 "A 다음 B"로 쓰는데 이 기록은 극딜 절반 이상에서 B 를 먼저 쓴 경우.
 * 예: 칼리 랭커 극딜 29회 모두 스틱스를 보이드 버스트보다 먼저 썼다(칼리 B는 반대).
 * 순서가 데미지에 주는 영향은 추정할 근거가 없어 참고로만 낸다. 한 스킬에 어긋난 쌍이 여럿이면 합의율이 가장 높은 쌍만 낸다.
 */
@Component
@RequiredArgsConstructor
public class BurstOrderRule implements DiagnosisRule {

    private final BurstOrderExtractor burstOrderExtractor;

    @Override
    public List<Finding> evaluate(AnalysisContext context) {
        Optional<JobStatistics> statistics = context.reliableStatistics();
        if (statistics.isEmpty()) {
            return List.of();
        }
        List<List<BurstCast>> myBursts = burstOrderExtractor.burstCasts(context);
        if (myBursts.isEmpty()) {
            return List.of();
        }

        Map<String, Violation> worstBySkill = new LinkedHashMap<>();
        for (Precedence precedence : statistics.get().burstOrder().precedences()) {
            Violation violation = count(myBursts, precedence);
            if (violation.ordered() == 0 || violation.reversed() * 2 <= violation.ordered()) {
                continue;
            }
            worstBySkill.merge(precedence.before(), violation, (current, candidate) ->
                    Comparator.comparingDouble((Violation v) -> v.precedence().rate())
                            .thenComparingInt(v -> v.precedence().orderedCount())
                            .compare(candidate, current) > 0 ? candidate : current);
        }

        return worstBySkill.values().stream()
                .map(violation -> toFinding(context, violation))
                .flatMap(Optional::stream)
                .toList();
    }

    /** 이 기록의 극딜 중 두 스킬이 1초 넘게 떨어져 나온 횟수와, 그중 순서가 반대였던 횟수 */
    private static Violation count(List<List<BurstCast>> bursts, Precedence precedence) {
        int ordered = 0;
        int reversed = 0;
        for (List<BurstCast> burst : bursts) {
            Optional<Long> before = timeOf(burst, precedence.before());
            Optional<Long> after = timeOf(burst, precedence.after());
            if (before.isEmpty() || after.isEmpty()
                    || Math.abs(before.get() - after.get()) <= BurstOrderStatisticsCalculator.SIMULTANEOUS_MS) {
                continue;
            }
            ordered++;
            if (after.get() < before.get()) {
                reversed++;
            }
        }
        return new Violation(precedence, ordered, reversed);
    }

    private static Optional<Long> timeOf(List<BurstCast> burst, String baseName) {
        return burst.stream().filter(cast -> cast.baseName().equals(baseName)).map(BurstCast::timeMs).findFirst();
    }

    private static Optional<Finding> toFinding(AnalysisContext context, Violation violation) {
        Precedence precedence = violation.precedence();
        Optional<SkillUsage> skill = context.find(precedence.before());
        String afterName = context.find(precedence.after()).map(SkillUsage::skillName).orElse(precedence.after());
        return skill.map(s -> Finding.unmeasured(FindingType.BURST_ORDER_REVERSED, s, String.format(Locale.ROOT,
                "랭커 극딜 %d회 중 %.0f%%가 이 스킬을 %s보다 먼저 썼습니다. 이 기록은 %d번 중 %d번 %s 뒤에 썼습니다.",
                precedence.orderedCount(), precedence.rate() * 100, afterName,
                violation.ordered(), violation.reversed(), afterName)));
    }

    private record Violation(Precedence precedence, int ordered, int reversed) {
    }
}
