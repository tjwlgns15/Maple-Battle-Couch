package com.battlecoach.diagnosis.rule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.battlecoach.diagnosis.domain.AnalysisContext;
import com.battlecoach.diagnosis.domain.Finding;
import com.battlecoach.diagnosis.domain.FindingType;
import com.battlecoach.diagnosis.domain.SkillUsage;
import com.battlecoach.spec.domain.CharacterSpec;
import com.battlecoach.spec.domain.CooldownStats;
import com.battlecoach.spec.domain.SkillSpec;

/** 비교 규칙(CastCountGapRule, LoadoutRule). 칼리 B(나) vs 칼리 A(기준) 사례를 단순화했다. */
class ComparisonRuleTest {

    private static final CooldownStats STATS = CooldownStats.of(4, 6, 27, 0);

    private final CastCountGapRule castCountGapRule = new CastCountGapRule();
    private final LoadoutRule loadoutRule = new LoadoutRule();

    @Test
    void 기준_기록이_없으면_비교_규칙은_아무것도_내지_않는다() {
        AnalysisContext single = context(300_000, 1_000, spec(), skill("스틱스", 52_000L, 3_000L, 0));

        assertThat(castCountGapRule.evaluate(single)).isEmpty();
        assertThat(loadoutRule.evaluate(single)).isEmpty();
    }

    @Test
    void 기준보다_적게_쓴_시전_수를_전투_시간으로_보정해_내_1회_초_환산으로_영향도를_낸다() {
        // 나: 300초 동안 스틱스 3회, 데미지 3,000 → 1회 1,000 / DPS 1,000 = 1초
        AnalysisContext mine = context(300_000, 1_000, spec(),
                skill("스틱스", 52_000L, 3_000L, 5_000, 115_000, 225_000),
                skill("레디 투 다이", 52_000L, null, 1_000, 55_000, 110_000, 165_000, 220_000, 275_000));
        // 기준: 600초 동안 12회 → 내 전투 시간으로 보정하면 6회. 레디 투 다이와 늘 함께 썼다.
        AnalysisContext ref = context(600_000, 5_000, spec(),
                skill("스틱스", 52_000L, 60_000L, times(12, 2_000, 52_500)),
                skill("레디 투 다이", 52_000L, null, times(12, 1_800, 52_500)));

        List<Finding> findings = castCountGapRule.evaluate(mine.comparedWith(ref));

        assertThat(findings).singleElement().satisfies(finding -> {
            assertThat(finding.type()).isEqualTo(FindingType.CAST_COUNT_GAP);
            assertThat(finding.skillBaseName()).isEqualTo("스틱스");
            assertThat(finding.impactSeconds()).isCloseTo(3.0, within(1e-9)); // 3회 부족 × 1초
            assertThat(finding.message())
                    .contains("전투 시간 보정 6.0회")
                    .contains("레디 투 다이와 1초 안에 12/12회")
                    .contains("이 기록은 0/3회");
        });
    }

    @Test
    void 기준보다_많이_썼거나_비교할_수_없는_스킬은_내지_않는다() {
        AnalysisContext mine = context(300_000, 1_000, spec(),
                skill("판데모니움", 24_000L, 14_000L, times(14, 0, 24_000)),
                skill("크레센텀", 4_700L, 1_000L, 0));
        AnalysisContext ref = context(300_000, 1_000, spec(),
                skill("판데모니움", 23_000L, 12_000L, times(12, 0, 25_000)),
                skill("크레센텀", 4_700L, 1_000L, times(50, 0, 5_000)));

        assertThat(castCountGapRule.evaluate(mine.comparedWith(ref))).isEmpty();
    }

    @Test
    void 기준이_쓴_스킬을_보유하지_않았으면_미보유_보유했으면_미사용으로_나눈다() {
        CharacterSpec mySpec = spec("불굴의 결의");
        CharacterSpec refSpec = spec("플레게톤", "불굴의 결의");
        AnalysisContext mine = context(300_000, 1_000, mySpec);
        AnalysisContext ref = context(300_000, 2_000, refSpec,
                skill("플레게톤", 108_000L, 4_000L, 0, 110_000, 220_000),
                skill("불굴의 결의", 108_000L, null, 0, 110_000),
                skill("소울 컨트랙트", 90_000L, 2_000L, 0)); // 두 캐릭터 스킬 목록에 모두 없다

        List<Finding> findings = loadoutRule.evaluate(mine.comparedWith(ref));

        assertThat(findings).extracting(Finding::skillBaseName, Finding::type).containsExactly(
                org.assertj.core.groups.Tuple.tuple("플레게톤", FindingType.MISSING_SKILL),
                org.assertj.core.groups.Tuple.tuple("불굴의 결의", FindingType.UNUSED_SKILL));
        assertThat(findings.get(0).impactSeconds()).isCloseTo(2.0, within(1e-9)); // 기준 4,000 / 2,000
        assertThat(findings.get(1).isMeasured()).isFalse();
    }

    private static CharacterSpec spec(String... owned) {
        return CharacterSpec.of(STATS, Arrays.stream(owned)
                .map(name -> new SkillSpec(name, name, 30, 120_000L, null, true, true, false, false))
                .toList());
    }

    private static AnalysisContext context(long playTimeMs, long dps, CharacterSpec spec, SkillUsage... skills) {
        return AnalysisContext.single(playTimeMs, dps, spec, List.of(skills), List.of());
    }

    private static SkillUsage skill(String name, Long cooldownMs, Long damage, long... castTimes) {
        return new SkillUsage(name, name, Arrays.stream(castTimes).boxed().toList(), damage, cooldownMs, null);
    }

    private static long[] times(int count, long start, long step) {
        long[] times = new long[count];
        for (int i = 0; i < count; i++) {
            times[i] = start + i * step;
        }
        return times;
    }
}
