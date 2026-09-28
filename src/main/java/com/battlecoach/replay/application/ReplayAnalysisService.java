package com.battlecoach.replay.application;

import java.util.Optional;
import java.util.OptionalInt;

import org.springframework.stereotype.Service;

import com.battlecoach.diagnosis.domain.AnalysisContext;
import com.battlecoach.diagnosis.domain.DiagnosisEngine;
import com.battlecoach.diagnosis.statistics.JobStatistics;
import com.battlecoach.diagnosis.statistics.JobStatisticsProvider;
import com.battlecoach.replay.application.dto.RankerStanding;
import com.battlecoach.replay.application.dto.ReplayAnalysis;
import com.battlecoach.replay.application.dto.ReplayDetail;
import com.battlecoach.replay.domain.ReplayId;

import lombok.RequiredArgsConstructor;

/**
 * 기록 하나에 대한 단일 진단, 쿨타임 검증 표, 랭커 대비를 만든다. 호출 전에 리플레이가 저장돼 있어야 한다.
 * 같은 직업·기간 랭커 통계가 있으면 컨텍스트에 붙여 통계 진단 규칙도 함께 돌린다.
 */
@Service
@RequiredArgsConstructor
public class ReplayAnalysisService {

    private final AnalysisContextFactory analysisContextFactory;
    private final DiagnosisEngine diagnosisEngine;
    private final CooldownReportService cooldownReportService;
    private final ReplayPeriodRecorder replayPeriodRecorder;
    private final JobStatisticsProvider jobStatisticsProvider;
    private final RankerStandingAssembler rankerStandingAssembler;

    public ReplayAnalysis analyze(ReplayDetail replay) {
        AnalysisContext context = analysisContextFactory.create(replay);
        OptionalInt period = replayPeriodRecorder.periodOf(ReplayId.of(replay.replayId()));

        RankerStanding standing;
        if (period.isEmpty()) {
            standing = RankerStanding.unavailable(null, 0,
                    "이 기록의 연무장 기간을 알 수 없습니다. 캐릭터 검색으로 기록 목록을 거쳐 들어오면 기간이 저장됩니다.");
        } else {
            Optional<JobStatistics> statistics = jobStatisticsProvider.find(
                    replay.characterClass(), period.getAsInt(), replay.replayId());
            if (statistics.isEmpty()) {
                standing = RankerStanding.unavailable(period.getAsInt(), 0,
                        "같은 직업·기간의 랭커 표본이 아직 없습니다.");
            } else {
                context = context.withStatistics(statistics.get());
                standing = rankerStandingAssembler.assemble(context, period.getAsInt(), statistics.get());
            }
        }

        return new ReplayAnalysis(
                diagnosisEngine.diagnose(context),
                context.bursts(),
                cooldownReportService.report(context),
                standing);
    }
}
