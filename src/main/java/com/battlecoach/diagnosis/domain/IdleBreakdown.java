package com.battlecoach.diagnosis.domain;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * 스킬 하나가 쿨이 돈 뒤 쓰이지 않은 시간을 원인별로 나눈 것.
 * 놓친 시전 진단, "아낀 것 vs 놀린 것" 분석, 타임라인의 쉰 구간 표시가 함께 쓴다.
 * <ul>
 *   <li>전투 시작부터 모든 스킬이 사용 가능하다고 본다. 미적용으로 일찍 쓴 시전은 쉰 시간이 없다.</li>
 *   <li>쉰 구간이 극딜 구간 안에서 끝났고 극딜 전에 쿨이 돌았으면 "극딜 대기", 아니면 "그 외"다.</li>
 *   <li>마지막으로 쿨이 돈 뒤 전투 끝까지 남은 시간은 따로 둔다. 효과가 들어갈 시간({@link #MIN_TAIL_MS} 또는
 *       스킬 지속시간 중 긴 쪽)이 남았을 때만 놓친 시전으로 센다.
 *       (칼리 A의 120초 버프는 전투 종료 6초 전에 쿨이 돌았지만 지속시간 30~60초라 세지 않는다)</li>
 * </ul>
 *
 * @param spans 쉰 구간 전부(시간순). 합계 필드는 이 구간들을 종류별로 더한 값이다
 */
public record IdleBreakdown(
        long cooldownMs,
        long heldForBurstMs,
        long unusedMs,
        long tailMs,
        long minUsefulTailMs,
        List<IdleSpan> spans
) {

    /** 전투 끝에 이만큼도 남지 않았으면 써도 의미가 없다고 본다. 지속시간이 있으면 그 값을 쓴다. */
    public static final long MIN_TAIL_MS = 5_000;

    public IdleBreakdown {
        spans = List.copyOf(spans);
    }

    /** @param skill 실효 쿨을 아는 스킬 ({@link SkillUsage#hasCooldown()}) */
    public static IdleBreakdown of(SkillUsage skill, AnalysisContext context) {
        if (!skill.hasCooldown()) {
            throw new IllegalArgumentException("실효 쿨을 모르는 스킬입니다: " + skill.skillName());
        }
        long cooldown = skill.effectiveCooldownMs();
        long readyAt = 0;
        List<IdleSpan> spans = new ArrayList<>();
        for (long castAt : skill.castTimesMs()) {
            if (castAt > readyAt) {
                Optional<BurstWindow> burst = context.burstAt(castAt);
                boolean heldForBurst = burst.isPresent() && readyAt < burst.get().startMs();
                spans.add(new IdleSpan(readyAt, castAt, heldForBurst ? IdleSpan.Kind.HELD_FOR_BURST : IdleSpan.Kind.UNUSED));
            }
            readyAt = castAt + cooldown;
        }
        if (context.playTimeMs() > readyAt) {
            spans.add(new IdleSpan(readyAt, context.playTimeMs(), IdleSpan.Kind.TAIL));
        }
        long minUseful = Math.max(MIN_TAIL_MS, skill.durationMs() == null ? 0 : skill.durationMs());
        return new IdleBreakdown(cooldown, sum(spans, IdleSpan.Kind.HELD_FOR_BURST), sum(spans, IdleSpan.Kind.UNUSED),
                sum(spans, IdleSpan.Kind.TAIL), minUseful, spans);
    }

    private static long sum(List<IdleSpan> spans, IdleSpan.Kind kind) {
        return spans.stream().filter(span -> span.kind() == kind).mapToLong(IdleSpan::durationMs).sum();
    }

    public long totalMs() {
        return heldForBurstMs + unusedMs + tailMs;
    }

    public int missedCasts() {
        int middle = (int) ((heldForBurstMs + unusedMs) / cooldownMs);
        int tail = tailMs < minUsefulTailMs ? 0 : (int) ((tailMs - minUsefulTailMs) / cooldownMs) + 1;
        return middle + tail;
    }

    /** 전투 종료 전 구간이 놓친 시전으로 셀 만큼 긴지 */
    public boolean isTailUseful() {
        return tailMs >= minUsefulTailMs;
    }

    public Optional<IdleSpan> longest(IdleSpan.Kind kind) {
        return spans.stream().filter(span -> span.kind() == kind).max(Comparator.comparingLong(IdleSpan::durationMs));
    }
}
