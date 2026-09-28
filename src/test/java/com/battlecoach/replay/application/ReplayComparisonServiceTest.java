package com.battlecoach.replay.application;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class ReplayComparisonServiceTest {

    @Test
    void 다른_직업끼리는_비교할_수_없다() {
        assertThatThrownBy(() -> ReplayComparisonService.requireSameClass("칼리", "아델"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("같은 직업끼리만")
                .hasMessageContaining("칼리")
                .hasMessageContaining("아델");
    }

    @Test
    void 같은_직업이면_통과한다() {
        assertThatCode(() -> ReplayComparisonService.requireSameClass("칼리", "칼리")).doesNotThrowAnyException();
    }
}
