package kr.ac.catholic.aeroharvest.simulation;

import java.util.List;

public record SimulationResult(
        String buildingId,
        String buildingName,
        SourceMode sourceMode,
        Integer hour,
        double windSpeedMS,
        double densityWM2,
        double placementFactor,
        double recoveredPowerW,
        double annualEnergyKwh,
        double annualSavingsKrw,
        double annualCarbonReductionKg,
        Double simplePaybackYears,
        String resultType,
        String calculationNotice,
        String economicsNotice,
        AppliedConditions appliedConditions,
        List<String> limitations,
        Interpretation interpretation,
        List<TurbineScenario> turbineScenarios
) {
    public record AppliedConditions(double collectionAreaM2, double efficiencyPercent, double distanceM,
                                    double angleDegrees, double operatingHoursPerDay, int operatingDaysPerYear,
                                    double tariffKrwPerKwh, double carbonFactorKgPerKwh,
                                    Double installationCostKrw) {}

    /**
     * 화면 "결과 해석" 칸에 그대로 넣는 문장. revenue = 예상 수익 상세분석, environment = 환경 영향 상세분석.
     * 줄바꿈 없는 문자열이며, 문장 규칙은 ResultInterpreter에 있습니다.
     */
    public record Interpretation(String revenue, String environment) {}

    /**
     * 터빈 선택지별 결과. 화면에서 터빈을 바꾸면 다시 요청하지 않고 이 목록에서 골라 보여 줍니다.
     * 수치는 위 기본 결과(배율 1)에 터빈 배율을 곱한 값이고, 회수기간 = (가격 + 설치비) ÷ 연간 절감액입니다.
     */
    public record TurbineScenario(String turbineId, String name, long priceKrw, long installCostKrw,
                                  double powerWeight, double recoveredPowerW, double annualEnergyKwh,
                                  double annualSavingsKrw, double annualCarbonReductionKg,
                                  Double paybackYears, Interpretation interpretation) {}
}
