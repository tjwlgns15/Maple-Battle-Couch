# battle-coach

메이플스토리 **연무장** 기록을 조회하고, 스킬 사용 내역을 분석해 딜 사이클의 문제점을 진단·비교해 주는 웹 서비스.
포트폴리오 프로젝트이며, "데이터 수집 → 정규화 → 알고리즘 분석 → 설명 가능한 피드백" 흐름을 보여주는 것이 목표다.

## 1. 목적

- 기존 서비스(메무장, 츄츄지지 연무장 기록실, 메이플스카우터)는 DPS 랭킹과 타임라인을 **보여주기만** 하고, 차이는 유저가 눈으로 찾아야 한다.
- 이 서비스는 **자동 진단**이 차별점이다. "쿨이 돌았는데 안 쓴 스킬", "연동 스킬 누락", "극딜 정렬 문제"를 데미지 영향도 순으로 알려준다.
- **모든 직업**을 지원한다. 직업별로 값을 수동 입력하지 않고, API 데이터 파싱과 통계로 자동 확보한다.

## 2. 기능 (최종 목표)

1. **닉네임 검색**: 해당 캐릭터의 연무장 기록 목록(기간별)을 조회한다.
2. **기록 상세**: 결과 요약, 스킬별 데미지 점유율, 스킬 사용 타임라인(간트 차트), 극딜 구간 표시, **단일 진단**을 보여준다.
3. **두 기록 비교**: 같은 캐릭터의 시즌 간 비교나 다른 캐릭터와의 비교. 스펙 차이는 정규화하고, 스킬 구성 차이와 운용 차이를 분리해 진단한다.
4. (확장) 직업별 랭커 기록을 수집해 통계 기준으로 진단한다.

## 3. 기술 스택과 컨벤션

- Java 17, Spring Boot 4.x, Gradle, JPA/Hibernate, MySQL, Caffeine, Lombok. 프런트엔드는 Thymeleaf와 ECharts(예정).
- 기본 패키지: `com.battlecoach`
- 컨벤션
  - SRP를 지킨다.
  - setter를 쓰지 않는다. 정적 팩토리 메서드를 쓰고, getter 같은 반복 코드는 Lombok 어노테이션으로 처리한다.
  - DTO와 값 객체는 `record`로 만든다.
  - 엔티티는 `@NoArgsConstructor(access = PROTECTED)`에 정적 팩토리를 둔다.
  - 인터페이스로 추상화한다.
- 작업 방식
  - 코드를 쓰기 전에 설계를 먼저 합의한다.
  - 수정할 때는 변경된 파일만 전달한다.
  - 검증되지 않은 사항은 명시한다.
  - 제공된 코드로 해결이 안 되면 추측해서 고치지 말고 필요한 코드를 요청한다.

## 4. Nexon Open API (실제 응답으로 확인한 사실)

인증: 헤더 `x-nxopen-api-key`. 제한: 개발 키는 초당 5건, 하루 1,000건. 서비스 키는 초당 500건, 하루 2천만 건. 데이터는 평균 15분 뒤에 반영된다.

| 엔드포인트 | 용도 |
|---|---|
| `GET /maplestory/v1/id?character_name=` | ocid |
| `GET /maplestory/v1/battle-practice/replay-id?ocid=` | 기록 목록 |
| `GET /maplestory/v1/battle-practice/result?replay_id=` | 결과, 스킬별 데미지 |
| `GET /maplestory/v1/battle-practice/skill-timeline?replay_id=&page_no=` | 스킬 사용 내역 |
| `GET /maplestory/v1/battle-practice/character-info?replay_id=` | 입장 시점의 스펙과 스킬 |
| `GET /maplestory/v1/ranking/overall?date=&class=&page=` | 종합 랭킹(레벨·경험치 순, 200명/페이지, ocid 없음) |

### ranking/overall
- `class` 형식은 `직업군-전직`(예: `마법사-비숍`, `전사-히어로`)이다. 전직이 없는 직업은 `칼리-전체전직`이다. 형식이 틀리면 **오류 없이 필터를 무시하고 전체 랭킹**을 준다.
- **레벨 순위라 DPS 순위가 아니다.** 연무장 DPS 랭킹 API는 없다.
- 칼리 4기 기준(2026-09-27 랭킹 184명): 기록 없음 76%(140명), 다른 기간만 18%(33명), 4기 기록 6%(11명). 4기 표본 1개에 API 약 36건이 들었다.
- **Windows(Git Bash) curl의 `--data-urlencode`는 한글을 깨뜨린다.** 수동 호출할 때는 미리 URL 인코딩한 값을 쓴다.

### replay-id
- 기간(`period_no`)마다 기록이 **최대 1건**이다. 등록하지 않은 기간은 목록에 없다(예: 4, 1만 존재).
- `register_date`는 날짜 단위다(`2026-08-06T00:00+09:00`). 과거 시즌 기록도 계속 조회된다.
- **기간(`period_no`)은 이 목록에만 있다.** result와 character-info에는 없다. 그래서 목록을 받을 때마다 `replay_period`에 저장한다.
- **미확인:** 같은 기간에 다시 등록하면 `replay_id`가 바뀌는지.
- 기록이 없는 것으로 보이는 캐릭터(종합 랭킹 상위, ocid 정상)에서 **HTTP 400 `OPENAPI00004`(Please input valid parameter)**가 왔다. 빈 목록이 아니다. 잘못된 경로는 403 `OPENAPI00002`라 경로 문제는 아니다. 기록이 있는 캐릭터(연자히, 칼리얏)는 200과 목록을 받았다. 따라서 "기록 없음 = 400 `OPENAPI00004`"일 가능성이 높다. 다만 전용 코드가 아니라서 확정하지는 않았다.

### skill-timeline
- 한 기록이 1페이지에 모두 담긴다(약 6분 분량, 수백 건). **메인 공격(아츠 : 플러리 등)도 기록된다.**
- 필드: `elapse_time`(ms), `skill_name`, `hexa_skill_specificity_flag`(0 일반, 1 오리진, 2 어센트), `sequence_name`, `sequence_key`.
- **시퀀스 안에서는 `elapse_time`과 기록 순서가 몇 ms 역전된다.** 시간순으로 정렬하되 원래 기록 순서도 보존한다.
- `sequence_name`은 유저가 직접 붙인 이름이다("극", "극딜", "준극"). 판단 기준은 `sequence_key`와 시간 밀집도로 한다.
- 시퀀스 한 번의 실행은 0.1~1.1초 안에 끝난다(칼리 2명). 같은 키가 다시 실행되기까지는 52.8초 이상이 걸렸다.
- 같은 키라도 실행마다 시전 수가 다르다. 연자히는 키 하나로 9~10건(극딜)과 3건(쿨이 짧은 스킬만)을 번갈아 실행했다. 쿨이 안 돈 스킬은 건너뛰는 것으로 보인다. 칼리얏은 키 2개(극 11건, 준극 3~4건)로 나눠 썼다.
- **시전 시작 시각만 기록된다.** 오리진(샌드스톰)은 약 7.7초, 보이드 버스트와 에르다 노바는 약 4초 동안 기록이 비므로, 공백을 놀린 시간으로 판정하면 안 된다.

### result
- `total_play_time`(ms), `total_damage`, `total_dps`, `end_type`, `skill_statistic[]`.
- **`use_count`는 시전 횟수가 아니라 발동 단위 횟수다**(판데모니움 1회 시전에 24, 보이드 버스트 12, 샌드스톰 77). 시전 횟수는 timeline에서 센다.
- timeline에 없는 패시브·연동 스킬(베놈 버스트, 디시빙 블레이드, 레조네이트 VI, 솔 헤카테, 아츠 : 아스트라)이 데미지의 약 21%를 차지한다.
- **이름 표기가 다르다.** timeline은 `보이드 러쉬`, result는 `보이드 러쉬 VI`다. 로마 숫자 접미사를 떼어낸 `baseName`으로 연결한다.

### character-info
- `stat_object.basic_stat_object.final_stat`에 **쿨감이 합산된 값**이 있다: `재사용 대기시간 감소 (초)`, `재사용 대기시간 감소 (%)`, `재사용 대기시간 미적용`(%), `버프 지속시간`.
- `skill_object.character_skill[].skill_effect` 텍스트에 "재사용 대기시간 N초", "N초 동안"이 있어 파싱할 수 있다.
  - 쿨 표기는 두 형식이다: `재사용 대기시간 30초`, `재사용 대기시간 : 240초`. 자기 쿨은 줄 맨 앞에 온다. 줄 중간의 "보이드 러쉬의 재사용 대기시간 5초 감소" 같은 문구는 다른 스킬 이야기다.
  - **텍스트에 실제 줄바꿈과 문자 그대로의 `
`이 섞여 있다.** `skill_effect`가 null인 스킬도 있다.
  - 예외 문구: "재사용 대기시간 감소(의) 효과를 받지 않는다"(컨티뉴어스 링), "재사용 대기시간 초기화의 효과를 받지 않는다"(보이드 러쉬·블리츠, 리스트레인트 링, 불굴의 결의).
  - 헥사 스킬은 `데스 블로섬`(Lv1)과 `데스 블로섬 VI`(Lv30)가 따로 온다. 쿨 표기는 같았다.
  - 스킬 레벨이 쿨에 반영된 텍스트가 온다(에르다 노바 Lv30은 100초, Lv26은 116초).
  - **소울 컨트랙트는 `character_skill`에 없다.** 쿨을 알 수 없다.
- `hexa_matrix_object`, `v_matrix_object`, `link_skill_object`로 스킬 구성과 레벨을 알 수 있다.

## 5. 검증된 분석 사실 (칼리 랭커 2명 데이터로 확인)

- **실효 쿨은 커뮤니티 규칙을 쓴다**([인벤 정리 글, 2022-12](https://www.inven.co.kr/board/maple/2304/32859). 공식 문서는 없다).
  - % 감소를 먼저 곱한다(1초 미만으로는 줄지 않는다). 그다음 초 감소를 10초를 넘는 부분까지만 그대로 뺀다.
  - 남은 초 감소는 1초당 5%씩 곱한다. 초 감소로는 5초 미만이 되지 않는다.
  - 예: 쿨감 4초, 6%일 때 판데모니움 30초 → 24.2초(실측 최소 24.24초), 에르다 노바 116초 → 105.04초(104.92초).
  - 예전에 쓰던 (기본 − 초) × (1 − %)는 24.44초, 105.28초로 **실측보다 길게** 나와서 버렸다. 계산 쿨이 실측 간격보다 길면 모순이다.
  - 쿨이 긴 스킬(30초 이상) 전부에서 실측 최소 간격이 계산값 이상이었다. 예외인 보이드 버스트와 듄 버스트 각 1회는 미적용 발동으로 추정한다.
  - **미검증:** 하이퍼 "쿨타임 리듀스"처럼 스킬 하나에만 붙는 % 감소(칼리에는 없다), 미적용이 "초기화" 거부 문구의 대상인지.
- **쿨보다 짧은 재사용 간격은 "재사용 대기시간 미적용" 발동이다**(해당 캐릭터는 27%). 예: 소울 컨트랙트 0.5초 간격. 그래서 "최소 간격 = 쿨"로 추정하는 방식은 쓰지 않는다. 쿨은 character-info로 계산한다.
- **스펙 정규화**: 두 랭커의 DPS 차이가 2.8배였다. 비교 단위는 `초 환산값 = 스킬 데미지 ÷ total_dps`로 한다.
- **초 환산의 합은 항상 전투 시간과 같다**(칼리얏 336초 = 336초). 한 스킬이 오르면 다른 스킬이 내려가는 상대 지표라, 스킬별 초 환산 차이를 손해로 읽으면 틀린다.
  - 예: 연자히의 차크람 스플릿이 칼리얏보다 +11.5초인 것은 다른 스킬 레벨이 낮아서다.
  - 그래서 차이를 둘로 나눈다. **시전 수 효과** = (내 시전 − 기준 시전(보정)) × 기준 1회 초 환산, **1회 효율 효과** = (내 1회 − 기준 1회) × 내 시전. 두 효과의 합은 차이와 같다.
  - 손해(영향도)는 "부족한 시전 수 × 내 1회 초 환산"처럼 **내 DPS 기준**으로만 계산한다.
- **영향도로 오탐을 거른다.** 에르다 노바(점유율 0.0%) 누락이나 듄 버스트(0.1%) 비정렬은 무의미하다. 스틱스 3회 누락과 플레게톤 미사용은 합쳐 약 5.7초 환산으로, DPS의 약 1.7%에 해당하는 의미 있는 차이다.
- **구성 차이와 운용 차이를 구분한다.** 플레게톤은 솔 헤카테 30레벨에 해금되는데 B는 8레벨이었다. 이건 스펙 차이다. 반면 스틱스는 쿨 52.6초인데 105초 간격으로만 썼다. 이건 운용 차이로, 팩텀 감응 설정 문제로 추정된다.
- **극딜을 위해 아낀 것과 놀린 것을 구분해야 한다.** A는 판데모니움을 극딜 직전까지 아껴서 12회, B는 쿨마다 써서 14회였다. 이 샘플에서는 B가 초 환산 합계로 더 높았다(17.2초 vs 21.4초). 다만 샘플이 2개뿐이라 가설로만 둔다.

## 6. 진단 설계

- **1단계 (기록만으로 가능):** 극딜 구간 탐지, 점유율, 연동 스킬 쌍이 빠진 구간.
- **2단계 (character-info 파싱):** 실효 쿨 계산, 놓친 시전 수, 버프 지속시간, 극딜 버프 자동 분류(효과 텍스트).
- **3단계 (랭커 통계):** 스킬별 초 환산 중앙값, 극딜 시퀀스 순서 분포, 스킬 구성 채택률.
- 진단 결과는 `Finding(type, skillBaseName, impactSeconds, message)`로 표현한다. 규칙은 `DiagnosisRule` 인터페이스로 분리한다(개방-폐쇄 원칙). `DiagnosisEngine`이 영향도 임계값(예: 1초) 미만을 걸러낸 뒤 영향도 순으로 정렬한다.
- 단일 진단의 기준은 "프로파일(파싱과 통계)", 비교 진단의 기준은 "상대 기록"이다. 같은 엔진에 `AnalysisContext`만 다르게 넣는다.
- 1차 버전에서 **진단 대상에서 제외**할 것:
  - 버프 중에 쿨이 바뀌는 스킬 (오블리비온: 4차 이하 헥스 스킬 쿨 50% 감소)
  - 적중할 때마다 쿨이 줄어드는 스킬 (보이드 러쉬/블리츠: 아츠 적중 −1초, 크레센텀 −5초)
  - 쿨 표기가 없는 스킬 (보이드 어웨이크)

## 7. 현재 구현 상태 (1단계 어댑터·캐시 + 조회 화면)

> `./gradlew build` 통과(테스트 86개). 조회 화면, 쿨타임 표, 단일·비교·통계 진단은 실제 기록(연자히 4기, 칼리얏 1기·4기, 칼리 랭커 4기 표본 11개)으로 확인했다.

```
com.battlecoach
├─ BattleCoachApplication
├─ global
│  ├─ config    CacheConfig(Caffeine: ocid 1일, replayList 10분), CacheNames, CacheProperties, ClockConfig
│  ├─ concurrent SingleFlight           같은 키의 동시 요청을 1회 실행으로 합침
│  └─ error     GlobalExceptionHandler(Nexon 4xx→400, 429→503, 5xx→502), ErrorResponse
├─ nexon
│  ├─ NexonApiClient / RestClientNexonApiClient   RestClient, 429·5xx·네트워크 오류 지수 백오프 재시도
│  ├─ NexonRateLimiter   간격 기반 토큰 버킷 (단일 인스턴스용)
│  ├─ NexonApiProperties, NexonClientConfig, NexonApiException, NexonDates(KST 날짜 변환)
│  └─ dto   OcidResponse, ReplayIdListResponse, BattlePracticeResultResponse,
│           SkillTimelineResponse, CharacterInfoBasicResponse(basic_object만), NexonErrorResponse, RawResponse<T>
└─ replay
   ├─ domain  Replay(집계 루트) ─ ReplaySkillStat, ReplayCast
   │          ReplayRawData(원본 JSON 3종 보관, 재분석용)
   │          값 객체: SkillName(baseName), HexaType, CharacterProfile, BattleSummary, SkillDamage, ReplayId, CharacterName
   ├─ repository  ReplayRepository, ReplayRawDataRepository
   ├─ application
   │   OcidResolver(@Cacheable)
   │   CharacterReplayService(@Cacheable 기록 목록)
   │   ReplayQueryService    DB 조회 → 없으면 SingleFlight로 API 호출 후 저장
   │   ReplayLoader          API 3종 호출 후 집계 조립 (트랜잭션 밖)
   │   ReplayWriter          @Transactional, saveIfAbsent
   │   ReplayReader          readOnly 트랜잭션 안에서 DTO 변환
   │   dto  ReplayListItem, ReplayDetail(SkillStatView, CastView)
   ├─ api  ReplayApiController
   │       GET /api/characters/{characterName}/replays
   │       GET /api/replays/{replayId}
   └─ web  ReplayPageController (Thymeleaf)
           GET /  검색 폼,  GET /search → /characters/{name} 리다이렉트
           GET /characters/{characterName}  기록 목록
           GET /replays/{replayId}  결과 요약 + 점유율 차트 + 타임라인 차트(ECharts, static/js/replay-detail.js)
           PageExceptionHandler  화면 요청 오류 → templates/error.html (API 는 GlobalExceptionHandler 가 JSON)
           ScriptJsonWriter  차트 데이터를 <script type="application/json"> 에 넣을 때 '<' 이스케이프 (시퀀스 이름 XSS 방지)
           KoreanNumberFormat  "9조 5,833억" 표기 (@koreanNumberFormat)
```

- `spec` 패키지 (리플레이와 독립된 스킬 스펙 도메인)
  - `domain`: `CooldownStats`, `SkillText`(쿨 표기와 예외 문구 파싱), `SkillSpec`, `CharacterSpec`(헥사 VI 항목 우선), `CooldownCalculator` / `StandardCooldownCalculator`(커뮤니티 규칙)
  - `parser`: `CharacterSpecParser` / `NexonCharacterSpecParser` (`nexon.dto.CharacterInfoSpecResponse`)
- `replay.application.CharacterSpecLoader`: 저장된 `ReplayRawData.characterInfoJson`을 파싱하고, 결과는 `characterSpec` 캐시에 둔다(크기 제한만 두고 TTL은 없다). API를 다시 부르지 않는다.
- `replay.application.CooldownReportService` → `CooldownReport`: 스킬별 기본·실효 쿨, 시전 수, 실측 최소·중앙 간격, "이른 사용"(실효 쿨보다 0.2초 넘게 짧은 간격 수), 비고. 상세 화면의 "스킬 쿨타임" 표로 보여준다.
- `diagnosis` 패키지 (단일 진단. 비교 진단도 같은 엔진을 쓴다)
  - `domain`: `SkillUsage`(스킬별 시전 시각, 데미지, 실효 쿨, 지속시간, 이른 사용 비율), `BurstWindow`, `AnalysisContext`(초 환산 `toSeconds`), `Finding`(영향도 null = 참고), `FindingType`, `DiagnosisRule`, `DiagnosisEngine`(1초 미만 제거, 영향도 순), `DiagnosisResult(findings, notes)`
  - `burst`: `BurstDetector` / `CooldownClusterBurstDetector`: 실효 쿨 90초 이상 스킬 3개 이상이 3초 안에 몰리면 극딜 시작으로 본다. 길이는 그 스킬들 "N초 동안"의 중앙값이다(버프 지속 증가 미반영). 칼리 2명에서 120초 주기 3회씩 찾았고 길이는 40~50초였다.
  - `rule`: `MissedCastRule`(단일), `CastCountGapRule`·`LoadoutRule`(비교. 기준 기록이 없으면 아무것도 내지 않는다), `CooldownEligibility`(쿨 10초 이상이면서 쿨 변동 스킬이 아닌지 판정. 규칙들이 공유한다)
  - `sequence`: `BurstOrderExtractor`(첫 극딜 구간 −5초~+15초, 쿨 10초 이상 스킬의 첫 시전 순서, `baseName`), `SequenceAligner` / `NeedlemanWunschAligner`(일치 +2, 간격 −1, 불일치 −3이라 다른 스킬끼리는 짝짓지 않는다)
  - `AnalysisContext`는 `CharacterSpec`과 `reference`(비교 기준 기록, 단일이면 null)를 담는다. `FindingType`마다 `FindingCategory`(운용 / 구성·스펙)가 있다. 엔진은 같은 스킬, 같은 분류의 결과 중 영향도가 가장 큰 것만 남긴다(놓친 시전과 시전 수 부족을 두 번 세지 않게 하려고).
- `replay.application.AnalysisContextFactory`: `ReplayDetail`과 `CharacterSpec`을 `baseName`으로 묶는다(데미지도 `baseName`으로 합산). `ReplayAnalysisService`가 진단, 극딜 구간, 쿨타임 표를 한 번에 만든다.
- 상세 화면 맨 위 "진단" 섹션(초 환산, DPS 대비 %, 참고 접기). 타임라인에 극딜 구간 노란 음영.
- 비교: `ReplayComparisonService` → `ReplayComparison`(진단, 스킬별 비교표 `SkillRow`, 극딜 순서 정렬, 양쪽 극딜 구간). `ReplayCompareController`
  - `GET /compare/select?base={id}[&name=]`: 기준 기록 고르기(이름이 없으면 같은 캐릭터의 다른 기록)
  - `GET /compare?base={내 기록}&target={기준 기록}`: 요약, 진단(운용 / 구성·스펙), 극딜 순서 정렬, 스킬별 비교, 합친 타임라인 1개
  - **같은 직업끼리만 비교한다.** `ReplayComparisonService.requireSameClass`가 URL을 직접 입력한 경우까지 막는다. 선택 화면에서 다른 캐릭터를 검색하면 `CharacterClassResolver`(`/character/basic` 1건, `characterClass` 캐시 1일)로 직업부터 확인한다. 다르면 기록 목록(과 본문 3건)을 부르지 않는다. `/character/basic`의 `character_class` 표기는 연무장 직업명과 같다.
  - 합친 타임라인(`renderComparisonTimeline`): 스킬마다 한 줄을 쓰고, 내 기록(파란 원)은 줄 위쪽 −0.2, 기준 기록(주황 마름모)은 아래쪽 +0.2에 찍는다. Y축은 값 축(점 위치와 줄 경계선)과 카테고리 축(줄 가운데 스킬 이름) 두 개를 겹쳐 쓴다. ECharts 값 축은 `min`부터 눈금을 매겨서, 값 축 하나로는 줄 가운데에 이름을 둘 수 없었다. Y축을 뒤집으면 X축이 0에 붙으므로 `axisLine.onZero=false`를 둔다. 극딜 구간은 기록별 색(노랑 / 주황)의 옅은 음영이다.
  - JS: `static/js/charts.js`(공통: 점유율·타임라인·비교 타임라인 차트, `window.BattleCoachCharts`) + `replay-detail.js` / `compare.js`
- 통계 (`diagnosis.statistics`)
  - `JobStatistics`(직업·기간별, 표본 5개 미만이면 `isReliable()=false` → 통계 규칙 미실행), `SkillDistribution`(채택률, 분당 시전·초 환산 분위수), `PairStatistic`, `pairGroups()`(쌍을 이은 묶음), `Quartiles`
  - `JobStatisticsCalculator`: 쌍은 쿨 10초 이상이면서 시전 수 ±1인 스킬끼리, 한 표본에서 A 시전의 80% 이상을 B와 1초 안에 쓰면 짝이다. 표본의 80% 이상이 짝이면 남긴다.
  - `JobStatisticsProvider`(인터페이스) ← `ranker.application.JobStatisticsService`(저장된 표본으로 계산, 진단 대상 기록은 뺀다, `jobStatistics` 캐시 10분)
  - 규칙: `CastRateRule`(분당 시전이 랭커 하위 25% 미만 → 중앙값까지 모자란 시전 × 내 1회 초 환산), `LinkedPairRule`(랭커 대부분이 함께 쓰는 쌍을 절반 미만으로 함께 썼으면 참고)
  - 칼리 4기 묶음: 2분 극딜 버프 6개(레이스 오브 갓, 매직 서킷, 그란디스, 오블리비온, 레조네이트 : 얼티메이텀, 리스트레인트 링), 1분 주기 데스 블로섬·스틱스·레디 투 다이(91%), 스파이더 인 미러·크레스트 오브 더 솔라
- `ranker` 패키지 (수집)
  - `RankerCollector`: 종합 랭킹 순서대로 ocid → 기록 목록 → 대상 기간 기록을 `ReplayQueryService`로 저장하고 `RankerSample`로 등록한다. 결과는 `RankerProbe`(NOT_FOUND / NO_RECORD / OTHER_PERIOD_ONLY / SAMPLED)로 남겨 다시 부르지 않는다. 다음 단계 호출이 상한을 넘으면 그 전에 멈춘다. `TaskExecutor`로 비동기 실행하며 한 번에 하나만 돈다.
  - `RankerAdminController`(`collector.enabled=true`일 때만 등록, 인증 없음 → 로컬 전용): `POST /api/admin/rankers/collect?jobClass=칼리-전체전직&maxCalls=400&maxRankers=200`, `GET /status`, `GET /statistics?characterClass=칼리&periodNo=4`
- 상세 화면 "랭커 대비" 섹션: 스킬별 채택률, 분당 시전·초 환산(나 vs 랭커 25/50/75%), 함께 쓰는 스킬 묶음과 내 함께 쓴 횟수. 기간을 모르면(목록을 거치지 않음) 안내만 한다.
- `SequenceSegment`(domain): `sequence_key`가 같은 시전을 기록 순서대로 묶어 실행 구간을 만든다. 사이에 끼어든 일반 시전은 무시하고, 같은 키의 직전 시전과 10초(`MAX_GAP_MS`)보다 멀면 새 실행으로 본다. 실측으로 확인한 실행 안의 간격은 1.1초 이하, 실행 사이 간격은 52.8초 이상이다. `Replay.getSequenceSegments()` → `ReplayDetail.sequenceSegments`.

- 테스트: `SingleFlightTest`(CountDownLatch로 동시성 검증), `SkillNameTest`, `NexonDatesTest`, `SequenceSegmentTest`, `KoreanNumberFormatTest`, `SkillTextTest`, `StandardCooldownCalculatorTest`, `NexonCharacterSpecParserTest`(샘플 `src/test/resources/nexon/character-info-kali.json`: 칼리얏 원문에서 basic, final_stat, character_skill만 남김)
- 설정: `application.yml`은 `${NEXON_API_KEY}`, `${DB_USERNAME}`, `${DB_PASSWORD}`만 참조한다. 로컬 값은 `src/main/resources/application-local.yml`(gitignore, `spring.config.import: optional:`)에 둔다. 스키마는 `ddl-auto: update`로 생성.

## 8. 이제 해야 할 것

### 0) 1단계 빌드 검증
- [x] `./gradlew build`를 통과시킨다. 의심 지점:
  - Boot 4 starter 이름 (`spring-boot-starter-webmvc`, `spring-boot-starter-restclient`)
  - Jackson 3 패키지 (`tools.jackson.databind.json.JsonMapper`, `tools.jackson.core.JacksonException`)
  - `@Embeddable record`(Hibernate 7) 매핑
  - 패키지 전용(package-private) `@Component` 클래스와 `@Transactional` 프록시
- [ ] 실제 API 호출로 확인한다.
  - battle-practice 경로가 맞는지
  - 존재하지 않는 캐릭터명일 때의 오류 코드와 상태
  - 기록이 없는 캐릭터일 때의 응답(빈 목록인지 오류인지)
  - 재등록 시 `replay_id`가 바뀌는지
- [ ] 필요하면 시전 기록 약 700행 INSERT를 JdbcTemplate 배치로 바꾼다(IDENTITY라 JPA 배치가 안 됨).

### 1) 조회 화면 (Thymeleaf + ECharts)
- [x] 닉네임 검색 → 기간별 기록 목록
- [x] 기록 상세: 결과 요약, 스킬 점유율 차트(상위 20개 + 기타), 타임라인 차트(시퀀스 구간 음영, 오리진/어센트 색 구분, Ctrl+휠 확대)
- [x] 실제 기록이 있는 캐릭터로 확인하고 `SequenceSegment.MAX_GAP_MS` 검증 (칼리 2명, 각 6회로 정확히 분리)
- [ ] 기록 없음(400 `OPENAPI00004`)을 오류 대신 "기록 없음"으로 보여줄지 결정 (위 4장 참고)
- 시간 밀집도 기반 극딜 구간 탐지(`BurstDetector`)는 3) 단일 진단에서 한다.

### 2) 스킬 스펙 파서와 쿨 계산
- [x] `CharacterInfo` 파싱: `final_stat`에서 쿨감(초, %, 미적용, 버프 지속시간), `skill_object`에서 `SkillSpec`(기본 쿨, 지속시간, "재사용 대기시간 감소 효과를 받지 않는다" 등 제외 문구, 효과 텍스트)
- [x] `CooldownCalculator`: 커뮤니티 규칙 (5장 참고)
- [x] 상세 화면에 쿨타임 검증 표 추가
- [ ] 효과 텍스트의 지속시간("N초 동안")과 버프 지속시간 반영 → 극딜 버프 분류할 때 (3단계)
- [ ] **다른 직업 1~2개의 character-info 샘플로 파서가 일반적으로 통하는지 검증** (충전형, 소환형, 스택형 스킬이 있는 직업)
- [x] 게임 규칙 확인: 커뮤니티 규칙으로 정했다. 공식 문서는 찾지 못했다.

### 3) 단일 진단
- [x] `AnalysisContext`, `DiagnosisRule`, `DiagnosisEngine`, `Finding`
- [x] 극딜 구간 탐지 `CooldownClusterBurstDetector` (타임라인 표시)
- [ ] 규칙
  - [x] `MissedCastRule`: 쿨이 돌아온 뒤 다음 사용까지 걸린 시간 누적. 미적용으로 일찍 쓴 경우는 손실이 아님.
    - 결과: 칼리얏 판데모니움 2회 2.9초(극딜 대기 21.5초, 그 외 36.1초), 연자히 스틱스 3회 2.6초(극딜 대기 126.2초)
    - 실효 쿨 10초 미만은 제외한다(짧은 쿨 스킬끼리 시전 시간을 두고 경쟁한다). 간격 4개 이상이면서 이른 사용 비율이 미적용 확률 + 20%p를 넘으면 "쿨 변동"으로 보고 제외하고 참고에 적는다.
    - 전투 시작 시점에는 모든 스킬이 준비돼 있다고 본다.
    - 전투 끝까지 남은 시간은 "5초와 스킬 지속시간 중 긴 쪽"이 남았을 때만 놓친 시전으로 센다. 중간에 쉰 시간만 더해 쿨로 나누면, 연자히가 크레스트 오브 더 솔라(쿨 221초)를 226초 이후 전투 끝(339초)까지 쓰지 않은 누락을 놓친다. 반대로 칼리얏의 120초 버프는 전투 종료 6초 전에 쿨이 돌아서 세지 않는다.
    - 데미지 항목이 없는 스킬(버프)은 영향도 없이 참고로만 보여준다.
    - 적용 대상은 데이터로 가린다: "이른 사용" 비율이 미적용 확률보다 훨씬 높은 스킬은 쿨이 실행 중에 바뀌는 스킬로 보고 제외한다. 칼리의 차크람 퓨리·스플릿·스윕, 보이드 러쉬·블리츠는 이른 사용이 50~90%였다. 쿨이 긴 스킬은 0~1회였다.
    - 칼리 사례: 연자히 스틱스는 쿨 52.4초인데 105~115초 간격으로 썼다(쿨타임 표에서 바로 보인다).
  - [ ] `LinkedSkillRule` → **4)·5)로 옮겼다.** 기록 하나로는 짝을 판단할 수 없다. 극딜 때는 모든 스킬이 같이 나가서 함께 쓴 횟수로 보면 거의 모든 스킬이 짝이 되고, 연자히는 스틱스를 레디 투 다이와 같이 쓴 적이 없다. 누락 자체는 `MissedCastRule`이 잡는다.
  - [ ] `BurstAlignmentRule` → **4)·5)로 옮겼다.** 영향도를 계산하려면 버프 배율을 추정해야 해서 근거가 약하다. 대신 놓친 시전의 원인을 "극딜 대기"와 "그 외"로 나눠 보여준다.
- [x] 초 환산 영향도로 정렬하고 임계값 미만은 거른다.
- 보유했지만 한 번도 쓰지 않은 스킬은 다루지 않는다. `character_skill`에 펫·공용 스킬이 섞여 있어 잡음이 크다.

### 4) 비교 진단
- [x] 초 환산 정규화, `baseName` 기준 스킬별 시전 수와 점유율 차이 → 시전 수 효과와 1회 효율 효과로 분해(5장 참고)
- [x] `CastCountGapRule`: 기준보다 적게 쓴 시전 수(전투 시간 보정) × 내 1회 초 환산
  - 연동 스킬 힌트: 기준 기록에서 시전 수가 같고 1초 안에 80% 이상 함께 쓴 스킬(동률이면 가장 가까운 스킬). 연자히 vs 칼리얏: "레디 투 다이와 6/6회, 이 기록은 0/3회". 누락을 두 번 세지 않도록 `LinkedSkillRule`을 따로 만들지 않고 여기에 합쳤다.
- [x] `LoadoutRule`: 기준이 쓴 스킬을 한 번도 안 썼을 때, 내 스킬 목록에 없으면 미보유·미해금(구성·스펙), 있으면 미사용(운용). 기준 캐릭터 스킬 목록에도 없는 공용 스킬(소울 컨트랙트)은 판단하지 않는다. 연자히: 플레게톤 미해금 2.3초, 불굴의 결의 미보유(참고)
- [x] 극딜 순서 비교 (Needleman-Wunsch)
- [x] 비교 화면: 타임라인 2개, 운용 / 구성·스펙으로 나눈 진단 목록
- [ ] `BurstAlignmentRule`: 1회 효율 효과를 레벨이 같은 스킬에서 "극딜 정렬 의심"으로 진단할지. 다른 스킬 레벨과 버프 레벨이 섞여 있어 지금은 표로만 보여준다.

### 5) 랭커 수집과 3단계 진단 (확장)
- [x] 종합 랭킹 API(직업 필터) → ocid → replay-id 수집 배치. 호출량 제한을 지키고, 연무장 기록 보유율을 먼저 측정한다. (칼리 4기 표본 11개, 보유율은 위 4장)
- [x] 직업별 스킬 통계(초 환산 분위수, 분당 시전, 스킬 채택률, 함께 쓰는 묶음), `period_no`별로 관리
- [x] `CastRateRule`, `LinkedPairRule`, 상세 화면 "랭커 대비"
- [ ] 극딜 시퀀스 순서 분포(랭커 표준 순서)
- [x] 다른 직업 수집으로 파서·진단 기준값 검증 (2026-09-28, 4기 표본: 아크메이지(썬,콜) 1, 나이트로드 1, 히어로 1, 보우마스터 3)
  - 수집: 직업당 110건으로 랭커 약 50명씩 확인했다. 4기 기록 보유율은 2~6%로 칼리(6%)보다 낮았다. 직업당 표본 5개면 150~250건이 든다.
  - **API 비용의 대부분은 기록이 없는 랭커 확인이다.** 2026-09-28 하루 약 950건 중 786건이 랭커 393명 확인(ocid + 기록 목록 2건씩)이었다. 그중 316명(80%)은 기록이 없었다. 리플레이 본문은 약 51건이었다. 랭킹 API가 ocid를 주지 않아 이 2건은 줄일 수 없다. 확인 결과는 `ranker_probe`에 남아 다시 들지 않는다.
  - `maxSamples`(표본 수 상한) 파라미터를 추가했다.
  - 통하는 것: 쿨 파싱(스킬 정보 없음은 공용 소울 컨트랙트와 파생 스킬 `폭풍의 시 VI : 난사 모드`, `파이널 블레이드`뿐), 극딜 탐지(6개 모두 2분 주기 3회), 쿨 변동 제외(쉐도우 리츄얼, 풍마수리검, 썬더 브레이크, 서브제로 퍼미네이션), 쿨 30초 이상 스킬의 이른 사용 0~1회.
  - 미적용 확률은 칼리 27%, 다른 직업 3~7%다. 쿨 변동 판정 기준(미적용 + 20%p)은 직업별 값을 따라간다.
  - [x] **재발동 스킬 문제**: `초월 : 불굴의 결의`("스킬을 다시 사용하여 즉시 종료")는 다시 누른 것도 시전으로 기록돼 간격이 약 2.3초로 나왔다. 그래서 "쿨 변동"으로 잘못 제외됐다(히어로, 보우마스터).
    - 해결: `SkillText.isReactivatable()`(정규식 `스킬\s*(을\s*)?(다시 사용|재사용)(?!\s*대기)`)이 참이고 지속시간이 있는 스킬은, 앞 시전의 지속시간 안에 다시 누른 입력을 시전에서 뺀다(`SkillUsage.mergeReactivations`).
    - 칼리 해당 스킬: 리스트레인트 링, 불굴의 결의, 데저트 베일, 에르다 샤워, 레디 투 다이, 보이드 버스트, 데스 블로섬. 칼리 기록의 진단 결과는 바뀌지 않았다.
    - 타임라인 차트에는 원래 입력이 그대로 보인다. 쿨타임 표 비고에 "지속 중 다시 누른 입력은 시전에서 뺌"을 표시한다.
  - [ ] **쿨 11~12초 스킬**: 포 시즌 VI(나이트로드, 8회 4.3초), 써든레이드 VI, 윈드 오브 프레이 VI(보우마스터)가 놓친 시전으로 잡힌다. 실제 손해인지, 짧은 쿨 스킬끼리 시전 시간을 두고 경쟁한 결과인지 표본 1~3개로는 구분할 수 없다. 기준 10초를 15초로 올릴지 표본을 더 모아 판단한다.
  - 보우마스터 Marksbowman의 스틱스 4회 누락(극딜 대기 242.8초)은 연자히와 같은 패턴이다(2분 극딜에만 사용).
- [ ] "아낀 것 vs 놀린 것" 가설 검증: 판데모니움 극딜 대기 시간과 판데모니움 초 환산의 관계 (표본을 더 모은 뒤)
- [ ] 서비스 키 승인 신청(하루 1,000건으로는 수집이 불가능)

### 나중에 고려
- 서버를 여러 대로 늘릴 때 `NexonRateLimiter`와 `SingleFlight`를 Redis 기반으로 교체
- 버프 중에 쿨이 바뀌는 스킬(오블리비온 등) 시뮬레이션, 연동 스킬 데미지의 시전 귀속
