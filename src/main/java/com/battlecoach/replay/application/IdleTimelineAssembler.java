package com.battlecoach.replay.application;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.stereotype.Component;

import com.battlecoach.diagnosis.domain.AnalysisContext;
import com.battlecoach.diagnosis.domain.DiagnosisResult;
import com.battlecoach.diagnosis.domain.Finding;
import com.battlecoach.diagnosis.domain.FindingType;
import com.battlecoach.diagnosis.domain.IdleBreakdown;
import com.battlecoach.diagnosis.rule.CooldownEligibility;
import com.battlecoach.replay.application.dto.SkillIdleView;

/**
 * 타임라인에 그릴 쉰 구간을 만든다. 대상은 놓친 시전 진단과 같다(실효 쿨 15초 이상, 쿨 변동 스킬 제외).
 * 합계는 {@link IdleBreakdown} 과 같고, 화면에는 입력 지연과 구분하기 어려운 짧은 구간을 빼고 넘긴다.
 */
@Component
class IdleTimelineAssembler {

    /** 이보다 짧은 구간은 그리지 않는다. */
    static final long MIN_DISPLAY_MS = 1_000;

    List<SkillIdleView> assemble(AnalysisContext context, DiagnosisResult diagnosis) {
        Set<String> diagnosed = Stream.concat(diagnosis.findings().stream(), diagnosis.notes().stream())
                .filter(finding -> finding.type() != FindingType.EXCLUDED_DYNAMIC_COOLDOWN)
                .map(Finding::skillBaseName)
                .collect(Collectors.toSet());
        return context.skills().stream()
                .filter(skill -> skill.castCount() > 0 && CooldownEligibility.hasAbsolutelyTrackableCooldown(skill))
                .filter(skill -> !CooldownEligibility.hasDynamicCooldown(skill, context))
                .map(skill -> new SkillIdleView(
                        skill.baseName(),
                        skill.skillName(),
                        skill.effectiveCooldownMs() / 1000.0,
                        diagnosed.contains(skill.baseName()),
                        IdleBreakdown.of(skill, context).spans().stream()
                                .filter(span -> span.durationMs() >= MIN_DISPLAY_MS)
                                .toList()))
                .filter(view -> !view.spans().isEmpty())
                .toList();
    }
}
