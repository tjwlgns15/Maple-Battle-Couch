package com.battlecoach.replay.application;

import java.util.Optional;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import com.battlecoach.global.concurrent.SingleFlight;
import com.battlecoach.replay.application.dto.ReplayDetail;
import com.battlecoach.replay.domain.ReplayId;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 리플레이 조회. DB 에 있으면 그대로 반환하고, 없으면 Nexon API 에서 받아 저장한 뒤 반환한다.
 * 같은 리플레이에 대한 동시 요청은 SingleFlight 로 합쳐 API 를 한 번만 호출한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReplayQueryService {

    private final ReplayReader replayReader;
    private final ReplayLoader replayLoader;
    private final ReplayWriter replayWriter;
    private final SingleFlight<String, ReplayDetail> singleFlight = new SingleFlight<>();

    /** DB 에 저장된 리플레이만 본다. API 를 부르지 않는다(통계 계산용). */
    public Optional<ReplayDetail> findStored(ReplayId replayId) {
        return replayReader.find(replayId);
    }

    public ReplayDetail getReplay(ReplayId replayId) {
        return replayReader.find(replayId)
                .orElseGet(() -> singleFlight.execute(replayId.value(), () -> loadAndStore(replayId)));
    }

    private ReplayDetail loadAndStore(ReplayId replayId) {
        // 앞선 요청이 방금 저장을 끝냈을 수 있으므로 한 번 더 확인한다.
        return replayReader.find(replayId).orElseGet(() -> {
            ReplayBundle bundle = replayLoader.load(replayId);
            try {
                replayWriter.saveIfAbsent(bundle);
            } catch (DataIntegrityViolationException e) {
                log.info("리플레이 {} 는 다른 인스턴스가 먼저 저장했습니다.", replayId.value());
            }
            return replayReader.find(replayId)
                    .orElseThrow(() -> new IllegalStateException("저장한 리플레이를 찾을 수 없습니다: " + replayId.value()));
        });
    }
}
