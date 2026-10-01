package com.battlecoach.replay.application;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.battlecoach.replay.domain.ReplayPeriod;
import com.battlecoach.replay.domain.StoredReplayChangedEvent;
import com.battlecoach.replay.repository.ReplayPeriodRepository;
import com.battlecoach.replay.repository.ReplayRawDataRepository;
import com.battlecoach.replay.repository.ReplayRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
class ReplayWriter {

    private final ReplayRepository replayRepository;
    private final ReplayRawDataRepository rawDataRepository;
    private final ReplayPeriodRepository replayPeriodRepository;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 이미 저장된 리플레이면 아무것도 하지 않는다(불변 데이터).
     * 다른 서버 인스턴스와 동시에 저장하면 PK 충돌(DataIntegrityViolationException)이 날 수 있으며, 호출자가 처리한다.
     */
    @Transactional
    public void saveIfAbsent(ReplayBundle bundle) {
        String replayId = bundle.replay().getReplayId();
        if (replayRepository.existsById(replayId)) {
            return;
        }
        replayRepository.save(bundle.replay());
        rawDataRepository.save(bundle.rawData());
        eventPublisher.publishEvent(new StoredReplayChangedEvent(
                replayId, bundle.replay().getCharacterProfile().characterClass(), periodOf(replayPeriodRepository, replayId)));
    }

    /** 기록 목록을 거쳐 왔으면 기간이 이미 저장돼 있다. */
    static Integer periodOf(ReplayPeriodRepository repository, String replayId) {
        return repository.findById(replayId).map(ReplayPeriod::getPeriodNo).orElse(null);
    }
}
