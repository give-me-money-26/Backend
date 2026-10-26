package kr.ac.catholic.aeroharvest.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
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
public class ModelController {
    private final ModelService service;

    public ModelController(ModelService service) { this.service = service; }

    @GetMapping("/turbines")
    public List<Map<String, Object>> turbines() { return service.turbines(); }

    @GetMapping("/turbines/{turbineId}")
    public Map<String, Object> turbine(@PathVariable String turbineId) { return service.turbine(turbineId); }

    @PostMapping("/simulations/timeseries")
    public Map<String, Object> timeseries(@Valid @RequestBody TimeseriesRequest request) {
        return service.timeseries(request);
    }

    @PostMapping("/optimizations/placement")
    public Map<String, Object> optimize(@Valid @RequestBody PlacementOptimizationRequest request) {
        return service.optimize(request);
    }

    @PostMapping("/validations")
    public Map<String, Object> createValidation(@Valid @RequestBody ValidationRequest request) {
        return service.createValidation(request);
    }

    @GetMapping("/validations")
    public List<Map<String, Object>> validations(@RequestParam(required = false) String buildingId) {
        return service.validations(buildingId);
    }

    @GetMapping("/validations/{validationId}")
    public Map<String, Object> validation(@PathVariable String validationId) {
        return service.validation(validationId);
    }

    public enum QualityPolicy { VALID_ONLY, VALID_AND_SUSPECT }
    public enum PlacementSource { TIME_SLOT, MEASUREMENT_AVERAGE }

    public record TimeseriesRequest(
            @NotBlank String buildingId,
            @NotBlank String ventId,
            @NotBlank String turbineId,
            @NotNull OffsetDateTime from,
            @NotNull OffsetDateTime to,
            QualityPolicy qualityPolicy,
            @NotNull @Positive Double distanceM,
            @NotNull Double angleDegrees,
            @NotNull @PositiveOrZero Double tariffKrwPerKwh,
            @NotNull @PositiveOrZero Double carbonFactorKgPerKwh,
            @PositiveOrZero Double installationCostKrw,
            @PositiveOrZero Double maintenanceCostKrw
    ) {}

    public record Range(@NotNull Double min, @NotNull Double max, @NotNull @Positive Double step) {}

    public record PlacementOptimizationRequest(
            @NotBlank String buildingId,
            @NotBlank String ventId,
            @NotNull PlacementSource source,
            @Min(0) @Max(23) Integer hour,
            @NotNull @Positive Double collectionAreaM2,
            @NotNull @PositiveOrZero Double efficiencyPercent,
            @NotNull @Valid Range distanceRangeM,
            @NotNull @Valid Range angleRangeDegrees,
            @Min(1) @Max(100) Integer limit
    ) {}

    public record ValidationPair(@NotNull @PositiveOrZero Double predictedW,
                                 @NotNull @PositiveOrZero Double actualW) {}

    public record ValidationThresholds(@PositiveOrZero Double maxMaeW,
                                       @PositiveOrZero Double maxRmseW,
                                       @PositiveOrZero Double maxMapePercent,
                                       Double maxAbsoluteBiasW) {}

    public record ValidationRequest(
            @NotBlank String buildingId,
            @NotBlank String ventId,
            @NotBlank String predictionModelVersion,
            @NotEmpty List<@Valid ValidationPair> samples,
            @Valid ValidationThresholds thresholds
    ) {}
}
