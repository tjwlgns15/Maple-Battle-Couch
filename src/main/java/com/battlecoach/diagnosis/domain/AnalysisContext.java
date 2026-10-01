package com.battlecoach.diagnosis.domain;

import java.util.List;
import java.util.Optional;

import com.battlecoach.diagnosis.statistics.JobStatistics;
import com.battlecoach.spec.domain.CharacterSpec;
import com.battlecoach.spec.domain.CooldownStats;

/**
 * 진단 규칙이 보는 입력. 단일 진단은 기록 하나와 그 캐릭터의 스펙으로 만들고,
 * 비교 진단은 기준 기록({@code reference}), 통계 진단은 비교 대상 통계({@code statistics})를 더해 같은 엔진에 넣는다.
 *
 * @param reference  비교 기준 기록. 없으면 null
 * @param statistics 같은 직업·기간 비교 대상 통계. 없으면 null
 */
public record AnalysisContext(
        long playTimeMs,
        long totalDps,
        CharacterSpec spec,
        List<SkillUsage> skills,
        List<BurstWindow> bursts,
        AnalysisContext reference,
        JobStatistics statistics
) {

    public AnalysisContext {
        skills = List.copyOf(skills);
        bursts = List.copyOf(bursts);
    }

    public static AnalysisContext single(long playTimeMs, long totalDps, CharacterSpec spec,
                                         List<SkillUsage> skills, List<BurstWindow> bursts) {
        return new AnalysisContext(playTimeMs, totalDps, spec, skills, bursts, null, null);
    }

    public AnalysisContext comparedWith(AnalysisContext reference) {
        if (reference.reference() != null) {
            throw new IllegalArgumentException("기준 기록은 단일 기록이어야 합니다.");
        }
        return new AnalysisContext(playTimeMs, totalDps, spec, skills, bursts, reference, statistics);
    }

    public AnalysisContext withStatistics(JobStatistics statistics) {
        return new AnalysisContext(playTimeMs, totalDps, spec, skills, bursts, reference, statistics);
    }

    public Optional<AnalysisContext> referenceContext() {
        return Optional.ofNullable(reference);
    }

    /** 표본이 충분한 통계만 돌려준다. */
    public Optional<JobStatistics> reliableStatistics() {
        return Optional.ofNullable(statistics).filter(JobStatistics::isReliable);
    }

    public double playTimeMinutes() {
        return playTimeMs / 60_000.0;
    }

    public CooldownStats cooldownStats() {
        return spec.cooldownStats();
    }

    public Optional<SkillUsage> find(String baseName) {
        return skills.stream().filter(skill -> skill.baseName().equals(baseName)).findFirst();
    }

    public Optional<BurstWindow> burstAt(long timeMs) {
        return bursts.stream().filter(burst -> burst.contains(timeMs)).findFirst();
    }

    /** 데미지를 "평균 DPS로 몇 초 치"로 바꾼다. 스펙이 다른 기록끼리 비교할 수 있는 단위다. */
    public double toSeconds(double damage) {
        return totalDps <= 0 ? 0 : damage / totalDps;
    }

    /** 기준 기록의 시전 수를 이 기록의 전투 시간에 맞춘 배율 */
    public double playTimeScaleTo(AnalysisContext other) {
        return other.playTimeMs() <= 0 ? 1 : (double) playTimeMs / other.playTimeMs();
    }
}
