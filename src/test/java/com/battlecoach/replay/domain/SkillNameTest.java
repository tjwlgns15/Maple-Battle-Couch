package com.battlecoach.replay.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class SkillNameTest {

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "보이드 러쉬 VI|보이드 러쉬",
            "매직 서킷 풀드라이브 VI|매직 서킷 풀드라이브",
            "레조네이트 : 얼티메이텀|레조네이트 : 얼티메이텀",
            "헥스 : 샌드스톰|헥스 : 샌드스톰",
            "스파크|스파크"
    })
    void 헥사_로마숫자_접미사를_제거한다(String raw, String expected) {
        assertThat(SkillName.of(raw).baseName()).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource({"0,NORMAL", "1,ORIGIN", "2,ASCENT", "9,UNKNOWN"})
    void 헥사_플래그를_변환한다(String flag, HexaType expected) {
        assertThat(HexaType.fromFlag(flag)).isEqualTo(expected);
    }
}
