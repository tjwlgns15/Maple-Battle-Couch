package com.battlecoach.diagnosis.burst;

import java.util.List;

import com.battlecoach.diagnosis.domain.BurstWindow;
import com.battlecoach.diagnosis.domain.SkillUsage;

/** 기록에서 극딜 구간을 찾는다. */
public interface BurstDetector {

    /** @return 시작 시각 오름차순, 서로 겹치지 않는 구간 */
    List<BurstWindow> detect(List<SkillUsage> skills);
}
