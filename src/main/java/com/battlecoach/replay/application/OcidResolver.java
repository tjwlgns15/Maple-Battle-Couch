package com.battlecoach.replay.application;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import com.battlecoach.global.config.CacheNames;
import com.battlecoach.nexon.NexonApiClient;
import com.battlecoach.replay.domain.CharacterName;

import lombok.RequiredArgsConstructor;

/** 캐릭터명 → ocid. @Cacheable 프록시가 동작하도록 호출하는 서비스와 별도 빈으로 둔다. */
@Component
@RequiredArgsConstructor
public class OcidResolver {

    private final NexonApiClient nexonApiClient;

    @Cacheable(cacheNames = CacheNames.OCID, key = "#characterName.value()")
    public String resolve(CharacterName characterName) {
        return nexonApiClient.findOcid(characterName.value());
    }
}
