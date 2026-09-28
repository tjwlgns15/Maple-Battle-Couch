package com.battlecoach.replay.web;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class KoreanNumberFormatTest {

    private final KoreanNumberFormat format = new KoreanNumberFormat();

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "0|0",
            "9999|9,999",
            "10000|1만",
            "123456789|1억 2,345만",
            "9583333333333|9조 5,833억",
            "3450000000000000|3,450조",
            "3000012345678|3조",
            "12345678901234567|1경 2,345조",
            "-123456789|-1억 2,345만"
    })
    void 상위_두_단위로_줄인다(long value, String expected) {
        assertThat(format.format(value)).isEqualTo(expected);
    }
}
