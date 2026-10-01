package com.battlecoach.replay.application;

import java.util.Optional;
import java.util.OptionalInt;

import org.springframework.stereotype.Service;

import com.battlecoach.diagnosis.domain.AnalysisContext;
import com.battlecoach.diagnosis.domain.DiagnosisEngine;
import com.battlecoach.diagnosis.domain.DiagnosisResult;
import com.battlecoach.diagnosis.statistics.ComparisonCriteria;
import com.battlecoach.diagnosis.statistics.ComparisonStatistics;
import com.battlecoach.diagnosis.statistics.JobStatistics;
import com.battlecoach.diagnosis.statistics.JobStatisticsProvider;
import com.battlecoach.replay.application.dto.ComparisonStanding;
import com.battlecoach.replay.application.dto.ReplayAnalysis;
import com.battlecoach.replay.application.dto.ReplayDetail;
import com.battlecoach.replay.domain.ReplayId;

import lombok.RequiredArgsConstructor;

/**
 * 기록 하나에 대한 단일 진단, 쿨타임 검증 표, 비교 대상 대비를 만든다. 호출 전에 리플레이가 저장돼 있어야 한다.
 * 같은 직업·기간 비교 대상 통계가 있으면 컨텍스트에 붙여 통계 진단 규칙도 함께 돌린다.
 */
@Service
@RequiredArgsConstructor
public class ReplayAnalysisService {

    private final AnalysisContextFactory analysisContextFactory;
    private final DiagnosisEngine diagnosisEngine;
    private final CooldownReportService cooldownReportService;
    private final ReplayPeriodRecorder replayPeriodRecorder;
    private final JobStatisticsProvider jobStatisticsProvider;
    private final ComparisonStandingAssembler comparisonStandingAssembler;
    private final IdleTimelineAssembler idleTimelineAssembler;

    /** @param criteria 비교 대상을 고를 조건 */
    public ReplayAnalysis analyze(ReplayDetail replay, ComparisonCriteria criteria) {
        AnalysisContext context = analysisContextFactory.create(replay);
        OptionalInt period = replayPeriodRecorder.periodOf(ReplayId.of(replay.replayId()));

        ComparisonStanding standing;
        if (period.isEmpty()) {
            standing = ComparisonStanding.unavailable(null, criteria, 0,
                    "이 기록의 연무장 기간을 알 수 없습니다. 캐릭터 검색으로 기록 목록을 거쳐 들어오면 기간이 저장됩니다.");
        } else {
            int periodNo = period.getAsInt();
            Optional<ComparisonStatistics> comparison = jobStatisticsProvider.find(
                    replay.characterClass(), periodNo, replay.replayId(), criteria);
            Optional<JobStatistics> statistics = comparison.flatMap(ComparisonStatistics::jobStatistics);
            int poolSize = comparison.map(ComparisonStatistics::poolSize).orElse(0);
            if (statistics.isEmpty()) {
                standing = ComparisonStanding.unavailable(periodNo, criteria, poolSize, unavailableReason(criteria, poolSize));
            } else {
                context = context.withStatistics(statistics.get());
                standing = comparisonStandingAssembler.assemble(context, periodNo, criteria, poolSize, statistics.get());
            }
        }

        DiagnosisResult diagnosis = diagnosisEngine.diagnose(context);
        return new ReplayAnalysis(
                diagnosis,
                context.bursts(),
                cooldownReportService.report(context),
                standing,
                idleTimelineAssembler.assemble(context, diagnosis));
    }

    private static String unavailableReason(ComparisonCriteria criteria, int poolSize) {
        if (poolSize == 0) {
            return "같은 직업·기간으로 저장된 다른 기록이 아직 없습니다. 같은 직업 캐릭터를 검색하면 기록이 쌓입니다.";
        }
        return "저장된 기록 " + poolSize + "개 중 '" + criteria.label() + "' 조건에 맞는 기록이 없습니다."
                + " 전투력 대비 DPS는 전투력이 있는 기록이 5개 이상이어야 계산할 수 있습니다.";
    }
}
