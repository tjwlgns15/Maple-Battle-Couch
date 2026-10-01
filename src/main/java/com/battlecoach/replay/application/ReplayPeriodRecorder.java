package com.battlecoach.replay.application;

import java.util.List;
import java.util.OptionalInt;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.battlecoach.replay.application.dto.ReplayListItem;
import com.battlecoach.replay.domain.ReplayId;
import com.battlecoach.replay.domain.ReplayPeriod;
import com.battlecoach.replay.repository.ReplayPeriodRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** replay-id 목록에서 본 (리플레이, 기간)을 저장하고 조회한다. */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReplayPeriodRecorder {

    private final ReplayPeriodRepository replayPeriodRepository;

    @Transactional
    public void record(List<ReplayListItem> items) {
        Set<String> known = replayPeriodRepository.findAllById(items.stream().map(ReplayListItem::replayId).toList())
                .stream()
                .map(ReplayPeriod::getReplayId)
                .collect(Collectors.toSet());
        List<ReplayPeriod> fresh = items.stream()
                .filter(item -> !known.contains(item.replayId()))
                .map(item -> ReplayPeriod.of(item.replayId(), item.periodNo()))
                .toList();
        try {
            replayPeriodRepository.saveAll(fresh);
        } catch (DataIntegrityViolationException e) {
            log.info("다른 요청이 같은 리플레이 기간을 먼저 저장했습니다: {}", e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public OptionalInt periodOf(ReplayId replayId) {
        return replayPeriodRepository.findById(replayId.value())
                .map(period -> OptionalInt.of(period.getPeriodNo()))
                .orElse(OptionalInt.empty());
    }

}
