package com.battlecoach.global.concurrent;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * 같은 키에 대한 동시 요청을 하나의 실행으로 합친다.
 * 먼저 들어온 요청만 loader를 실행하고, 나머지는 그 결과(또는 예외)를 공유한다.
 * 실행이 끝나면 키를 제거하므로 결과를 캐시하지 않는다. (캐시는 호출자 책임)
 */
public final class SingleFlight<K, V> {

    private final Map<K, CompletableFuture<V>> inFlight = new ConcurrentHashMap<>();

    public V execute(K key, Supplier<V> loader) {
        CompletableFuture<V> mine = new CompletableFuture<>();
        CompletableFuture<V> existing = inFlight.putIfAbsent(key, mine);
        if (existing != null) {
            return await(existing);
        }
        try {
            V value = loader.get();
            mine.complete(value);
            return value;
        } catch (RuntimeException e) {
            mine.completeExceptionally(e);
            throw e;
        } finally {
            inFlight.remove(key, mine);
        }
    }

    private V await(CompletableFuture<V> future) {
        try {
            return future.join();
        } catch (CompletionException e) {
            if (e.getCause() instanceof RuntimeException cause) {
                throw cause;
            }
            throw e;
        }
    }
}
