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
        AppliedConditions appliedConditions,
        List<String> limitations
) {
    public record AppliedConditions(double collectionAreaM2, double efficiencyPercent, double distanceM,
                                    double angleDegrees, double operatingHoursPerDay, int operatingDaysPerYear,
                                    double tariffKrwPerKwh, double carbonFactorKgPerKwh,
                                    Double installationCostKrw) {}
}

