package com.battlecoach.replay.application;

import lombok.Getter;

/** 그 이름의 캐릭터가 없다. ocid 조회가 4xx(OPENAPI00004)일 때 낸다. */
@Getter
public class CharacterNotFoundException extends RuntimeException {

    private final String characterName;

    public CharacterNotFoundException(String characterName) {
        super("'" + characterName + "' 캐릭터를 찾을 수 없습니다. 이름을 확인해 주세요.");
        this.characterName = characterName;
    }
}
