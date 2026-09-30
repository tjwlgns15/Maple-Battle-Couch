package com.battlecoach.diagnosis.rule;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.battlecoach.diagnosis.domain.AnalysisContext;
import com.battlecoach.diagnosis.domain.DiagnosisRule;
import com.battlecoach.diagnosis.domain.Finding;
import com.battlecoach.diagnosis.domain.FindingType;
import com.battlecoach.diagnosis.domain.IdleBreakdown;
import com.battlecoach.diagnosis.domain.MissedPlacement;
import com.battlecoach.diagnosis.domain.SkillUsage;

/**
 * 쿨이 돌아온 뒤 쓰지 않은 시간을 모두 더해 실효 쿨로 나누면 놓친 시전 수다({@link IdleBreakdown}).
 * 놓친 시전 × 1회 평균 데미지를 초로 환산한 값이 영향도다.
 * 쓰지 않은 구간이 극딜 구간 안에서 끝났으면 "극딜 대기"로 따로 적는다. 의도한 대기일 수 있어서다.
 * 내 기록만 보는 절대 기준이라 실효 쿨 15초 이상 스킬만 본다({@link CooldownEligibility#MIN_ABSOLUTE_COOLDOWN_MS}).
 */
@Component
public class MissedCastRule implements DiagnosisRule {

    @Override
    public List<Finding> evaluate(AnalysisContext context) {
        List<Finding> findings = new ArrayList<>();
        for (SkillUsage skill : context.skills()) {
            if (skill.castCount() == 0 || !CooldownEligibility.hasAbsolutelyTrackableCooldown(skill)) {
                continue;
            }
            if (CooldownEligibility.hasDynamicCooldown(skill, context)) {
                findings.add(Finding.unmeasured(FindingType.EXCLUDED_DYNAMIC_COOLDOWN, skill, String.format(Locale.ROOT,
                        "실효 쿨보다 일찍 쓴 비율이 %.0f%%로 미적용 확률(%.0f%%)보다 높아, 쿨이 실행 중에 바뀌는 스킬로 보고 진단에서 뺐습니다.",
                        skill.earlyIntervalRatio() * 100, context.cooldownStats().resetChancePercent())));
                continue;
            }
            toFinding(skill, context, IdleBreakdown.of(skill, context)).ifPresent(findings::add);
        }
        return findings;
    }

    private static Optional<Finding> toFinding(SkillUsage skill, AnalysisContext context, IdleBreakdown idle) {
        int missed = idle.missedCasts();
        if (missed == 0) {
            return Optional.empty();
        }
        String message = String.format(Locale.ROOT,
                "실효 쿨 %.1f초인데 쿨이 돈 뒤 쓰지 않은 시간이 %.1f초라 약 %d회를 놓쳤습니다. (극딜 대기 %.1f초, 그 외 %.1f초, 전투 종료 전 %.1f초)",
                seconds(idle.cooldownMs()), seconds(idle.totalMs()), missed,
                seconds(idle.heldForBurstMs()), seconds(idle.unusedMs()), seconds(idle.tailMs()))
                + placementSentence(idle.placement(), context);
        String advice = MissedCastAdvisor.advise(idle, context).orElse(null);
        if (skill.damage() == null || skill.damage() <= 0) {
            return Optional.of(Finding.unmeasured(FindingType.MISSED_CAST, skill,
                    message + " 데미지가 없는 스킬(버프 등)이라 영향도는 계산하지 않았습니다.").withAdvice(advice));
        }
        double lostDamage = (double) skill.damage() / skill.castCount() * missed;
        return Optional.of(Finding.measured(FindingType.MISSED_CAST, skill, context.toSeconds(lostDamage), message)
                .withAdvice(advice));
    }

    /**
     * 극딜 안팎 위치와 영향도 오차의 방향. result 는 스킬별 합계만 줘서 극딜 안팎 1회 데미지를 나눌 수 없으므로
     * 숫자 범위 대신 방향만 알린다.
     */
    static String placementSentence(MissedPlacement placement, AnalysisContext context) {
        if (context.bursts().isEmpty() || placement.inBurst() + placement.outOfBurst() == 0) {
            return "";
        }
        StringBuilder sentence = new StringBuilder(String.format(Locale.ROOT,
                " 쿨이 돈 시각부터 썼다면 극딜 안 %d회, 밖 %d회", placement.inBurst(), placement.outOfBurst()));
        if (placement.unplaced() > 0) {
            sentence.append(String.format(Locale.ROOT, "(여러 공백을 합친 %d회는 위치 미정)", placement.unplaced()));
        }
        sentence.append("입니다.");
        if (placement.outOfBurst() > 0 && placement.inBurst() == 0) {
            sentence.append(" 영향도는 평균 1회 데미지로 계산해서 실제 손해는 이보다 작을 수 있습니다.");
        } else if (placement.inBurst() > 0 && placement.outOfBurst() == 0) {
            sentence.append(" 영향도는 평균 1회 데미지로 계산해서 실제 손해는 이보다 클 수 있습니다.");
        } else {
            sentence.append(" 영향도는 평균 1회 데미지로 계산해서, 극딜 밖 몫은 실제보다 크게, 극딜 안 몫은 작게 잡혔을 수 있습니다.");
        }
        return sentence.toString();
    }

    private static double seconds(long ms) {
        return ms / 1000.0;
    }
}
