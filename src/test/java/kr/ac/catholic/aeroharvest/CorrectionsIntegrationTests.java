package kr.ac.catholic.aeroharvest;

import kr.ac.catholic.aeroharvest.building.BuildingController;
import kr.ac.catholic.aeroharvest.common.NotFoundException;
import kr.ac.catholic.aeroharvest.field.FieldDataService;
import kr.ac.catholic.aeroharvest.measurement.MeasurementRepository;
import kr.ac.catholic.aeroharvest.measurement.MeasurementRequest;
import kr.ac.catholic.aeroharvest.measurement.MeasurementService;
import kr.ac.catholic.aeroharvest.measurement.OperatingState;
import kr.ac.catholic.aeroharvest.measurement.QualityFlag;
import kr.ac.catholic.aeroharvest.model.ModelController;
import kr.ac.catholic.aeroharvest.model.ModelService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class CorrectionsIntegrationTests {
    private static final String BUILDING = "NICHOLLS";
    private static final String VENT = "NICHOLLS-ROOF-01";
    private static final String SENSOR = "SENSOR-DEMO-001";

    @Autowired MeasurementService measurements;
    @Autowired MeasurementRepository measurementRepository;
    @Autowired FieldDataService fieldData;
    @Autowired ModelService model;
    @Autowired BuildingController buildings;

    @Test
    void exposesPreliminaryDensityProvenanceWithoutChangingPdfValues() {
        var detail = buildings.detail(BUILDING);

        assertThat(detail.densityNotice()).contains("실제 발전 전력");
        assertThat(detail.hourlySeries()).hasSize(13);
        assertThat(detail.hourlySeries().get(5).densityWM2()).isEqualTo(18.9);
        assertThat(detail.hourlySeries().get(5).densityUnit()).isEqualTo("W_PER_M2");
        assertThat(detail.hourlySeries().get(5).sourceType()).isEqualTo("PRELIMINARY_MEASUREMENT_PDF");
        assertThat(detail.hourlySeries().get(5).dataStatus()).isEqualTo("PRELIMINARY");
    }

    @Test
    void rejectsUnknownSensorAndVentFromAnotherBuilding() {
        var time = OffsetDateTime.parse("2030-01-01T10:00:00+09:00");

        assertThatThrownBy(() -> measurements.create(request("DASOL-ROOF-01", SENSOR, time, 3.0)))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("건물에 속한 배기구");
        assertThatThrownBy(() -> measurements.create(request(VENT, "UNKNOWN-SENSOR", time, 3.0)))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("센서를 찾을 수 없습니다");
    }

    @Test
    void averagesIndividualWindDensitiesInsteadOfCubingAverageSpeed() {
        var from = OffsetDateTime.parse("2030-02-01T10:00:00+09:00");
        measurements.create(request(VENT, SENSOR, from, 0.0));
        measurements.create(request(VENT, SENSOR, from.plusHours(1), 10.0));
        measurementRepository.flush();

        var summary = fieldData.summary(BUILDING, VENT, from, from.plusHours(1), false);

        assertThat((double) summary.get("averageWindSpeedMS")).isEqualTo(5.0);
        assertThat((double) summary.get("averageDensityWM2")).isEqualTo(306.25);
        assertThat(summary).containsEntry("densityAggregation", "AVERAGE_OF_INDIVIDUAL_DENSITIES");
    }

    @Test
    void timeseriesStopsAtRequestedEndWithoutExtendingLastSample() {
        var from = OffsetDateTime.parse("2030-03-01T14:00:00+09:00");
        measurements.create(request(VENT, SENSOR, from, 3.0));
        measurements.create(request(VENT, SENSOR, from.plusHours(1), 4.0));
        measurements.create(request(VENT, SENSOR, from.plusHours(2), 5.0));
        measurementRepository.flush();

        var result = model.timeseries(new ModelController.TimeseriesRequest(
                BUILDING, VENT, "TURBINE-DEMO-001", from, from.plusHours(2),
                ModelController.QualityPolicy.VALID_ONLY, 1.0, 0.0,
                150.0, 0.4, 100_000.0, 0.0));

        assertThat((double) result.get("energyKwh")).isEqualTo(0.002);
        assertThat(result).containsEntry("resultType", "PRELIMINARY_SCENARIO");
        @SuppressWarnings("unchecked")
        var series = (List<Map<String, Object>>) result.get("series");
        assertThat((double) series.get(2).get("representedHours")).isZero();
    }

    private static MeasurementRequest request(String ventId, String sensorId,
                                              OffsetDateTime measuredAt, double windSpeed) {
        return new MeasurementRequest(BUILDING, ventId, measuredAt, "Asia/Seoul", windSpeed,
                0.8, sensorId, QualityFlag.VALID, OperatingState.ON);
    }
}

