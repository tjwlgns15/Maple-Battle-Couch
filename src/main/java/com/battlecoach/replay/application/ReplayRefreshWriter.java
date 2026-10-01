package com.battlecoach.replay.application;

import java.time.LocalDateTime;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.battlecoach.global.config.CacheNames;
import com.battlecoach.replay.domain.CharacterProfile;
import com.battlecoach.replay.domain.StoredReplayChangedEvent;
import com.battlecoach.replay.repository.ReplayPeriodRepository;
import com.battlecoach.replay.repository.ReplayRawDataRepository;
import com.battlecoach.replay.repository.ReplayRepository;

import lombok.RequiredArgsConstructor;

/** 갱신 결과를 DB 에 반영한다. API 호출은 트랜잭션 밖({@link ReplayRefreshService})에서 끝낸다. */
@Component
@RequiredArgsConstructor
class ReplayRefreshWriter {

    private final ReplayRepository replayRepository;
    private final ReplayRawDataRepository rawDataRepository;
    private final ReplayPeriodRepository replayPeriodRepository;
    private final ApplicationEventPublisher eventPublisher;

    /** 다시 받은 character-info 로 캐릭터 기본 정보와 원문을 바꾼다. 원문이 바뀌었을 수 있어 스펙 캐시를 비운다. */
    @Transactional
    @CacheEvict(cacheNames = CacheNames.CHARACTER_SPEC, key = "#replayId")
    public void apply(String replayId, CharacterProfile latestProfile, String characterInfoJson, LocalDateTime now) {
        replayRepository.findById(replayId).ifPresent(replay -> {
            replay.refresh(latestProfile, now);
            eventPublisher.publishEvent(new StoredReplayChangedEvent(replayId, latestProfile.characterClass(),
                    ReplayWriter.periodOf(replayPeriodRepository, replayId)));
        });
        rawDataRepository.findById(replayId).ifPresent(raw -> raw.replaceCharacterInfo(characterInfoJson));
    }

    /**
     * API 에서 더 이상 조회되지 않는 리플레이를 본문, 원문, 기간까지 지운다.
     * 캐시를 비우려면 기간이 필요하므로 지우기 전에 기간을 읽어 이벤트에 담는다.
     */
    @Transactional
    @CacheEvict(cacheNames = CacheNames.CHARACTER_SPEC, key = "#replayId")
    public void delete(String replayId) {
        replayRepository.findById(replayId).ifPresent(replay -> {
            eventPublisher.publishEvent(new StoredReplayChangedEvent(replayId, replay.getCharacterProfile().characterClass(),
                    ReplayWriter.periodOf(replayPeriodRepository, replayId)));
            replayRepository.delete(replay);
        });
        rawDataRepository.deleteById(replayId);
        replayPeriodRepository.deleteById(replayId);
    }
}
