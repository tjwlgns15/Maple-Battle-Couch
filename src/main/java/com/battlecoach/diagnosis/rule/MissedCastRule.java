package com.battlecoach.diagnosis.rule;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.battlecoach.diagnosis.domain.AnalysisContext;
import com.battlecoach.diagnosis.domain.BurstWindow;
import com.battlecoach.diagnosis.domain.DiagnosisRule;
import com.battlecoach.diagnosis.domain.Finding;
import com.battlecoach.diagnosis.domain.FindingType;
import com.battlecoach.diagnosis.domain.SkillUsage;

/**
 * 쿨이 돌아온 뒤 쓰지 않은 시간을 모두 더해 실효 쿨로 나누면 놓친 시전 수다.
 * 놓친 시전 × 1회 평균 데미지를 초로 환산한 값이 영향도다.
 * 쓰지 않은 구간이 극딜 구간 안에서 끝났으면 "극딜 대기"로 따로 적는다. 의도한 대기일 수 있어서다.
 * 대상 스킬 기준은 {@link CooldownEligibility} 를 따른다.
 */
@Component
public class MissedCastRule implements DiagnosisRule {

    /** 전투 끝에 이만큼도 남지 않았으면 써도 의미가 없다고 본다. 지속시간이 있으면 그 값을 쓴다. */
    static final long MIN_TAIL_MS = 5_000;

    @Override
    public List<Finding> evaluate(AnalysisContext context) {
        List<Finding> findings = new ArrayList<>();
        for (SkillUsage skill : context.skills()) {
            if (skill.castCount() == 0 || !CooldownEligibility.hasTrackableCooldown(skill)) {
                continue;
            }
            if (CooldownEligibility.hasDynamicCooldown(skill, context)) {
                findings.add(Finding.unmeasured(FindingType.EXCLUDED_DYNAMIC_COOLDOWN, skill, String.format(Locale.ROOT,
                        "실효 쿨보다 일찍 쓴 비율이 %.0f%%로 미적용 확률(%.0f%%)보다 높아, 쿨이 실행 중에 바뀌는 스킬로 보고 진단에서 뺐습니다.",
                        skill.earlyIntervalRatio() * 100, context.cooldownStats().resetChancePercent())));
                continue;
            }
            idleOf(skill, context).toFinding(skill, context).ifPresent(findings::add);
        }
        return findings;
    }

    /**
     * 전투 시작부터 모든 스킬이 사용 가능하다고 본다. 미적용으로 일찍 쓴 시전은 쉰 시간이 없다.
     * 전투 중간에 쉰 시간은 모두 더해 쿨로 나눈다. 마지막으로 쿨이 돈 뒤 전투 끝까지 남은 시간은
     * 효과가 들어갈 시간({@link #MIN_TAIL_MS} 또는 스킬 지속시간 중 긴 쪽)이 남았을 때만 놓친 시전으로 센다.
     * (칼리얏의 120초 버프는 전투 종료 6초 전에 쿨이 돌았지만 지속시간 30~60초라 세지 않는다)
     */
    private static Idle idleOf(SkillUsage skill, AnalysisContext context) {
        long cooldown = skill.effectiveCooldownMs();
        long readyAt = 0;
        long heldForBurst = 0;
        long unused = 0;
        for (long castAt : skill.castTimesMs()) {
            if (castAt > readyAt) {
                Optional<BurstWindow> burst = context.burstAt(castAt);
                if (burst.isPresent() && readyAt < burst.get().startMs()) {
                    heldForBurst += castAt - readyAt;
                } else {
                    unused += castAt - readyAt;
                }
            }
            readyAt = castAt + cooldown;
        }
        long tail = Math.max(0, context.playTimeMs() - readyAt);
        long minUseful = Math.max(MIN_TAIL_MS, skill.durationMs() == null ? 0 : skill.durationMs());
        return new Idle(cooldown, heldForBurst, unused, tail, minUseful);
    }

    private record Idle(long cooldownMs, long heldForBurstMs, long unusedMs, long tailMs, long minUsefulTailMs) {

        long totalMs() {
            return heldForBurstMs + unusedMs + tailMs;
        }

        int missedCasts() {
            int middle = (int) ((heldForBurstMs + unusedMs) / cooldownMs);
            int tail = tailMs < minUsefulTailMs ? 0 : (int) ((tailMs - minUsefulTailMs) / cooldownMs) + 1;
            return middle + tail;
        }

        Optional<Finding> toFinding(SkillUsage skill, AnalysisContext context) {
            int missed = missedCasts();
            if (missed == 0) {
                return Optional.empty();
            }
            String message = String.format(Locale.ROOT,
                    "실효 쿨 %.1f초인데 쿨이 돈 뒤 쓰지 않은 시간이 %.1f초라 약 %d회를 놓쳤습니다. (극딜 대기 %.1f초, 그 외 %.1f초, 전투 종료 전 %.1f초)",
                    seconds(cooldownMs), seconds(totalMs()), missed,
                    seconds(heldForBurstMs), seconds(unusedMs), seconds(tailMs));
            if (skill.damage() == null || skill.damage() <= 0) {
                return Optional.of(Finding.unmeasured(FindingType.MISSED_CAST, skill,
                        message + " 데미지가 없는 스킬(버프 등)이라 영향도는 계산하지 않았습니다."));
            }
            double lostDamage = (double) skill.damage() / skill.castCount() * missed;
            return Optional.of(Finding.measured(FindingType.MISSED_CAST, skill, context.toSeconds(lostDamage), message));
        }

        private static double seconds(long ms) {
            return ms / 1000.0;
        }
    }
}
