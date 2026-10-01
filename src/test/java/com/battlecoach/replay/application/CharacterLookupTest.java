package com.battlecoach.replay.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

import com.battlecoach.nexon.NexonApiClient;
import com.battlecoach.nexon.NexonApiException;
import com.battlecoach.replay.domain.CharacterName;

/** 없는 캐릭터와 기록이 없는 캐릭터는 둘 다 400 OPENAPI00004 가 와서, 어느 단계에서 났는지로 구분한다. */
class CharacterLookupTest {

    private static final NexonApiException INVALID_PARAMETER =
            NexonApiException.of(400, "OPENAPI00004", "Please input valid parameter");

    private final NexonApiClient nexonApiClient = mock(NexonApiClient.class);
    private final OcidResolver ocidResolver = new OcidResolver(nexonApiClient);

    @Test
    void ocid_조회가_4xx면_없는_캐릭터다() {
        when(nexonApiClient.findOcid("없는이름")).thenThrow(INVALID_PARAMETER);

        assertThatThrownBy(() -> ocidResolver.resolve(CharacterName.of("없는이름")))
                .isInstanceOf(CharacterNotFoundException.class)
                .hasMessageContaining("없는이름");
    }

    @Test
    void 호출량_초과는_없는_캐릭터로_바꾸지_않는다() {
        when(nexonApiClient.findOcid("이름")).thenThrow(NexonApiException.of(429, "OPENAPI00007", "rate"));

        assertThatThrownBy(() -> ocidResolver.resolve(CharacterName.of("이름"))).isInstanceOf(NexonApiException.class);
    }

    @Test
    void 기록_목록이_4xx면_기록이_없는_것으로_빈_목록을_준다() {
        when(nexonApiClient.findOcid("기록없음")).thenReturn("ocid");
        when(nexonApiClient.findReplayIds("ocid")).thenThrow(INVALID_PARAMETER);
        CharacterReplayService service =
                new CharacterReplayService(ocidResolver, nexonApiClient, mock(ReplayPeriodRecorder.class));

        assertThat(service.findReplays(CharacterName.of("기록없음"))).isEmpty();
    }
}
