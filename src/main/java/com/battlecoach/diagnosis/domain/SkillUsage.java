package com.battlecoach.diagnosis.domain;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 한 기록에서 스킬 하나를 어떻게 썼는지.
 *
 * @param castTimesMs         시전 시작 시각 오름차순
 * @param damage              result 의 데미지 합(baseName 기준). 데미지 항목이 없는 버프 등은 null
 * @param effectiveCooldownMs 실효 쿨. 쿨 표기가 없거나 스펙을 모르면 null
 * @param durationMs          효과 텍스트의 지속시간. 없으면 null
 */
public record SkillUsage(
        String baseName,
        String skillName,
        List<Long> castTimesMs,
        Long damage,
        Long effectiveCooldownMs,
        Long durationMs
) {

    /** 시전 시작 시각은 입력 지연 등으로 수십~백 ms 흔들리므로 이만큼은 쿨보다 짧아도 정상으로 본다. */
    public static final long EARLY_TOLERANCE_MS = 200;

    public SkillUsage {
        castTimesMs = castTimesMs.stream().sorted().toList();
    }

    /**
     * 지속 중 다시 누른 입력을 앞 시전에 합친다. 불굴의 결의는 발동 후 약 2초에 다시 눌러 끝내는데,
     * 이를 시전으로 세면 간격이 쿨보다 훨씬 짧아 "쿨 변동" 스킬로 잘못 분류된다.
     *
     * @param sortedTimesMs 오름차순 시전 시각
     * @param durationMs    효과 지속시간(버프 지속 증가 미반영)
     */
    public static List<Long> mergeReactivations(List<Long> sortedTimesMs, long durationMs) {
        List<Long> merged = new ArrayList<>();
        for (long time : sortedTimesMs) {
            if (merged.isEmpty() || time > merged.get(merged.size() - 1) + durationMs) {
                merged.add(time);
            }
        }
        return merged;
    }

    public int castCount() {
        return castTimesMs.size();
    }

    public boolean hasCooldown() {
        return effectiveCooldownMs != null;
    }

    /** 오름차순 정렬된 시전 간격 */
    public List<Long> sortedIntervalsMs() {
        List<Long> intervals = new ArrayList<>();
        for (int i = 1; i < castTimesMs.size(); i++) {
            intervals.add(castTimesMs.get(i) - castTimesMs.get(i - 1));
        }
        intervals.sort(Comparator.naturalOrder());
        return intervals;
    }

    /** 실효 쿨보다 짧았던 간격 수. 미적용 발동이나 실행 중 쿨 변동(오블리비온 등)의 흔적이다. */
    public int earlyIntervalCount() {
        if (!hasCooldown()) {
            return 0;
        }
        return (int) sortedIntervalsMs().stream()
                .filter(interval -> interval < effectiveCooldownMs - EARLY_TOLERANCE_MS)
                .count();
    }

    public double earlyIntervalRatio() {
        int intervals = castTimesMs.size() - 1;
        return intervals <= 0 ? 0 : (double) earlyIntervalCount() / intervals;
    }

    /** 이 스킬 시전 중 다른 스킬 시전이 windowMs 안에 있었던 횟수 */
    public int pairedCountWith(SkillUsage partner, long windowMs) {
        return (int) castTimesMs.stream()
                .filter(time -> partner.castTimesMs().stream().anyMatch(other -> Math.abs(other - time) <= windowMs))
                .count();
    }
}
