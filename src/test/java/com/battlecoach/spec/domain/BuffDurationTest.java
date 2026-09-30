package com.battlecoach.spec.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** 버프 지속시간 증가를 받는 지속시간인지 가르고 반영한다. 문구는 칼리·다른 직업 character-info 원문에서 가져왔다. */
class BuffDurationTest {

    private static final CooldownStats BUFF_79 = CooldownStats.of(0, 0, 0, 79);

    @Test
    void 버프_지속시간은_늘어난다() {
        assertThat(new SkillText("MP 250 소비, 30초 동안 지속 사용 시 보이드 러쉬").isBuffDurationExtendable()).isTrue();
        assertThat(new SkillText("MP 1000 소비, 60초 동안 데미지 45% 증가 공격 스킬 사용 시").isBuffDurationExtendable()).isTrue();
        assertThat(new SkillText("HP 100 소비, 30초 동안 최종 데미지 증가 1단계").isBuffDurationExtendable()).isTrue();
    }

    @Test
    void 소환_영역_공격_지속은_늘지_않는다() {
        assertThat(new SkillText("HP 600 소비, 15초 동안 생성되는 영역 안에서 자신의 공격력이 68%").isBuffDurationExtendable()).isFalse();
        assertThat(new SkillText("MP 330 소비, 30초 동안 일정 간격마다 최대 10명의 적을 1010%의 데미지로 6번 공격하는 절망의 장미 소환")
                .isBuffDurationExtendable()).isFalse();
        assertThat(new SkillText("3초 동안 키다운하여 최대 15명의 적을 756%의 데미지로 15번 공격").isBuffDurationExtendable()).isFalse();
        assertThat(new SkillText("10초 동안 행동 불가 상태").isBuffDurationExtendable()).isFalse();
        assertThat(new SkillText("50초 동안 지속되며 일정 시간마다 공격 상태에 돌입").isBuffDurationExtendable()).isFalse();
    }

    @Test
    void 예외_문구가_있으면_늘지_않는다() {
        assertThat(new SkillText("10초 동안 무적 상태\n재사용 대기시간 초기화 및 버프 지속시간 증가의 효과를 받지 않고")
                .isBuffDurationExtendable()).isFalse();
        assertThat(new SkillText("20초 동안 데미지 증가\n버프 지속시간 증가의 효과를 받지 않는다").isBuffDurationExtendable()).isFalse();
    }

    @Test
    void 소수점은_절_경계로_보지_않는다() {
        assertThat(new SkillText("4.3초 동안 데미지 10% 증가, 적을 공격").isBuffDurationExtendable()).isTrue();
    }

    @Test
    void 늘어나는_지속시간에만_버프_지속시간_퍼센트를_곱한다() {
        SkillSpec oblivion = SkillSpec.of("오블리비온", 1, new SkillText("30초 동안 지속\n재사용 대기시간 120초"));
        SkillSpec ring = SkillSpec.of("리스트레인트 링", 4, new SkillText("15초 동안 생성되는 영역 안에서\n재사용 대기시간 120초"));

        assertThat(oblivion.effectiveDurationMs(BUFF_79)).isEqualTo(53_700L);
        assertThat(ring.effectiveDurationMs(BUFF_79)).isEqualTo(15_000L);
    }
}
