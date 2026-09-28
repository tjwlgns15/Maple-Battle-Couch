package com.battlecoach.spec.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class StandardCooldownCalculatorTest {

    private final StandardCooldownCalculator calculator = new StandardCooldownCalculator();

    @ParameterizedTest(name = "기본 {0}ms, {1}초, {2}% → {3}ms")
    @CsvSource({
            // 10초 초과: % 먼저, 그다음 초를 그대로 뺀다 (칼리 실측 대상)
            "30000, 5, 6, 23200",
            "30000, 4, 6, 24200",
            "60000, 4, 6, 52400",
            "116000, 4, 6, 105040",
            // 10초 이하: % 후 1초당 5% 곱 (인벤 정리 글 예시 8초, 6%, 4초 → 6.016초)
            "8000, 4, 6, 6016",
            // % 후 10초를 넘는 부분만 그대로 빼고 나머지는 1초당 5% (12초, 5%, 2초 → 9.7초)
            "12000, 2, 5, 9700",
            // 초 감소로는 5초 미만이 되지 않는다
            "6000, 5, 6, 5000",
            "5000, 5, 6, 4700",
            // 쿨감이 없으면 그대로
            "30000, 0, 0, 30000"
    })
    void 커뮤니티_규칙으로_실효_쿨을_계산한다(long baseMs, double seconds, double percent, long expectedMs) {
        SkillSpec spec = spec(baseMs, true);

        assertThat(calculator.effectiveCooldownMs(spec, CooldownStats.of(seconds, percent, 0, 0)))
                .isEqualTo(expectedMs);
    }

    @Test
    void 쿨감을_받지_않는_스킬은_기본_쿨_그대로다() {
        assertThat(calculator.effectiveCooldownMs(spec(120_000, false), CooldownStats.of(5, 6, 0, 0)))
                .isEqualTo(120_000);
    }

    @Test
    void 쿨_표기가_없으면_계산할_수_없다() {
        SkillSpec noCooldown = new SkillSpec("스파크", "스파크", 5, null, null, true, true, false);

        assertThatThrownBy(() -> calculator.effectiveCooldownMs(noCooldown, CooldownStats.of(5, 6, 0, 0)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static SkillSpec spec(long baseMs, boolean reducible) {
        return new SkillSpec("테스트", "테스트", 30, baseMs, null, reducible, true, false);
    }
}
