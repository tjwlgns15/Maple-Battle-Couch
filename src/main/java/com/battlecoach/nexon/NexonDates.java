package com.battlecoach.nexon;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;

/**
 * Nexon API 날짜 문자열(예: "2026-08-06T00:00+09:00") 해석.
 * Jackson 기본 설정은 OffsetDateTime 을 UTC로 조정해 날짜가 하루 밀릴 수 있어 직접 KST로 변환한다.
 */
public final class NexonDates {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private NexonDates() {
    }

    public static LocalDate toKstDate(String value) {
        return OffsetDateTime.parse(value)
                .atZoneSameInstant(KST)
                .toLocalDate();
    }
}
