package com.battlecoach.replay.domain;

import java.util.regex.Pattern;

/**
 * 스킬 이름 값 객체.
 * 헥사 강화 스킬은 "보이드 러쉬 VI"처럼 로마 숫자 접미사가 붙고, API마다 표기가 다르다.
 * (skill-timeline: "보이드 러쉬", result: "보이드 러쉬 VI")
 * 두 API를 연결하거나 헥사 진행도가 다른 기록끼리 비교할 때는 baseName 을 키로 쓴다.
 */
public record SkillName(String value) {

    private static final Pattern ROMAN_NUMERAL_SUFFIX = Pattern.compile("\\s+[IVX]+$");

    public SkillName {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("스킬 이름이 비어 있습니다.");
        }
        value = value.strip();
    }

    public static SkillName of(String value) {
        return new SkillName(value);
    }

    public String baseName() {
        return ROMAN_NUMERAL_SUFFIX.matcher(value).replaceFirst("");
    }
}
