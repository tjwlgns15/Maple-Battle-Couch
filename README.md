# battle-coach

메이플스토리 연무장 기록 조회, 진단, 비교 서비스.

## 실행

```bash
export NEXON_API_KEY=발급받은_키
export DB_USERNAME=root
export DB_PASSWORD=...
# MySQL에 battle_coach 스키마 생성 후
./gradlew bootRun
```

## API (1단계: 조회와 캐시)

| 메서드 | 경로 | 설명 |
|---|---|---|
| GET | `/api/characters/{characterName}/replays` | 기간별 연무장 기록 목록 (Caffeine 10분 캐시) |
| GET | `/api/replays/{replayId}` | 결과, 스킬별 데미지, 스킬 사용 내역 (MySQL 영구 캐시) |

## 첫 실행 때 확인할 것

- 연무장 API 경로(`/maplestory/v1/battle-practice/...`)가 실제로 응답하는지
- 같은 기간에 다시 등록하면 `replay_id`가 바뀌는지
- 존재하지 않는 캐릭터명으로 조회했을 때 Nexon 오류 코드와 HTTP 상태
