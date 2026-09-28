package com.battlecoach.replay.api;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.battlecoach.replay.application.CharacterReplayService;
import com.battlecoach.replay.application.ReplayQueryService;
import com.battlecoach.replay.application.dto.ReplayDetail;
import com.battlecoach.replay.application.dto.ReplayListItem;
import com.battlecoach.replay.domain.CharacterName;
import com.battlecoach.replay.domain.ReplayId;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ReplayApiController {

    private final CharacterReplayService characterReplayService;
    private final ReplayQueryService replayQueryService;

    @GetMapping("/characters/{characterName}/replays")
    public List<ReplayListItem> findReplays(@PathVariable String characterName) {
        return characterReplayService.findReplays(CharacterName.of(characterName));
    }

    @GetMapping("/replays/{replayId}")
    public ReplayDetail getReplay(@PathVariable String replayId) {
        return replayQueryService.getReplay(ReplayId.of(replayId));
    }
}
