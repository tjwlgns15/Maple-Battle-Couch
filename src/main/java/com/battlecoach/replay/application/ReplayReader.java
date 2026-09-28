package com.battlecoach.replay.application;

import java.util.Optional;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.battlecoach.replay.application.dto.ReplayDetail;
import com.battlecoach.replay.domain.ReplayId;
import com.battlecoach.replay.repository.ReplayRepository;

import lombok.RequiredArgsConstructor;

/** 지연 로딩 컬렉션을 트랜잭션 안에서 읽어 DTO 로 변환한다. (open-in-view: false) */
@Component
@RequiredArgsConstructor
class ReplayReader {

    private final ReplayRepository replayRepository;

    @Transactional(readOnly = true)
    public Optional<ReplayDetail> find(ReplayId replayId) {
        return replayRepository.findById(replayId.value())
                .map(ReplayDetail::from);
    }
}
