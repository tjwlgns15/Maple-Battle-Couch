package com.battlecoach.spec.domain;

import org.springframework.stereotype.Component;

/**
 * 커뮤니티에 정리된 쿨감 규칙 (공식 문서 없음, 인벤 2022-12 정리 글 기준).
 * <ol>
 *   <li>% 감소를 먼저 곱한다. 1초 미만으로는 줄지 않는다.</li>
 *   <li>초 감소는 10초를 넘는 부분까지만 그대로 뺀다.</li>
 *   <li>남은 초 감소는 1초당 5%씩 곱으로 줄인다. (예: 8초, 6%, 4초 → 8 × 0.94 × 0.8 = 6.016초)</li>
 *   <li>초 감소로는 5초 미만이 되지 않는다.</li>
 * </ol>
 * 칼리 2명 실측(판데모니움, 스틱스, 120초 버프류, 에르다 노바)에서 계산값이 실측 최소 간격 이하였다.
 * 옛 공식 (기본 − 초) × (1 − %) 은 연자히 판데모니움을 24.44초로 계산해 실측 24.24초보다 길었다.
 */
@Component
public class StandardCooldownCalculator implements CooldownCalculator {

    private static final double MIN_BY_PERCENT_MS = 1_000;
    private static final double MIN_BY_SECONDS_MS = 5_000;
    private static final double DIRECT_REDUCTION_FLOOR_MS = 10_000;
    private static final double PERCENT_PER_REMAINING_SECOND = 0.05;

    @Override
    public long effectiveCooldownMs(SkillSpec spec, CooldownStats stats) {
        if (!spec.hasCooldown()) {
            throw new IllegalArgumentException("쿨 표기가 없는 스킬입니다: " + spec.skillName());
        }
        double base = spec.baseCooldownMs();
        if (!spec.cooldownReducible()) {
            return Math.round(base);
        }

        double afterPercent = Math.max(
                base * (1 - stats.reductionPercent() / 100),
                Math.min(base, MIN_BY_PERCENT_MS));

        double secondsMs = stats.reductionSeconds() * 1000;
        double direct = Math.min(secondsMs, Math.max(0, afterPercent - DIRECT_REDUCTION_FLOOR_MS));
        double remainingSeconds = (secondsMs - direct) / 1000;
        double afterSeconds = (afterPercent - direct) * (1 - PERCENT_PER_REMAINING_SECOND * remainingSeconds);

        return Math.round(Math.max(afterSeconds, Math.min(afterPercent, MIN_BY_SECONDS_MS)));
    }
}
