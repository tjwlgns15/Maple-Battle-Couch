package com.battlecoach.replay.application;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.battlecoach.diagnosis.domain.AnalysisContext;
import com.battlecoach.diagnosis.domain.SkillUsage;
import com.battlecoach.replay.application.dto.CooldownReport;
import com.battlecoach.replay.application.dto.CooldownReport.Note;
import com.battlecoach.replay.application.dto.CooldownReport.Row;
import com.battlecoach.spec.domain.SkillSpec;

/** 타임라인에 나온 스킬마다 계산 쿨과 실측 시전 간격을 나란히 놓는다. */
@Service
public class CooldownReportService {

    private static final Comparator<Row> ORDER = Comparator
            .comparing(Row::baseCooldownMs, Comparator.nullsLast(Comparator.reverseOrder()))
            .thenComparing(Row::skillName);

    public CooldownReport report(AnalysisContext context) {
        List<Row> rows = context.skills().stream()
                .map(skill -> toRow(skill, context.spec().find(skill.baseName())))
                .sorted(ORDER)
                .toList();
        return new CooldownReport(context.cooldownStats(), rows);
    }

    private static Row toRow(SkillUsage skill, Optional<SkillSpec> spec) {
        List<Long> intervals = skill.sortedIntervalsMs();
        return new Row(
                skill.skillName(),
                spec.map(SkillSpec::level).orElse(null),
                spec.map(SkillSpec::baseCooldownMs).orElse(null),
                skill.effectiveCooldownMs(),
                skill.castCount(),
                intervals.isEmpty() ? null : intervals.get(0),
                intervals.isEmpty() ? null : intervals.get(intervals.size() / 2),
                skill.earlyIntervalCount(),
                notesOf(spec));
    }

    private static List<Note> notesOf(Optional<SkillSpec> spec) {
        if (spec.isEmpty()) {
            return List.of(Note.NO_SPEC);
        }
        SkillSpec skill = spec.get();
        if (!skill.hasCooldown()) {
            return List.of(Note.NO_COOLDOWN);
        }
        List<Note> notes = new ArrayList<>();
        if (!skill.cooldownReducible()) {
            notes.add(Note.NOT_REDUCIBLE);
        }
        if (!skill.cooldownResettable()) {
            notes.add(Note.NOT_RESETTABLE);
        }
        if (skill.reactivatable() && skill.durationMs() != null) {
            notes.add(Note.REACTIVATION_MERGED);
        }
        return List.copyOf(notes);
    }
}
