package kr.ac.catholic.aeroharvest.measurement;

import kr.ac.catholic.aeroharvest.building.BuildingRepository;
import kr.ac.catholic.aeroharvest.common.NotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.zone.ZoneRulesException;

@Service
public class MeasurementService {
    private final MeasurementRepository measurements;
    private final BuildingRepository buildings;

    public MeasurementService(MeasurementRepository measurements, BuildingRepository buildings) {
        this.measurements = measurements;
        this.buildings = buildings;
    }

    @Transactional
    public MeasurementResponse create(MeasurementRequest request) {
        try {
            ZoneId.of(request.timezone());
        } catch (ZoneRulesException exception) {
            throw new IllegalArgumentException("유효하지 않은 timezone입니다: " + request.timezone());
        }
        var building = buildings.findById(request.buildingId())
                .orElseThrow(() -> new NotFoundException("건물을 찾을 수 없습니다: " + request.buildingId()));
        var measurement = new Measurement(building, request.ventId(), request.measuredAt(), request.timezone(),
                request.windSpeedMS(), request.ventAreaM2(), request.sensorId(), request.qualityFlag(),
                request.operatingState());
        return MeasurementResponse.from(measurements.save(measurement));
    }

    @Transactional(readOnly = true)
    public Page<MeasurementResponse> search(String buildingId, OffsetDateTime from, OffsetDateTime to,
                                            Pageable pageable) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new IllegalArgumentException("from은 to보다 늦을 수 없습니다.");
        }
        if (buildingId != null && !buildings.existsById(buildingId)) {
            throw new NotFoundException("건물을 찾을 수 없습니다: " + buildingId);
        }
        return measurements.search(buildingId, from, to, pageable).map(MeasurementResponse::from);
    }
}

