package com.battlecoach.replay.repository;

/** 직업별 저장된 리플레이 수 */
public record ClassCount(String characterClass, long count) {
}
