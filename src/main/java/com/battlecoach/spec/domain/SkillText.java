package com.battlecoach.spec.domain;

import java.util.Arrays;
import java.util.OptionalLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 스킬 효과·설명 텍스트에서 쿨타임 관련 정보를 읽는다.
 * Nexon 응답에는 실제 줄바꿈과 문자 그대로의 "\n" 이 섞여 있어 먼저 줄바꿈으로 정규화한다.
 */
public record SkillText(String value) {

    /**
     * 자기 쿨 표기는 줄 맨 앞에 온다. 예: "재사용 대기시간 30초", "재사용 대기시간 : 240초".
     * "보이드 러쉬의 재사용 대기시간 5초 감소" 같은 다른 스킬 언급은 줄 중간이라 걸리지 않는다.
     */
    private static final Pattern OWN_COOLDOWN = Pattern.compile(
            "(?m)^\\s*재사용 대기시간\\s*:?\\s*(?:(\\d+)분\\s*)?(?:(\\d+(?:\\.\\d+)?)초)?");
    /** "30초 동안", "4.3초 동안" 중 첫 번째. 버프·소환 지속시간 추정용이며 키다운 시간 등도 걸릴 수 있다. */
    private static final Pattern DURATION = Pattern.compile("(\\d+(?:\\.\\d+)?)초 동안");
    /**
     * 지속 중 같은 스킬을 다시 눌러 효과를 바꾸는 스킬. 다시 누른 것도 skill-timeline 에 시전으로 기록된다.
     * 예: "스킬을 다시 사용하여 즉시 종료"(불굴의 결의), "스킬 재사용 시 2단계 진입"(레디 투 다이), "스킬을 재사용하여 재설치"(데스 블로섬).
     * "스킬의 재사용 대기시간" 같은 쿨 문구는 걸리지 않는다.
     */
    private static final Pattern REACTIVATION = Pattern.compile("스킬\\s*(?:을\\s*)?(?:다시 사용|재사용)(?!\\s*대기)");
    /**
     * "N초 동안" 뒤 절에 이 말이 있으면 버프가 아니라 소환·영역·설치물·공격 지속시간으로 본다. 버프 지속시간 증가를 받지 않는다(사용자 확인).
     * 예: "15초 동안 생성되는 영역"(리스트레인트 링), "30초 동안 ... 절망의 장미 소환"(데스 블로섬), "3초 동안 키다운"(샌드스톰),
     * "10초 동안 행동 불가"(에르다 노바), "60초 동안 ... 거대 표창 5개 생성"(생사여탈), "50초 동안 지속되며 일정 시간마다 공격 상태"(스파이더 인 미러),
     * "30초 동안 ... 12번 공격"(스피릿 오브 스노우). "공격 스킬 사용 시"처럼 버프 조건에 나오는 "공격"은 걸리지 않게 횟수·상태 표현만 본다.
     */
    private static final Pattern NON_BUFF_CLAUSE = Pattern.compile("소환|영역|설치|생성|구현|키다운|행동 불가|\\d+번 공격|공격 상태");
    /** "버프 지속시간 증가의 효과를 받지 않는다", "재사용 대기시간 초기화 및 버프 지속시간 증가의 효과를 받지 않고" */
    private static final Pattern REFUSES_BUFF_DURATION = Pattern.compile("버프 지속시간 증가[^.\\n]{0,20}효과를 받지 않");
    /** 절 경계: 쉼표, 줄바꿈, 마침표(소수점 "4.3초"는 제외) */
    private static final Pattern CLAUSE_DELIMITER = Pattern.compile("[,\\n]|\\.(?!\\d)");
    private static final Pattern REDUCTION_FOLLOWS = Pattern.compile("^\\s*(감소|증가)");
    private static final Pattern SENTENCE_DELIMITER = Pattern.compile("[\\n.]");

    private static final String REFUSAL = "받지 않";
    private static final String COOLDOWN_REDUCTION = "재사용 대기시간 감소";
    private static final String COOLDOWN_RESET = "재사용 대기시간 초기화";

    public SkillText {
        value = value == null ? "" : value
                .replace("\\r\\n", "\n")
                .replace("\\n", "\n")
                .replace("\r\n", "\n");
    }

    public static SkillText of(String... parts) {
        return new SkillText(String.join("\n", Arrays.stream(parts).map(p -> p == null ? "" : p).toList()));
    }

    public OptionalLong cooldownMs() {
        Matcher matcher = OWN_COOLDOWN.matcher(value);
        while (matcher.find()) {
            String minutes = matcher.group(1);
            String seconds = matcher.group(2);
            if (minutes == null && seconds == null) {
                continue; // "재사용 대기시간 미적용" 등
            }
            if (REDUCTION_FOLLOWS.matcher(value.substring(matcher.end())).find()) {
                continue;
            }
            double totalSeconds = (minutes == null ? 0 : Integer.parseInt(minutes) * 60)
                    + (seconds == null ? 0 : Double.parseDouble(seconds));
            return OptionalLong.of(Math.round(totalSeconds * 1000));
        }
        return OptionalLong.empty();
    }

    public OptionalLong durationMs() {
        Matcher matcher = DURATION.matcher(value);
        return matcher.find()
                ? OptionalLong.of(Math.round(Double.parseDouble(matcher.group(1)) * 1000))
                : OptionalLong.empty();
    }

    /**
     * 첫 "N초 동안"이 버프 지속시간이고 버프 지속시간 증가를 받는지. 소환·영역·공격 지속과 예외 문구가 있는 스킬은 아니다.
     * 미검증: 극딜 버프에 실제로 적용되는지는 데이터로 확인하지 못했고 사용자 설명(버프만 늘고 소환·영역은 늘지 않는다)을 따랐다.
     */
    public boolean isBuffDurationExtendable() {
        Matcher matcher = DURATION.matcher(value);
        if (!matcher.find() || REFUSES_BUFF_DURATION.matcher(value).find()) {
            return false;
        }
        String clause = CLAUSE_DELIMITER.split(value.substring(matcher.start()), 2)[0];
        return !NON_BUFF_CLAUSE.matcher(clause).find();
    }

    public boolean isReactivatable() {
        return REACTIVATION.matcher(value).find();
    }

    /** "재사용 대기시간 감소 효과를 받지 않는다" (예: 컨티뉴어스 링) */
    public boolean refusesCooldownReduction() {
        return hasRefusalOf(COOLDOWN_REDUCTION);
    }

    /** "재사용 대기시간 초기화의 효과를 받지 않는다" (예: 보이드 러쉬, 리스트레인트 링) */
    public boolean refusesCooldownReset() {
        return hasRefusalOf(COOLDOWN_RESET);
    }

    private boolean hasRefusalOf(String effect) {
        return SENTENCE_DELIMITER.splitAsStream(value)
                .anyMatch(sentence -> sentence.contains(REFUSAL) && sentence.contains(effect));
    }
}
