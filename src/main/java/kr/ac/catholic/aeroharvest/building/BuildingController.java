package kr.ac.catholic.aeroharvest.building;

import kr.ac.catholic.aeroharvest.common.NotFoundException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/buildings")
public class BuildingController {
    private static final double AIR_DENSITY_FACTOR = 0.6125;
    private final BuildingRepository buildings;
    private final HourlyWindDensityRepository densities;

    public BuildingController(BuildingRepository buildings, HourlyWindDensityRepository densities) {
        this.buildings = buildings;
        this.densities = densities;
    }

    @GetMapping
    public List<BuildingSummary> list() {
        return buildings.findAllByOrderByPriorityRankAsc().stream().map(building -> {
            var series = densities.findByBuildingIdOrderByHourOfDay(building.getId());
            double peak = series.stream().mapToDouble(HourlyWindDensity::getDensityWM2).max().orElse(0);
            return new BuildingSummary(building.getId(), building.getName(), building.getCode(),
                    building.getMapX(), building.getMapY(), building.getPriorityRank(), building.getModelType(),
                    building.getDataStatus(), peak);
        }).toList();
    }

    @GetMapping("/{buildingId}")
    public BuildingDetail detail(@PathVariable String buildingId) {
        var building = buildings.findById(buildingId)
                .orElseThrow(() -> new NotFoundException("건물을 찾을 수 없습니다: " + buildingId));
        var series = densities.findByBuildingIdOrderByHourOfDay(buildingId).stream()
                .map(value -> new HourlyPoint(value.getHourOfDay(), value.getDensityWM2(),
                        Math.cbrt(value.getDensityWM2() / AIR_DENSITY_FACTOR), value.getSourceType()))
                .toList();
        return new BuildingDetail(building.getId(), building.getName(), building.getCode(), building.getMapX(),
                building.getMapY(), building.getPriorityRank(), building.getModelType(), building.getAdvice(),
                building.getDataStatus(), series);
    }

    public record BuildingSummary(String id, String name, String code, double mapX, double mapY,
                                  int priorityRank, String modelType, String dataStatus, double peakDensityWM2) {}
    public record HourlyPoint(int hour, double densityWM2, double inferredWindSpeedMS, String sourceType) {}
    public record BuildingDetail(String id, String name, String code, double mapX, double mapY,
                                 int priorityRank, String modelType, String advice, String dataStatus,
                                 List<HourlyPoint> hourlySeries) {}
}

