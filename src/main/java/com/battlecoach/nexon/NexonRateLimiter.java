package com.battlecoach.nexon;

import java.util.concurrent.TimeUnit;

/**
 * 호출 간격을 일정하게 유지하는 단순 토큰 버킷(버스트 없음).
 * permitsPerSecond = 5 이면 호출 사이 간격이 최소 200ms가 되도록 호출 스레드를 대기시킨다.
 * 단일 인스턴스 기준이며, 서버를 여러 대 띄우면 Redis 등 공유 저장소 기반으로 교체해야 한다.
 */
public final class NexonRateLimiter {

    private final long intervalNanos;
    private long nextAvailableNanos;

    public NexonRateLimiter(int permitsPerSecond) {
        this.intervalNanos = TimeUnit.SECONDS.toNanos(1) / permitsPerSecond;
        this.nextAvailableNanos = System.nanoTime();
    }

    public void acquire() {
        long waitNanos = reserve();
        if (waitNanos > 0) {
            sleep(waitNanos);
        }
    }

    private synchronized long reserve() {
        long now = System.nanoTime();
        long slot = Math.max(now, nextAvailableNanos);
        nextAvailableNanos = slot + intervalNanos;
        return slot - now;
    }

    private static void sleep(long nanos) {
        try {
            TimeUnit.NANOSECONDS.sleep(nanos);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Nexon API 호출 대기 중 인터럽트되었습니다.", e);
        }
    }
}
