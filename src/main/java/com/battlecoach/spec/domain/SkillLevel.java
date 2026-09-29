package com.battlecoach.spec.domain;

/**
 * 스킬 한 개의 데미지를 정하는 레벨. 강화 코어(V 매트릭스)는 character_skill 에 "헥스 : 판데모니움 강화"처럼 따로 오므로 함께 본다.
 *
 * @param enhancementLevel "{스킬} 강화" 항목의 레벨. 없으면 null
 */
public record SkillLevel(int level, Integer enhancementLevel) {

    public String label() {
        return enhancementLevel == null ? "Lv" + level : "Lv" + level + " · 강화 " + enhancementLevel;
    }
}
