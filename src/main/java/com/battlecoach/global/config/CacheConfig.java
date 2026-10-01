package com.battlecoach.global.config;

import java.time.Duration;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.github.benmanes.caffeine.cache.Caffeine;

/**
 * 캐시별로 TTL이 달라 spring.cache.caffeine.spec(전역 단일 설정) 대신 캐시마다 직접 등록한다.
 * 리플레이 본문은 불변 데이터라 이 캐시가 아니라 MySQL에 영구 저장한다.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    private static final long MAX_ENTRIES = 10_000;
    private static final long MAX_SPEC_ENTRIES = 1_000;

    @Bean
    public CacheManager cacheManager(CacheProperties properties) {
        CaffeineCacheManager manager = new CaffeineCacheManager();
        manager.setAllowNullValues(false);
        manager.registerCustomCache(CacheNames.OCID, expiringAfter(properties.ocidTtl()));
        manager.registerCustomCache(CacheNames.CHARACTER_CLASS, expiringAfter(properties.ocidTtl()));
        manager.registerCustomCache(CacheNames.REPLAY_LIST, expiringAfter(properties.replayListTtl()));
        manager.registerCustomCache(CacheNames.JOB_STATISTICS, expiringAfter(properties.jobStatisticsTtl()));
        manager.registerCustomCache(CacheNames.COMPARISON_POOL, expiringAfter(properties.jobStatisticsTtl()));
        manager.registerCustomCache(CacheNames.CHARACTER_SPEC, Caffeine.newBuilder()
                .maximumSize(MAX_SPEC_ENTRIES)
                .build());
        return manager;
    }

    private static com.github.benmanes.caffeine.cache.Cache<Object, Object> expiringAfter(Duration ttl) {
        return Caffeine.newBuilder()
                .expireAfterWrite(ttl)
                .maximumSize(MAX_ENTRIES)
                .build();
    }
}
