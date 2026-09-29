package com.battlecoach.diagnosis.statistics;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * 직업·기간별 랭커 표본 통계. 레벨 랭킹에서 모은 표본이라 "잘 치는 사람"의 통계라는 보장은 없다.
 *
 * @param pairs      표본 다수가 함께 쓰는 스킬 쌍만 담는다
 * @param burstOrder 모든 극딜 구간의 스킬 순서 통계
 */
public record JobStatistics(
        String characterClass,
        int periodNo,
        int sampleCount,
        Map<String, SkillDistribution> skills,
        List<PairStatistic> pairs,
        BurstOrderStatistics burstOrder
) {

    /** 이보다 표본이 적으면 통계 진단을 내지 않는다. */
    public static final int MIN_SAMPLES = 5;

    public JobStatistics {
        skills = Map.copyOf(skills);
        pairs = List.copyOf(pairs);
        burstOrder = burstOrder == null ? BurstOrderStatistics.empty() : burstOrder;
    }

    public boolean isReliable() {
        return sampleCount >= MIN_SAMPLES;
    }

    public Optional<SkillDistribution> skill(String baseName) {
        return Optional.ofNullable(skills.get(baseName));
    }

    /** 가장 많은 표본이 함께 쓰는 짝. 비율이 같으면 두 스킬을 모두 쓴 표본이 많은 쪽이다. */
    public Optional<PairStatistic> bestPairFor(String baseName) {
        return pairs.stream()
                .filter(pair -> pair.baseName().equals(baseName))
                .max(Comparator.comparingDouble(PairStatistic::pairedRate)
                        .thenComparingInt(PairStatistic::bothUsedCount)
                        .thenComparing(PairStatistic::partnerBaseName, Comparator.reverseOrder()));
    }

    /**
     * 함께 쓰는 쌍을 이어 묶음으로 만든다(연결 요소). 극딜 버프끼리는 모두 서로 짝이라 쌍으로 나열하면 수십 개가 된다.
     * 칼리 4기: 2분 극딜 버프 6개, 1분 주기 데스 블로섬·스틱스·레디 투 다이.
     *
     * @return 묶음별 baseName 목록. 큰 묶음부터
     */
    public List<List<String>> pairGroups() {
        Map<String, Set<String>> adjacency = new LinkedHashMap<>();
        for (PairStatistic pair : pairs) {
            adjacency.computeIfAbsent(pair.baseName(), key -> new LinkedHashSet<>()).add(pair.partnerBaseName());
            adjacency.computeIfAbsent(pair.partnerBaseName(), key -> new LinkedHashSet<>()).add(pair.baseName());
        }
        Set<String> visited = new LinkedHashSet<>();
        List<List<String>> groups = new ArrayList<>();
        for (String start : adjacency.keySet()) {
            if (!visited.add(start)) {
                continue;
            }
            List<String> group = new ArrayList<>();
            List<String> queue = new ArrayList<>(List.of(start));
            while (!queue.isEmpty()) {
                String current = queue.remove(0);
                group.add(current);
                for (String next : adjacency.getOrDefault(current, Set.of())) {
                    if (visited.add(next)) {
                        queue.add(next);
                    }
                }
            }
            groups.add(List.copyOf(group));
        }
        groups.sort(Comparator.comparingInt((List<String> group) -> group.size()).reversed());
        return List.copyOf(groups);
    }

    /** 묶음 안 쌍들의 가장 낮은 비율 */
    public double minPairedRate(List<String> group) {
        return pairs.stream()
                .filter(pair -> group.contains(pair.baseName()) && group.contains(pair.partnerBaseName()))
                .mapToDouble(PairStatistic::pairedRate)
                .min()
                .orElse(0);
    }
}
