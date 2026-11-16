package kr.ac.catholic.aeroharvest.simulation;

import java.util.List;

/**
 * 상세분석 화면의 터빈 선택지. DB에 저장하지 않는 고정 상수입니다.
 * 회수 전력에 powerWeight를 곱하고, 회수기간 = (priceKrw + installCostKrw) ÷ 연간 절감액.
 */
public record TurbineOption(String id, String name, long priceKrw, long installCostKrw, double powerWeight) {

    /** 프론트 src/data/turbineModels.js(2026-11-16 기준) 값을 그대로 옮긴 가정값 — 출처 확인 필요. */
    public static final List<TurbineOption> CATALOG = List.of(
            new TurbineOption("TURBINE-1", "터빈 1", 60_000, 40_000, 1.0),
            new TurbineOption("TURBINE-2", "터빈 2", 120_000, 80_000, 1.5),
            new TurbineOption("TURBINE-3", "터빈 3", 180_000, 120_000, 2.0));

    public long totalCostKrw() {
        return priceKrw + installCostKrw;
    }
}
