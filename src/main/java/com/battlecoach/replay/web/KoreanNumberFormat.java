package com.battlecoach.replay.web;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Component;

/**
 * 큰 데미지 수치를 "9조 5,833억"처럼 상위 두 단위로 줄여 보여준다.
 * 템플릿에서 {@code ${@koreanNumberFormat.format(value)}} 로 쓴다.
 */
@Component
public class KoreanNumberFormat {

    private static final long[] UNIT_VALUES = {10_000_000_000_000_000L, 1_000_000_000_000L, 100_000_000L, 10_000L};
    private static final String[] UNIT_NAMES = {"경", "조", "억", "만"};
    private static final int MAX_PARTS = 2;

    public String format(long value) {
        if (value < 0) {
            return "-" + format(-value);
        }
        NumberFormat grouping = NumberFormat.getIntegerInstance(Locale.KOREA);
        if (value < UNIT_VALUES[UNIT_VALUES.length - 1]) {
            return grouping.format(value);
        }

        List<String> parts = new ArrayList<>();
        long remainder = value;
        for (int i = 0; i < UNIT_VALUES.length && parts.size() < MAX_PARTS; i++) {
            long amount = remainder / UNIT_VALUES[i];
            remainder %= UNIT_VALUES[i];
            if (amount > 0) {
                parts.add(grouping.format(amount) + UNIT_NAMES[i]);
            } else if (!parts.isEmpty()) {
                break; // 상위 단위 바로 아래가 0이면 거기서 끊는다. (예: 3조 0억 → 3조)
            }
        }
        return String.join(" ", parts);
    }

    /** 스탯 값처럼 소수가 드문 수를 "5", "6.5" 로 보여준다. */
    public String plain(double value) {
        return BigDecimal.valueOf(value).stripTrailingZeros().toPlainString();
    }
}
