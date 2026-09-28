package com.battlecoach.replay.domain;

import java.util.regex.Pattern;

/**
 * 연무장 리플레이 식별자. 잘못된 값으로 Nexon API 호출량을 낭비하지 않도록 형식을 먼저 검사한다.
 * 샘플은 32자리 16진수였지만 공식 명세로 확인되지 않아 영숫자 64자 이하로 느슨하게 검사한다.
 */
public record ReplayId(String value) {

    private static final Pattern FORMAT = Pattern.compile("^[0-9a-zA-Z]{1,64}$");

    public ReplayId {
        if (value == null || !FORMAT.matcher(value).matches()) {
            throw new IllegalArgumentException("올바르지 않은 리플레이 식별자입니다: " + value);
        }
    }

    public static ReplayId of(String value) {
        return new ReplayId(value);
    }
}
