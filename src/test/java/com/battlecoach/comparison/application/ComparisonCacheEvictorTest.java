package com.battlecoach.comparison.application;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;

import com.battlecoach.global.config.CacheNames;
import com.battlecoach.replay.domain.StoredReplayChangedEvent;

class ComparisonCacheEvictorTest {

    private final ConcurrentMapCacheManager cacheManager =
            new ConcurrentMapCacheManager(CacheNames.COMPARISON_POOL, CacheNames.JOB_STATISTICS);
    private final ComparisonCacheEvictor evictor = new ComparisonCacheEvictor(cacheManager);

    @BeforeEach
    void fill() {
        cacheManager.getCache(CacheNames.COMPARISON_POOL).put("렌|5", "풀");
        cacheManager.getCache(CacheNames.COMPARISON_POOL).put("칼리|5", "풀");
        cacheManager.getCache(CacheNames.JOB_STATISTICS).put("렌|5|x|DPS50", "통계");
    }

    @Test
    void 바뀐_기록의_직업_기간_풀만_비우고_통계는_모두_비운다() {
        evictor.onStoredReplayChanged(new StoredReplayChangedEvent("id", "렌", 5));

        assertThat(cacheManager.getCache(CacheNames.COMPARISON_POOL).get("렌|5")).isNull();
        assertThat(cacheManager.getCache(CacheNames.COMPARISON_POOL).get("칼리|5")).isNotNull();
        assertThat(cacheManager.getCache(CacheNames.JOB_STATISTICS).get("렌|5|x|DPS50")).isNull();
    }

    @Test
    void 기간을_모르는_기록은_풀에_없으므로_풀을_건드리지_않는다() {
        evictor.onStoredReplayChanged(new StoredReplayChangedEvent("id", "렌", null));

        assertThat(cacheManager.getCache(CacheNames.COMPARISON_POOL).get("렌|5")).isNotNull();
    }
}
