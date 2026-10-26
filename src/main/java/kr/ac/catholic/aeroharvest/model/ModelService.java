package kr.ac.catholic.aeroharvest.model;

import kr.ac.catholic.aeroharvest.common.NotFoundException;
import kr.ac.catholic.aeroharvest.common.UnprocessableEntityException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ModelService {
    private static final double DENSITY_FACTOR = 0.5 * 1.225;
    private final JdbcClient jdbc;

    public ModelService(JdbcClient jdbc) { this.jdbc = jdbc; }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> turbines() {
        return jdbc.sql("select * from turbines order by id")
                .query((rs, row) -> turbineMap(rs, false)).list();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> turbine(String turbineId) {
        var turbine = jdbc.sql("select * from turbines where id = :id").param("id", turbineId)
                .query((rs, row) -> turbineMap(rs, true)).optional()
                .orElseThrow(() -> new NotFoundException("터빈을 찾을 수 없습니다: " + turbineId));
        turbine.put("powerCurve", curve(turbineId).stream().map(p -> mapOf(
                "windSpeedMS", p.windSpeed(), "powerW", p.power())).toList());
        return turbine;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> timeseries(ModelController.TimeseriesRequest request) {
        requireBuildingAndVent(request.buildingId(), request.ventId());
        if (request.from().isAfter(request.to())) {
            throw new IllegalArgumentException("from은 to보다 늦을 수 없습니다.");
        }
        var turbine = turbine(request.turbineId());
        var curve = curve(request.turbineId());
        if (curve.size() < 2) {
            throw new UnprocessableEntityException("터빈 power curve가 두 점 이상 필요합니다.");
        }
        var policy = request.qualityPolicy() == null
                ? ModelController.QualityPolicy.VALID_ONLY : request.qualityPolicy();
        String qualitySql = policy == ModelController.QualityPolicy.VALID_ONLY
                ? "and quality_flag = 'VALID'" : "and quality_flag <> 'INVALID'";
        var points = jdbc.sql("""
                select measured_at, wind_speed_m_s, quality_flag
                from measurements
                where building_id = :buildingId and vent_id = :ventId
                  and measured_at >= :fromTime and measured_at <= :toTime
                """ + qualitySql + " order by measured_at")
                .param("buildingId", request.buildingId()).param("ventId", request.ventId())
                .param("fromTime", request.from()).param("toTime", request.to())
                .query((rs, row) -> new WindPoint(rs.getObject("measured_at", OffsetDateTime.class),
                        rs.getDouble("wind_speed_m_s"), rs.getString("quality_flag"))).list();
        if (points.isEmpty()) {
            throw new UnprocessableEntityException("조건에 맞는 측정 시계열이 없습니다.");
        }
        int allCount = jdbc.sql("""
                select count(*) from measurements where building_id = :buildingId and vent_id = :ventId
                  and measured_at >= :fromTime and measured_at <= :toTime
                """).param("buildingId", request.buildingId()).param("ventId", request.ventId())
                .param("fromTime", request.from()).param("toTime", request.to()).query(Integer.class).single();

        double placement = placementFactor(request.distanceM(), request.angleDegrees());
        double energyKwh = 0;
        List<Map<String, Object>> series = new ArrayList<>();
        double previousHours = 1.0;
        for (int i = 0; i < points.size(); i++) {
            var point = points.get(i);
            double hours;
            if (i + 1 < points.size()) {
                hours = Duration.between(point.measuredAt(), points.get(i + 1).measuredAt()).toMinutes() / 60.0;
                hours = Math.max(0, Math.min(hours, 6));
                if (hours > 0) previousHours = hours;
            } else {
                hours = previousHours;
            }
            double rawPower = interpolate(curve, point.windSpeed());
            double power = rawPower * placement;
            energyKwh += power * hours / 1000.0;
            series.add(mapOf("measuredAt", point.measuredAt(), "windSpeedMS", point.windSpeed(),
                    "powerW", power, "representedHours", hours, "qualityFlag", point.quality()));
        }
        double maintenance = request.maintenanceCostKrw() == null ? 0 : request.maintenanceCostKrw();
        double savings = energyKwh * request.tariffKrwPerKwh() - maintenance;
        Double payback = request.installationCostKrw() != null && savings > 0
                ? request.installationCostKrw() / savings : null;
        List<String> limitations = new ArrayList<>();
        limitations.add("마지막 측정값은 직전 간격을 대표한다고 가정합니다.");
        limitations.add("6시간을 초과하는 간격은 6시간으로 제한합니다.");
        if (!"VERIFIED".equals(turbine.get("verificationStatus"))) {
            limitations.add("검증되지 않은 DRAFT power curve입니다. 설치 판단에 사용하지 마세요.");
        }
        return mapOf("dataWindow", mapOf("from", request.from(), "to", request.to(),
                        "points", points.size(), "excludedPoints", allCount - points.size()),
                "model", mapOf("turbineId", request.turbineId(), "curveVersion", turbine.get("curveVersion"),
                        "verificationStatus", turbine.get("verificationStatus"), "placementFactor", placement),
                "energyKwh", energyKwh, "savingsKrw", savings,
                "carbonKg", energyKwh * request.carbonFactorKgPerKwh(), "simplePaybackYears", payback,
                "series", series, "limitations", limitations);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> optimize(ModelController.PlacementOptimizationRequest request) {
        requireBuildingAndVent(request.buildingId(), request.ventId());
        validateRange(request.distanceRangeM(), "distanceRangeM");
        validateRange(request.angleRangeDegrees(), "angleRangeDegrees");
        double density;
        if (request.source() == ModelController.PlacementSource.TIME_SLOT) {
            if (request.hour() == null) throw new IllegalArgumentException("TIME_SLOT에는 hour가 필요합니다.");
            density = jdbc.sql("""
                    select density_w_m2 from hourly_wind_densities
                    where building_id = :buildingId and hour_of_day = :hour
                    """).param("buildingId", request.buildingId()).param("hour", request.hour())
                    .query(Double.class).optional()
                    .orElseThrow(() -> new NotFoundException("해당 시간대 데이터가 없습니다."));
        } else {
            Double measuredDensity = jdbc.sql("""
                    select avg(0.6125 * power(wind_speed_m_s, 3)) from measurements
                    where building_id = :buildingId and vent_id = :ventId and quality_flag = 'VALID'
                    """).param("buildingId", request.buildingId()).param("ventId", request.ventId())
                    .query(Double.class).optional().orElse(null);
            if (measuredDensity == null) throw new UnprocessableEntityException("VALID 측정값이 없습니다.");
            density = measuredDensity;
        }
        List<Map<String, Object>> candidates = new ArrayList<>();
        int guard = 0;
        for (double distance = request.distanceRangeM().min(); distance <= request.distanceRangeM().max() + 1e-9;
             distance += request.distanceRangeM().step()) {
            for (double angle = request.angleRangeDegrees().min(); angle <= request.angleRangeDegrees().max() + 1e-9;
                 angle += request.angleRangeDegrees().step()) {
                if (++guard > 10000) throw new IllegalArgumentException("탐색 후보는 최대 10000개입니다.");
                double factor = placementFactor(distance, angle);
                double power = density * request.collectionAreaM2() * request.efficiencyPercent() / 100.0 * factor;
                candidates.add(mapOf("distanceM", round(distance), "angleDegrees", round(angle),
                        "placementFactor", factor, "predictedPowerW", power,
                        "feasibility", "REQUIRES_SITE_REVIEW"));
            }
        }
        candidates.sort(Comparator.comparingDouble((Map<String, Object> c) -> (double) c.get("predictedPowerW")).reversed());
        int limit = request.limit() == null ? 10 : request.limit();
        var selected = new ArrayList<>(candidates.subList(0, Math.min(limit, candidates.size())));
        for (int i = 0; i < selected.size(); i++) selected.get(i).put("rank", i + 1);
        return mapOf("buildingId", request.buildingId(), "ventId", request.ventId(), "source", request.source(),
                "densityWM2", density, "candidates", selected, "modelVersion", "HEURISTIC-v1",
                "isInstallationRecommendation", false,
                "limitations", List.of("거리·각도 설명용 휴리스틱입니다.", "안전·압력손실·접근성을 현장에서 검토해야 합니다."));
    }

    @Transactional
    public Map<String, Object> createValidation(ModelController.ValidationRequest request) {
        requireBuildingAndVent(request.buildingId(), request.ventId());
        int count = request.samples().size();
        double mae = request.samples().stream().mapToDouble(p -> Math.abs(p.predictedW() - p.actualW())).average().orElse(0);
        double rmse = Math.sqrt(request.samples().stream()
                .mapToDouble(p -> Math.pow(p.predictedW() - p.actualW(), 2)).average().orElse(0));
        double bias = request.samples().stream().mapToDouble(p -> p.predictedW() - p.actualW()).average().orElse(0);
        var nonZero = request.samples().stream().filter(p -> p.actualW() != 0).toList();
        Double mape = nonZero.isEmpty() ? null : nonZero.stream()
                .mapToDouble(p -> Math.abs((p.predictedW() - p.actualW()) / p.actualW()) * 100).average().orElse(0);
        boolean configured = request.thresholds() != null;
        Boolean passed = configured ? passes(request.thresholds(), mae, rmse, mape, bias) : null;
        String id = "VAL-" + UUID.randomUUID();
        jdbc.sql("""
                insert into validations (id, building_id, vent_id, prediction_model_version, sample_count,
                    mae_w, rmse_w, mape_percent, bias_w, status, thresholds_configured, passed)
                values (:id, :buildingId, :ventId, :version, :count, :mae, :rmse, :mape, :bias,
                    'COMPLETED', :configured, :passed)
                """).param("id", id).param("buildingId", request.buildingId()).param("ventId", request.ventId())
                .param("version", request.predictionModelVersion()).param("count", count).param("mae", mae)
                .param("rmse", rmse).param("mape", mape).param("bias", bias)
                .param("configured", configured).param("passed", passed).update();
        return validation(id);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> validations(String buildingId) {
        String sql = "select * from validations" + (buildingId == null ? "" : " where building_id = :buildingId")
                + " order by created_at desc";
        var spec = jdbc.sql(sql);
        if (buildingId != null) spec = spec.param("buildingId", buildingId);
        return spec.query(this::validationMap).list();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> validation(String validationId) {
        return jdbc.sql("select * from validations where id = :id").param("id", validationId)
                .query(this::validationMap).optional()
                .orElseThrow(() -> new NotFoundException("검증 결과를 찾을 수 없습니다: " + validationId));
    }

    private Map<String, Object> turbineMap(ResultSet rs, boolean detailed) throws SQLException {
        return mapOf("id", rs.getString("id"), "manufacturer", rs.getString("manufacturer"),
                "model", rs.getString("model"), "rotorAreaM2", nullableDouble(rs, "rotor_area_m2"),
                "cutInWindSpeedMS", nullableDouble(rs, "cut_in_wind_speed_m_s"),
                "ratedWindSpeedMS", nullableDouble(rs, "rated_wind_speed_m_s"),
                "ratedPowerW", nullableDouble(rs, "rated_power_w"),
                "cutOutWindSpeedMS", nullableDouble(rs, "cut_out_wind_speed_m_s"),
                "generatorEfficiencyPercent", nullableDouble(rs, "generator_efficiency_percent"),
                "curveVersion", rs.getString("curve_version"), "source", rs.getString("source"),
                "validFrom", rs.getObject("valid_from"), "verificationStatus", rs.getString("verification_status"));
    }

    private List<CurvePoint> curve(String turbineId) {
        return jdbc.sql("""
                select wind_speed_m_s, power_w from turbine_power_curve_points
                where turbine_id = :id order by wind_speed_m_s
                """).param("id", turbineId).query((rs, row) ->
                new CurvePoint(rs.getDouble("wind_speed_m_s"), rs.getDouble("power_w"))).list();
    }

    private Map<String, Object> validationMap(ResultSet rs, int row) throws SQLException {
        return mapOf("id", rs.getString("id"), "buildingId", rs.getString("building_id"),
                "ventId", rs.getString("vent_id"), "predictionModelVersion", rs.getString("prediction_model_version"),
                "sampleCount", rs.getInt("sample_count"),
                "metrics", mapOf("maeW", rs.getDouble("mae_w"), "rmseW", rs.getDouble("rmse_w"),
                        "mapePercent", nullableDouble(rs, "mape_percent"), "biasW", rs.getDouble("bias_w")),
                "status", rs.getString("status"),
                "acceptance", mapOf("thresholdsConfigured", rs.getBoolean("thresholds_configured"),
                        "passed", nullableBoolean(rs, "passed")),
                "createdAt", rs.getObject("created_at", OffsetDateTime.class));
    }

    private void requireBuildingAndVent(String buildingId, String ventId) {
        int buildings = jdbc.sql("select count(*) from buildings where id = :id").param("id", buildingId)
                .query(Integer.class).single();
        if (buildings == 0) throw new NotFoundException("건물을 찾을 수 없습니다: " + buildingId);
        int vents = jdbc.sql("select count(*) from vents where id = :id and building_id = :buildingId")
                .param("id", ventId).param("buildingId", buildingId).query(Integer.class).single();
        if (vents == 0) throw new NotFoundException("건물에 속한 배기구를 찾을 수 없습니다: " + ventId);
    }

    private static void validateRange(ModelController.Range range, String name) {
        if (range.min() > range.max()) throw new IllegalArgumentException(name + ".min은 max보다 클 수 없습니다.");
    }

    private static double placementFactor(double distance, double angle) {
        return Math.exp(-0.13 * (distance - 1)) * Math.pow(Math.cos(Math.toRadians(angle)), 2);
    }

    private static double interpolate(List<CurvePoint> curve, double speed) {
        if (speed < curve.get(0).windSpeed() || speed > curve.get(curve.size() - 1).windSpeed()) return 0;
        for (int i = 1; i < curve.size(); i++) {
            var low = curve.get(i - 1);
            var high = curve.get(i);
            if (speed <= high.windSpeed()) {
                double ratio = (speed - low.windSpeed()) / (high.windSpeed() - low.windSpeed());
                return low.power() + ratio * (high.power() - low.power());
            }
        }
        return 0;
    }

    private static boolean passes(ModelController.ValidationThresholds t, double mae, double rmse,
                                  Double mape, double bias) {
        if (t.maxMaeW() != null && mae > t.maxMaeW()) return false;
        if (t.maxRmseW() != null && rmse > t.maxRmseW()) return false;
        if (t.maxMapePercent() != null && (mape == null || mape > t.maxMapePercent())) return false;
        return t.maxAbsoluteBiasW() == null || Math.abs(bias) <= t.maxAbsoluteBiasW();
    }

    private static Double nullableDouble(ResultSet rs, String name) throws SQLException {
        double value = rs.getDouble(name);
        return rs.wasNull() ? null : value;
    }

    private static Boolean nullableBoolean(ResultSet rs, String name) throws SQLException {
        boolean value = rs.getBoolean(name);
        return rs.wasNull() ? null : value;
    }

    private static double round(double value) { return Math.round(value * 10000.0) / 10000.0; }

    private static Map<String, Object> mapOf(Object... values) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < values.length; i += 2) map.put((String) values[i], values[i + 1]);
        return map;
    }

    private record CurvePoint(double windSpeed, double power) {}
    private record WindPoint(OffsetDateTime measuredAt, double windSpeed, String quality) {}
}
