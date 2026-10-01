package com.battlecoach.comparison.application;

import java.util.Optional;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.battlecoach.global.config.CacheNames;
import com.battlecoach.replay.domain.StoredReplayChangedEvent;

import lombok.RequiredArgsConstructor;

/**
 * 저장된 기록이 바뀌면 그 직업·기간의 비교 풀 캐시와 통계 캐시를 비운다.
 * 비우지 않으면 기록 모음의 직업별 개수(DB 를 바로 셈)와 표(풀 캐시)가 TTL 동안 어긋난다.
 * 커밋 뒤에 비워야 다른 요청이 커밋 전 상태로 다시 캐시하지 않는다.
 */
@Component
@RequiredArgsConstructor
class ComparisonCacheEvictor {

    private final CacheManager cacheManager;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    void onStoredReplayChanged(StoredReplayChangedEvent event) {
        if (event.periodNo() != null) {
            cache(CacheNames.COMPARISON_POOL).ifPresent(pool -> pool.evict(event.characterClass() + "|" + event.periodNo()));
        }
        // 통계 캐시 키에는 제외 리플레이와 비교 조건이 들어 있어 골라 지울 수 없다. 풀 캐시에서 다시 계산하므로 비용이 작다.
        cache(CacheNames.JOB_STATISTICS).ifPresent(Cache::clear);
    }

    private Optional<Cache> cache(String name) {
        return Optional.ofNullable(cacheManager.getCache(name));
    }
}
