package com.battlecoach.diagnosis.rule;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.battlecoach.diagnosis.domain.AnalysisContext;
import com.battlecoach.diagnosis.domain.BurstWindow;
import com.battlecoach.diagnosis.domain.IdleBreakdown;
import com.battlecoach.diagnosis.domain.IdleSpan;
import com.battlecoach.diagnosis.domain.SkillUsage;
import com.battlecoach.spec.domain.CharacterSpec;
import com.battlecoach.spec.domain.CooldownStats;

class MissedCastAdvisorTest {

    private static final long PLAY_TIME_MS = 100_000;
    private static final CharacterSpec SPEC = CharacterSpec.of(CooldownStats.of(0, 0, 0, 0), List.of());

    @Test
    void 쉰_구간을_시간순으로_남기고_합계는_구간을_더한_값이다() {
        // 쿨 20초: 0초 → 50초(20~50초 쉼) → 70초 → 전투 끝까지(90~100초)
        SkillUsage skill = skill(20_000, 0, 50_000, 70_000);

        IdleBreakdown idle = IdleBreakdown.of(skill, context(List.of(), skill));

        assertThat(idle.spans()).containsExactly(
                new IdleSpan(20_000, 50_000, IdleSpan.Kind.UNUSED),
                new IdleSpan(90_000, 100_000, IdleSpan.Kind.TAIL));
        assertThat(idle.unusedMs()).isEqualTo(30_000);
        assertThat(idle.tailMs()).isEqualTo(10_000);
    }

    @Test
    void 그_외_공백이_가장_크면_가장_길게_쉰_구간을_알려_준다() {
        SkillUsage skill = skill(20_000, 0, 50_000, 70_000, 90_000);
        AnalysisContext context = context(List.of(), skill);

        assertThat(MissedCastAdvisor.advise(IdleBreakdown.of(skill, context), context)).get().asString()
                .contains("쿨이 돌면 바로 쓰세요").contains("20~50초(30초)");
    }

    @Test
    void 극딜_대기가_쿨보다_길면_극딜_전에_한_번_더_쓸_시각을_알려_준다() {
        // 20초부터 가능했는데 극딜(55~85초) 안인 60초에 사용. 20초에 쓰면 40초에 쿨이 돌아 극딜 전에 준비된다.
        SkillUsage skill = skill(20_000, 0, 60_000, 80_000);
        AnalysisContext context = context(List.of(new BurstWindow(55_000, 85_000)), skill);

        assertThat(MissedCastAdvisor.advise(IdleBreakdown.of(skill, context), context)).get().asString()
                .contains("극딜을 기다리며 40초를 쉬었습니다").contains("20초에 한 번 쓰면 극딜 시작(55초)");
    }

    @Test
    void 극딜_대기가_쿨보다_짧으면_시각을_단정하지_않는다() {
        SkillUsage skill = skill(20_000, 0, 35_000, 55_000, 75_000, 95_000);
        AnalysisContext context = context(List.of(new BurstWindow(30_000, 60_000)), skill);

        assertThat(MissedCastAdvisor.advise(IdleBreakdown.of(skill, context), context)).get().asString()
                .contains("사이클을 확인해 보세요");
    }

    @Test
    void 전투_종료_전_공백이_가장_크면_끝까지_쓰라고_한다() {
        SkillUsage skill = skill(20_000, 0, 20_000);
        AnalysisContext context = context(List.of(), skill);

        assertThat(MissedCastAdvisor.advise(IdleBreakdown.of(skill, context), context)).get().asString()
                .contains("전투 종료 60초 전에 쿨이 돌았는데");
    }

    @Test
    void 기준이_함께_쓰는_스킬을_따로_썼으면_시퀀스에_넣으라고_한다() {
        SkillUsage skill = skill(20_000, 0, 20_000, 40_000, 60_000, 80_000);

        assertThat(MissedCastAdvisor.adviseShortfall(skill, context(List.of(), skill), "레디 투 다이", "기준 기록")).get().asString()
                .startsWith("레디 투 다이와 같은 시퀀스");
        // 따로 쓴 짝도 없고 쉰 시간도 쿨 1회분이 안 되면 원인을 단정하지 않는다
        assertThat(MissedCastAdvisor.adviseShortfall(skill, context(List.of(), skill), null, "기준 기록")).isEmpty();
    }

    @Test
    void 조사는_받침에_맞춘다() {
        assertThat(KoreanJosa.withAnd("헥스 : 판데모니움")).isEqualTo("헥스 : 판데모니움과");
        assertThat(KoreanJosa.withAnd("솔 헤카테 : 스틱스")).isEqualTo("솔 헤카테 : 스틱스와");
        assertThat(KoreanJosa.withAnd("보이드 블리츠 VI")).isEqualTo("보이드 블리츠 VI와");
        assertThat(KoreanJosa.withObject("레디 투 다이")).isEqualTo("레디 투 다이를");
        assertThat(KoreanJosa.withObject("데스 블로섬")).isEqualTo("데스 블로섬을");
    }

    private static SkillUsage skill(long cooldownMs, long... castTimes) {
        return new SkillUsage("판데모니움", "판데모니움", Arrays.stream(castTimes).boxed().toList(), 10_000L, cooldownMs, null);
    }

    private static AnalysisContext context(List<BurstWindow> bursts, SkillUsage skill) {
        return AnalysisContext.single(PLAY_TIME_MS, 1_000, SPEC, List.of(skill), bursts);
    }
}
