package kr.ac.catholic.aeroharvest.field;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import kr.ac.catholic.aeroharvest.measurement.MeasurementRequest;
import kr.ac.catholic.aeroharvest.measurement.QualityFlag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@Validated
@RestController
@RequestMapping("/api/v1")
public class FieldDataController {
    private final FieldDataService service;

    public FieldDataController(FieldDataService service) { this.service = service; }

    @GetMapping("/buildings/{buildingId}/vents")
    public List<Map<String, Object>> vents(@PathVariable String buildingId) {
        return service.ventsForBuilding(buildingId);
    }

    @GetMapping("/vents/{ventId}")
    public Map<String, Object> vent(@PathVariable String ventId) { return service.vent(ventId); }

    @GetMapping("/sensors")
    public List<Map<String, Object>> sensors() { return service.sensors(); }

    @GetMapping("/sensors/{sensorId}")
    public Map<String, Object> sensor(@PathVariable String sensorId) { return service.sensor(sensorId); }

    @PostMapping("/measurements/batch")
    public Map<String, Object> createBatch(@Valid @RequestBody BatchRequest request) {
        return service.createBatch(request.batchId(), request.records());
    }

    @GetMapping("/measurements/{measurementId}")
    public Map<String, Object> measurement(@PathVariable long measurementId) {
        return service.measurement(measurementId);
    }

    @PatchMapping("/measurements/{measurementId}/quality")
    public Map<String, Object> updateQuality(@PathVariable long measurementId,
                                              @Valid @RequestBody QualityUpdateRequest request) {
        return service.updateQuality(measurementId, request.qualityFlag(), request.reason(), request.actor());
    }

    @GetMapping("/measurements/{measurementId}/history")
    public List<Map<String, Object>> qualityHistory(@PathVariable long measurementId) {
        return service.qualityHistory(measurementId);
    }

    @GetMapping("/measurements/summary")
    public Map<String, Object> summary(
            @RequestParam(required = false) String buildingId,
            @RequestParam(required = false) String ventId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to,
            @RequestParam(defaultValue = "false") boolean includeSuspect) {
        return service.summary(buildingId, ventId, from, to, includeSuspect);
    }

    @GetMapping("/buildings/{buildingId}/scene-config")
    public Map<String, Object> sceneConfig(@PathVariable String buildingId) {
        return service.sceneConfig(buildingId);
    }

    @GetMapping("/buildings/{buildingId}/dashboard")
    public Map<String, Object> dashboard(@PathVariable String buildingId) {
        return service.dashboard(buildingId);
    }

    @GetMapping(value = "/exports/measurements", produces = "text/csv")
    public ResponseEntity<byte[]> export(
            @RequestParam(required = false) String buildingId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=measurements.csv")
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .body(service.exportMeasurements(buildingId, from, to));
    }

    @GetMapping("/reference-factors/current")
    public List<Map<String, Object>> referenceFactors() { return service.referenceFactors(); }

    public record BatchRequest(@NotBlank String batchId, @NotEmpty List<@Valid BatchRecord> records) {}
    public record BatchRecord(@NotBlank String clientRecordId, @NotNull @Valid MeasurementRequest measurement) {}
    public record QualityUpdateRequest(@NotNull QualityFlag qualityFlag, @NotBlank String reason,
                                       @NotBlank String actor) {}
}

