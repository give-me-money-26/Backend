# 01 API

기준 URL은 `http://localhost:8080/api/v1`입니다. 상세 필드와 예시는 [`openapi.yaml`](./openapi.yaml)에 있습니다.

| 메서드 | 경로 | 화면/기능 |
| --- | --- | --- |
| GET | `/buildings` | 캠퍼스 지도 핀, 우선순위, 최대 밀도 |
| GET | `/buildings/{buildingId}` | 건물 정보, 10~22시 그래프, 설치 검토 방향 |
| GET | `/assumptions/defaults` | 계산 조건 초기값과 공식 |
| POST | `/simulations` | 현재 조건의 전력·연간 발전량·수익·환경 결과 |
| POST | `/simulations/compare` | 기준안과 현재안의 전력 차이 및 비율 |
| POST | `/measurements` | 현장 센서 측정값 등록 |
| GET | `/measurements` | 건물·기간별 측정값 페이지 조회 |
| GET | `/buildings/{buildingId}/vents` | 건물 배기구 목록 |
| GET | `/vents/{ventId}` | 배기구 상세·근거 상태 |
| GET | `/sensors`, `/sensors/{sensorId}` | 센서 목록·상세 |
| POST | `/measurements/batch` | 최대 1,000건 배치 등록·중복 방지 |
| GET | `/measurements/{measurementId}` | 측정값 상세 |
| PATCH | `/measurements/{measurementId}/quality` | 품질 플래그 변경과 이력 기록 |
| GET | `/measurements/{measurementId}/history` | 품질 변경 이력 |
| GET | `/measurements/summary` | 풍속·밀도 시간대 집계 |
| GET | `/buildings/{buildingId}/scene-config` | 3D 장면용 배기구 설정 |
| GET | `/buildings/{buildingId}/dashboard` | 건물 대시보드 통합 응답 |
| GET | `/exports/measurements` | 측정값 CSV 내보내기 |
| GET | `/reference-factors/current` | 현재 적용 계수와 출처 상태 |
| GET | `/turbines`, `/turbines/{turbineId}` | 터빈과 성능곡선 |
| POST | `/simulations/timeseries` | 실측 시계열 기반 발전량 계산 |
| POST | `/optimizations/placement` | 거리·각도 후보 탐색 |
| POST/GET | `/validations` | 예측 검증 생성·목록 |
| GET | `/validations/{validationId}` | 검증 결과 상세 |
| GET | `/candidates`, `/candidates/{candidateId}` | 후보지 근거·누락자료 |
| POST/GET | `/decisions` | 판정 생성·목록 |
| GET/PATCH | `/decisions/{decisionId}` | 판정 상세·변경 이력 |

`TIME_SLOT`은 건물의 테스트 밀도를 사용하고, `DIRECT_SPEED`는 입력 풍속으로 밀도를 다시 계산합니다. 누락된 데이터는 0으로 간주하지 않고 404 또는 400으로 응답합니다.

현재 시드의 배기구·센서·터빈 곡선·요금/탄소계수는 실측 또는 공식 출처가 확정되기 전의
`ESTIMATED`, `UNKNOWN`, `DRAFT` 자료입니다. 응답에도 상태와 제한 문구를 포함하며 설치 확정 추천을 반환하지 않습니다.

