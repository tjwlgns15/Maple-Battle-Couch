package com.battlecoach.replay.web;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.battlecoach.diagnosis.domain.BurstWindow;
import com.battlecoach.replay.application.CharacterClassResolver;
import com.battlecoach.replay.application.CharacterReplayService;
import com.battlecoach.replay.application.ReplayComparisonService;
import com.battlecoach.replay.application.ReplayQueryService;
import com.battlecoach.replay.application.dto.ReplayComparison;
import com.battlecoach.replay.application.dto.ReplayComparison.SkillRow;
import com.battlecoach.replay.application.dto.ReplayDetail;
import com.battlecoach.replay.application.dto.ReplayListItem;
import com.battlecoach.replay.domain.CharacterName;
import com.battlecoach.replay.domain.ReplayId;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class ReplayCompareController {

    private final ReplayQueryService replayQueryService;
    private final CharacterReplayService characterReplayService;
    private final CharacterClassResolver characterClassResolver;
    private final ReplayComparisonService replayComparisonService;
    private final ScriptJsonWriter scriptJsonWriter;

    /**
     * 비교할 기준 기록 고르기. 캐릭터명을 주지 않으면 같은 캐릭터의 다른 기록을 보여준다.
     * 같은 캐릭터라도 전직 전후 기록이면 직업이 다를 수 있으나, 그 경우는 비교 시점에 서비스가 막는다.
     */
    @GetMapping("/compare/select")
    public String select(@RequestParam String base,
                         @RequestParam(required = false) String name,
                         Model model) {
        ReplayDetail baseReplay = replayQueryService.getReplay(ReplayId.of(base));
        CharacterName targetName = CharacterName.of(name == null || name.isBlank() ? baseReplay.characterName() : name);
        model.addAttribute("base", baseReplay);
        model.addAttribute("targetName", targetName.value());

        // 다른 캐릭터면 현재 직업을 먼저 확인한다(1건). 다른 직업이면 기록 목록을 부르지 않는다.
        if (!targetName.value().equals(baseReplay.characterName())) {
            String targetClass = characterClassResolver.resolve(targetName);
            if (!targetClass.equals(baseReplay.characterClass())) {
                model.addAttribute("classMismatch", "같은 직업끼리만 비교할 수 있습니다. "
                        + targetName.value() + "의 직업은 " + targetClass + "입니다.");
                model.addAttribute("candidates", List.of());
                return "compare-select";
            }
        }
        List<ReplayListItem> candidates = characterReplayService.findReplays(targetName).stream()
                .filter(item -> !item.replayId().equals(base))
                .toList();
        model.addAttribute("candidates", candidates);
        return "compare-select";
    }

    @GetMapping("/compare")
    public String compare(@RequestParam String base, @RequestParam String target, Model model) {
        ReplayComparison comparison = replayComparisonService.compare(
                replayQueryService.getReplay(ReplayId.of(base)),
                replayQueryService.getReplay(ReplayId.of(target)));

        model.addAttribute("c", comparison);
        model.addAttribute("chartJson", scriptJsonWriter.write(new CompareChartData(
                comparison.base(), comparison.baseBursts(),
                comparison.target(), comparison.targetBursts(),
                comparison.skills(), comparison.playTimeScale())));
        return "compare";
    }

    /**
     * 비교 화면 차트(합친 타임라인, 시전 횟수 차이, 딜 비중 차이 원인)에 필요한 데이터
     *
     * @param playTimeScale 기준 기록의 시전 수를 내 전투 시간에 맞춘 배율
     */
    public record CompareChartData(
            ReplayDetail base,
            List<BurstWindow> baseBursts,
            ReplayDetail target,
            List<BurstWindow> targetBursts,
            List<SkillRow> skills,
            double playTimeScale
    ) {
    }
}
