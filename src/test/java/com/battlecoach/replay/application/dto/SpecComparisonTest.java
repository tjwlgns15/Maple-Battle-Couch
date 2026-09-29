package com.battlecoach.replay.application.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.battlecoach.replay.application.dto.ReplayComparison.SkillRow;

class SpecComparisonTest {

    @Test
    void 레벨이_다른_스킬은_차이가_큰_순으로_한쪽만_가진_스킬과_같은_스킬은_따로_나눈다() {
        SpecComparison specs = SpecComparison.from(List.of(
                row("헥스 : 샌드스톰", 9, 30),
                row("데스 블로섬", 25, 30),
                row("헥스 : 판데모니움", 30, 30),
                row("솔 헤카테 : 플레게톤", null, 30),
                row("소울 컨트랙트", null, null)));

        assertThat(specs.different()).extracting(SpecComparison.Row::skillName)
                .containsExactly("헥스 : 샌드스톰", "데스 블로섬");
        assertThat(specs.different().get(0).difference()).isEqualTo(-21);
        assertThat(specs.missing()).extracting(SpecComparison.Row::skillName).containsExactly("솔 헤카테 : 플레게톤");
        assertThat(specs.same()).extracting(SpecComparison.Row::skillName).containsExactly("헥스 : 판데모니움");
    }

    @Test
    void 레벨_막대_폭은_두_레벨_중_큰_값을_100퍼센트로_본다() {
        SpecComparison.Row row = new SpecComparison.Row("헥스 : 샌드스톰", 9, 30);

        assertThat(row.basePercent()).isEqualTo(30);
        assertThat(row.targetPercent()).isEqualTo(100);
        assertThat(new SpecComparison.Row("스킬", null, 30).basePercent()).isZero();
    }

    private static SkillRow row(String name, Integer baseLevel, Integer targetLevel) {
        return new SkillRow(name, baseLevel, targetLevel, 0, 0, 0, 0, null, null, null, null);
    }
}
