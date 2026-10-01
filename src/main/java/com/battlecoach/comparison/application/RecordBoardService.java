package com.battlecoach.comparison.application;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.battlecoach.diagnosis.domain.AnalysisContext;
import com.battlecoach.diagnosis.statistics.EfficiencyModel;
import com.battlecoach.diagnosis.statistics.EfficiencyModelFitter;
import com.battlecoach.replay.repository.ClassCount;
import com.battlecoach.replay.repository.PeriodCount;
import com.battlecoach.replay.repository.ReplayRepository;
import com.battlecoach.spec.domain.PowerStats;

import lombok.RequiredArgsConstructor;

/** 저장된 기록을 기간·직업별로 보여준다. API 를 부르지 않는다. 비교 풀과 같은 기록이다. */
@Service
@RequiredArgsConstructor
public class RecordBoardService {

    private final ReplayRepository replayRepository;
    private final ComparisonPoolLoader comparisonPoolLoader;
    private final EfficiencyModelFitter efficiencyModelFitter;

    /** 기간이나 직업을 고르지 않았거나 기록이 없는 값이면 가장 최근 기간, 기록이 가장 많은 직업을 고른다. */
    public RecordBoard board(Integer periodNo, String characterClass, String sort) {
        List<PeriodCount> periods = replayRepository.countByPeriod();
        if (periods.isEmpty()) {
            return RecordBoard.empty();
        }
        int period = periods.stream().map(PeriodCount::periodNo).filter(p -> p.equals(periodNo)).findFirst()
                .orElse(periods.get(0).periodNo());
        List<ClassCount> classes = replayRepository.countByClass(period);
        String jobClass = classes.stream().map(ClassCount::characterClass).filter(c -> c.equals(characterClass))
                .findFirst().orElse(classes.get(0).characterClass());
        String sortKey = RecordBoard.SORT_EFFICIENCY.equals(sort) ? RecordBoard.SORT_EFFICIENCY : RecordBoard.SORT_DPS;

        List<PoolEntry> pool = comparisonPoolLoader.load(jobClass, period);
        Optional<EfficiencyModel> model = efficiencyModelFitter.fit(pool.stream().map(PoolEntry::context).toList());
        return new RecordBoard(periods, period, classes, jobClass, sortKey, model.isPresent(),
                rows(pool, model.orElse(null), sortKey));
    }

    private static List<RecordBoard.Row> rows(List<PoolEntry> pool, EfficiencyModel model, String sortKey) {
        List<Draft> drafts = new ArrayList<>();
        for (PoolEntry entry : pool) {
            AnalysisContext context = entry.context();
            PowerStats power = context.spec().powerStats();
            Double efficiency = model == null ? null : model.sampleEfficiencyOf(context).orElse(null);
            drafts.add(new Draft(entry, context.totalDps(), power.hasCombatPower() ? power.combatPower() : null, efficiency));
        }
        List<Draft> byDps = drafts.stream().sorted(Comparator.comparingLong(Draft::totalDps).reversed()).toList();
        List<Draft> ordered = RecordBoard.SORT_EFFICIENCY.equals(sortKey)
                ? drafts.stream().sorted(Comparator.comparing(Draft::efficiency,
                        Comparator.nullsLast(Comparator.reverseOrder()))).toList()
                : byDps;

        List<RecordBoard.Row> rows = new ArrayList<>();
        for (int i = 0; i < ordered.size(); i++) {
            Draft draft = ordered.get(i);
            PoolEntry entry = draft.entry();
            rows.add(new RecordBoard.Row(i + 1, byDps.indexOf(draft) + 1, entry.replayId(), entry.characterName(),
                    entry.characterLevel(), entry.registerDate(), draft.totalDps(), draft.combatPower(), draft.efficiency()));
        }
        return rows;
    }

    private record Draft(PoolEntry entry, long totalDps, Long combatPower, Double efficiency) {
    }
}
