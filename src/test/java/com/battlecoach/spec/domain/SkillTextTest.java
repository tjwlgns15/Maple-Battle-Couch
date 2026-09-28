package com.battlecoach.spec.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SkillTextTest {

    @Test
    void 줄_맨_앞의_쿨_표기를_읽는다() {
        SkillText text = new SkillText("MP 700 소비, 3초 동안 공격\n재사용 대기시간 30초");

        assertThat(text.cooldownMs()).hasValue(30_000);
    }

    @Test
    void 콜론이_붙은_표기와_소수_초를_읽는다() {
        assertThat(new SkillText("HP 100 소비\n재사용 대기시간 : 240초").cooldownMs()).hasValue(240_000);
        assertThat(new SkillText("재사용 대기시간 7.5초").cooldownMs()).hasValue(7_500);
    }

    @Test
    void 분_단위_표기를_읽는다() {
        assertThat(new SkillText("재사용 대기시간 3분").cooldownMs()).hasValue(180_000);
        assertThat(new SkillText("재사용 대기시간 1분 30초").cooldownMs()).hasValue(90_000);
    }

    @Test
    void 문자_그대로의_역슬래시_n_도_줄바꿈으로_본다() {
        SkillText text = new SkillText("공격\\n재사용 대기시간 60초\\n설치 후 재설치 가능");

        assertThat(text.cooldownMs()).hasValue(60_000);
    }

    @Test
    void 다른_스킬의_쿨_감소_언급은_자기_쿨이_아니다() {
        SkillText text = new SkillText(
                "MP 25 소비, 최대 8명의 적을 230% 데미지로 4번 공격\n"
                        + "사용 시 보이드 러쉬/보이드 블리츠의 재사용 대기시간 5초 감소\n"
                        + "재사용 대기시간 5초");

        assertThat(text.cooldownMs()).hasValue(5_000);
    }

    @Test
    void 줄_맨_앞이라도_감소_문구면_건너뛴다() {
        assertThat(new SkillText("재사용 대기시간 2초 감소").cooldownMs()).isEmpty();
        assertThat(new SkillText("재사용 대기시간 미적용 확률 증가").cooldownMs()).isEmpty();
    }

    @Test
    void 쿨_표기가_없으면_비어_있다() {
        assertThat(new SkillText("최대 7명의 적을 296% 데미지로 7번 공격").cooldownMs()).isEmpty();
        assertThat(new SkillText(null).cooldownMs()).isEmpty();
    }

    @Test
    void 쿨감을_받지_않는_스킬을_찾는다() {
        SkillText text = new SkillText("컨티뉴어스 링은 재사용 대기시간 초기화, 재사용 대기시간 감소의 효과를 받지 않는다.");

        assertThat(text.refusesCooldownReduction()).isTrue();
        assertThat(text.refusesCooldownReset()).isTrue();
    }

    @Test
    void 초기화만_받지_않는_스킬을_찾는다() {
        SkillText text = SkillText.of("재사용 대기시간 120초", "리스트레인트 링은 재사용 대기시간 초기화의 효과를 받지 않는다.");

        assertThat(text.refusesCooldownReset()).isTrue();
        assertThat(text.refusesCooldownReduction()).isFalse();
    }

    @Test
    void 효과를_받는다는_문구는_거부가_아니다() {
        SkillText text = new SkillText("재사용 대기시간 120초\n재사용 대기시간 감소 효과를 받는다.");

        assertThat(text.refusesCooldownReduction()).isFalse();
    }

    @Test
    void 첫_지속시간_표기를_읽는다() {
        assertThat(new SkillText("MP 1000 소비, 4.3초 동안 강림\n재사용 대기시간 120초").durationMs()).hasValue(4_300);
        assertThat(new SkillText("MP 100 소비, 60초 동안 데미지 10% 증가").durationMs()).hasValue(60_000);
        assertThat(new SkillText("재사용 대기시간 12초").durationMs()).isEmpty();
    }

    @Test
    void 지속_중_다시_눌러_효과를_바꾸는_스킬을_찾는다() {
        assertThat(new SkillText("10초 동안 무적 상태\n스킬을 다시 사용하여 즉시 종료 가능").isReactivatable()).isTrue();
        assertThat(new SkillText("1단계 지속 중 스킬 재사용 시 2단계 진입").isReactivatable()).isTrue();
        assertThat(new SkillText("설치 후 아래 방향키와 함께 스킬을 재사용하여 재설치 가능").isReactivatable()).isTrue();
        assertThat(new SkillText("보이드 러쉬, 보이드 블리츠 스킬의 재사용 대기시간 초기화").isReactivatable()).isFalse();
        assertThat(new SkillText("스킬 재사용 대기시간 10% 감소").isReactivatable()).isFalse();
    }
}
