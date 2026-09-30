package com.battlecoach.replay.application;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.battlecoach.diagnosis.burst.BurstDetector;
import com.battlecoach.diagnosis.domain.AnalysisContext;
import com.battlecoach.diagnosis.domain.SkillUsage;
import com.battlecoach.replay.application.dto.ReplayDetail;
import com.battlecoach.replay.application.dto.ReplayDetail.CastView;
import com.battlecoach.replay.application.dto.ReplayDetail.SkillStatView;
import com.battlecoach.replay.domain.ReplayId;
import com.battlecoach.spec.domain.CharacterSpec;
import com.battlecoach.spec.domain.CooldownCalculator;
import com.battlecoach.spec.domain.SkillSpec;

import lombok.RequiredArgsConstructor;

/** 기록(시전·데미지)과 스킬 스펙을 baseName 으로 묶어 진단 입력을 만든다. */
@Component
@RequiredArgsConstructor
public class AnalysisContextFactory {

    private final CharacterSpecLoader characterSpecLoader;
    private final CooldownCalculator cooldownCalculator;
    private final BurstDetector burstDetector;

    public AnalysisContext create(ReplayDetail replay) {
        CharacterSpec spec = characterSpecLoader.load(ReplayId.of(replay.replayId()));
        Map<String, Long> damageByBaseName = damageByBaseName(replay.skillStats());

        List<SkillUsage> skills = castsByBaseName(replay.casts()).entrySet().stream()
                .map(entry -> {
                    List<CastView> casts = entry.getValue();
                    SkillSpec skillSpec = spec.find(entry.getKey()).orElse(null);
                    Long cooldown = effectiveCooldown(skillSpec, spec);
                    Long duration = skillSpec == null ? null : skillSpec.effectiveDurationMs(spec.cooldownStats());
                    return new SkillUsage(
                            entry.getKey(),
                            casts.get(0).skillName(),
                            castTimes(casts, skillSpec, duration, cooldown),
                            damageByBaseName.get(entry.getKey()),
                            cooldown,
                            duration);
                })
                .toList();

        return AnalysisContext.single(
                replay.totalPlayTimeMs(),
                replay.totalDps(),
                spec,
                skills,
                burstDetector.detect(skills));
    }

    /**
     * 지속 중 다시 눌러 효과를 바꾸는 스킬은 다시 누른 입력을 시전으로 세지 않는다.
     * 합치는 범위는 지속시간과 실효 쿨 중 짧은 쪽이다. 버프 지속시간 증가로 지속이 쿨보다 길어지면(레디 투 다이 30초 × 1.79 = 54초,
     * 쿨 약 52초) 쿨이 돌아 새로 쓴 시전까지 합쳐 버리기 때문이다.
     */
    static List<Long> castTimes(List<CastView> casts, SkillSpec skillSpec, Long durationMs, Long cooldownMs) {
        List<Long> times = casts.stream().map(CastView::elapseMs).toList();
        if (skillSpec == null || !skillSpec.reactivatable() || durationMs == null) {
            return times;
        }
        long window = cooldownMs == null ? durationMs : Math.min(durationMs, cooldownMs - SkillUsage.EARLY_TOLERANCE_MS);
        return SkillUsage.mergeReactivations(times, window);
    }

    private Long effectiveCooldown(SkillSpec skillSpec, CharacterSpec spec) {
        if (skillSpec == null || !skillSpec.hasCooldown()) {
            return null;
        }
        return cooldownCalculator.effectiveCooldownMs(skillSpec, spec.cooldownStats());
    }

    /** result 는 "보이드 러쉬 VI", timeline 은 "보이드 러쉬" 라 baseName 으로 합친다. */
    static Map<String, Long> damageByBaseName(List<SkillStatView> stats) {
        Map<String, Long> damage = new LinkedHashMap<>();
        stats.forEach(stat -> damage.merge(stat.baseName(), stat.damage(), Long::sum));
        return damage;
    }

    /** 첫 시전 순서를 유지한다. 시퀀스 안에서는 elapseMs 와 기록 순서가 역전되므로 시각 → 기록 순서로 정렬한다. */
    private static Map<String, List<CastView>> castsByBaseName(List<CastView> casts) {
        Map<String, List<CastView>> byBaseName = new LinkedHashMap<>();
        casts.stream()
                .sorted(Comparator.comparingLong(CastView::elapseMs).thenComparingInt(CastView::recordedOrder))
                .forEach(cast -> byBaseName.computeIfAbsent(cast.baseName(), key -> new ArrayList<>()).add(cast));
        return byBaseName;
    }
}
