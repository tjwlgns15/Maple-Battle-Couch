package com.battlecoach.replay.application;

import java.util.Comparator;
import java.util.List;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.battlecoach.global.config.CacheNames;
import com.battlecoach.nexon.NexonApiClient;
import com.battlecoach.nexon.NexonDates;
import com.battlecoach.replay.application.dto.ReplayListItem;
import com.battlecoach.replay.domain.CharacterName;

import lombok.RequiredArgsConstructor;

/** 닉네임으로 연무장 기록 목록(기간별 1건)을 조회한다. */
@Service
@RequiredArgsConstructor
public class CharacterReplayService {

    private final OcidResolver ocidResolver;
    private final NexonApiClient nexonApiClient;
    private final ReplayPeriodRecorder replayPeriodRecorder;

    /** 목록에만 있는 기간 정보를 함께 저장한다(통계를 기간별로 나누기 위해). */
    @Cacheable(cacheNames = CacheNames.REPLAY_LIST, key = "#characterName.value()")
    public List<ReplayListItem> findReplays(CharacterName characterName) {
        List<ReplayListItem> items = findByOcid(ocidResolver.resolve(characterName));
        replayPeriodRecorder.record(items);
        return items;
    }

    private List<ReplayListItem> findByOcid(String ocid) {
        return nexonApiClient.findReplayIds(ocid).entries().stream()
                .map(entry -> ReplayListItem.of(
                        entry.periodNo(),
                        NexonDates.toKstDate(entry.registerDate()),
                        entry.replayId()))
                .sorted(Comparator.comparingInt(ReplayListItem::periodNo).reversed())
                .toList();
    }
}
