package com.battlecoach.replay.application;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import com.battlecoach.nexon.NexonApiClient;
import com.battlecoach.nexon.NexonApiException;
import com.battlecoach.nexon.dto.CharacterInfoBasicResponse;
import com.battlecoach.nexon.dto.RawResponse;
import com.battlecoach.replay.repository.ReplayRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 마지막으로 API 와 맞춘 지 오래된 리플레이를 다시 확인한다. 1건에 API 1건(character-info)을 쓴다.
 * <ul>
 *   <li>조회되면 캐릭터 기본 정보(이름·직업·레벨)와 character-info 원문을 새 값으로 바꾼다. 데미지·시전 기록은 바뀌지 않는 값이라 다시 받지 않는다.</li>
 *   <li>4xx(호출량 초과 제외)가 오면 API 에서 지워진 기록으로 보고 저장된 데이터를 지운다.</li>
 *   <li>호출량 초과나 서버 오류가 나면 거기서 멈추고 다음 실행에 이어서 한다.</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReplayRefreshService {

    private final ReplayRepository replayRepository;
    private final NexonApiClient nexonApiClient;
    private final ReplayRefreshWriter replayRefreshWriter;
    private final ReplayRefreshProperties properties;
    private final Clock clock;

    /** @param maxReplays 이번에 갱신할 최대 리플레이 수(= 최대 API 호출 수) */
    public RefreshResult refreshStale(int maxReplays) {
        LocalDateTime now = LocalDateTime.now(clock);
        List<String> targets = replayRepository.findStale(now.minus(properties.maxAge()), PageRequest.of(0, maxReplays));
        int refreshed = 0;
        int deleted = 0;
        for (String replayId : targets) {
            try {
                RawResponse<CharacterInfoBasicResponse> info = nexonApiClient.findCharacterInfo(replayId);
                replayRefreshWriter.apply(replayId, ReplayLoader.toProfile(info.body()), info.rawJson(), LocalDateTime.now(clock));
                refreshed++;
            } catch (NexonApiException e) {
                if (!e.isClientError()) {
                    return new RefreshResult(targets.size(), refreshed, deleted,
                            "Nexon API 오류로 중단: " + e.getHttpStatus() + " " + e.getErrorName());
                }
                log.info("리플레이 {} 가 API 에서 조회되지 않아 지웁니다: {} {}", replayId, e.getErrorName(), e.getMessage());
                replayRefreshWriter.delete(replayId);
                deleted++;
            }
        }
        return new RefreshResult(targets.size(), refreshed, deleted, null);
    }

    public RefreshResult refreshStale() {
        return refreshStale(properties.maxPerRun());
    }

    /**
     * @param targets    이번에 갱신 대상으로 고른 수
     * @param stopReason 중간에 멈췄으면 그 이유
     */
    public record RefreshResult(int targets, int refreshed, int deleted, String stopReason) {
    }
}
