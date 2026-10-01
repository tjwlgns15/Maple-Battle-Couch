package com.battlecoach.comparison.application;

import java.util.List;
import java.util.Optional;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import com.battlecoach.global.config.CacheNames;
import com.battlecoach.replay.application.AnalysisContextFactory;
import com.battlecoach.replay.application.ReplayQueryService;
import com.battlecoach.replay.domain.ReplayId;
import com.battlecoach.replay.repository.ReplayRepository;

import lombok.RequiredArgsConstructor;

/**
 * 같은 직업·기간으로 저장된 모든 리플레이(비교 풀). 사용자가 검색해 저장된 기록이 모두 들어간다.
 * API 를 부르지 않는다. 기록마다 시전 기록을 읽고 원문을 파싱해야 해서 무거우므로 짧게 캐시한다.
 */
@Component
@RequiredArgsConstructor
public class ComparisonPoolLoader {

    private final ReplayRepository replayRepository;
    private final ReplayQueryService replayQueryService;
    private final AnalysisContextFactory analysisContextFactory;

    @Cacheable(cacheNames = CacheNames.COMPARISON_POOL, key = "#characterClass + '|' + #periodNo")
    public List<PoolEntry> load(String characterClass, int periodNo) {
        return replayRepository.findReplayIds(characterClass, periodNo).stream()
                .map(id -> replayQueryService.findStored(ReplayId.of(id)))
                .flatMap(Optional::stream)
                .map(replay -> new PoolEntry(replay.replayId(), replay.characterName(), replay.characterLevel(),
                        replay.registerDate(), analysisContextFactory.create(replay)))
                .toList();
    }
}
