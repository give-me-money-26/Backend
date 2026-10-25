package kr.ac.catholic.aeroharvest.building;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface HourlyWindDensityRepository extends JpaRepository<HourlyWindDensity, Long> {
    List<HourlyWindDensity> findByBuildingIdOrderByHourOfDay(String buildingId);
    Optional<HourlyWindDensity> findByBuildingIdAndHourOfDay(String buildingId, int hourOfDay);
}

