package com.battlecoach.replay.application;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import com.battlecoach.global.config.CacheNames;
import com.battlecoach.nexon.NexonApiClient;
import com.battlecoach.replay.domain.CharacterName;

import lombok.RequiredArgsConstructor;

/** 캐릭터명 → 현재 직업. 다른 직업 기록을 받아 오기 전에(본문 3건) 1건으로 거르기 위해 쓴다. */
@Component
@RequiredArgsConstructor
public class CharacterClassResolver {

    private final OcidResolver ocidResolver;
    private final NexonApiClient nexonApiClient;

    @Cacheable(cacheNames = CacheNames.CHARACTER_CLASS, key = "#characterName.value()")
    public String resolve(CharacterName characterName) {
        return nexonApiClient.findCharacterBasic(ocidResolver.resolve(characterName)).characterClass();
    }
}
