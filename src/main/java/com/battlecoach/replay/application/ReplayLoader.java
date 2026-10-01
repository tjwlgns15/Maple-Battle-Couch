package com.battlecoach.replay.application;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.battlecoach.nexon.NexonApiClient;
import com.battlecoach.nexon.NexonDates;
import com.battlecoach.nexon.dto.BattlePracticeResultResponse;
import com.battlecoach.nexon.dto.CharacterInfoBasicResponse;
import com.battlecoach.nexon.dto.RawResponse;
import com.battlecoach.nexon.dto.SkillTimelineResponse;
import com.battlecoach.replay.domain.BattleSummary;
import com.battlecoach.replay.domain.CharacterProfile;
import com.battlecoach.replay.domain.HexaType;
import com.battlecoach.replay.domain.Replay;
import com.battlecoach.replay.domain.ReplayId;
import com.battlecoach.replay.domain.ReplayRawData;
import com.battlecoach.replay.domain.SkillDamage;
import com.battlecoach.replay.domain.SkillName;

import lombok.RequiredArgsConstructor;

/**
 * Nexon API 3종(result, skill-timeline, character-info)을 호출해 Replay 집계를 조립한다.
 * DB 트랜잭션 밖에서 실행해 HTTP 대기 중에 커넥션을 잡지 않는다.
 */
@Component
@RequiredArgsConstructor
class ReplayLoader {

    private static final int FIRST_PAGE = 1;

    private final NexonApiClient nexonApiClient;
    private final Clock clock;

    ReplayBundle load(ReplayId replayId) {
        String id = replayId.value();
        RawResponse<BattlePracticeResultResponse> result = nexonApiClient.findResult(id);
        List<RawResponse<SkillTimelineResponse>> timelinePages = loadAllTimelinePages(id);
        RawResponse<CharacterInfoBasicResponse> characterInfo = nexonApiClient.findCharacterInfo(id);

        Replay replay = Replay.create(
                id,
                toProfile(characterInfo.body()),
                toSummary(result.body()),
                LocalDateTime.now(clock));
        addSkillStats(replay, result.body());
        addCasts(replay, timelinePages);

        ReplayRawData rawData = ReplayRawData.of(
                id,
                result.rawJson(),
                toJsonArray(timelinePages),
                characterInfo.rawJson());
        return new ReplayBundle(replay, rawData);
    }

    private List<RawResponse<SkillTimelineResponse>> loadAllTimelinePages(String replayId) {
        List<RawResponse<SkillTimelineResponse>> pages = new ArrayList<>();
        RawResponse<SkillTimelineResponse> page = nexonApiClient.findSkillTimeline(replayId, FIRST_PAGE);
        pages.add(page);
        while (page.body().hasNextPage()) {
            page = nexonApiClient.findSkillTimeline(replayId, page.body().pageNo() + 1);
            pages.add(page);
        }
        return pages;
    }

    static CharacterProfile toProfile(CharacterInfoBasicResponse characterInfo) {
        CharacterInfoBasicResponse.Basic basic = characterInfo.basicObject();
        if (basic == null) {
            throw new IllegalStateException("character-info 응답에 basic_object 가 없습니다.");
        }
        return CharacterProfile.of(basic.characterName(), basic.characterClass(), basic.characterLevel());
    }

    private static BattleSummary toSummary(BattlePracticeResultResponse result) {
        return BattleSummary.of(
                NexonDates.toKstDate(result.registerDate()),
                result.totalPlayTime(),
                result.totalDamage(),
                result.totalDps(),
                result.endType(),
                result.likeCount());
    }

    private static void addSkillStats(Replay replay, BattlePracticeResultResponse result) {
        for (BattlePracticeResultResponse.SkillStatistic stat : result.statistics()) {
            replay.addSkillStat(
                    SkillName.of(stat.skillName()),
                    SkillDamage.of(
                            stat.damage(),
                            toPercent(stat.damagePercent()),
                            stat.dps(),
                            stat.useCount(),
                            stat.damagePerUse(),
                            stat.attackCount(),
                            stat.maxDamage(),
                            stat.minDamage()));
        }
    }

    private static void addCasts(Replay replay, List<RawResponse<SkillTimelineResponse>> pages) {
        int order = 0;
        for (RawResponse<SkillTimelineResponse> page : pages) {
            for (SkillTimelineResponse.Event event : page.body().events()) {
                replay.addCast(
                        order++,
                        event.elapseTime(),
                        SkillName.of(event.skillName()),
                        HexaType.fromFlag(event.hexaSkillSpecificityFlag()),
                        event.sequenceName(),
                        event.sequenceKey());
            }
        }
    }

    private static BigDecimal toPercent(String value) {
        return value == null || value.isBlank() ? BigDecimal.ZERO : new BigDecimal(value.strip());
    }

    private static String toJsonArray(List<RawResponse<SkillTimelineResponse>> pages) {
        return pages.stream()
                .map(RawResponse::rawJson)
                .collect(Collectors.joining(",", "[", "]"));
    }
}
