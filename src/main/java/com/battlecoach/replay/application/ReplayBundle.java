package com.battlecoach.replay.application;

import com.battlecoach.replay.domain.Replay;
import com.battlecoach.replay.domain.ReplayRawData;

/** API 에서 받아 조립한, 아직 저장되지 않은 리플레이와 원문 데이터 */
record ReplayBundle(Replay replay, ReplayRawData rawData) {
}
