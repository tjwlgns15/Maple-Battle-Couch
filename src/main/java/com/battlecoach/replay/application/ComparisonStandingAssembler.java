package com.battlecoach.replay.application;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.battlecoach.diagnosis.domain.AnalysisContext;
import com.battlecoach.diagnosis.domain.SkillUsage;
import com.battlecoach.diagnosis.sequence.BurstOrderExtractor;
import com.battlecoach.diagnosis.sequence.SequenceAligner;
import com.battlecoach.diagnosis.statistics.BurstOrderStatistics;
import com.battlecoach.diagnosis.statistics.ComparisonCriteria;
import com.battlecoach.diagnosis.statistics.EfficiencyModel;
import com.battlecoach.diagnosis.statistics.JobStatistics;
import com.battlecoach.diagnosis.statistics.JobStatisticsCalculator;
import com.battlecoach.diagnosis.statistics.SkillDistribution;
import com.battlecoach.diagnosis.statistics.SkillDistribution.SecondsBasis;
import com.battlecoach.replay.application.dto.ComparisonStanding;
import com.battlecoach.replay.application.dto.ComparisonStanding.Efficiency;
import com.battlecoach.replay.application.dto.ComparisonStanding.GroupRow;
import com.battlecoach.replay.application.dto.ComparisonStanding.Member;
import com.battlecoach.replay.application.dto.ComparisonStanding.OrderRow;
import com.battlecoach.replay.application.dto.ComparisonStanding.Row;
import com.battlecoach.spec.domain.SkillLevel;

import lombok.RequiredArgsConstructor;

/** 내 기록의 스킬별 값을 비교 대상 분포 옆에 놓는다. */
@Component
@RequiredArgsConstructor
class ComparisonStandingAssembler {

    /** 비교 대상 절반 이상이 쓰는 스킬은 내가 안 썼어도 표에 넣는다. */
    private static final double SHOW_UNUSED_ADOPTION = 0.5;

    private static final Comparator<Row> ORDER = Comparator
            .comparing((Row row) -> row.seconds() == null ? null : row.seconds().p50(),
                    Comparator.nullsLast(Comparator.reverseOrder()))
            .thenComparing(Row::skillName);

    private final BurstOrderExtractor burstOrderExtractor;
    private final SequenceAligner sequenceAligner;

    ComparisonStanding assemble(AnalysisContext context, int periodNo, ComparisonCriteria criteria, int poolSize,
                            JobStatistics statistics) {
        List<Row> rows = statistics.skills().values().stream()
                .map(distribution -> toRow(context, distribution))
                .flatMap(Optional::stream)
                .sorted(ORDER)
                .toList();
        return new ComparisonStanding(periodNo, criteria, poolSize, statistics.sampleCount(), statistics.isReliable(), null,
                rows, groupRows(context, statistics), statistics.burstOrder().burstCount(),
                burstOrderRows(context, statistics.burstOrder()),
                statistics.efficiencyModel().map(model -> efficiency(context, model)).orElse(null));
    }

    private static Efficiency efficiency(AnalysisContext context, EfficiencyModel model) {
        return new Efficiency(model.efficiencyOf(context).orElse(null), model.minEfficiency(), model.maxEfficiency(),
                model.usesHexa(), model.sampleCount(), EfficiencyModel.LOW_EFFICIENCY_WEIGHT,
                context.spec().powerStats(), model.minPower(), model.maxPower());
    }

    /** 내 첫 극딜 순서를 비교 대상 표준 순서(극딜 절반 이상에서 쓰는 스킬, 상대 위치 중앙값 순)에 맞춘다. */
    private List<OrderRow> burstOrderRows(AnalysisContext context, BurstOrderStatistics burstOrder) {
        List<String> standard = burstOrder.standardOrder().stream().map(BurstOrderStatistics.Entry::baseName).toList();
        if (standard.isEmpty()) {
            return List.of();
        }
        return sequenceAligner.align(burstOrderExtractor.firstBurstOrder(context), standard).stream()
                .map(pair -> new OrderRow(pair.left(), pair.right(),
                        pair.right() == null ? null
                                : burstOrder.entry(pair.right()).map(BurstOrderStatistics.Entry::adoptionRate).orElse(null)))
                .toList();
    }

    private static Optional<Row> toRow(AnalysisContext context, SkillDistribution distribution) {
        Optional<SkillUsage> mine = context.find(distribution.baseName()).filter(skill -> skill.castCount() > 0);
        if (mine.isEmpty() && distribution.adoptionRate() < SHOW_UNUSED_ADOPTION) {
            return Optional.empty();
        }
        double myRate = mine.map(skill -> skill.castCount() / context.playTimeMinutes()).orElse(0.0);
        Double mySeconds = mine.map(SkillUsage::damage).map(context::toSeconds).orElse(null);
        SkillLevel myLevel = context.spec().levelOf(distribution.baseName()).orElse(null);
        Optional<SecondsBasis> seconds = distribution.secondsFor(myLevel);
        return Optional.of(new Row(
                mine.map(SkillUsage::skillName).orElse(distribution.skillName()),
                distribution.adoptionRate(),
                distribution.userCount(),
                myRate,
                distribution.castsPerMinute(),
                mySeconds,
                seconds.map(SecondsBasis::quartiles).orElse(null),
                myRate < distribution.castsPerMinute().p25(),
                myLevel == null ? null : myLevel.label(),
                seconds.map(SecondsBasis::sampleCount).orElse(0),
                seconds.map(SecondsBasis::levelMatched).orElse(false)));
    }

    private static List<GroupRow> groupRows(AnalysisContext context, JobStatistics statistics) {
        return statistics.pairGroups().stream()
                .map(group -> new GroupRow(statistics.minPairedRate(group), group.stream()
                        .map(baseName -> member(context, statistics, baseName, group))
                        .toList()))
                .toList();
    }

    private static Member member(AnalysisContext context, JobStatistics statistics, String baseName, List<String> group) {
        Optional<SkillUsage> mine = context.find(baseName);
        String name = mine.map(SkillUsage::skillName)
                .orElseGet(() -> statistics.skill(baseName).map(SkillDistribution::skillName).orElse(baseName));
        if (mine.isEmpty()) {
            return new Member(name, 0, 0);
        }
        List<SkillUsage> others = group.stream()
                .filter(other -> !other.equals(baseName))
                .map(context::find)
                .flatMap(Optional::stream)
                .toList();
        int together = (int) mine.get().castTimesMs().stream()
                .filter(time -> others.stream().anyMatch(other -> other.castTimesMs().stream()
                        .anyMatch(t -> Math.abs(t - time) <= JobStatisticsCalculator.PAIR_WINDOW_MS)))
                .count();
        return new Member(name, together, mine.get().castCount());
    }
}
