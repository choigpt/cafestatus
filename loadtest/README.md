# K6 부하테스트

Redis 캐시 + Pub/Sub 구현의 성능과 정합성을 검증하기 위한 K6 부하테스트 환경.

## 구성 요소

| 서비스 | 역할 | 포트 |
|--------|------|------|
| InfluxDB 1.8 | K6 메트릭 저장소 | 8086 |
| Grafana | 실시간 대시보드 | 3000 |
| K6 | 부하테스트 실행기 | - |

## 실행 방법

### 1. 전체 스택 기동

```bash
docker-compose -f docker-compose.yml -f docker-compose.loadtest.yml up -d mysql redis app influxdb grafana
```

### 2. 더미 데이터 시딩 (선택)

SQL 시드를 실행하면 사장님 10명 + 카페 100개 + 초기 상태가 일괄 로드됩니다.
K6 setup 단계에서 API 호출 대신 로그인만으로 데이터를 사용하므로 **테스트 시작이 빨라집니다**.

```bash
docker-compose -f docker-compose.yml -f docker-compose.loadtest.yml \
  --profile seed run --rm seed
```

시드 없이도 K6가 자체적으로 API를 통해 데이터를 생성하므로, 이 단계는 선택사항입니다.

**시드 데이터 구성:**
| 항목 | 수량 | 비고 |
|------|------|------|
| 사장님 | 10명 | `owner1~10@loadtest.com` / `TestPass123!` |
| 카페 | 100개 | 서울 10개 권역에 분산 (강남, 종로, 홍대, 이태원, 성수, 잠실, 신촌, 합정, 건대, 여의도) |
| 상태 | 100건 | 다양한 crowdLevel/party 조합 |

### 3. Grafana 대시보드 접속

- URL: http://localhost:3000
- 로그인: anonymous 접근 허용 (자동 Admin)
- K6 대시보드가 자동으로 프로비저닝됨

### 3. 시나리오별 테스트 실행

```bash
# 읽기 부하 (캐시 히트율 검증)
docker-compose -f docker-compose.yml -f docker-compose.loadtest.yml \
  run --rm k6 run /scripts/scenario-read.js

# 쓰기 부하 (캐시 갱신 성능)
docker-compose -f docker-compose.yml -f docker-compose.loadtest.yml \
  run --rm k6 run /scripts/scenario-write.js

# 혼합 부하 (읽기 90% + 쓰기 10%)
docker-compose -f docker-compose.yml -f docker-compose.loadtest.yml \
  run --rm k6 run /scripts/scenario-mixed.js

# 정합성 검증 (쓰기 직후 읽기)
docker-compose -f docker-compose.yml -f docker-compose.loadtest.yml \
  run --rm k6 run /scripts/scenario-consistency.js

# SSE 동시 연결 테스트
docker-compose -f docker-compose.yml -f docker-compose.loadtest.yml \
  run --rm k6 run /scripts/scenario-sse.js
```

## 테스트 시나리오

### scenario-read.js — 읽기 부하
- VU 100명 × 2분
- `GET /api/cafes/{id}/status` + `GET /api/cafes/near`
- **목표**: p95 < 50ms, 에러율 < 0.1%

### scenario-write.js — 쓰기 부하
- VU 50명 × 2분
- `PUT /api/owner/cafes/{id}/status`
- **목표**: p95 < 200ms, 에러율 < 0.1%

### scenario-mixed.js — 혼합 부하
- 읽기 200 req/s + 쓰기 20 req/s × 3분
- **목표**: 읽기 p95 < 50ms, 쓰기 p95 < 200ms

### scenario-consistency.js — 정합성 검증 ★
- VU 10명 × 500 iterations
- 쓰기 직후 읽기에서 최신값이 보이는지 검증
- **목표**: consistency_failures == 0

### scenario-sse.js — SSE 동시 연결
- SSE 구독 50명 + 상태 업데이트 5명 × 30초
- **목표**: SSE 연결 성공률 > 95%

## 캐시 비활성화 비교 테스트

Redis 캐시 효과를 측정하려면 `application-prod.yaml`에서:

```yaml
cache:
  redis:
    enabled: false  # 캐시 비활성화
```

동일 시나리오를 실행한 후 Grafana에서 RPS/p95 차이를 비교.

## 정리

```bash
docker-compose -f docker-compose.yml -f docker-compose.loadtest.yml down -v
```
