package com.battlecoach.replay.domain;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 유저가 등록한 시퀀스(sequence_key)가 한 번 실행된 구간.
 * 시퀀스 내부에서는 elapse_time 과 기록 순서가 몇 ms 역전되므로 시작·끝은 최솟값·최댓값으로 잡는다.
 */
public record SequenceSegment(
        String sequenceKey,
        String sequenceName,
        long startMs,
        long endMs,
        int castCount
) {

    /**
     * 같은 키의 직전 시전과 이 시간보다 멀면 다른 실행으로 본다.
     * 칼리 2명 실측: 한 실행 안의 간격은 최대 약 1.1초, 같은 키의 다음 실행까지는 최소 약 52.8초였다.
     * 다른 직업의 긴 시퀀스를 고려해 그 사이에서 넉넉한 값을 쓴다.
     */
    static final long MAX_GAP_MS = 10_000;

    /**
     * 기록 순서대로 훑으며 키별로 구간을 만든다. 사이에 끼어든 다른 시전(메인 공격 등)은 구간을 끊지 않는다.
     *
     * @return 시작 시각 오름차순
     */
    static List<SequenceSegment> extract(List<ReplayCast> casts) {
        Map<String, Builder> open = new HashMap<>();
        List<SequenceSegment> segments = new ArrayList<>();

        casts.stream()
                .filter(ReplayCast::isInSequence)
                .sorted(Comparator.comparingInt(ReplayCast::getRecordedOrder))
                .forEach(cast -> {
                    Builder current = open.get(cast.getSequenceKey());
                    if (current != null && current.isContinuedBy(cast)) {
                        current.add(cast);
                        return;
                    }
                    if (current != null) {
                        segments.add(current.build());
                    }
                    open.put(cast.getSequenceKey(), new Builder(cast));
                });

        open.values().forEach(builder -> segments.add(builder.build()));
        segments.sort(Comparator.comparingLong(SequenceSegment::startMs));
        return segments;
    }

    private static final class Builder {

        private final String sequenceKey;
        private final String sequenceName;
        private long startMs;
        private long endMs;
        private long lastMs;
        private int castCount;

        private Builder(ReplayCast first) {
            this.sequenceKey = first.getSequenceKey();
            this.sequenceName = first.getSequenceName();
            this.startMs = first.getElapseMs();
            this.endMs = first.getElapseMs();
            this.lastMs = first.getElapseMs();
            this.castCount = 1;
        }

        private boolean isContinuedBy(ReplayCast cast) {
            return cast.getElapseMs() - lastMs <= MAX_GAP_MS;
        }

        private void add(ReplayCast cast) {
            startMs = Math.min(startMs, cast.getElapseMs());
            endMs = Math.max(endMs, cast.getElapseMs());
            lastMs = cast.getElapseMs();
            castCount++;
        }

        private SequenceSegment build() {
            return new SequenceSegment(sequenceKey, sequenceName, startMs, endMs, castCount);
        }
    }
}
