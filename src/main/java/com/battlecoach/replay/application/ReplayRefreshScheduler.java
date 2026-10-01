package com.battlecoach.replay.application;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** 매일 한 번 오래된 리플레이를 갱신한다. replay.refresh.enabled=false 면 등록하지 않는다. */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "replay.refresh", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
class ReplayRefreshScheduler {

    private final ReplayRefreshService replayRefreshService;

    @Scheduled(cron = "${replay.refresh.cron}", zone = "Asia/Seoul")
    void refresh() {
        try {
            log.info("리플레이 갱신 결과 - {}", replayRefreshService.refreshStale());
        } catch (RuntimeException e) {
            log.error("리플레이 갱신 실패", e);
        }
    }
}
