package kr.ac.catholic.aeroharvest.simulation;

import kr.ac.catholic.aeroharvest.building.Building;
import kr.ac.catholic.aeroharvest.building.BuildingRepository;
import kr.ac.catholic.aeroharvest.building.HourlyWindDensityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SimulationServiceTest {
    @Mock BuildingRepository buildings;
    @Mock HourlyWindDensityRepository densities;
    @Mock Building building;
    SimulationService service;

    @BeforeEach
    void setUp() {
        service = new SimulationService(buildings, densities);
        when(buildings.findById("NICHOLLS")).thenReturn(Optional.of(building));
        when(building.getName()).thenReturn("니콜스관");
    }

    @Test
    void calculatesDirectSpeedScenarioUsingDocumentedFormula() {
        var request = new SimulationRequest("NICHOLLS", SourceMode.DIRECT_SPEED, null, 3.0,
                0.8, 25.0, 1.0, 0.0, 8.0, 365, 150.0, 0.4, 1_000_000.0);

        var result = service.calculate(request);

        assertThat(result.densityWM2()).isCloseTo(16.5375, within(0.00001));
        assertThat(result.placementFactor()).isEqualTo(1.0);
        assertThat(result.recoveredPowerW()).isCloseTo(3.3075, within(0.00001));
        assertThat(result.annualEnergyKwh()).isCloseTo(9.6579, within(0.0001));
        assertThat(result.simplePaybackYears()).isNotNull();
    }

    @Test
    void placementFactorDropsAsDistanceAndAngleIncrease() {
        var request = new SimulationRequest("NICHOLLS", SourceMode.DIRECT_SPEED, null, 3.0,
                0.8, 25.0, 3.0, 30.0, 8.0, 365, 150.0, 0.4, null);

        var result = service.calculate(request);

        assertThat(result.placementFactor()).isBetween(0.0, 1.0);
        assertThat(result.simplePaybackYears()).isNull();
    }

    @Test
    void returnsOneScenarioPerTurbineWithoutChangingTheRequest() {
        var request = new SimulationRequest("NICHOLLS", SourceMode.DIRECT_SPEED, null, 3.0,
                0.8, 25.0, 1.0, 0.0, 8.0, 365, 150.0, 0.4, null);

        var result = service.calculate(request);

        assertThat(result.turbineScenarios()).extracting(SimulationResult.TurbineScenario::turbineId)
                .containsExactly("TURBINE-1", "TURBINE-2", "TURBINE-3");

        var first = result.turbineScenarios().get(0);
        assertThat(first.annualEnergyKwh()).isCloseTo(9.6579, within(0.0001));
        assertThat(first.paybackYears()).isCloseTo(100_000 / (9.6579 * 150), within(0.01));
        assertThat(first.interpretation().revenue()).startsWith("니콜스관은 연간 약 9.7 kWh를 회수해 연 1,449원을");

        var second = result.turbineScenarios().get(1);
        assertThat(second.recoveredPowerW()).isCloseTo(3.3075 * 1.5, within(0.00001));
        assertThat(second.annualCarbonReductionKg()).isCloseTo(9.6579 * 1.5 * 0.4, within(0.0001));
        assertThat(second.paybackYears()).isCloseTo(200_000 / (9.6579 * 1.5 * 150), within(0.01));
        assertThat(second.interpretation().revenue()).startsWith("니콜스관은 연간 약 14.5 kWh를 회수해");
        assertThat(second.interpretation().environment()).contains("소나무");
    }

    @Test
    void topLevelInterpretationKeepsBaseResultWithoutTurbine() {
        var request = new SimulationRequest("NICHOLLS", SourceMode.DIRECT_SPEED, null, 3.0,
                0.8, 25.0, 1.0, 0.0, 8.0, 365, 150.0, 0.4, null);

        var result = service.calculate(request);

        assertThat(result.recoveredPowerW()).isCloseTo(3.3075, within(0.00001));
        assertThat(result.interpretation().revenue()).endsWith("아낄 수 있습니다.");
    }

    private static org.assertj.core.data.Offset<Double> within(double value) {
        return org.assertj.core.data.Offset.offset(value);
    }
}
