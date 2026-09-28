package com.battlecoach.nexon;

import com.battlecoach.nexon.dto.BattlePracticeResultResponse;
import java.time.LocalDate;

import com.battlecoach.nexon.dto.CharacterBasicResponse;
import com.battlecoach.nexon.dto.CharacterInfoBasicResponse;
import com.battlecoach.nexon.dto.OverallRankingResponse;
import com.battlecoach.nexon.dto.RawResponse;
import com.battlecoach.nexon.dto.ReplayIdListResponse;
import com.battlecoach.nexon.dto.SkillTimelineResponse;

/**
 * Nexon Open API 호출 추상화. 호출량 제한과 재시도는 구현체가 책임진다.
 */
public interface NexonApiClient {

    /** GET /maplestory/v1/id?character_name= */
    String findOcid(String characterName);

    /** GET /maplestory/v1/character/basic?ocid= (현재 직업 확인용) */
    CharacterBasicResponse findCharacterBasic(String ocid);

    /** GET /maplestory/v1/battle-practice/replay-id?ocid= */
    ReplayIdListResponse findReplayIds(String ocid);

    /** GET /maplestory/v1/battle-practice/result?replay_id= */
    RawResponse<BattlePracticeResultResponse> findResult(String replayId);

    /** GET /maplestory/v1/battle-practice/skill-timeline?replay_id=&page_no= */
    RawResponse<SkillTimelineResponse> findSkillTimeline(String replayId, int pageNo);

    /** GET /maplestory/v1/battle-practice/character-info?replay_id= */
    RawResponse<CharacterInfoBasicResponse> findCharacterInfo(String replayId);

    /**
     * GET /maplestory/v1/ranking/overall?date=&class=&page=
     *
     * @param jobClass "직업군-전직" 형식. 예: "마법사-비숍", 전직이 없는 직업은 "칼리-전체전직"
     */
    OverallRankingResponse findOverallRanking(LocalDate date, String jobClass, int page);
}
