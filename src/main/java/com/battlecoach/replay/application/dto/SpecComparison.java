package com.battlecoach.replay.application.dto;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import com.battlecoach.replay.application.dto.ReplayComparison.SkillRow;

/**
 * 두 기록에 나온 스킬의 레벨 비교(스펙). 운용과 섞이지 않게 비교 화면에서 따로 보여준다.
 *
 * @param different 레벨이 다른 스킬. 차이가 큰 순
 * @param missing   한쪽 캐릭터의 스킬 목록에만 있는 스킬 (미보유·미해금)
 * @param same      레벨이 같은 스킬
 */
public record SpecComparison(List<Row> different, List<Row> missing, List<Row> same) {

    public SpecComparison {
        different = List.copyOf(different);
        missing = List.copyOf(missing);
        same = List.copyOf(same);
    }

    /** 양쪽 모두 스킬 목록에 없는 스킬(소울 컨트랙트 같은 공용 스킬)은 뺀다. */
    public static SpecComparison from(List<SkillRow> skills) {
        List<Row> rows = skills.stream()
                .filter(skill -> skill.baseLevel() != null || skill.targetLevel() != null)
                .map(skill -> new Row(skill.skillName(), skill.baseLevel(), skill.targetLevel()))
                .toList();
        return new SpecComparison(
                rows.stream()
                        .filter(Row::isBothOwned)
                        .filter(row -> row.difference() != 0)
                        .sorted(Comparator.comparingInt((Row row) -> Math.abs(row.difference())).reversed()
                                .thenComparing(Row::skillName))
                        .toList(),
                rows.stream().filter(row -> !row.isBothOwned()).sorted(Comparator.comparing(Row::skillName)).toList(),
                rows.stream()
                        .filter(Row::isBothOwned)
                        .filter(row -> row.difference() == 0)
                        .sorted(Comparator.comparing(Row::skillName))
                        .toList());
    }

    /**
     * @param baseLevel   내 레벨. 스킬 목록에 없으면 null
     * @param targetLevel 기준 레벨. 스킬 목록에 없으면 null
     */
    public record Row(String skillName, Integer baseLevel, Integer targetLevel) {

        public boolean isBothOwned() {
            return baseLevel != null && targetLevel != null;
        }

        /** 내 레벨 − 기준 레벨. 한쪽이 없으면 0 */
        public int difference() {
            return isBothOwned() ? baseLevel - targetLevel : 0;
        }

        /** 레벨 막대 폭(%). 두 레벨 중 큰 값을 100%로 본다. */
        public int basePercent() {
            return percent(baseLevel);
        }

        public int targetPercent() {
            return percent(targetLevel);
        }

        private int percent(Integer level) {
            int max = Math.max(Objects.requireNonNullElse(baseLevel, 0), Objects.requireNonNullElse(targetLevel, 0));
            return level == null || max == 0 ? 0 : Math.round(level * 100f / max);
        }
    }
}
