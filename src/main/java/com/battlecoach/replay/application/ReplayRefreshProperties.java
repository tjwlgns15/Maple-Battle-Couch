package com.battlecoach.replay.application;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 저장된 리플레이 갱신 설정. Nexon Open API 문서 고지: "API를 통해 데이터를 크롤링한 경우 30일 이내에 크롤링한 데이터를 갱신해야 할 의무".
 *
 * @param enabled   매일 자동 갱신을 돌릴지
 * @param cron      자동 갱신 시각 (Asia/Seoul)
 * @param maxAge    마지막으로 API 와 맞춘 지 이보다 오래되면 갱신한다. 30일보다 짧게 둬서 하루 상한에 걸려 밀려도 30일을 넘기지 않게 한다
 * @param maxPerRun 한 번에 갱신할 리플레이 수. 1건에 API 1건이 든다
 */
@ConfigurationProperties(prefix = "replay.refresh")
public record ReplayRefreshProperties(
        boolean enabled,
        String cron,
        Duration maxAge,
        int maxPerRun
) {
}
