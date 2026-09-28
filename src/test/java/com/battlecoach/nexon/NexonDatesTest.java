package com.battlecoach.nexon;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

class NexonDatesTest {

    @Test
    void 초가_없는_KST_자정_문자열을_같은_날짜로_해석한다() {
        assertThat(NexonDates.toKstDate("2026-08-06T00:00+09:00"))
                .isEqualTo(LocalDate.of(2026, 8, 6));
    }
}
