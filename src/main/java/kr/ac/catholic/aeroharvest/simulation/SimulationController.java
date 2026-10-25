package kr.ac.catholic.aeroharvest.simulation;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/v1")
public class SimulationController {
    private final SimulationService service;

    public SimulationController(SimulationService service) {
        this.service = service;
    }

    @PostMapping("/simulations")
    public SimulationResult calculate(@Valid @RequestBody SimulationRequest request) {
        return service.calculate(request);
    }

    @PostMapping("/simulations/compare")
    public ComparisonResult compare(@Valid @RequestBody ComparisonRequest request) {
        var baseline = service.calculate(request.baseline());
        var current = service.calculate(request.current());
        double delta = current.recoveredPowerW() - baseline.recoveredPowerW();
        Double percent = baseline.recoveredPowerW() > 0 ? delta / baseline.recoveredPowerW() * 100 : null;
        return new ComparisonResult(baseline, current, delta, percent);
    }

    @GetMapping("/assumptions/defaults")
    public DefaultAssumptions defaults() {
        return new DefaultAssumptions(1.225, 0.8, 25, 8, 365, 150, 0.4, 2, 0,
                List.of("P = D × A × η × f", "f = exp(-0.13 × (거리-1)) × cos²(각도)",
                        "E = P × 일 운전시간 × 연 운전일 ÷ 1,000"));
    }

    public record ComparisonRequest(@NotNull @Valid SimulationRequest baseline,
                                    @NotNull @Valid SimulationRequest current) {}
    public record ComparisonResult(SimulationResult baseline, SimulationResult current,
                                   double recoveredPowerDeltaW, Double recoveredPowerDeltaPercent) {}
    public record DefaultAssumptions(double airDensityKgM3, double collectionAreaM2,
                                     double efficiencyPercent, double operatingHoursPerDay,
                                     int operatingDaysPerYear, double tariffKrwPerKwh,
                                     double carbonFactorKgPerKwh, double distanceM, double angleDegrees,
                                     List<String> formulas) {}
}
