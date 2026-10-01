package com.battlecoach.replay.api;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.battlecoach.replay.application.ReplayRefreshService;
import com.battlecoach.replay.application.ReplayRefreshService.RefreshResult;

import lombok.RequiredArgsConstructor;

/**
 * 리플레이 관리 API. 인증이 없으므로 admin.enabled=true 일 때만 등록한다(로컬 전용).
 * 예: POST /api/admin/replays/refresh?max=50
 */
@RestController
@RequestMapping("/api/admin/replays")
@ConditionalOnProperty(prefix = "admin", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
public class ReplayAdminController {

    private final ReplayRefreshService replayRefreshService;

    /** 매일 도는 갱신을 지금 돌린다. max 는 API 호출 수 상한이다. */
    @PostMapping("/refresh")
    public RefreshResult refresh(@RequestParam(defaultValue = "50") int max) {
        return replayRefreshService.refreshStale(Math.max(1, Math.min(max, 800)));
    }
}
