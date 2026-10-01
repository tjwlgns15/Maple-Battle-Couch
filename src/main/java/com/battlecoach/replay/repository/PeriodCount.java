package com.battlecoach.replay.repository;

/** 기간별 저장된 리플레이 수 */
public record PeriodCount(int periodNo, long count) {
}
