package com.battlecoach.replay.web;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.battlecoach.replay.application.CharacterReplayService;
import com.battlecoach.replay.application.ReplayAnalysisService;
import com.battlecoach.replay.application.ReplayQueryService;
import com.battlecoach.replay.application.dto.CooldownReport;
import com.battlecoach.replay.application.dto.RankerStanding;
import com.battlecoach.replay.application.dto.SkillIdleView;
import com.battlecoach.replay.application.dto.ReplayAnalysis;
import com.battlecoach.replay.application.dto.ReplayDetail;
import com.battlecoach.replay.domain.CharacterName;
import com.battlecoach.replay.domain.ReplayId;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class ReplayPageController {

    private final CharacterReplayService characterReplayService;
    private final ReplayQueryService replayQueryService;
    private final ReplayAnalysisService replayAnalysisService;
    private final ScriptJsonWriter scriptJsonWriter;

    @GetMapping("/")
    public String index() {
        return "index";
    }

    /** 검색 폼 제출을 캐릭터 URL로 보낸다. 캐릭터명은 RedirectAttributes 가 URL 인코딩한다. */
    @GetMapping("/search")
    public String search(@RequestParam String characterName, RedirectAttributes redirectAttributes) {
        redirectAttributes.addAttribute("characterName", CharacterName.of(characterName).value());
        return "redirect:/characters/{characterName}";
    }

    @GetMapping("/characters/{characterName}")
    public String replays(@PathVariable String characterName, Model model) {
        CharacterName name = CharacterName.of(characterName);
        model.addAttribute("characterName", name.value());
        model.addAttribute("replays", characterReplayService.findReplays(name));
        return "character-replays";
    }

    @GetMapping("/replays/{replayId}")
    public String replay(@PathVariable String replayId, Model model) {
        ReplayDetail replay = replayQueryService.getReplay(ReplayId.of(replayId));
        ReplayAnalysis analysis = replayAnalysisService.analyze(replay);
        model.addAttribute("replay", replay);
        model.addAttribute("diagnosis", analysis.diagnosis());
        model.addAttribute("cooldowns", analysis.cooldowns());
        model.addAttribute("standing", analysis.rankerStanding());
        model.addAttribute("idleSpans", analysis.idleSpans());
        model.addAttribute("replayJson", scriptJsonWriter.write(replay));
        model.addAttribute("burstJson", scriptJsonWriter.write(analysis.bursts()));
        model.addAttribute("analysisJson", scriptJsonWriter.write(new AnalysisChartData(
                analysis.rankerStanding().rows(), analysis.cooldowns().rows(), analysis.idleSpans())));
        return "replay-detail";
    }

    /** 상세 화면 분석 차트(랭커 분포 위치, 쿨 대비 사용 간격, 타임라인 쉰 구간)에 필요한 데이터 */
    public record AnalysisChartData(List<RankerStanding.Row> standing, List<CooldownReport.Row> cooldowns,
                                    List<SkillIdleView> idle) {
    }
}
