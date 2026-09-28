package com.battlecoach.replay.application;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.battlecoach.global.config.CacheNames;
import com.battlecoach.replay.domain.ReplayId;
import com.battlecoach.replay.repository.ReplayRawDataRepository;
import com.battlecoach.spec.domain.CharacterSpec;
import com.battlecoach.spec.parser.CharacterSpecParser;

import lombok.RequiredArgsConstructor;

/**
 * 저장된 character-info 원문을 파싱한다. API 를 다시 부르지 않으므로 파서를 고치면 기존 리플레이도 바로 재분석된다.
 * 호출 전에 리플레이가 저장돼 있어야 한다 (ReplayQueryService.getReplay).
 */
@Component
@RequiredArgsConstructor
class CharacterSpecLoader {

    private final ReplayRawDataRepository rawDataRepository;
    private final CharacterSpecParser characterSpecParser;

    @Cacheable(cacheNames = CacheNames.CHARACTER_SPEC, key = "#replayId.value()")
    @Transactional(readOnly = true)
    public CharacterSpec load(ReplayId replayId) {
        String json = rawDataRepository.findById(replayId.value())
                .orElseThrow(() -> new IllegalStateException("리플레이 원문이 없습니다: " + replayId.value()))
                .getCharacterInfoJson();
        return characterSpecParser.parse(json);
    }
}
