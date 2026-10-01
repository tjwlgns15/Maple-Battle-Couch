package com.battlecoach.replay.application;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.battlecoach.diagnosis.domain.AnalysisContext;
import com.battlecoach.diagnosis.domain.DiagnosisEngine;
import com.battlecoach.diagnosis.domain.SkillUsage;
import com.battlecoach.diagnosis.sequence.BurstOrderExtractor;
import com.battlecoach.diagnosis.sequence.SequenceAligner;
import com.battlecoach.replay.application.dto.CooldownStatsView;
import com.battlecoach.replay.application.dto.ReplayComparison;
import com.battlecoach.replay.application.dto.ReplayComparison.SkillRow;
import com.battlecoach.replay.application.dto.ReplayDetail;
import com.battlecoach.replay.application.dto.ReplayDetail.CastView;
import com.battlecoach.replay.application.dto.SpecComparison;
import com.battlecoach.spec.domain.SkillSpec;

import lombok.RequiredArgsConstructor;

/** 내 기록(base)을 기준 기록(target)과 비교한다. 두 리플레이 모두 저장돼 있어야 한다. */
@Service
@RequiredArgsConstructor
public class ReplayComparisonService {

    private final AnalysisContextFactory analysisContextFactory;
    private final DiagnosisEngine diagnosisEngine;
    private final BurstOrderExtractor burstOrderExtractor;
    private final SequenceAligner sequenceAligner;

    public ReplayComparison compare(ReplayDetail base, ReplayDetail target) {
        if (base.replayId().equals(target.replayId())) {
            throw new IllegalArgumentException("같은 기록끼리는 비교할 수 없습니다.");
        }
        requireSameClass(base.characterClass(), target.characterClass());
        AnalysisContext baseContext = analysisContextFactory.create(base);
        AnalysisContext targetContext = analysisContextFactory.create(target);
        AnalysisContext compared = baseContext.comparedWith(targetContext);
        double scale = baseContext.playTimeScaleTo(targetContext);

        List<SkillRow> skills = skillRows(base, baseContext, target, targetContext, scale);
        return new ReplayComparison(
                base,
                target,
                scale,
                diagnosisEngine.diagnose(compared),
                skills,
                SpecComparison.from(skills),
                cooldownView(baseContext),
                cooldownView(targetContext),
                sequenceAligner.align(
                        burstOrderExtractor.firstBurstOrder(baseContext),
                        burstOrderExtractor.firstBurstOrder(targetContext)),
                baseContext.bursts(),
                targetContext.bursts());
    }

    private static CooldownStatsView cooldownView(AnalysisContext context) {
        return CooldownStatsView.of(context.cooldownStats(), context.spec().cooldownSources());
    }

    /**
     * 다른 직업끼리는 스킬 구성이 달라 시전 수·구성 비교가 의미 없다. 비교 대상 선택 화면에서도 거르지만
     * URL 을 직접 입력한 경우를 위해 여기서도 막는다.
     */
    public static void requireSameClass(String baseClass, String targetClass) {
        if (!baseClass.equals(targetClass)) {
            throw new IllegalArgumentException(
                    "같은 직업끼리만 비교할 수 있습니다. (내 기록: " + baseClass + ", 기준 기록: " + targetClass + ")");
        }
    }

    /** 데미지 통계와 시전 기록에 나온 모든 스킬을 baseName 으로 모은다. 패시브·연동 스킬도 포함한다. */
    private static List<SkillRow> skillRows(ReplayDetail base, AnalysisContext baseContext,
                                            ReplayDetail target, AnalysisContext targetContext, double scale) {
        Map<String, Long> baseDamage = AnalysisContextFactory.damageByBaseName(base.skillStats());
        Map<String, Long> targetDamage = AnalysisContextFactory.damageByBaseName(target.skillStats());

        Set<String> baseNames = new LinkedHashSet<>();
        baseNames.addAll(baseDamage.keySet());
        baseNames.addAll(targetDamage.keySet());
        base.casts().stream().map(CastView::baseName).forEach(baseNames::add);
        target.casts().stream().map(CastView::baseName).forEach(baseNames::add);

        return baseNames.stream()
                .map(baseName -> {
                    Side mine = Side.of(baseName, baseContext, baseDamage, displayName(baseName, base));
                    Side ref = Side.of(baseName, targetContext, targetDamage, displayName(baseName, target));
                    return toRow(mine.name() != null ? mine.name() : ref.name(), mine, ref, scale);
                })
                .sorted(Comparator.comparingDouble((SkillRow row) -> Math.max(row.baseSeconds(), row.targetSecondsScaled()))
                        .reversed())
                .toList();
    }

    private static SkillRow toRow(String skillName, Side mine, Side ref, double scale) {
        Double myPerCast = mine.perCastSeconds();
        Double refPerCast = ref.perCastSeconds();
        Double castEffect = null;
        Double efficiencyEffect = null;
        if (myPerCast != null && refPerCast != null) {
            castEffect = (mine.casts() - ref.casts() * scale) * refPerCast;
            efficiencyEffect = (myPerCast - refPerCast) * mine.casts();
        }
        return new SkillRow(
                skillName,
                mine.level(),
                ref.level(),
                mine.casts(),
                ref.casts(),
                mine.seconds(),
                ref.seconds() * scale,
                myPerCast,
                refPerCast,
                castEffect,
                efficiencyEffect);
    }

    /** 시전 기록이 있으면 timeline 표기(헥사 접미사 없음), 없으면 result 표기를 쓴다. */
    private static String displayName(String baseName, ReplayDetail replay) {
        return replay.casts().stream()
                .filter(cast -> cast.baseName().equals(baseName))
                .map(CastView::skillName)
                .findFirst()
                .or(() -> replay.skillStats().stream()
                        .filter(stat -> stat.baseName().equals(baseName))
                        .map(ReplayDetail.SkillStatView::skillName)
                        .findFirst())
                .orElse(null);
    }

    private record Side(String name, Integer level, int casts, double seconds) {

        static Side of(String baseName, AnalysisContext context, Map<String, Long> damage, String name) {
            int casts = context.find(baseName).map(SkillUsage::castCount).orElse(0);
            double seconds = context.toSeconds(damage.getOrDefault(baseName, 0L));
            Integer level = context.spec().find(baseName).map(SkillSpec::level).orElse(null);
            return new Side(name, level, casts, seconds);
        }

        Double perCastSeconds() {
            return casts > 0 && seconds > 0 ? seconds / casts : null;
        }
    }
}
