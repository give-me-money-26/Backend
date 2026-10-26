package kr.ac.catholic.aeroharvest;

import kr.ac.catholic.aeroharvest.decision.DecisionController;
import kr.ac.catholic.aeroharvest.decision.DecisionService;
import kr.ac.catholic.aeroharvest.field.FieldDataController;
import kr.ac.catholic.aeroharvest.field.FieldDataService;
import kr.ac.catholic.aeroharvest.measurement.MeasurementRequest;
import kr.ac.catholic.aeroharvest.measurement.OperatingState;
import kr.ac.catholic.aeroharvest.measurement.QualityFlag;
import kr.ac.catholic.aeroharvest.model.ModelController;
import kr.ac.catholic.aeroharvest.model.ModelService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class ExpandedApiIntegrationTests {
    private static final String BUILDING = "NICHOLLS";
    private static final String VENT = "NICHOLLS-ROOF-01";
    private static final String SENSOR = "SENSOR-DEMO-001";
    private static final String TURBINE = "TURBINE-DEMO-001";

    @Autowired FieldDataService fieldData;
    @Autowired ModelService model;
    @Autowired DecisionService decisions;
    @Autowired @Qualifier("requestMappingHandlerMapping") RequestMappingHandlerMapping mappings;

    @Test
    void exposesAllTwentySixRequiredRoutes() {
        Set<String> routes = mappings.getHandlerMethods().keySet().stream()
                .flatMap(info -> info.getMethodsCondition().getMethods().stream()
                        .flatMap(method -> info.getPatternValues().stream()
                                .map(pattern -> method.name() + " " + pattern)))
                .collect(Collectors.toSet());
        assertThat(routes).contains(
                "GET /api/v1/buildings/{buildingId}/vents", "GET /api/v1/vents/{ventId}",
                "GET /api/v1/sensors", "GET /api/v1/sensors/{sensorId}",
                "POST /api/v1/measurements/batch", "GET /api/v1/measurements/{measurementId}",
                "PATCH /api/v1/measurements/{measurementId}/quality",
                "GET /api/v1/measurements/{measurementId}/history", "GET /api/v1/measurements/summary",
                "GET /api/v1/buildings/{buildingId}/scene-config",
                "GET /api/v1/buildings/{buildingId}/dashboard", "GET /api/v1/exports/measurements",
                "GET /api/v1/reference-factors/current", "GET /api/v1/turbines",
                "GET /api/v1/turbines/{turbineId}", "POST /api/v1/simulations/timeseries",
                "POST /api/v1/optimizations/placement", "POST /api/v1/validations",
                "GET /api/v1/validations", "GET /api/v1/validations/{validationId}",
                "GET /api/v1/candidates", "GET /api/v1/candidates/{candidateId}",
                "POST /api/v1/decisions", "GET /api/v1/decisions",
                "GET /api/v1/decisions/{decisionId}", "PATCH /api/v1/decisions/{decisionId}");
    }

    @Test
    void supportsFieldModelValidationAndDecisionWorkflow() {
        assertThat(fieldData.ventsForBuilding(BUILDING)).hasSize(1);
        assertThat(fieldData.vent(VENT)).containsEntry("buildingId", BUILDING);
        assertThat(fieldData.sensors()).isNotEmpty();
        assertThat(fieldData.sensor(SENSOR)).containsEntry("status", "UNKNOWN");

        OffsetDateTime start = OffsetDateTime.of(2026, 10, 26, 14, 0, 0, 0, ZoneOffset.ofHours(9));
        String token = UUID.randomUUID().toString();
        var records = List.of(
                batchRecord(token + "-1", start, 3.0, QualityFlag.VALID),
                batchRecord(token + "-2", start.plusHours(1), 4.0, QualityFlag.SUSPECT),
                batchRecord(token + "-3", start.plusHours(2), 5.0, QualityFlag.VALID));
        var batch = fieldData.createBatch("BATCH-" + token, records);
        assertThat(batch).containsEntry("accepted", 3);

        long measurementId = ((Number) fieldData.summary(BUILDING, VENT, start, start.plusHours(2), true)
                .get("count")).longValue();
        assertThat(measurementId).isEqualTo(3);

        @SuppressWarnings("unchecked")
        var created = (List<kr.ac.catholic.aeroharvest.measurement.MeasurementResponse>) batch.get("measurements");
        long firstId = created.get(0).id();
        assertThat(fieldData.measurement(firstId)).containsEntry("clientRecordId", token + "-1");
        fieldData.updateQuality(firstId, QualityFlag.SUSPECT, "통합 테스트 품질 검토", "integration-test");
        assertThat(fieldData.qualityHistory(firstId)).hasSize(1);
        assertThat(new String(fieldData.exportMeasurements(BUILDING, start, start.plusHours(2)), StandardCharsets.UTF_8))
                .contains("clientRecordId", token + "-1");
        assertThat(fieldData.sceneConfig(BUILDING)).containsEntry("sceneVersion", "scene-v1");
        assertThat(fieldData.dashboard(BUILDING)).containsKeys("measurementSummary", "missingEvidence");
        assertThat(fieldData.referenceFactors()).hasSize(3);

        assertThat(model.turbines()).hasSize(1);
        assertThat(model.turbine(TURBINE)).containsKey("powerCurve");
        var timeseries = model.timeseries(new ModelController.TimeseriesRequest(
                BUILDING, VENT, TURBINE, start, start.plusHours(2),
                ModelController.QualityPolicy.VALID_AND_SUSPECT, 1.0, 0.0,
                150.0, 0.4, 100_000.0, 0.0));
        assertThat((double) timeseries.get("energyKwh")).isPositive();

        var optimization = model.optimize(new ModelController.PlacementOptimizationRequest(
                BUILDING, VENT, ModelController.PlacementSource.TIME_SLOT, 15,
                0.8, 30.0, new ModelController.Range(1.0, 2.0, 1.0),
                new ModelController.Range(0.0, 30.0, 30.0), 3));
        assertThat((List<?>) optimization.get("candidates")).hasSize(3);
        assertThat(optimization).containsEntry("isInstallationRecommendation", false);

        var validation = model.createValidation(new ModelController.ValidationRequest(
                BUILDING, VENT, "HEURISTIC-v1",
                List.of(new ModelController.ValidationPair(10.0, 9.0),
                        new ModelController.ValidationPair(20.0, 18.0)), null));
        String validationId = (String) validation.get("id");
        assertThat(model.validation(validationId)).containsEntry("status", "COMPLETED");
        assertThat(model.validations(BUILDING)).isNotEmpty();

        String candidateId = BUILDING + ":" + VENT;
        assertThat(decisions.candidates(BUILDING)).hasSize(1);
        assertThat(decisions.candidate(candidateId)).containsEntry("isInstallationRecommendation", false);
        var decision = decisions.create(new DecisionController.DecisionCreateRequest(
                candidateId, DecisionController.DecisionStatus.MEASURE,
                "실측 반복 횟수와 장비 교정정보 보강", null, null));
        String decisionId = (String) decision.get("id");
        var updated = decisions.update(decisionId, new DecisionController.DecisionUpdateRequest(
                DecisionController.DecisionStatus.REVIEW, "근거 보강 후 재검토", null, null, "integration-test"));
        assertThat(updated).containsEntry("status", "REVIEW");
        assertThat((List<?>) updated.get("history")).hasSize(1);
        assertThat(decisions.decisions(candidateId, DecisionController.DecisionStatus.REVIEW)).hasSize(1);
    }

    private static FieldDataController.BatchRecord batchRecord(String clientId, OffsetDateTime measuredAt,
                                                               double windSpeed, QualityFlag quality) {
        return new FieldDataController.BatchRecord(clientId, new MeasurementRequest(
                BUILDING, VENT, measuredAt, "Asia/Seoul", windSpeed, 0.8,
                SENSOR, quality, OperatingState.ON));
    }
}
