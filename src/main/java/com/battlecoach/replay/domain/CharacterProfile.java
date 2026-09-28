package com.battlecoach.replay.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/** 연무장 입장 시점의 캐릭터 기본 정보 (character-info 의 basic_object) */
@Embeddable
public record CharacterProfile(
        @Column(name = "character_name", nullable = false, length = 30) String characterName,
        @Column(name = "character_class", nullable = false, length = 30) String characterClass,
        @Column(name = "character_level", nullable = false) int characterLevel
) {

    public static CharacterProfile of(String characterName, String characterClass, int characterLevel) {
        return new CharacterProfile(characterName, characterClass, characterLevel);
    }
}
