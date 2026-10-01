package com.battlecoach.replay.application.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.battlecoach.replay.application.dto.CooldownStatsView.Item;
import com.battlecoach.spec.domain.StatSource;

class CooldownStatsViewTest {

    @Test
    void 출처_합이_합계보다_작으면_기타를_붙인다() {
        Item item = new Item("쿨타임 감소", "-", "초", 5, List.of(StatSource.of("모자 잠재능력", 4)), true);

        assertThat(item.displaySources()).containsExactly(StatSource.of("모자 잠재능력", 4), StatSource.of("기타", 1));
    }

    @Test
    void 게임_표기가_출처_합을_내림한_값이면_내림으로_본다() {
        Item item = new Item("재사용 대기시간 미적용", "", "%", 27,
                List.of(StatSource.of("어빌리티", 20), StatSource.of("유니온 아티팩트", 7.5)), true);

        assertThat(item.roundedDown()).isTrue();
        assertThat(item.displaySources()).hasSize(2);
    }

    @Test
    void 출처를_찾지_않는_항목은_기타를_붙이지_않는다() {
        Item item = new Item("버프 지속시간", "+", "%", 88, List.of(), false);

        assertThat(item.displaySources()).isEmpty();
    }
}
