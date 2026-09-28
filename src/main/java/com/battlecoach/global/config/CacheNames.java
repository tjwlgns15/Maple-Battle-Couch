package com.battlecoach.global.config;

public final class CacheNames {

    /** 캐릭터명 → ocid. ocid는 게임 콘텐츠 변경으로 바뀔 수 있어 TTL을 둔다. */
    public static final String OCID = "ocid";

    /** 캐릭터명 → 현재 직업. 비교 대상 고를 때 다른 직업을 걸러낸다. 전직 변경은 드물어 ocid 와 같은 TTL 을 둔다. */
    public static final String CHARACTER_CLASS = "characterClass";

    /** 캐릭터명 → 연무장 기록 목록. 새 기록 등록을 반영하기 위해 짧은 TTL을 둔다. */
    public static final String REPLAY_LIST = "replayList";

    /** 리플레이 → 스킬 스펙. 리플레이 원문에서 파싱하므로 바뀌지 않아 크기로만 제한한다. */
    public static final String CHARACTER_SPEC = "characterSpec";

    /** 직업·기간(·제외 리플레이) → 랭커 통계. 수집 중 표본이 늘어나므로 짧은 TTL 을 둔다. */
    public static final String JOB_STATISTICS = "jobStatistics";

    private CacheNames() {
    }
}
