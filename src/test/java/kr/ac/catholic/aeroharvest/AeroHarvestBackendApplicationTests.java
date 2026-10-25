package kr.ac.catholic.aeroharvest;

import kr.ac.catholic.aeroharvest.building.BuildingRepository;
import kr.ac.catholic.aeroharvest.measurement.MeasurementRequest;
import kr.ac.catholic.aeroharvest.measurement.MeasurementService;
import kr.ac.catholic.aeroharvest.measurement.OperatingState;
import kr.ac.catholic.aeroharvest.measurement.QualityFlag;
import kr.ac.catholic.aeroharvest.simulation.SimulationRequest;
import kr.ac.catholic.aeroharvest.simulation.SimulationService;
import kr.ac.catholic.aeroharvest.simulation.SourceMode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class AeroHarvestBackendApplicationTests {
	@Autowired
	BuildingRepository buildings;
	@Autowired
	SimulationService simulations;
	@Autowired
	MeasurementService measurements;

	@Test
	void contextLoadsAndSeedsFiveBuildings() {
		assertThat(buildings.count()).isEqualTo(5);
	}

	@Test
	void calculatesUsingSeededTimeSlot() {
		var request = new SimulationRequest("NICHOLLS", SourceMode.TIME_SLOT, 15, null,
				0.8, 25.0, 2.0, 0.0, 8.0, 365, 150.0, 0.4, null);

		assertThat(simulations.calculate(request).densityWM2()).isEqualTo(18.9);
	}

	@Test
	void storesAndSearchesAFieldMeasurement() {
		var measuredAt = OffsetDateTime.parse("2026-10-24T15:00:00+09:00");
		measurements.create(new MeasurementRequest("NICHOLLS", "ROOF-01", measuredAt, "Asia/Seoul",
				3.2, 0.8, "SENSOR-001", QualityFlag.VALID, OperatingState.ON));

		var page = measurements.search("NICHOLLS", measuredAt.minusMinutes(1), measuredAt.plusMinutes(1),
				PageRequest.of(0, 20));

		assertThat(page.getTotalElements()).isEqualTo(1);
		assertThat(page.getContent().get(0).sensorId()).isEqualTo("SENSOR-001");
	}

}
