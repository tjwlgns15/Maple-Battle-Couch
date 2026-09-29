package com.battlecoach.diagnosis.statistics;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalDouble;

import org.springframework.stereotype.Component;

import com.battlecoach.diagnosis.domain.AnalysisContext;
import com.battlecoach.diagnosis.domain.IdleBreakdown;
import com.battlecoach.diagnosis.domain.SkillUsage;

/**
 * 표본마다 한 스킬의 극딜 대기 시간, 분당 시전 수, 초 환산을 모아 순위 상관을 본다.
 * 칼리얏(판데모니움을 극딜까지 아껴 12회)과 연자히(쿨마다 14회) 두 기록에서 나온 가설을 표본으로 확인하기 위한 분석이다.
 */
@Component
public class HoldTradeoffAnalyzer {

    /** @param samples 라벨(캐릭터명 등) → 분석 컨텍스트 */
    public Optional<HoldTradeoff> analyze(String baseName, Map<String, AnalysisContext> samples) {
        List<HoldTradeoff.Point> points = new ArrayList<>();
        String skillName = null;
        for (Map.Entry<String, AnalysisContext> sample : samples.entrySet()) {
            AnalysisContext context = sample.getValue();
            Optional<SkillUsage> usage = context.find(baseName)
                    .filter(skill -> skill.castCount() > 0 && skill.hasCooldown());
            if (usage.isEmpty() || context.playTimeMinutes() <= 0) {
                continue;
            }
            SkillUsage skill = usage.get();
            skillName = skill.skillName();
            IdleBreakdown idle = IdleBreakdown.of(skill, context);
            points.add(new HoldTradeoff.Point(
                    sample.getKey(),
                    idle.heldForBurstMs() / 1000.0,
                    idle.unusedMs() / 1000.0,
                    skill.castCount(),
                    skill.castCount() / context.playTimeMinutes(),
                    skill.damage() == null ? null : context.toSeconds(skill.damage())));
        }
        if (points.isEmpty()) {
            return Optional.empty();
        }

        List<HoldTradeoff.Point> withSeconds = points.stream().filter(point -> point.seconds() != null).toList();
        return Optional.of(new HoldTradeoff(
                baseName,
                skillName,
                points,
                toNullable(Correlation.spearman(
                        withSeconds.stream().map(HoldTradeoff.Point::heldForBurstSeconds).toList(),
                        withSeconds.stream().map(HoldTradeoff.Point::seconds).map(Objects::requireNonNull).toList())),
                toNullable(Correlation.spearman(
                        points.stream().map(HoldTradeoff.Point::heldForBurstSeconds).toList(),
                        points.stream().map(HoldTradeoff.Point::castsPerMinute).toList()))));
    }

    private static Double toNullable(OptionalDouble value) {
        return value.isPresent() ? value.getAsDouble() : null;
    }
}
