# AeroHarvest Backend

피그마 화면과 `AeroHarvest-기획안-v3.md`, 기존 프런트 프로토타입을 기준으로 만든 Spring Boot REST API입니다.

## 한눈에 보기

- Spring Boot 4.1.2 / Java 17+
- 건물 5개와 시간대별 테스트 밀도 제공
- 풍속·배치·운전 조건 기반 발전량, 절감액, 탄소 저감, 단순 회수기간 계산
- 기준안과 현재안 비교
- 배기구·센서·현장 측정 배치 수집, 품질 이력, 집계·CSV 내보내기
- 터빈 곡선, 시계열 발전량, 배치 탐색, 예측 검증
- 후보지 근거 조회와 MEASURE/HOLD/REVIEW 의사결정 이력
- H2 + Flyway + Spring Data JPA
- 입력 검증, 공통 오류 응답, CORS, Actuator 상태 확인

## 문서 순서

1. [`01_api`](./01_api/README.md): API 목록, 요청·응답, OpenAPI 계약
2. [`02_기능`](./02_기능/구현목록.md): 구현한 기능과 판단 근거
3. [`03_데이터`](./03_데이터/데이터설계.md): 테이블과 테스트/실측 데이터 구분
4. [`04_실행및검증`](./04_실행및검증/실행방법.md): 실행, 테스트, 운영 전환
5. [`05_프론트연동`](./05_프론트연동/연동가이드.md): 기존 `app.js`를 API로 교체하는 방법
6. [`06_추후과제`](./06_추후과제/백로그.md): 현장 적용 전에 남은 항목

## 바로 실행

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-21'
.\mvnw.cmd spring-boot:run
```

상태 확인: `GET http://localhost:8080/actuator/health`

확장 API 26개를 포함한 전체 계약은 [`01_api/openapi.yaml`](./01_api/openapi.yaml), 구현 목록은
[`02_기능/구현목록.md`](./02_기능/구현목록.md)에서 확인할 수 있습니다.

