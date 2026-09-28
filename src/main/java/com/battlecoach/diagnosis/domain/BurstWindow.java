package com.battlecoach.diagnosis.domain;

/** 극딜 구간 [startMs, endMs] */
public record BurstWindow(long startMs, long endMs) {

    public BurstWindow {
        if (endMs < startMs) {
            throw new IllegalArgumentException("극딜 구간의 끝이 시작보다 빠릅니다: " + startMs + " > " + endMs);
        }
    }

    public boolean contains(long timeMs) {
        return startMs <= timeMs && timeMs <= endMs;
    }
}
