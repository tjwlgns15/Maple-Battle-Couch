package com.battlecoach.diagnosis.sequence;

import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Component;

import com.battlecoach.diagnosis.domain.AnalysisContext;
import com.battlecoach.diagnosis.domain.BurstWindow;
import com.battlecoach.diagnosis.domain.SkillUsage;

/**
 * 극딜 구간 앞뒤에서 쿨이 긴 스킬을 처음 쓴 순서를 뽑는다.
 * 극딜 직전에 까는 설치기(데스 블로섬 등)도 들어가도록 구간 시작 조금 전부터 본다.
 * 기록마다 헥사 접미사 표기가 달라("매직 서킷 풀드라이브" / "… VI") baseName 으로 뽑는다.
 */
@Component
public class BurstOrderExtractor {

    static final long BEFORE_BURST_MS = 5_000;
    static final long AFTER_BURST_START_MS = 15_000;
    /** 계속 쓰는 짧은 쿨 스킬은 순서 비교에서 잡음이라 뺀다. */
    static final long MIN_COOLDOWN_MS = 10_000;

    /** 극딜 한 번 안에서 스킬을 처음 쓴 시각 */
    public record BurstCast(String baseName, long timeMs) {
    }

    /** @return 극딜 구간마다 시각 순으로 정렬한 첫 시전 목록 */
    public List<List<BurstCast>> burstCasts(AnalysisContext context) {
        return context.bursts().stream()
                .map(burst -> castsAround(context, burst))
                .filter(casts -> !casts.isEmpty())
                .toList();
    }

    public List<String> firstBurstOrder(AnalysisContext context) {
        return burstCasts(context).stream()
                .findFirst()
                .map(casts -> casts.stream().map(BurstCast::baseName).toList())
                .orElse(List.of());
    }

    private static List<BurstCast> castsAround(AnalysisContext context, BurstWindow burst) {
        long from = burst.startMs() - BEFORE_BURST_MS;
        long to = burst.startMs() + AFTER_BURST_START_MS;
        return context.skills().stream()
                .filter(SkillUsage::hasCooldown)
                .filter(skill -> skill.effectiveCooldownMs() >= MIN_COOLDOWN_MS)
                .flatMap(skill -> skill.castTimesMs().stream()
                        .filter(time -> from <= time && time <= to)
                        .findFirst()
                        .map(time -> new BurstCast(skill.baseName(), time))
                        .stream())
                .sorted(Comparator.comparingLong(BurstCast::timeMs))
                .toList();
    }
}
