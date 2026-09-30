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

- Java 17, Spring Boot 4.x, Gradle, JPA/Hibernate, MySQL, Caffeine, Lombok. 프런트엔드는 Thymeleaf와 ECharts.
- 디자인: 어두운 "천공" 테마(메이플스토리 연무장의 구름 위 하늘 신전 색감을 참고했고, 게임 이미지·로고는 쓰지 않는다).
  - 색은 `static/css/app.css`의 `:root` 토큰으로만 쓴다. `charts.js`도 토큰을 읽어 ECharts 테마(`battle-coach`)를 등록한다.
  - 역할: 청록(주요 동작·내 기록), 연두(실행 버튼, 글자는 짙은 남색), 보라 그라데이션(상단 수치 카드), 산호(손해·기준 기록), 금색(영향도·강조), 보라 음영(극딜 구간), 청록 음영(시퀀스).
  - 섹션 제목은 `<h2 data-label="Diagnosis">`처럼 영문 대문자 라벨을 단다(게임 UI의 "REPLAY BOARD" 느낌). 페이지 제목 위에는 `<p class="eyebrow">`.
  - 첫 화면 장식(빛기둥·돌기둥·화로·오브·운무)은 `index.html`의 인라인 SVG로 직접 그렸다. `z-index: -1` 장식은 부모에 `isolation: isolate`가 있어야 배경 뒤로 숨지 않는다.
  - 정적 파일은 내용 해시 주소(`spring.web.resources.chain.strategy.content`)로 나간다. 해시 계산이 캐시되므로 CSS·JS를 고치면 서버를 다시 띄워야 반영된다.
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
- 기록이 없는 것으로 보이는 캐릭터(종합 랭킹 상위, ocid 정상)에서 **HTTP 400 `OPENAPI00004`(Please input valid parameter)**가 왔다. 빈 목록이 아니다. 잘못된 경로는 403 `OPENAPI00002`라 경로 문제는 아니다. 기록이 있는 캐릭터(칼리 B, 칼리 A)는 200과 목록을 받았다. 따라서 "기록 없음 = 400 `OPENAPI00004`"일 가능성이 높다. 다만 전용 코드가 아니라서 확정하지는 않았다.

### skill-timeline
- 한 기록이 1페이지에 모두 담긴다(약 6분 분량, 수백 건). **메인 공격(아츠 : 플러리 등)도 기록된다.**
- 필드: `elapse_time`(ms), `skill_name`, `hexa_skill_specificity_flag`(0 일반, 1 오리진, 2 어센트), `sequence_name`, `sequence_key`.
- **시퀀스 안에서는 `elapse_time`과 기록 순서가 몇 ms 역전된다.** 시간순으로 정렬하되 원래 기록 순서도 보존한다.
- `sequence_name`은 유저가 직접 붙인 이름이다("극", "극딜", "준극"). 판단 기준은 `sequence_key`와 시간 밀집도로 한다.
- 시퀀스 한 번의 실행은 0.1~1.1초 안에 끝난다(칼리 2명). 같은 키가 다시 실행되기까지는 52.8초 이상이 걸렸다.
- 같은 키라도 실행마다 시전 수가 다르다. 칼리 B는 키 하나로 9~10건(극딜)과 3건(쿨이 짧은 스킬만)을 번갈아 실행했다. 쿨이 안 돈 스킬은 건너뛰는 것으로 보인다. 칼리 A는 키 2개(극 11건, 준극 3~4건)로 나눠 썼다.
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
- **초 환산의 합은 항상 전투 시간과 같다**(칼리 A 336초 = 336초). 한 스킬이 오르면 다른 스킬이 내려가는 상대 지표라, 스킬별 초 환산 차이를 손해로 읽으면 틀린다.
  - 예: 칼리 B의 차크람 스플릿이 칼리 A보다 +11.5초인 것은 다른 스킬 레벨이 낮아서다.
  - 그래서 차이를 둘로 나눈다. **시전 수 효과** = (내 시전 − 기준 시전(보정)) × 기준 1회 초 환산, **1회 효율 효과** = (내 1회 − 기준 1회) × 내 시전. 두 효과의 합은 차이와 같다.
  - 손해(영향도)는 "부족한 시전 수 × 내 1회 초 환산"처럼 **내 DPS 기준**으로만 계산한다.
- **영향도로 오탐을 거른다.** 에르다 노바(점유율 0.0%) 누락이나 듄 버스트(0.1%) 비정렬은 무의미하다. 스틱스 3회 누락과 플레게톤 미사용은 합쳐 약 5.7초 환산으로, DPS의 약 1.7%에 해당하는 의미 있는 차이다.
- **구성 차이와 운용 차이를 구분한다.** 플레게톤은 솔 헤카테 30레벨에 해금되는데 B는 8레벨이었다. 이건 스펙 차이다. 반면 스틱스는 쿨 52.6초인데 105초 간격으로만 썼다. 이건 운용 차이로, 팩텀 감응 설정 문제로 추정된다.
- **극딜을 위해 아낀 것과 놀린 것을 구분해야 한다.** A는 판데모니움을 극딜 직전까지 아껴서 12회, B는 쿨마다 써서 14회였다. 이 샘플에서는 B가 초 환산 합계로 더 높았다(17.2초 vs 21.4초). 다만 샘플이 2개뿐이라 가설로만 뒀다.
  - **칼리 4기 랭커 11명으로 확인(2026-09-28, `GET /api/admin/rankers/hold-tradeoff`, 스피어만 상관):**
    - 판데모니움: 극딜 대기 0~27초로 제각각인데 시전 수는 모두 12~13회였다(대기 ~ 분당 시전 ρ = +0.04). 대기 ~ 초 환산은 ρ = +0.35(약한 양의 상관). **"쿨마다 쓰는 쪽이 낫다"는 2명 가설은 지지되지 않았다.** 20~27초 정도 아끼는 것은 시전 수를 줄이지 않았다. 칼리 B의 14회는 랭커 범위(12~13회) 밖이었다.
    - 스틱스: 대기 ~ 분당 시전 ρ = −0.76, 대기 ~ 초 환산 ρ = −0.54. 7회 쓴 2명은 대기가 짧았다. 쿨 51초 스킬은 아끼면 그대로 1회가 줄어든다.
    - 데스 블로섬: 대기 ~ 초 환산 ρ = +0.52, 듄 버스트: +0.57이었다. 초 환산에는 스킬·버프 레벨이 섞여 있어 인과로 읽으면 안 된다.
    - 표본이 11개라 경향일 뿐이다. 유의성 검정은 하지 않았다.

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

> `./gradlew build` 통과(테스트 126개). 조회 화면, 쿨타임 표, 단일·비교·통계 진단은 실제 기록(칼리 B 4기, 칼리 A 1기·4기, 칼리 랭커 4기 표본 11개)으로 확인했다.

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
  - `burst`: `BurstDetector` / `CooldownClusterBurstDetector`: 실효 쿨 90초 이상 스킬 3개 이상이 3초 안에 몰리면 극딜 시작으로 본다. 길이는 그 스킬들 지속시간의 중앙값(몰린 스킬 절반 이상이 켜져 있는 동안)이다.
    - **버프 지속시간 반영(A3, 2026-09-29)**: `SkillSpec.effectiveDurationMs` = 지속 × (1 + 버프 지속시간%). 버프에만 적용하고 소환·영역·설치물·공격 지속에는 적용하지 않는다(**사용자 확인. 데이터로는 검증하지 못했다**). `SkillText.isBuffDurationExtendable`: 첫 "N초 동안" 절에 소환|영역|설치|생성|구현|키다운|행동 불가|N번 공격|공격 상태가 있거나, "버프 지속시간 증가 … 효과를 받지 않" 예외 문구가 있으면 제외한다.
      - 검증을 시도했지만 실패했다: 레디 투 다이는 지속 중 다시 누른 기록이 없었다(간격 52~59초 = 쿨). 오블리비온의 헥스 쿨 50% 감소가 끝나는 시점은 미적용·적중 쿨 감소 노이즈 때문에 판정할 수 없었다.
      - 지속시간이 쿨보다 긴 스킬(상시 버프)은 극딜 길이에서 뺀다. 쓸만한 샤프 아이즈(쿨 180초, 지속 270초)가 섞여 아크메이지(썬,콜) 극딜이 264초가 됐었다. 그 표본은 버프 지속 155%라 60초 버프도 153초가 되어 빠지고, 링·프로스트 아크·스피릿 오브 스노우로 20초가 됐다.
      - 결과(극딜 길이): 칼리 40초 안팎 → 54~72초, 보우마스터 40 → 68초, 히어로 30~50 → 51~53초, 나이트로드 30 → 47초. 5개 직업 기록의 진단 영향도는 그대로였고, "아낀 것 vs 놀린 것" 상관(5장)도 같았다.
      - 다시 누른 입력을 합치는 범위는 지속시간과 실효 쿨 중 짧은 쪽이다. 레디 투 다이는 30초 × 1.79 = 54초로 쿨(약 52초)보다 길어져, 그대로 두면 새 시전까지 합쳐진다.
  - `rule`: `MissedCastRule`(단일), `CastCountGapRule`·`LoadoutRule`(비교. 기준 기록이 없으면 아무것도 내지 않는다), `CooldownEligibility`(쿨 10초 이상이면서 쿨 변동 스킬이 아닌지 판정. 규칙들이 공유한다)
  - `sequence`: `BurstOrderExtractor`(첫 극딜 구간 −5초~+15초, 쿨 10초 이상 스킬의 첫 시전 순서, `baseName`), `SequenceAligner` / `NeedlemanWunschAligner`(일치 +2, 간격 −1, 불일치 −3이라 다른 스킬끼리는 짝짓지 않는다)
  - `AnalysisContext`는 `CharacterSpec`과 `reference`(비교 기준 기록, 단일이면 null)를 담는다. `FindingType`마다 `FindingCategory`(운용 / 구성·스펙)가 있다. 엔진은 같은 스킬, 같은 분류의 결과 중 영향도가 가장 큰 것만 남긴다(놓친 시전과 시전 수 부족을 두 번 세지 않게 하려고).
- `replay.application.AnalysisContextFactory`: `ReplayDetail`과 `CharacterSpec`을 `baseName`으로 묶는다(데미지도 `baseName`으로 합산). `ReplayAnalysisService`가 진단, 극딜 구간, 쿨타임 표를 한 번에 만든다.
- 상세 화면 맨 위 "진단" 섹션(초 환산, DPS 대비 %, 참고 접기). 타임라인에 극딜 구간 노란 음영.
- 비교: `ReplayComparisonService` → `ReplayComparison`(진단, 스킬별 비교표 `SkillRow`, 극딜 순서 정렬, 양쪽 극딜 구간). `ReplayCompareController`
  - `GET /compare/select?base={id}[&name=]`: 기준 기록 고르기(이름이 없으면 같은 캐릭터의 다른 기록)
  - `GET /compare?base={내 기록}&target={기준 기록}`: 요약, 진단(운용 / 구성·스펙), 극딜 순서 정렬, 스킬 레벨 비교, 운용 비교, 합친 타임라인 1개
    - 스킬 레벨 비교(`SpecComparison`): 레벨이 다른 스킬(차이 큰 순, 레벨 막대와 ▼▲ 뱃지), 한쪽만 가진 스킬(미보유), 같은 스킬(접기). 운용과 섞지 않으려고 스킬별 표에서 떼어냈다.
    - 운용 비교: ① 시전 횟수 차이(기준 대비 %, 0 가운데 가로 막대, 1회 미만 차이는 뺌. 기본 공격 수백 회와 쿨기 수 회를 같은 눈금에 두려고 비율을 쓴다) ② 딜 비중 차이의 원인(시전 수 효과·1회 효율 효과 누적 막대, 차이 큰 12개, 한쪽에 시전 기록이 없으면 회색 "나눌 수 없음") ③ 전체 수치 표는 접기
  - **같은 직업끼리만 비교한다.** `ReplayComparisonService.requireSameClass`가 URL을 직접 입력한 경우까지 막는다. 선택 화면에서 다른 캐릭터를 검색하면 `CharacterClassResolver`(`/character/basic` 1건, `characterClass` 캐시 1일)로 직업부터 확인한다. 다르면 기록 목록(과 본문 3건)을 부르지 않는다. `/character/basic`의 `character_class` 표기는 연무장 직업명과 같다.
  - 합친 타임라인(`renderComparisonTimeline`): 스킬마다 한 줄을 쓰고, 내 기록(파란 원)은 줄 위쪽 −0.2, 기준 기록(주황 마름모)은 아래쪽 +0.2에 찍는다. Y축은 값 축(점 위치와 줄 경계선)과 카테고리 축(줄 가운데 스킬 이름) 두 개를 겹쳐 쓴다. ECharts 값 축은 `min`부터 눈금을 매겨서, 값 축 하나로는 줄 가운데에 이름을 둘 수 없었다. Y축을 뒤집으면 X축이 0에 붙으므로 `axisLine.onZero=false`를 둔다. 극딜 구간은 기록별 색(노랑 / 주황)의 옅은 음영이다.
  - JS: `static/js/charts.js`(공통: 점유율·타임라인·비교 타임라인 차트, `window.BattleCoachCharts`) + `replay-detail.js` / `compare.js`
- 통계 (`diagnosis.statistics`)
  - `JobStatistics`(직업·기간별, 표본 5개 미만이면 `isReliable()=false` → 통계 규칙 미실행), `SkillDistribution`(채택률, 분당 시전·초 환산 분위수), `PairStatistic`, `pairGroups()`(쌍을 이은 묶음), `Quartiles`
  - `JobStatisticsCalculator`: 쌍은 쿨 10초 이상이면서 시전 수 ±1인 스킬끼리, 한 표본에서 A 시전의 80% 이상을 B와 1초 안에 쓰면 짝이다. 표본의 80% 이상이 짝이면 남긴다.
  - `JobStatisticsProvider`(인터페이스) ← `ranker.application.JobStatisticsService`(저장된 표본으로 계산, 진단 대상 기록은 뺀다, `jobStatistics` 캐시 10분)
  - 규칙: `CastRateRule`(분당 시전이 랭커 하위 25% 미만 → 중앙값까지 모자란 시전 × 내 1회 초 환산), `LinkedPairRule`(랭커 대부분이 함께 쓰는 쌍을 절반 미만으로 함께 썼으면 참고)
  - **표본 품질(운용 효율)**: `EfficiencyModelFitter` → `EfficiencyModel`(`JobStatistics.efficiency`). 같은 직업·기간 랭커로 `log DPS ~ log 전투력 [+ 헥사 코어 레벨 합]`을 최소제곱 적합하고, 실제 DPS가 추세보다 몇 % 높은지를 운용 효율로 본다.
    - 전투력(`final_stat`의 "전투력")에는 헥사·5차 스킬 레벨이 빠지고 직업마다 산식이 다르다(사용자 확인). 그래서 같은 직업 안에서만 쓰고, 표본 8개 이상이면 헥사 합(`hexa_matrix_object`)을 넣는다. 표본 5개 미만이면 만들지 않는다.
    - 칼리 4기 11명: 전투력만 R² 0.985(잔차 −6 ~ +6%), 헥사 합을 더하면 0.989. 잔차 ~ 헥사 합 r = +0.27. 랭커는 헥사가 거의 차 있어(263~420) 헥사 계수가 1레벨당 +0.06%로 작다.
    - **랭커 스펙 범위 밖으로는 외삽하지 않는다**(`covers`). 칼리 B(전투력 3.01억, 헥사 합 137)에 적용하면 헥사 부족분이 설명되지 않아 −22%가 나왔다. 화면에는 "범위 밖"과 양쪽 값만 보여준다.
    - 운용 효율 하위 25% 표본은 분당 시전·초 환산 분위수에서 가중치 0.5(`Quartiles.weighted`, 가중치가 같으면 기존 선형 보간과 같다). 빼지 않는 이유는 표본이 5~11개뿐이라서다. 극딜 순서와 쌍 통계는 가중하지 않는다.
    - maplescouter "헥사환산"은 공식을 모르고 외부 서비스 값이라 쓰지 않았다. 랭커끼리는 헥사 항의 설명력이 작아 이득도 작다고 판단했다.
  - **초 환산은 스킬 레벨이 같은 랭커끼리 비교한다**: `SkillDistribution.secondsFor(SkillLevel)`. 레벨은 스킬 레벨 + 강화 코어 레벨(`character_skill`에 "헥스 : 판데모니움 강화"처럼 따로 온다. V 매트릭스 강화 코어는 최대 60). 같은 레벨 랭커가 5명 미만이거나 내 레벨을 모르면 전체 분포로 비교하고 차트 이름에 `*`를 붙인다. 칼리 랭커 1명 기준 20개 중 14개가 같은 레벨로 비교됐다. 칼리 B는 레벨이 낮아 대부분 전체 분포였다.
  - 칼리 4기 묶음: 2분 극딜 버프 6개(레이스 오브 갓, 매직 서킷, 그란디스, 오블리비온, 레조네이트 : 얼티메이텀, 리스트레인트 링), 1분 주기 데스 블로섬·스틱스·레디 투 다이(91%), 스파이더 인 미러·크레스트 오브 더 솔라
- `ranker` 패키지 (수집)
  - `RankerCollector`: 종합 랭킹 순서대로 ocid → 기록 목록 → 대상 기간 기록을 `ReplayQueryService`로 저장하고 `RankerSample`로 등록한다. 결과는 `RankerProbe`(NOT_FOUND / NO_RECORD / OTHER_PERIOD_ONLY / SAMPLED)로 남겨 다시 부르지 않는다. 다음 단계 호출이 상한을 넘으면 그 전에 멈춘다. `TaskExecutor`로 비동기 실행하며 한 번에 하나만 돈다.
  - `RankerAdminController`(`collector.enabled=true`일 때만 등록, 인증 없음 → 로컬 전용): `POST /api/admin/rankers/collect?jobClass=칼리-전체전직&maxCalls=400&maxRankers=200`, `GET /status`, `GET /statistics?characterClass=칼리&periodNo=4`
- 상세 화면 구성(위에서부터): 요약 카드 → 섹션 이동 칩(고정) → 진단(한 줄 요약 "고칠 점 N개 · 합계 X초 손해" + 카드) → 랭커 대비 → 쿨 대비 실제 사용 간격 → 점유율 → 타임라인. 수치 표는 모두 "전체 수치 보기"로 접는다.
  - 랭커 대비: "랭커 분포 속 내 위치" 차트(`renderRankerDistribution`, 랭커 25~75% 띠·중앙값 선·내 값 점, 랭커 중앙값 = 100%로 맞춤, 탭으로 분당 시전 수 / 초 환산 전환, 하위 25% 미만은 산호색 점으로 위에), 함께 쓰는 스킬 묶음 칩(절반 넘게 따로 쓰면 산호), 극딜 순서 칩 두 줄(랭커 표준 / 내 첫 극딜, 어긋난 스킬은 금색). 기간을 모르면(목록을 거치지 않음) 안내만 한다.
  - 쿨 대비 실제 사용 간격(`renderCooldownUsage`): 중앙 사용 간격 ÷ 실효 쿨. 1.15배 이하 청록, 1.5배 이하 금색, 그 이상 산호. 판단 제외 스킬은 회색으로 아래에 둔다. 제외 기준은 `CooldownReport.Row.usageExclusion`으로 서버가 준다(놓친 시전과 같은 `CooldownEligibility` 기준: 쿨 모름 / 15초 미만 / 쿨 변동).
  - 차트 데이터는 `analysis-data` 스크립트(`ReplayPageController.AnalysisChartData`)로 넘긴다.
- **처방과 쉰 구간 (B1·B2)**
  - `Finding.advice`: `message`는 근거, `advice`는 할 일이다. 카드에는 처방을 굵게 먼저, 근거를 흐리게 아래에 둔다(상세·비교 화면 모두).
  - `IdleBreakdown.spans`(`IdleSpan`: 극딜 대기 / 그 외 / 전투 종료 전). 합계 필드는 구간의 합이라 놓친 시전 계산은 그대로다.
  - `MissedCastAdvisor`: 가장 큰 원인에 맞춰 처방을 고른다. 그 외 → 가장 길게 쉰 구간. 극딜 대기 구간이 쿨보다 길어 "대기 시작 + 쿨 ≤ 극딜 시작"이면 → 그 시각에 한 번 더 쓰라고 한다. 전투 종료 전 → 끝까지 쓰라고 한다.
    - 시전 수 부족·랭커보다 적은 시전(`adviseShortfall`): 기준이 함께 쓰는 짝을 따로 썼으면 시퀀스 처방을 내고, 쉰 시간 처방을 붙인다. 쉰 시간이 쿨 1회분도 안 되면 원인을 단정하지 않는다.
    - 칼리 B 스틱스: "데스 블로섬 VI와 같은 시퀀스에 넣으면 랭커 대부분처럼 함께 나갑니다. 극딜을 기다리며 63초를 쉬었습니다. 168초에 한 번 쓰면 극딜 시작(223초) 전에 쿨이 다시 돕니다."
  - `KoreanJosa`: 스킬 이름 뒤 와/과, 를/을을 받침에 맞춘다(한글이 아닌 글자로 끝나면 받침 없음).
  - 타임라인: `IdleTimelineAssembler`(대상은 놓친 시전과 같다, 1초 미만 구간은 그리지 않음) → `attachIdleOverlay`(custom 시리즈 막대. 그 외 산호 / 극딜 대기 금색 / 전투 종료 전 회색). 기본은 진단·참고에 나온 스킬만 그리고, "쿨 15초 이상 스킬 모두 보기"로 전체를 그린다. 진단 카드의 "타임라인에서 보기"는 그 줄을 강조하고 가장 긴 쉰 구간 ±20초로 확대한다.
    - 확대 경계에 걸친 막대가 사라지지 않게 dataZoom `filterMode: 'weakFilter'`. 시전 시리즈에 `id: 'casts'`를 둬서, 나중에 합치는 시리즈가 순서대로 병합되며 덮어쓰지 않게 한다.
- **놓친 시전의 극딜 안팎(A1)**: `IdleBreakdown.placement` → `MissedPlacement(inBurst, outOfBurst, unplaced)`. 공백마다 쿨이 돈 시각부터 쿨 간격으로 가상 시전을 놓아 극딜 구간 안인지 본다. 공백 여러 개를 합쳐 1회가 된 몫은 위치 미정이다. `result`가 스킬별 합계만 줘서 극딜 안팎 1회 데미지를 나눌 수 없으므로 **숫자 범위 대신 방향만** 메시지에 붙인다(극딜 밖 몫은 평균보다 작고 안 몫은 크다). 극딜 대기로 놓친 시전은 대부분 극딜 밖에 놓인다.
- **미적용 확률을 기대 시전 수에 넣지 않는다(A2, 2026-09-29 검증)**: 칼리 미적용은 25~27%인데, 랭커 11명의 쿨 20초 이상 스킬 간격 486개 중 쿨 절반 미만으로 다시 쓴 것은 2.7%(13개)였다. 다른 직업(미적용 0~7%)도 0~6%였다(나이트로드 18.9%는 쿨 변동 스킬 영향). 기대 시전을 1/(1−p)배로 잡으면 랭커 전원이 약 27%를 놓친 것이 되어 쿨 11~12초 때와 같은 구조적 오탐이 된다. 같은 직업끼리 비교하는 규칙은 p가 비슷해 상쇄된다.
- `SequenceSegment`(domain): `sequence_key`가 같은 시전을 기록 순서대로 묶어 실행 구간을 만든다. 사이에 끼어든 일반 시전은 무시하고, 같은 키의 직전 시전과 10초(`MAX_GAP_MS`)보다 멀면 새 실행으로 본다. 실측으로 확인한 실행 안의 간격은 1.1초 이하, 실행 사이 간격은 52.8초 이상이다. `Replay.getSequenceSegments()` → `ReplayDetail.sequenceSegments`.

- 테스트: `SingleFlightTest`(CountDownLatch로 동시성 검증), `SkillNameTest`, `NexonDatesTest`, `SequenceSegmentTest`, `KoreanNumberFormatTest`, `SkillTextTest`, `StandardCooldownCalculatorTest`, `NexonCharacterSpecParserTest`(샘플 `src/test/resources/nexon/character-info-kali.json`: 칼리 A 원문에서 basic, final_stat, character_skill만 남김)
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
    - 결과: 칼리 A 판데모니움 2회 2.9초(극딜 대기 21.5초, 그 외 36.1초), 칼리 B 스틱스 3회 2.6초(극딜 대기 126.2초)
    - 실효 쿨 15초 미만은 제외한다(짧은 쿨 스킬끼리 시전 시간을 두고 경쟁한다. 근거는 5) 쿨 11~12초 스킬 항목). 간격 4개 이상이면서 이른 사용 비율이 미적용 확률 + 20%p를 넘으면 "쿨 변동"으로 보고 제외하고 참고에 적는다.
    - 전투 시작 시점에는 모든 스킬이 준비돼 있다고 본다.
    - 전투 끝까지 남은 시간은 "5초와 스킬 지속시간 중 긴 쪽"이 남았을 때만 놓친 시전으로 센다. 중간에 쉰 시간만 더해 쿨로 나누면, 칼리 B가 크레스트 오브 더 솔라(쿨 221초)를 226초 이후 전투 끝(339초)까지 쓰지 않은 누락을 놓친다. 반대로 칼리 A의 120초 버프는 전투 종료 6초 전에 쿨이 돌아서 세지 않는다.
    - 데미지 항목이 없는 스킬(버프)은 영향도 없이 참고로만 보여준다.
    - 적용 대상은 데이터로 가린다: "이른 사용" 비율이 미적용 확률보다 훨씬 높은 스킬은 쿨이 실행 중에 바뀌는 스킬로 보고 제외한다. 칼리의 차크람 퓨리·스플릿·스윕, 보이드 러쉬·블리츠는 이른 사용이 50~90%였다. 쿨이 긴 스킬은 0~1회였다.
    - 칼리 사례: 칼리 B 스틱스는 쿨 52.4초인데 105~115초 간격으로 썼다(쿨타임 표에서 바로 보인다).
  - [ ] `LinkedSkillRule` → **4)·5)로 옮겼다.** 기록 하나로는 짝을 판단할 수 없다. 극딜 때는 모든 스킬이 같이 나가서 함께 쓴 횟수로 보면 거의 모든 스킬이 짝이 되고, 칼리 B는 스틱스를 레디 투 다이와 같이 쓴 적이 없다. 누락 자체는 `MissedCastRule`이 잡는다.
  - [ ] `BurstAlignmentRule` → **4)·5)로 옮겼다.** 영향도를 계산하려면 버프 배율을 추정해야 해서 근거가 약하다. 대신 놓친 시전의 원인을 "극딜 대기"와 "그 외"로 나눠 보여준다.
- [x] 초 환산 영향도로 정렬하고 임계값 미만은 거른다.
- 보유했지만 한 번도 쓰지 않은 스킬은 다루지 않는다. `character_skill`에 펫·공용 스킬이 섞여 있어 잡음이 크다.

### 4) 비교 진단
- [x] 초 환산 정규화, `baseName` 기준 스킬별 시전 수와 점유율 차이 → 시전 수 효과와 1회 효율 효과로 분해(5장 참고)
- [x] `CastCountGapRule`: 기준보다 적게 쓴 시전 수(전투 시간 보정) × 내 1회 초 환산
  - 연동 스킬 힌트: 기준 기록에서 시전 수가 같고 1초 안에 80% 이상 함께 쓴 스킬(동률이면 가장 가까운 스킬). 칼리 B vs 칼리 A: "레디 투 다이와 6/6회, 이 기록은 0/3회". 누락을 두 번 세지 않도록 `LinkedSkillRule`을 따로 만들지 않고 여기에 합쳤다.
- [x] `LoadoutRule`: 기준이 쓴 스킬을 한 번도 안 썼을 때, 내 스킬 목록에 없으면 미보유·미해금(구성·스펙), 있으면 미사용(운용). 기준 캐릭터 스킬 목록에도 없는 공용 스킬(소울 컨트랙트)은 판단하지 않는다. 칼리 B: 플레게톤 미해금 2.3초, 불굴의 결의 미보유(참고)
- [x] 극딜 순서 비교 (Needleman-Wunsch)
- [x] 비교 화면: 타임라인 2개, 운용 / 구성·스펙으로 나눈 진단 목록
- [ ] `BurstAlignmentRule`: 1회 효율 효과를 레벨이 같은 스킬에서 "극딜 정렬 의심"으로 진단할지. 다른 스킬 레벨과 버프 레벨이 섞여 있어 지금은 표로만 보여준다.

### 5) 랭커 수집과 3단계 진단 (확장)
- [x] 종합 랭킹 API(직업 필터) → ocid → replay-id 수집 배치. 호출량 제한을 지키고, 연무장 기록 보유율을 먼저 측정한다. (칼리 4기 표본 11개, 보유율은 위 4장)
- [x] 직업별 스킬 통계(초 환산 분위수, 분당 시전, 스킬 채택률, 함께 쓰는 묶음), `period_no`별로 관리
- [x] `CastRateRule`, `LinkedPairRule`, 상세 화면 "랭커 대비"
- [x] 극딜 시퀀스 순서 분포(랭커 표준 순서)
  - `BurstOrderExtractor.burstCasts`: 모든 극딜 구간(시작 −5초 ~ +15초)에서 쿨 10초 이상 스킬의 첫 시전. 칼리 표본 11개에서 극딜 33회를 얻었다.
  - `BurstOrderStatistics`(`JobStatistics.burstOrder`): 스킬별 극딜 채택률, 상대 위치 중앙값(0 = 맨 앞, 1 = 맨 뒤), 합의된 앞뒤 쌍. 1초 안에 함께 나간 쌍은 같은 매크로로 보고 순서에서 뺀다. 극딜의 80% 이상에서 먼저면 합의로 본다.
  - 칼리 4기 표준 순서(채택률 50% 이상): 데스 블로섬 → 레이스 오브 갓 → 매직 서킷 → 그란디스 → 플레게톤(82%) → 오블리비온 → 레조네이트 : 얼티메이텀 → 리스트레인트 링 → 레디 투 다이 → 스틱스 → 판데모니움 → 듄 버스트 → 보이드 버스트. 합의 쌍 21개는 대부분 "매크로 스킬들 → 듄 버스트·보이드 버스트"다.
  - `BurstOrderRule`: 합의 쌍을 내 극딜 절반 넘게 반대로 쓰면 참고로 낸다(스킬당 합의율이 가장 높은 쌍 하나). 칼리 B의 "보이드 버스트 → 스틱스"는 0.3~0.6초 간격이라 동시로 보고 내지 않는다. 그 차이는 "연동 스킬 따로 사용"과 순서 표에 이미 드러난다.
  - 상세 화면 "랭커 대비"에 내 첫 극딜 순서와 표준 순서를 Needleman-Wunsch로 나란히 보여준다.
- [x] 다른 직업 수집으로 파서·진단 기준값 검증 (2026-09-28, 4기 표본: 아크메이지(썬,콜) 1, 나이트로드 1, 히어로 1, 보우마스터 3)
  - 수집: 직업당 110건으로 랭커 약 50명씩 확인했다. 4기 기록 보유율은 2~6%로 칼리(6%)보다 낮았다. 직업당 표본 5개면 150~250건이 든다.
  - **API 비용의 대부분은 기록이 없는 랭커 확인이다.** 2026-09-28 하루 약 950건 중 786건이 랭커 393명 확인(ocid + 기록 목록 2건씩)이었다. 그중 316명(80%)은 기록이 없었다. 리플레이 본문은 약 51건이었다. 랭킹 API가 ocid를 주지 않아 이 2건은 줄일 수 없다. 확인 결과는 `ranker_probe`에 남아 다시 들지 않는다.
  - `maxSamples`(표본 수 상한) 파라미터를 추가했다.
  - 2026-09-29 추가 수집(약 772건, 랭킹 기준일 2026-09-28): 아크메이지(썬,콜) 1 → 5, 히어로 1 → 5, 보우마스터 3 → 5, 나이트로드 1 → 3. 나이트로드는 추가로 150명을 확인했지만 4기 기록이 2명뿐이었다(1.3%). 표본 1개에 대략 40~90건이 들었다. 이제 칼리·아크메이지(썬,콜)·히어로·보우마스터는 통계 진단(표본 5개 이상)이 돈다.
  - 통하는 것: 쿨 파싱(스킬 정보 없음은 공용 소울 컨트랙트와 파생 스킬 `폭풍의 시 VI : 난사 모드`, `파이널 블레이드`뿐), 극딜 탐지(6개 모두 2분 주기 3회), 쿨 변동 제외(쉐도우 리츄얼, 풍마수리검, 썬더 브레이크, 서브제로 퍼미네이션), 쿨 30초 이상 스킬의 이른 사용 0~1회.
  - 미적용 확률은 칼리 27%, 다른 직업 3~7%다. 쿨 변동 판정 기준(미적용 + 20%p)은 직업별 값을 따라간다.
  - [x] **재발동 스킬 문제**: `초월 : 불굴의 결의`("스킬을 다시 사용하여 즉시 종료")는 다시 누른 것도 시전으로 기록돼 간격이 약 2.3초로 나왔다. 그래서 "쿨 변동"으로 잘못 제외됐다(히어로, 보우마스터).
    - 해결: `SkillText.isReactivatable()`(정규식 `스킬\s*(을\s*)?(다시 사용|재사용)(?!\s*대기)`)이 참이고 지속시간이 있는 스킬은, 앞 시전의 지속시간 안에 다시 누른 입력을 시전에서 뺀다(`SkillUsage.mergeReactivations`).
    - 칼리 해당 스킬: 리스트레인트 링, 불굴의 결의, 데저트 베일, 에르다 샤워, 레디 투 다이, 보이드 버스트, 데스 블로섬. 칼리 기록의 진단 결과는 바뀌지 않았다.
    - 타임라인 차트에는 원래 입력이 그대로 보인다. 쿨타임 표 비고에 "지속 중 다시 누른 입력은 시전에서 뺌"을 표시한다.
  - [x] **쿨 11~12초 스킬**: 포 시즌 VI(나이트로드, 8회 4.3초), 써든레이드 VI, 윈드 오브 프레이 VI(보우마스터)가 놓친 시전으로 잡힌다. 실제 손해인지, 짧은 쿨 스킬끼리 시전 시간을 두고 경쟁한 결과인지 표본 1~3개로는 구분할 수 없었다.
    - **2026-09-29 추가 표본으로 확인: 랭커 전원이 많이 놓친다(구조적 현상).** `GET /hold-tradeoff`에 표본별 실효 쿨과 놓친 시전 수를 추가해 봤다.
      - 포 시즌 VI(실효 10~11초): 3명 모두 8~11회, 시전 대비 42%. 써든레이드 VI: 9~14회, 58%.
      - 블리츠 실드 VI(히어로, 11초): 3~6회만 쓰고 24~32회를 놓쳤다. 딜 스킬이 아니라 상황용으로 보인다. 윈드 오브 프레이 VI는 보우마스터 5명 중 1명만 썼다.
      - 비교: 판데모니움(23~26초)은 칼리 11명 모두 0~2회, 시전 대비 11%였다.
    - **결정(2026-09-29)**: `MissedCastRule`(내 기록만 보는 절대 기준)만 대상 쿨을 15초 이상으로 올렸다(`CooldownEligibility.MIN_ABSOLUTE_COOLDOWN_MS`). `CastRateRule`·`CastCountGapRule`(상대 비교)은 구조적 손실이 기준 쪽에도 똑같이 있어 상쇄되므로 10초를 유지한다. 반영 후 나이트로드 표본 1명의 포 시즌·써든레이드, 보우마스터 표본 1명의 윈드 오브 프레이 오탐이 사라졌고, 칼리 기록의 진단은 그대로였다.
  - 보우마스터 표본 1명의 스틱스 4회 누락(극딜 대기 242.8초)은 칼리 B와 같은 패턴이다(2분 극딜에만 사용).
- [x] "아낀 것 vs 놀린 것" 가설 검증 → 결과는 5장. `IdleBreakdown`(쉰 시간을 극딜 대기 / 그 외 / 전투 종료 전으로 나눔, `MissedCastRule`과 공유), `HoldTradeoffAnalyzer`, `Correlation.spearman`, `RankerSampleLoader`(표본 로딩 공유)
  - [ ] 표본이 늘면 다시 돌려 본다. (스킬 레벨이 같은 표본끼리 비교하는 방식은 "랭커 대비" 초 환산에 적용했다.)
- [ ] 서비스 키 승인 신청(하루 1,000건으로는 수집이 불가능)

### 나중에 고려
- 서버를 여러 대로 늘릴 때 `NexonRateLimiter`와 `SingleFlight`를 Redis 기반으로 교체
- 버프 중에 쿨이 바뀌는 스킬(오블리비온 등) 시뮬레이션, 연동 스킬 데미지의 시전 귀속
