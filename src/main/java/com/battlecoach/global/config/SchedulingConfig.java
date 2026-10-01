package com.battlecoach.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** 리플레이 갱신 같은 주기 작업을 켠다. 작업마다 따로 켜고 끄는 설정을 둔다. */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
