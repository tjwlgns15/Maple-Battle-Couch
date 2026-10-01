package com.battlecoach.replay.application;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import com.battlecoach.global.config.CacheNames;
import com.battlecoach.nexon.NexonApiClient;
import com.battlecoach.nexon.NexonApiException;
import com.battlecoach.replay.domain.CharacterName;

import lombok.RequiredArgsConstructor;

/** 캐릭터명 → ocid. @Cacheable 프록시가 동작하도록 호출하는 서비스와 별도 빈으로 둔다. */
@Component
@RequiredArgsConstructor
public class OcidResolver {

    private final NexonApiClient nexonApiClient;

    /**
     * 없는 캐릭터명이면 400 OPENAPI00004 가 온다(2026-10-01 확인). 기록이 없을 때와 같은 코드라 단계로 구분한다.
     *
     * @throws CharacterNotFoundException 그 이름의 캐릭터가 없을 때
     */
    @Cacheable(cacheNames = CacheNames.OCID, key = "#characterName.value()")
    public String resolve(CharacterName characterName) {
        try {
            return nexonApiClient.findOcid(characterName.value());
        } catch (NexonApiException e) {
            if (e.isClientError()) {
                throw new CharacterNotFoundException(characterName.value());
            }
            throw e;
        }
    }
}
