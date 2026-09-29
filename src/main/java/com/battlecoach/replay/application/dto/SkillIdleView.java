package com.battlecoach.replay.application.dto;

import java.util.List;

import com.battlecoach.diagnosis.domain.IdleSpan;

/**
 * 타임라인에 그릴 스킬 하나의 쉰 구간.
 *
 * @param baseName  타임라인 줄 이름과 같은 baseName
 * @param diagnosed 진단(또는 참고)에 나온 스킬인지. 화면은 기본으로 이 스킬들만 그린다
 * @param spans     표시할 만큼 긴 구간만(시간순)
 */
public record SkillIdleView(
        String baseName,
        String skillName,
        double effectiveCooldownSeconds,
        boolean diagnosed,
        List<IdleSpan> spans
) {

    public SkillIdleView {
        spans = List.copyOf(spans);
    }
}
