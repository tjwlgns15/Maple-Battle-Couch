package com.battlecoach.diagnosis.burst;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.battlecoach.diagnosis.domain.BurstWindow;
import com.battlecoach.diagnosis.domain.SkillUsage;

/**
 * 쿨이 긴 스킬이 짧은 시간에 몰린 곳을 극딜 시작으로 본다. 직업별 극딜 스킬 목록 없이 동작한다.
 * 구간 길이는 몰린 스킬들의 효과 텍스트 "N초 동안" 중앙값이다. (버프 지속시간 증가는 반영하지 않아 보수적이다)
 * 칼리 2명에서는 120초 주기 극딜 3회를 찾았다. 60초 주기 준극딜은 대상이 아니다.
 */
@Component
public class CooldownClusterBurstDetector implements BurstDetector {

    static final long LONG_COOLDOWN_MS = 90_000;
    static final long CLUSTER_SPAN_MS = 3_000;
    static final int MIN_DISTINCT_SKILLS = 3;

    @Override
    public List<BurstWindow> detect(List<SkillUsage> skills) {
        List<Cast> casts = skills.stream()
                .filter(SkillUsage::hasCooldown)
                .filter(skill -> skill.effectiveCooldownMs() >= LONG_COOLDOWN_MS)
                .flatMap(skill -> skill.castTimesMs().stream().map(time -> new Cast(time, skill)))
                .sorted(Comparator.comparingLong(Cast::timeMs))
                .toList();

        List<BurstWindow> windows = new ArrayList<>();
        int i = 0;
        while (i < casts.size()) {
            long start = casts.get(i).timeMs();
            Set<SkillUsage> cluster = new LinkedHashSet<>();
            int next = i;
            while (next < casts.size() && casts.get(next).timeMs() <= start + CLUSTER_SPAN_MS) {
                cluster.add(casts.get(next).skill());
                next++;
            }
            Long duration = cluster.size() >= MIN_DISTINCT_SKILLS ? medianDuration(cluster) : null;
            if (duration == null) {
                i++;
                continue;
            }
            addMerging(windows, new BurstWindow(start, start + duration));
            i = next;
        }
        return List.copyOf(windows);
    }

    private static Long medianDuration(Set<SkillUsage> cluster) {
        List<Long> durations = cluster.stream()
                .map(SkillUsage::durationMs)
                .filter(Objects::nonNull)
                .sorted()
                .toList();
        return durations.isEmpty() ? null : durations.get(durations.size() / 2);
    }

    private static void addMerging(List<BurstWindow> windows, BurstWindow window) {
        if (!windows.isEmpty()) {
            BurstWindow last = windows.get(windows.size() - 1);
            if (window.startMs() <= last.endMs()) {
                windows.set(windows.size() - 1,
                        new BurstWindow(last.startMs(), Math.max(last.endMs(), window.endMs())));
                return;
            }
        }
        windows.add(window);
    }

    private record Cast(long timeMs, SkillUsage skill) {
    }
}
