package com.battlecoach.replay.domain;

/** 조회용 캐릭터명. 앞뒤 공백을 제거해 캐시 키가 갈라지지 않게 한다. */
public record CharacterName(String value) {

    private static final int MAX_LENGTH = 12;

    public CharacterName {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("캐릭터명을 입력해 주세요.");
        }
        value = value.strip();
        if (value.length() > MAX_LENGTH) {
            throw new IllegalArgumentException("캐릭터명이 너무 깁니다: " + value);
        }
    }

    public static CharacterName of(String value) {
        return new CharacterName(value);
    }
}
