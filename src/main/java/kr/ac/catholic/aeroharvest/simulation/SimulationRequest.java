package kr.ac.catholic.aeroharvest.simulation;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

public record SimulationRequest(
        @NotBlank String buildingId,
        @NotNull SourceMode sourceMode,
        @Min(0) @Max(23) Integer hour,
        @PositiveOrZero Double windSpeedMS,
        @NotNull @Positive Double collectionAreaM2,
        @NotNull @DecimalMin("0.0") @DecimalMax("59.3") Double efficiencyPercent,
        @NotNull @DecimalMin("1.0") @DecimalMax("8.0") Double distanceM,
        @NotNull @DecimalMin("-80.0") @DecimalMax("80.0") Double angleDegrees,
        @NotNull @DecimalMin("0.0") @DecimalMax("24.0") Double operatingHoursPerDay,
        @NotNull @Min(1) @Max(365) Integer operatingDaysPerYear,
        @NotNull @PositiveOrZero Double tariffKrwPerKwh,
        @NotNull @PositiveOrZero Double carbonFactorKgPerKwh,
        @PositiveOrZero Double installationCostKrw
) {}

