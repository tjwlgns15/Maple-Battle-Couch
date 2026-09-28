package com.battlecoach.spec.parser;

import com.battlecoach.spec.domain.CharacterSpec;

/** character-info 원문 JSON → 쿨타임 분석용 스펙 */
public interface CharacterSpecParser {

    CharacterSpec parse(String characterInfoJson);
}
