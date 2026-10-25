package kr.ac.catholic.aeroharvest.simulation;

import kr.ac.catholic.aeroharvest.building.BuildingRepository;
import kr.ac.catholic.aeroharvest.building.HourlyWindDensityRepository;
import kr.ac.catholic.aeroharvest.common.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SimulationService {
    static final double AIR_DENSITY_KG_M3 = 1.225;
    private static final double DENSITY_FACTOR = 0.5 * AIR_DENSITY_KG_M3;
    private final BuildingRepository buildings;
    private final HourlyWindDensityRepository densities;

    public SimulationService(BuildingRepository buildings, HourlyWindDensityRepository densities) {
        this.buildings = buildings;
        this.densities = densities;
    }

    @Transactional(readOnly = true)
    public SimulationResult calculate(SimulationRequest request) {
        var building = buildings.findById(request.buildingId())
                .orElseThrow(() -> new NotFoundException("건물을 찾을 수 없습니다: " + request.buildingId()));

        double density;
        double windSpeed;
        Integer hour = null;
        if (request.sourceMode() == SourceMode.TIME_SLOT) {
            if (request.hour() == null) throw new IllegalArgumentException("TIME_SLOT 계산에는 hour가 필요합니다.");
            var point = densities.findByBuildingIdAndHourOfDay(request.buildingId(), request.hour())
                    .orElseThrow(() -> new NotFoundException("해당 시간의 풍력 밀도 데이터가 없습니다."));
            density = point.getDensityWM2();
            windSpeed = Math.cbrt(density / DENSITY_FACTOR);
            hour = request.hour();
        } else {
            if (request.windSpeedMS() == null) {
                throw new IllegalArgumentException("DIRECT_SPEED 계산에는 windSpeedMS가 필요합니다.");
            }
            windSpeed = request.windSpeedMS();
            density = DENSITY_FACTOR * Math.pow(windSpeed, 3);
        }

        double placementFactor = Math.exp(-0.13 * (request.distanceM() - 1))
                * Math.pow(Math.cos(Math.toRadians(request.angleDegrees())), 2);
        double recoveredPower = density * request.collectionAreaM2()
                * request.efficiencyPercent() / 100 * placementFactor;
        double annualEnergy = recoveredPower * request.operatingHoursPerDay()
                * request.operatingDaysPerYear() / 1000;
        double annualSavings = annualEnergy * request.tariffKrwPerKwh();
        double annualCarbon = annualEnergy * request.carbonFactorKgPerKwh();
        Double payback = request.installationCostKrw() != null && annualSavings > 0
                ? request.installationCostKrw() / annualSavings : null;

        return new SimulationResult(request.buildingId(), building.getName(), request.sourceMode(), hour,
                windSpeed, density, placementFactor, recoveredPower, annualEnergy, annualSavings, annualCarbon,
                payback, new SimulationResult.AppliedConditions(request.collectionAreaM2(),
                request.efficiencyPercent(), request.distanceM(), request.angleDegrees(),
                request.operatingHoursPerDay(), request.operatingDaysPerYear(), request.tariffKrwPerKwh(),
                request.carbonFactorKgPerKwh(), request.installationCostKrw()),
                List.of("배치 계수는 설명용 가정식이며 CFD 또는 터빈 성능곡선이 아닙니다.",
                        "현재 조건이 입력한 운전시간 동안 유지된다는 단순 시나리오입니다.",
                        "설치 판단 전 소음, 압력손실, 배기량과 HVAC 영향을 현장에서 검증해야 합니다."));
    }
}

