package com.battlecoach.replay.application;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.battlecoach.diagnosis.domain.AnalysisContext;
import com.battlecoach.diagnosis.domain.SkillUsage;
import com.battlecoach.diagnosis.statistics.JobStatistics;
import com.battlecoach.diagnosis.statistics.JobStatisticsCalculator;
import com.battlecoach.diagnosis.statistics.SkillDistribution;
import com.battlecoach.replay.application.dto.RankerStanding;
import com.battlecoach.replay.application.dto.RankerStanding.GroupRow;
import com.battlecoach.replay.application.dto.RankerStanding.Member;
import com.battlecoach.replay.application.dto.RankerStanding.Row;

/** 내 기록의 스킬별 값을 랭커 분포 옆에 놓는다. */
@Component
class RankerStandingAssembler {

    /** 랭커 절반 이상이 쓰는 스킬은 내가 안 썼어도 표에 넣는다. */
    private static final double SHOW_UNUSED_ADOPTION = 0.5;

    private static final Comparator<Row> ORDER = Comparator
            .comparing((Row row) -> row.seconds() == null ? null : row.seconds().p50(),
                    Comparator.nullsLast(Comparator.reverseOrder()))
            .thenComparing(Row::skillName);

    RankerStanding assemble(AnalysisContext context, int periodNo, JobStatistics statistics) {
        List<Row> rows = statistics.skills().values().stream()
                .map(distribution -> toRow(context, distribution))
                .flatMap(Optional::stream)
                .sorted(ORDER)
                .toList();
        return new RankerStanding(periodNo, statistics.sampleCount(), statistics.isReliable(), null,
                rows, groupRows(context, statistics));
    }

    private static Optional<Row> toRow(AnalysisContext context, SkillDistribution distribution) {
        Optional<SkillUsage> mine = context.find(distribution.baseName()).filter(skill -> skill.castCount() > 0);
        if (mine.isEmpty() && distribution.adoptionRate() < SHOW_UNUSED_ADOPTION) {
            return Optional.empty();
        }
        double myRate = mine.map(skill -> skill.castCount() / context.playTimeMinutes()).orElse(0.0);
        Double mySeconds = mine.map(SkillUsage::damage).map(context::toSeconds).orElse(null);
        return Optional.of(new Row(
                mine.map(SkillUsage::skillName).orElse(distribution.skillName()),
                distribution.adoptionRate(),
                distribution.userCount(),
                myRate,
                distribution.castsPerMinute(),
                mySeconds,
                distribution.seconds(),
                myRate < distribution.castsPerMinute().p25()));
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
