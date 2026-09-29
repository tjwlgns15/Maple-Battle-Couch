package com.battlecoach.diagnosis.rule;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.battlecoach.diagnosis.domain.AnalysisContext;
import com.battlecoach.diagnosis.domain.DiagnosisRule;
import com.battlecoach.diagnosis.domain.Finding;
import com.battlecoach.diagnosis.domain.FindingType;
import com.battlecoach.diagnosis.domain.SkillUsage;
import com.battlecoach.spec.domain.SkillSpec;

/**
 * 비교 진단: 기준 기록은 썼는데 이 기록은 한 번도 쓰지 않은 스킬.
 * 이 캐릭터의 스킬 목록(character-info)에 있으면 "미사용"(운용), 없으면 "미보유·미해금"(스펙)이다.
 * 예: 플레게톤은 솔 헤카테 30레벨에 해금되는데 칼리 B는 8레벨이라 스킬 목록에 없다.
 * 영향도는 기준 기록의 초 환산으로 추정한다. (스펙이 다르면 실제 효과와 차이가 날 수 있다)
 */
@Component
public class LoadoutRule implements DiagnosisRule {

    @Override
    public List<Finding> evaluate(AnalysisContext context) {
        Optional<AnalysisContext> reference = context.referenceContext();
        if (reference.isEmpty()) {
            return List.of();
        }
        AnalysisContext ref = reference.get();

        List<Finding> findings = new ArrayList<>();
        for (SkillUsage refSkill : ref.skills()) {
            boolean usedByMe = context.find(refSkill.baseName()).map(skill -> skill.castCount() > 0).orElse(false);
            // 기준 캐릭터의 스킬 목록에도 없는 스킬(소울 컨트랙트 등 공용 스킬)은 보유 여부를 판단할 수 없다.
            if (usedByMe || refSkill.castCount() == 0 || ref.spec().find(refSkill.baseName()).isEmpty()) {
                continue;
            }
            Optional<SkillSpec> mySpec = context.spec().find(refSkill.baseName());
            FindingType type = mySpec.isPresent() ? FindingType.UNUSED_SKILL : FindingType.MISSING_SKILL;
            String message = mySpec
                    .map(spec -> String.format(Locale.ROOT,
                            "기준 기록은 %d회 썼는데 이 기록은 한 번도 쓰지 않았습니다. 스킬은 보유하고 있습니다(Lv%d).",
                            refSkill.castCount(), spec.level()))
                    .orElseGet(() -> String.format(Locale.ROOT,
                            "기준 기록은 %d회 썼지만 이 캐릭터의 스킬 목록에 없습니다(미보유 또는 미해금).",
                            refSkill.castCount()));

            if (refSkill.damage() == null || refSkill.damage() <= 0) {
                findings.add(Finding.unmeasured(type, refSkill, message + " 데미지가 없는 스킬(버프 등)이라 영향도는 계산하지 않았습니다."));
                continue;
            }
            double estimated = ref.toSeconds(refSkill.damage()) * context.playTimeScaleTo(ref);
            findings.add(Finding.measured(type, refSkill, estimated,
                    message + " 영향도는 기준 기록의 초 환산으로 추정했습니다."));
        }
        return findings;
    }
}
