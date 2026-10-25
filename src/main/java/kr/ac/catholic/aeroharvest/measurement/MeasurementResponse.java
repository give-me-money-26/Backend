package kr.ac.catholic.aeroharvest.measurement;

import java.time.OffsetDateTime;

public record MeasurementResponse(
        Long id,
        String buildingId,
        String buildingName,
        String ventId,
        OffsetDateTime measuredAt,
        String timezone,
        double windSpeedMS,
        double windDensityWM2,
        double ventAreaM2,
        String sensorId,
        QualityFlag qualityFlag,
        OperatingState operatingState,
        OffsetDateTime createdAt
) {
    static MeasurementResponse from(Measurement measurement) {
        double density = 0.5 * 1.225 * Math.pow(measurement.getWindSpeedMS(), 3);
        return new MeasurementResponse(measurement.getId(), measurement.getBuilding().getId(),
                measurement.getBuilding().getName(), measurement.getVentId(), measurement.getMeasuredAt(),
                measurement.getTimezone(), measurement.getWindSpeedMS(), density, measurement.getVentAreaM2(),
                measurement.getSensorId(), measurement.getQualityFlag(), measurement.getOperatingState(),
                measurement.getCreatedAt());
    }
}

