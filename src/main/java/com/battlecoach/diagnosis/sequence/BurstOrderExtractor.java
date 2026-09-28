package com.battlecoach.diagnosis.sequence;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.battlecoach.diagnosis.domain.AnalysisContext;
import com.battlecoach.diagnosis.domain.BurstWindow;
import com.battlecoach.diagnosis.domain.SkillUsage;

/**
 * 첫 극딜 구간 앞뒤에서 쿨이 긴 스킬을 처음 쓴 순서를 뽑는다.
 * 극딜 직전에 까는 설치기(데스 블로섬 등)도 들어가도록 구간 시작 조금 전부터 본다.
 * 기록마다 헥사 접미사 표기가 달라("매직 서킷 풀드라이브" / "… VI") baseName 으로 뽑는다.
 */
@Component
public class BurstOrderExtractor {

    static final long BEFORE_BURST_MS = 5_000;
    static final long AFTER_BURST_START_MS = 15_000;
    /** 계속 쓰는 짧은 쿨 스킬은 순서 비교에서 잡음이라 뺀다. */
    static final long MIN_COOLDOWN_MS = 10_000;

    public List<String> firstBurstOrder(AnalysisContext context) {
        Optional<BurstWindow> first = context.bursts().stream().findFirst();
        if (first.isEmpty()) {
            return List.of();
        }
        long from = first.get().startMs() - BEFORE_BURST_MS;
        long to = first.get().startMs() + AFTER_BURST_START_MS;

        return context.skills().stream()
                .filter(SkillUsage::hasCooldown)
                .filter(skill -> skill.effectiveCooldownMs() >= MIN_COOLDOWN_MS)
                .flatMap(skill -> skill.castTimesMs().stream()
                        .filter(time -> from <= time && time <= to)
                        .findFirst()
                        .map(time -> new FirstCast(skill.baseName(), time))
                        .stream())
                .sorted(Comparator.comparingLong(FirstCast::timeMs))
                .map(FirstCast::baseName)
                .toList();
    }

    private record FirstCast(String baseName, long timeMs) {
    }
}
