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

`TIME_SLOT`은 건물의 테스트 밀도를 사용하고, `DIRECT_SPEED`는 입력 풍속으로 밀도를 다시 계산합니다. 누락된 데이터는 0으로 간주하지 않고 404 또는 400으로 응답합니다.

