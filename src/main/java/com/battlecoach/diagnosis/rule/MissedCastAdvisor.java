package com.battlecoach.diagnosis.rule;

import java.util.Locale;
import java.util.Optional;

import com.battlecoach.diagnosis.domain.AnalysisContext;
import com.battlecoach.diagnosis.domain.BurstWindow;
import com.battlecoach.diagnosis.domain.IdleBreakdown;
import com.battlecoach.diagnosis.domain.IdleSpan;
import com.battlecoach.diagnosis.domain.SkillUsage;

/**
 * 쉰 시간의 가장 큰 원인에 맞춰 처방 문장을 고른다. 놓친 시전과 "비교 대상보다 적은 시전" 규칙이 함께 쓴다.
 * <ul>
 *   <li>그 외: 쿨이 돌면 바로 쓰라고 하고, 가장 길게 쉰 구간을 알려 준다.</li>
 *   <li>극딜 대기: 대기 구간이 쿨보다 길어 극딜 전에 한 번 더 쓰고도 극딜에 쿨이 돌아오면 그 시각을 알려 준다.</li>
 *   <li>전투 종료 전: 마지막까지 쓰라고 한다.</li>
 * </ul>
 */
final class MissedCastAdvisor {

    private MissedCastAdvisor() {
    }

    /**
     * 시전 수가 기준(기준 기록, 비교 대상)보다 적을 때. 기준이 함께 쓰는 스킬을 따로 썼으면 시퀀스에 넣으라고 하고,
     * 아니면 쉰 시간의 원인으로 처방한다.
     *
     * @param separatedPartner 기준은 함께 쓰는데 이 기록은 따로 쓴 스킬 이름. 없으면 null
     * @param referenceLabel   기준을 부르는 말("기준 기록", "비교 대상 대부분")
     */
    static Optional<String> adviseShortfall(SkillUsage skill, AnalysisContext context, String separatedPartner,
                                            String referenceLabel) {
        Optional<String> pairAdvice = Optional.ofNullable(separatedPartner).map(partner -> String.format(Locale.ROOT,
                "%s 같은 시퀀스(매크로)에 넣으면 %s처럼 함께 나갑니다.", KoreanJosa.withAnd(partner), referenceLabel));
        // 쉰 시간이 쿨 1회분도 안 되면 부족분의 원인이 이 기록의 공백이 아니다(미적용 발동, 전투 시간 차이 등).
        Optional<String> idleAdvice = Optional.of(skill)
                .filter(SkillUsage::hasCooldown)
                .map(s -> IdleBreakdown.of(s, context))
                .filter(idle -> idle.missedCasts() > 0)
                .flatMap(idle -> advise(idle, context));
        if (pairAdvice.isPresent() && idleAdvice.isPresent()) {
            return Optional.of(pairAdvice.get() + " " + idleAdvice.get());
        }
        return pairAdvice.or(() -> idleAdvice);
    }

    static Optional<String> advise(IdleBreakdown idle, AnalysisContext context) {
        long useful = idle.isTailUseful() ? idle.tailMs() : 0;
        if (idle.heldForBurstMs() == 0 && idle.unusedMs() == 0 && useful == 0) {
            return Optional.empty();
        }
        if (useful >= idle.unusedMs() && useful >= idle.heldForBurstMs()) {
            return Optional.of(String.format(Locale.ROOT,
                    "전투 종료 %.0f초 전에 쿨이 돌았는데 쓰지 않았습니다. 끝까지 쿨마다 쓰세요.", seconds(idle.tailMs())));
        }
        if (idle.unusedMs() >= idle.heldForBurstMs()) {
            return idle.longest(IdleSpan.Kind.UNUSED).map(span -> String.format(Locale.ROOT,
                    "쿨이 돌면 바로 쓰세요. 가장 길게 쉰 구간은 %s(%.0f초)입니다.", range(span), seconds(span.durationMs())));
        }
        return idle.longest(IdleSpan.Kind.HELD_FOR_BURST).map(span -> heldAdvice(span, idle.cooldownMs(), context));
    }

    private static String heldAdvice(IdleSpan span, long cooldownMs, AnalysisContext context) {
        Optional<Long> burstStart = context.burstAt(span.endMs()).map(BurstWindow::startMs);
        if (burstStart.isPresent() && span.startMs() + cooldownMs <= burstStart.get()) {
            return String.format(Locale.ROOT,
                    "극딜을 기다리며 %.0f초를 쉬었습니다. %.0f초에 한 번 쓰면 극딜 시작(%.0f초) 전에 쿨이 다시 돌아 극딜에도 쓸 수 있습니다.",
                    seconds(span.durationMs()), seconds(span.startMs()), seconds(burstStart.get()));
        }
        return "극딜을 기다리며 쉰 시간이 쌓여 1회 이상을 놓쳤습니다. 극딜 사이에 한 번 더 넣을 수 있는지 사이클을 확인해 보세요.";
    }

    static String range(IdleSpan span) {
        return String.format(Locale.ROOT, "%.0f~%.0f초", seconds(span.startMs()), seconds(span.endMs()));
    }

    private static double seconds(long ms) {
        return ms / 1000.0;
    }
}
