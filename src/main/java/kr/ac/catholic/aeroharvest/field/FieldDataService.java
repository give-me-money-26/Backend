package kr.ac.catholic.aeroharvest.field;

import kr.ac.catholic.aeroharvest.common.ConflictException;
import kr.ac.catholic.aeroharvest.common.NotFoundException;
import kr.ac.catholic.aeroharvest.measurement.MeasurementRequest;
import kr.ac.catholic.aeroharvest.measurement.MeasurementResponse;
import kr.ac.catholic.aeroharvest.measurement.MeasurementService;
import kr.ac.catholic.aeroharvest.measurement.OperatingState;
import kr.ac.catholic.aeroharvest.measurement.QualityFlag;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class FieldDataService {
    private static final double DENSITY_FACTOR = 0.5 * 1.225;
    private final JdbcClient jdbc;
    private final MeasurementService measurementService;

    public FieldDataService(JdbcClient jdbc, MeasurementService measurementService) {
        this.jdbc = jdbc;
        this.measurementService = measurementService;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> ventsForBuilding(String buildingId) {
        requireBuilding(buildingId);
        return jdbc.sql("""
                select id, building_id, name, type, position_x, position_y, position_z,
                       coordinate_system, width_m, height_m, area_m2, direction_degrees,
                       status, source_type, last_surveyed_at
                from vents where building_id = :buildingId order by id
                """).param("buildingId", buildingId).query(this::ventRow).list();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> vent(String ventId) {
        return jdbc.sql("""
                select id, building_id, name, type, position_x, position_y, position_z,
                       coordinate_system, width_m, height_m, area_m2, direction_degrees,
                       status, source_type, last_surveyed_at
                from vents where id = :ventId
                """).param("ventId", ventId).query(this::ventRow).optional()
                .orElseThrow(() -> new NotFoundException("배기구를 찾을 수 없습니다: " + ventId));
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> sensors() {
        return jdbc.sql("""
                select id, manufacturer, model, serial_number, measurement_types,
                       minimum_value, maximum_value, accuracy_text, calibrated_at,
                       calibration_expires_at, status from sensors order by id
                """).query(this::sensorRow).list();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> sensor(String sensorId) {
        return jdbc.sql("""
                select id, manufacturer, model, serial_number, measurement_types,
                       minimum_value, maximum_value, accuracy_text, calibrated_at,
                       calibration_expires_at, status from sensors where id = :sensorId
                """).param("sensorId", sensorId).query(this::sensorRow).optional()
                .orElseThrow(() -> new NotFoundException("센서를 찾을 수 없습니다: " + sensorId));
    }

    @Transactional
    public Map<String, Object> createBatch(String batchId, List<FieldDataController.BatchRecord> records) {
        if (records == null || records.isEmpty()) {
            throw new IllegalArgumentException("records는 한 개 이상이어야 합니다.");
        }
        if (records.size() > 1000) {
            throw new IllegalArgumentException("한 batch는 최대 1000건입니다.");
        }
        List<MeasurementResponse> created = new ArrayList<>();
        for (var record : records) {
            if (record.clientRecordId() == null || record.clientRecordId().isBlank()) {
                throw new IllegalArgumentException("clientRecordId가 필요합니다.");
            }
            int duplicates = jdbc.sql("select count(*) from measurements where client_record_id = :id")
                    .param("id", record.clientRecordId()).query(Integer.class).single();
            if (duplicates > 0) {
                throw new ConflictException("중복 clientRecordId입니다: " + record.clientRecordId());
            }
            requireVentAndSensor(record.measurement().ventId(), record.measurement().sensorId());
            var response = measurementService.create(record.measurement());
            jdbc.sql("update measurements set batch_id = :batchId, client_record_id = :clientId where id = :id")
                    .param("batchId", batchId).param("clientId", record.clientRecordId())
                    .param("id", response.id()).update();
            created.add(response);
        }
        return Map.of("batchId", batchId, "accepted", created.size(), "measurements", created);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> measurement(long id) {
        return jdbc.sql("""
                select m.id, m.building_id, b.name as building_name, m.vent_id, m.measured_at,
                       m.timezone, m.wind_speed_m_s, m.vent_area_m2, m.sensor_id,
                       m.quality_flag, m.operating_state, m.created_at, m.batch_id, m.client_record_id
                from measurements m join buildings b on b.id = m.building_id where m.id = :id
                """).param("id", id).query(this::measurementRow).optional()
                .orElseThrow(() -> new NotFoundException("측정값을 찾을 수 없습니다: " + id));
    }

    @Transactional
    public Map<String, Object> updateQuality(long id, QualityFlag newFlag, String reason, String actor) {
        String previous = jdbc.sql("select quality_flag from measurements where id = :id")
                .param("id", id).query(String.class).optional()
                .orElseThrow(() -> new NotFoundException("측정값을 찾을 수 없습니다: " + id));
        jdbc.sql("update measurements set quality_flag = :flag where id = :id")
                .param("flag", newFlag.name()).param("id", id).update();
        jdbc.sql("""
                insert into measurement_quality_history
                    (measurement_id, previous_flag, new_flag, reason, actor)
                values (:id, :previous, :next, :reason, :actor)
                """).param("id", id).param("previous", previous).param("next", newFlag.name())
                .param("reason", reason).param("actor", actor).update();
        return measurement(id);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> qualityHistory(long id) {
        measurement(id);
        return jdbc.sql("""
                select id, measurement_id, previous_flag, new_flag, reason, actor, changed_at
                from measurement_quality_history where measurement_id = :id order by changed_at desc, id desc
                """).param("id", id).query((rs, row) -> mapOf(
                        "id", rs.getLong("id"), "measurementId", rs.getLong("measurement_id"),
                        "previousFlag", rs.getString("previous_flag"), "newFlag", rs.getString("new_flag"),
                        "reason", rs.getString("reason"), "actor", rs.getString("actor"),
                        "changedAt", rs.getObject("changed_at", OffsetDateTime.class))).list();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> summary(String buildingId, String ventId, OffsetDateTime from, OffsetDateTime to,
                                       boolean includeSuspect) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new IllegalArgumentException("from은 to보다 늦을 수 없습니다.");
        }
        if (buildingId != null) requireBuilding(buildingId);
        StringBuilder sql = new StringBuilder("""
                select m.wind_speed_m_s, m.measured_at, m.quality_flag, m.operating_state
                from measurements m where m.quality_flag <> 'INVALID'
                """);
        Map<String, Object> params = new LinkedHashMap<>();
        if (!includeSuspect) sql.append(" and m.quality_flag = 'VALID'");
        if (buildingId != null) { sql.append(" and m.building_id = :buildingId"); params.put("buildingId", buildingId); }
        if (ventId != null) { sql.append(" and m.vent_id = :ventId"); params.put("ventId", ventId); }
        if (from != null) { sql.append(" and m.measured_at >= :fromTime"); params.put("fromTime", from); }
        if (to != null) { sql.append(" and m.measured_at <= :toTime"); params.put("toTime", to); }
        sql.append(" order by m.measured_at");
        var rows = jdbc.sql(sql.toString()).params(params).query((rs, n) -> new SummaryPoint(
                rs.getDouble("wind_speed_m_s"), rs.getObject("measured_at", OffsetDateTime.class),
                rs.getString("quality_flag"), rs.getString("operating_state"))).list();
        double average = rows.stream().mapToDouble(SummaryPoint::windSpeed).average().orElse(0);
        double maximum = rows.stream().mapToDouble(SummaryPoint::windSpeed).max().orElse(0);
        Map<Integer, List<SummaryPoint>> grouped = new LinkedHashMap<>();
        for (var row : rows) grouped.computeIfAbsent(row.measuredAt().getHour(), ignored -> new ArrayList<>()).add(row);
        List<Map<String, Object>> hourly = grouped.entrySet().stream().map(entry -> {
            double avg = entry.getValue().stream().mapToDouble(SummaryPoint::windSpeed).average().orElse(0);
            return mapOf("hour", entry.getKey(), "count", entry.getValue().size(), "averageWindSpeedMS", avg,
                    "averageDensityWM2", DENSITY_FACTOR * Math.pow(avg, 3));
        }).toList();
        return mapOf("buildingId", buildingId, "ventId", ventId, "from", from, "to", to,
                "qualityPolicy", includeSuspect ? "VALID_AND_SUSPECT" : "VALID_ONLY",
                "count", rows.size(), "averageWindSpeedMS", average, "maximumWindSpeedMS", maximum,
                "averageDensityWM2", DENSITY_FACTOR * Math.pow(average, 3), "hourly", hourly);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> sceneConfig(String buildingId) {
        requireBuilding(buildingId);
        return mapOf("buildingId", buildingId, "sceneVersion", "scene-v1",
                "coordinateSystem", "BUILDING_LOCAL_M", "modelUrl", null,
                "camera", Map.of("position", List.of(12, 10, 18), "target", List.of(0, 0, 0)),
                "vents", ventsForBuilding(buildingId),
                "limitations", List.of("현재 좌표는 ESTIMATED일 수 있습니다.", "정밀 CFD 결과가 아닙니다."));
    }

    @Transactional(readOnly = true)
    public Map<String, Object> dashboard(String buildingId) {
        var building = jdbc.sql("select id, name, code, data_status, advice from buildings where id = :id")
                .param("id", buildingId).query((rs, n) -> mapOf("id", rs.getString("id"),
                        "name", rs.getString("name"), "code", rs.getString("code"),
                        "dataStatus", rs.getString("data_status"), "advice", rs.getString("advice")))
                .optional().orElseThrow(() -> new NotFoundException("건물을 찾을 수 없습니다: " + buildingId));
        return mapOf("building", building, "vents", ventsForBuilding(buildingId),
                "measurementSummary", summary(buildingId, null, null, null, true),
                "missingEvidence", List.of("장비 교정정보", "HVAC 운전기록", "배기구 실측 좌표"));
    }

    @Transactional(readOnly = true)
    public byte[] exportMeasurements(String buildingId, OffsetDateTime from, OffsetDateTime to) {
        StringBuilder sql = new StringBuilder("""
                select id, building_id, vent_id, measured_at, timezone, wind_speed_m_s,
                       vent_area_m2, sensor_id, quality_flag, operating_state, batch_id, client_record_id
                from measurements where 1=1
                """);
        Map<String, Object> params = new LinkedHashMap<>();
        if (buildingId != null) { sql.append(" and building_id = :buildingId"); params.put("buildingId", buildingId); }
        if (from != null) { sql.append(" and measured_at >= :fromTime"); params.put("fromTime", from); }
        if (to != null) { sql.append(" and measured_at <= :toTime"); params.put("toTime", to); }
        sql.append(" order by measured_at");
        List<String> rows = jdbc.sql(sql.toString()).params(params).query((rs, n) -> String.join(",",
                csv(rs.getString("id")), csv(rs.getString("building_id")), csv(rs.getString("vent_id")),
                csv(rs.getString("measured_at")), csv(rs.getString("timezone")), csv(rs.getString("wind_speed_m_s")),
                csv(rs.getString("vent_area_m2")), csv(rs.getString("sensor_id")), csv(rs.getString("quality_flag")),
                csv(rs.getString("operating_state")), csv(rs.getString("batch_id")), csv(rs.getString("client_record_id")))).list();
        String header = "id,buildingId,ventId,measuredAt,timezone,windSpeedMS,ventAreaM2,sensorId,qualityFlag,operatingState,batchId,clientRecordId\n";
        return (header + String.join("\n", rows)).getBytes(StandardCharsets.UTF_8);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> referenceFactors() {
        return jdbc.sql("""
                select type, factor_value, unit, source_name, source_url, valid_from, valid_to,
                       version, scope, status from reference_factors
                where valid_to is null or valid_to >= current_date order by type, valid_from desc
                """).query((rs, n) -> mapOf("type", rs.getString("type"), "value", rs.getDouble("factor_value"),
                        "unit", rs.getString("unit"), "sourceName", rs.getString("source_name"),
                        "sourceUrl", rs.getString("source_url"), "validFrom", rs.getObject("valid_from"),
                        "validTo", rs.getObject("valid_to"), "version", rs.getString("version"),
                        "scope", rs.getString("scope"), "status", rs.getString("status"))).list();
    }

    private Map<String, Object> ventRow(ResultSet rs, int row) throws SQLException {
        return mapOf("id", rs.getString("id"), "buildingId", rs.getString("building_id"),
                "name", rs.getString("name"), "type", rs.getString("type"),
                "position", mapOf("x", nullableDouble(rs, "position_x"), "y", nullableDouble(rs, "position_y"),
                        "z", nullableDouble(rs, "position_z"), "coordinateSystem", rs.getString("coordinate_system")),
                "widthM", nullableDouble(rs, "width_m"), "heightM", nullableDouble(rs, "height_m"),
                "areaM2", nullableDouble(rs, "area_m2"), "directionDegrees", nullableDouble(rs, "direction_degrees"),
                "status", rs.getString("status"), "sourceType", rs.getString("source_type"),
                "lastSurveyedAt", rs.getObject("last_surveyed_at", OffsetDateTime.class));
    }

    private Map<String, Object> sensorRow(ResultSet rs, int row) throws SQLException {
        return mapOf("id", rs.getString("id"), "manufacturer", rs.getString("manufacturer"),
                "model", rs.getString("model"), "serialNumber", rs.getString("serial_number"),
                "measurementTypes", List.of(rs.getString("measurement_types").split(",")),
                "minimumValue", nullableDouble(rs, "minimum_value"), "maximumValue", nullableDouble(rs, "maximum_value"),
                "accuracy", rs.getString("accuracy_text"),
                "calibratedAt", rs.getObject("calibrated_at", OffsetDateTime.class),
                "calibrationExpiresAt", rs.getObject("calibration_expires_at", OffsetDateTime.class),
                "status", rs.getString("status"));
    }

    private Map<String, Object> measurementRow(ResultSet rs, int row) throws SQLException {
        double speed = rs.getDouble("wind_speed_m_s");
        return mapOf("id", rs.getLong("id"), "buildingId", rs.getString("building_id"),
                "buildingName", rs.getString("building_name"), "ventId", rs.getString("vent_id"),
                "measuredAt", rs.getObject("measured_at", OffsetDateTime.class), "timezone", rs.getString("timezone"),
                "windSpeedMS", speed, "windDensityWM2", DENSITY_FACTOR * Math.pow(speed, 3),
                "ventAreaM2", rs.getDouble("vent_area_m2"), "sensorId", rs.getString("sensor_id"),
                "qualityFlag", rs.getString("quality_flag"), "operatingState", rs.getString("operating_state"),
                "createdAt", rs.getObject("created_at", OffsetDateTime.class), "batchId", rs.getString("batch_id"),
                "clientRecordId", rs.getString("client_record_id"));
    }

    private void requireBuilding(String id) {
        int count = jdbc.sql("select count(*) from buildings where id = :id").param("id", id).query(Integer.class).single();
        if (count == 0) throw new NotFoundException("건물을 찾을 수 없습니다: " + id);
    }

    private void requireVentAndSensor(String ventId, String sensorId) {
        int vents = jdbc.sql("select count(*) from vents where id = :id").param("id", ventId).query(Integer.class).single();
        if (vents == 0) throw new NotFoundException("배기구를 찾을 수 없습니다: " + ventId);
        int sensors = jdbc.sql("select count(*) from sensors where id = :id").param("id", sensorId).query(Integer.class).single();
        if (sensors == 0) throw new NotFoundException("센서를 찾을 수 없습니다: " + sensorId);
    }

    private static Double nullableDouble(ResultSet rs, String name) throws SQLException {
        double value = rs.getDouble(name);
        return rs.wasNull() ? null : value;
    }

    private static String csv(String value) {
        if (value == null) return "";
        return '"' + value.replace("\"", "\"\"") + '"';
    }

    private static Map<String, Object> mapOf(Object... values) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < values.length; i += 2) map.put((String) values[i], values[i + 1]);
        return map;
    }

    private record SummaryPoint(double windSpeed, OffsetDateTime measuredAt, String quality, String operatingState) {}
}

