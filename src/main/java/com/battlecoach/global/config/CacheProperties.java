package com.battlecoach.global.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "cache")
public record CacheProperties(
        Duration ocidTtl,
        Duration replayListTtl,
        Duration jobStatisticsTtl
) {
}
