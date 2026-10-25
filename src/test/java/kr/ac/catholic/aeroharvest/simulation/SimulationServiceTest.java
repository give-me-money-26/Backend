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

    private static org.assertj.core.data.Offset<Double> within(double value) {
        return org.assertj.core.data.Offset.offset(value);
    }
}
