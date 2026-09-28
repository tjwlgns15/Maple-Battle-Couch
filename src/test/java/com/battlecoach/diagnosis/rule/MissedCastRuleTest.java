package com.battlecoach.diagnosis.rule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.battlecoach.diagnosis.domain.AnalysisContext;
import com.battlecoach.diagnosis.domain.BurstWindow;
import com.battlecoach.diagnosis.domain.Finding;
import com.battlecoach.diagnosis.domain.FindingType;
import com.battlecoach.diagnosis.domain.SkillUsage;
import com.battlecoach.spec.domain.CharacterSpec;
import com.battlecoach.spec.domain.CooldownStats;

class MissedCastRuleTest {

    private static final long PLAY_TIME_MS = 100_000;
    private static final long TOTAL_DPS = 1_000;
    private static final CooldownStats STATS = CooldownStats.of(5, 6, 27, 0);

    private final MissedCastRule rule = new MissedCastRule();

    @Test
    void 쿨마다_쓰면_놓친_시전이_없다() {
        SkillUsage skill = skill("판데모니움", 20_000L, 5_000L, 0, 20_000, 40_000, 60_000, 80_000);

        assertThat(rule.evaluate(context(List.of(), skill))).isEmpty();
    }

    @Test
    void 쓰지_않은_시간을_누적해_놓친_시전과_초_환산_영향도를_계산한다() {
        // 쿨 20초: 0초 사용 → 20초부터 가능했지만 50초에 사용(30초 쉼) → 70초부터 가능, 전투 종료 100초(30초 남음)
        SkillUsage skill = skill("판데모니움", 20_000L, 10_000L, 0, 50_000);

        List<Finding> findings = rule.evaluate(context(List.of(), skill));

        // 중간 30초 / 쿨 20초 = 1회, 끝 30초에서 70·90초 두 번 가능 = 2회 → 3회
        // 1회 데미지 10,000/2 = 5,000 → 15,000 / DPS 1,000 = 15초
        assertThat(findings).singleElement().satisfies(finding -> {
            assertThat(finding.type()).isEqualTo(FindingType.MISSED_CAST);
            assertThat(finding.impactSeconds()).isCloseTo(15.0, within(1e-9));
            assertThat(finding.message()).contains("약 3회").contains("극딜 대기 0.0초").contains("그 외 30.0초").contains("전투 종료 전 30.0초");
        });
    }

    @Test
    void 극딜_구간_안에서_끝난_대기는_극딜_대기로_나눈다() {
        // 20초부터 가능했지만 극딜(45~75초) 안인 50초에 사용 → 30초는 극딜 대기
        SkillUsage skill = skill("판데모니움", 20_000L, 10_000L, 0, 50_000, 70_000, 90_000);
        BurstWindow burst = new BurstWindow(45_000, 75_000);

        List<Finding> findings = rule.evaluate(context(List.of(burst), skill));

        assertThat(findings).singleElement()
                .extracting(Finding::message)
                .asString()
                .contains("약 1회")
                .contains("극딜 대기 30.0초")
                .contains("그 외 0.0초");
    }

    @Test
    void 전투_끝에_남은_시간이_지속시간보다_짧으면_놓친_시전으로_세지_않는다() {
        // 쿨 40초, 지속 30초 버프: 0·40초 사용 → 80초에 쿨이 돌고 전투 종료까지 20초 → 30초 효과가 다 들어가지 않는다
        SkillUsage buff = new SkillUsage("레이스 오브 갓", "레이스 오브 갓", List.of(0L, 40_000L), 1_000L, 40_000L, 30_000L);
        // 지속시간이 짧으면(3초) 20초 남은 것도 1회로 센다
        SkillUsage attack = new SkillUsage("판데모니움", "판데모니움", List.of(0L, 40_000L), 1_000L, 40_000L, 3_000L);

        assertThat(rule.evaluate(context(List.of(), buff))).isEmpty();
        assertThat(rule.evaluate(context(List.of(), attack))).singleElement()
                .extracting(Finding::message).asString().contains("약 1회");
    }

    @Test
    void 데미지가_없는_버프는_영향도를_계산하지_않는다() {
        SkillUsage buff = skill("레디 투 다이", 20_000L, null, 0, 50_000);

        assertThat(rule.evaluate(context(List.of(), buff))).singleElement()
                .satisfies(finding -> {
                    assertThat(finding.isMeasured()).isFalse();
                    assertThat(finding.message()).contains("영향도는 계산하지 않았습니다");
                });
    }

    @Test
    void 이른_사용_비율이_미적용_확률보다_훨씬_높으면_진단에서_뺀다() {
        // 쿨 12초인데 간격 5개 중 4개가 1초 → 80% > 27% + 20%p
        SkillUsage dynamic = skill("차크람 퓨리", 12_000L, 10_000L, 0, 1_000, 2_000, 3_000, 4_000, 90_000);

        assertThat(rule.evaluate(context(List.of(), dynamic))).singleElement()
                .extracting(Finding::type)
                .isEqualTo(FindingType.EXCLUDED_DYNAMIC_COOLDOWN);
    }

    @Test
    void 간격이_적으면_이른_사용이_있어도_제외하지_않는다() {
        // 3회 중 1회만 미적용으로 일찍 사용 (보이드 버스트 사례)
        SkillUsage skill = skill("보이드 버스트", 40_000L, 10_000L, 0, 30_000, 70_000);

        assertThat(rule.evaluate(context(List.of(), skill)))
                .extracting(Finding::type)
                .doesNotContain(FindingType.EXCLUDED_DYNAMIC_COOLDOWN);
    }

    @Test
    void 쿨이_짧거나_모르는_스킬은_보지_않는다() {
        SkillUsage shortCooldown = skill("크레센텀", 4_700L, 10_000L, 0, 50_000);
        SkillUsage noCooldown = skill("플러리", null, 10_000L, 0, 50_000);

        assertThat(rule.evaluate(context(List.of(), shortCooldown, noCooldown))).isEmpty();
    }

    private static SkillUsage skill(String name, Long cooldownMs, Long damage, long... castTimes) {
        return new SkillUsage(name, name, java.util.Arrays.stream(castTimes).boxed().toList(), damage, cooldownMs, null);
    }

    private static AnalysisContext context(List<BurstWindow> bursts, SkillUsage... skills) {
        return AnalysisContext.single(PLAY_TIME_MS, TOTAL_DPS, CharacterSpec.of(STATS, List.of()), List.of(skills), bursts);
    }

    @Test
    void 지속시간_안에_다시_누른_입력은_앞_시전에_합친다() {
        // 불굴의 결의(지속 10초): 0초 발동 → 2.3초에 다시 눌러 종료 → 110초 발동 → 112.4초 종료
        List<Long> merged = SkillUsage.mergeReactivations(List.of(0L, 2_300L, 110_000L, 112_400L), 10_000);

        assertThat(merged).containsExactly(0L, 110_000L);
    }
}
