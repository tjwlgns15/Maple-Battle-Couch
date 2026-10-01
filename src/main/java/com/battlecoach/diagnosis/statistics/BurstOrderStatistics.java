package com.battlecoach.diagnosis.statistics;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * 비교 대상 극딜 순서 통계. 극딜 한 번이 순서 하나다(칼리는 기록당 3회).
 * 극딜 순서는 대부분 시퀀스(매크로) 설정이므로 "비교 대상이 많이 쓰는 매크로 순서"에 가깝다.
 *
 * @param burstCount  모은 극딜 순서 수
 * @param entries     스킬별 극딜 채택률과 상대 위치
 * @param precedences 대부분의 극딜에서 A 가 B 보다 먼저 나온 쌍 (1초 안에 함께 나간 경우는 순서로 보지 않는다)
 */
public record BurstOrderStatistics(
        int burstCount,
        List<Entry> entries,
        List<Precedence> precedences
) {

    /** 표준 순서에 넣을 최소 극딜 채택률 */
    public static final double STANDARD_MIN_ADOPTION = 0.5;

    public BurstOrderStatistics {
        entries = List.copyOf(entries);
        precedences = List.copyOf(precedences);
    }

    public static BurstOrderStatistics empty() {
        return new BurstOrderStatistics(0, List.of(), List.of());
    }

    /** 극딜 절반 이상에서 쓰는 스킬을 상대 위치 중앙값 순으로 나열한다. */
    public List<Entry> standardOrder() {
        return entries.stream()
                .filter(entry -> entry.adoptionRate() >= STANDARD_MIN_ADOPTION)
                .sorted(Comparator.comparingDouble(Entry::medianPosition).thenComparing(Entry::baseName))
                .toList();
    }

    public Optional<Entry> entry(String baseName) {
        return entries.stream().filter(entry -> entry.baseName().equals(baseName)).findFirst();
    }

    /**
     * @param adoptionRate   이 스킬이 나온 극딜 비율
     * @param medianPosition 극딜 안 상대 위치의 중앙값 (0 = 맨 앞, 1 = 맨 뒤)
     */
    public record Entry(String baseName, double adoptionRate, double medianPosition) {
    }

    /**
     * @param orderedCount 두 스킬이 1초 넘게 떨어져 나온 극딜 수
     * @param rate         그중 before 가 먼저였던 비율
     */
    public record Precedence(String before, String after, int orderedCount, double rate) {
    }
}
