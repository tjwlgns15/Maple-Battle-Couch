package com.battlecoach.global.concurrent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

class SingleFlightTest {

    private static final int THREADS = 10;

    @Test
    void 같은_키의_동시_요청은_loader를_한_번만_실행한다() throws Exception {
        SingleFlight<String, String> singleFlight = new SingleFlight<>();
        AtomicInteger loadCount = new AtomicInteger();
        CountDownLatch ready = new CountDownLatch(THREADS);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(THREADS);

        List<Future<String>> results = new ArrayList<>();
        for (int i = 0; i < THREADS; i++) {
            results.add(executor.submit(() -> {
                ready.countDown();
                start.await();
                return singleFlight.execute("replay-1", () -> {
                    loadCount.incrementAndGet();
                    await(release);
                    return "loaded";
                });
            }));
        }

        ready.await();
        start.countDown();
        TimeUnit.MILLISECONDS.sleep(200); // 모든 스레드가 execute 에 진입할 시간을 준다
        release.countDown();

        for (Future<String> result : results) {
            assertThat(result.get(5, TimeUnit.SECONDS)).isEqualTo("loaded");
        }
        assertThat(loadCount.get()).isEqualTo(1);
        executor.shutdown();
    }

    @Test
    void 실행이_끝나면_키가_제거되어_다음_요청은_다시_실행한다() {
        SingleFlight<String, Integer> singleFlight = new SingleFlight<>();
        AtomicInteger loadCount = new AtomicInteger();

        singleFlight.execute("key", loadCount::incrementAndGet);
        singleFlight.execute("key", loadCount::incrementAndGet);

        assertThat(loadCount.get()).isEqualTo(2);
    }

    @Test
    void loader_예외는_호출자에게_그대로_전달된다() {
        SingleFlight<String, String> singleFlight = new SingleFlight<>();

        assertThatThrownBy(() -> singleFlight.execute("key", () -> {
            throw new IllegalStateException("boom");
        })).isInstanceOf(IllegalStateException.class).hasMessage("boom");
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }
}
