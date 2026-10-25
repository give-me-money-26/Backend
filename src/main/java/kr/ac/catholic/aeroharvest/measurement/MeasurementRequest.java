package kr.ac.catholic.aeroharvest.measurement;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.time.OffsetDateTime;

public record MeasurementRequest(
        @NotBlank String buildingId,
        @NotBlank String ventId,
        @NotNull OffsetDateTime measuredAt,
        @NotBlank String timezone,
        @NotNull @PositiveOrZero Double windSpeedMS,
        @NotNull @Positive Double ventAreaM2,
        @NotBlank String sensorId,
        @NotNull QualityFlag qualityFlag,
        @NotNull OperatingState operatingState
) {}

