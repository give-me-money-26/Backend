package kr.ac.catholic.aeroharvest.decision;

import kr.ac.catholic.aeroharvest.common.NotFoundException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class DecisionService {
    private final JdbcClient jdbc;

    public DecisionService(JdbcClient jdbc) { this.jdbc = jdbc; }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> candidates(String buildingId) {
        String sql = """
                select v.id as vent_id, v.building_id, b.name as building_name, v.name as vent_name,
                       v.status as vent_status, v.source_type,
                       (select count(*) from measurements m where m.vent_id = v.id) as measurement_count,
                       (select avg(m.wind_speed_m_s) from measurements m
                          where m.vent_id = v.id and m.quality_flag = 'VALID') as average_wind_speed,
                       (select max(h.density_w_m2) from hourly_wind_densities h
                          where h.building_id = v.building_id) as fallback_peak_density
                from vents v join buildings b on b.id = v.building_id
                """ + (buildingId == null ? "" : " where v.building_id = :buildingId")
                + " order by b.priority_rank, v.id";
        var spec = jdbc.sql(sql);
        if (buildingId != null) spec = spec.param("buildingId", buildingId);
        return spec.query(this::candidateRow).list();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> candidate(String candidateId) {
        String[] ids = splitCandidateId(candidateId);
        var candidate = jdbc.sql("""
                select v.id as vent_id, v.building_id, b.name as building_name, v.name as vent_name,
                       v.status as vent_status, v.source_type,
                       (select count(*) from measurements m where m.vent_id = v.id) as measurement_count,
                       (select avg(m.wind_speed_m_s) from measurements m
                          where m.vent_id = v.id and m.quality_flag = 'VALID') as average_wind_speed,
                       (select max(h.density_w_m2) from hourly_wind_densities h
                          where h.building_id = v.building_id) as fallback_peak_density
                from vents v join buildings b on b.id = v.building_id
                where v.building_id = :buildingId and v.id = :ventId
                """).param("buildingId", ids[0]).param("ventId", ids[1]).query(this::candidateRow).optional()
                .orElseThrow(() -> new NotFoundException("후보를 찾을 수 없습니다: " + candidateId));
        var validation = jdbc.sql("""
                select id, status, mae_w, rmse_w, mape_percent, bias_w, passed, created_at
                from validations where building_id = :buildingId and vent_id = :ventId
                order by created_at desc limit 1
                """).param("buildingId", ids[0]).param("ventId", ids[1])
                .query((rs, row) -> mapOf("id", rs.getString("id"), "status", rs.getString("status"),
                        "maeW", rs.getDouble("mae_w"), "rmseW", rs.getDouble("rmse_w"),
                        "mapePercent", nullableDouble(rs, "mape_percent"), "biasW", rs.getDouble("bias_w"),
                        "passed", nullableBoolean(rs, "passed"),
                        "createdAt", rs.getObject("created_at", OffsetDateTime.class))).optional().orElse(null);
        candidate.put("latestValidation", validation);
        candidate.put("latestDecision", latestDecision(candidateId));
        candidate.put("missingEvidence", missingEvidence(candidate));
        return candidate;
    }

    @Transactional
    public Map<String, Object> create(DecisionController.DecisionCreateRequest request) {
        var evidence = candidate(request.candidateId());
        String id = "DEC-" + UUID.randomUUID();
        jdbc.sql("""
                insert into decisions (id, candidate_id, status, reason, assignee, due_at, evidence_snapshot)
                values (:id, :candidateId, :status, :reason, :assignee, :dueAt, :evidence)
                """).param("id", id).param("candidateId", request.candidateId())
                .param("status", request.status().name()).param("reason", request.reason())
                .param("assignee", request.assignee()).param("dueAt", request.dueAt())
                .param("evidence", evidence.toString()).update();
        return decision(id);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> decisions(String candidateId, DecisionController.DecisionStatus status) {
        StringBuilder sql = new StringBuilder("select * from decisions where 1=1");
        Map<String, Object> params = new LinkedHashMap<>();
        if (candidateId != null) { sql.append(" and candidate_id = :candidateId"); params.put("candidateId", candidateId); }
        if (status != null) { sql.append(" and status = :status"); params.put("status", status.name()); }
        sql.append(" order by updated_at desc");
        return jdbc.sql(sql.toString()).params(params).query(this::decisionRow).list();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> decision(String decisionId) {
        var result = jdbc.sql("select * from decisions where id = :id").param("id", decisionId)
                .query(this::decisionRow).optional()
                .orElseThrow(() -> new NotFoundException("판정을 찾을 수 없습니다: " + decisionId));
        result.put("history", jdbc.sql("""
                select previous_status, new_status, reason, actor, changed_at
                from decision_history where decision_id = :id order by changed_at desc, id desc
                """).param("id", decisionId).query((rs, row) -> mapOf(
                        "previousStatus", rs.getString("previous_status"),
                        "newStatus", rs.getString("new_status"), "reason", rs.getString("reason"),
                        "actor", rs.getString("actor"),
                        "changedAt", rs.getObject("changed_at", OffsetDateTime.class))).list());
        return result;
    }

    @Transactional
    public Map<String, Object> update(String decisionId, DecisionController.DecisionUpdateRequest request) {
        var current = decision(decisionId);
        String previousStatus = (String) current.get("status");
        String nextStatus = request.status() == null ? previousStatus : request.status().name();
        String reason = request.reason() == null || request.reason().isBlank()
                ? (String) current.get("reason") : request.reason();
        String assignee = request.assignee() == null ? (String) current.get("assignee") : request.assignee();
        OffsetDateTime dueAt = request.dueAt() == null ? (OffsetDateTime) current.get("dueAt") : request.dueAt();
        jdbc.sql("""
                update decisions set status = :status, reason = :reason, assignee = :assignee,
                    due_at = :dueAt, updated_at = current_timestamp where id = :id
                """).param("status", nextStatus).param("reason", reason).param("assignee", assignee)
                .param("dueAt", dueAt).param("id", decisionId).update();
        jdbc.sql("""
                insert into decision_history (decision_id, previous_status, new_status, reason, actor)
                values (:id, :previous, :next, :reason, :actor)
                """).param("id", decisionId).param("previous", previousStatus).param("next", nextStatus)
                .param("reason", reason).param("actor", request.actor()).update();
        return decision(decisionId);
    }

    private Map<String, Object> candidateRow(ResultSet rs, int row) throws SQLException {
        long count = rs.getLong("measurement_count");
        Double average = nullableDouble(rs, "average_wind_speed");
        double peak = rs.getDouble("fallback_peak_density");
        String recommendation = "ESTIMATED".equals(rs.getString("source_type")) || count < 3 ? "MEASURE" : "REVIEW";
        String buildingId = rs.getString("building_id");
        String ventId = rs.getString("vent_id");
        return mapOf("id", buildingId + ":" + ventId, "buildingId", buildingId,
                "buildingName", rs.getString("building_name"), "ventId", ventId,
                "ventName", rs.getString("vent_name"), "ventStatus", rs.getString("vent_status"),
                "sourceType", rs.getString("source_type"), "measurementCount", count,
                "averageWindSpeedMS", average, "fallbackPeakDensityWM2", peak,
                "recommendation", recommendation,
                "isInstallationRecommendation", false);
    }

    private Map<String, Object> decisionRow(ResultSet rs, int row) throws SQLException {
        return mapOf("id", rs.getString("id"), "candidateId", rs.getString("candidate_id"),
                "status", rs.getString("status"), "reason", rs.getString("reason"),
                "assignee", rs.getString("assignee"), "dueAt", rs.getObject("due_at", OffsetDateTime.class),
                "evidenceSnapshot", rs.getString("evidence_snapshot"),
                "createdAt", rs.getObject("created_at", OffsetDateTime.class),
                "updatedAt", rs.getObject("updated_at", OffsetDateTime.class));
    }

    private Map<String, Object> latestDecision(String candidateId) {
        return jdbc.sql("select * from decisions where candidate_id = :id order by updated_at desc limit 1")
                .param("id", candidateId).query(this::decisionRow).optional().orElse(null);
    }

    private static List<String> missingEvidence(Map<String, Object> candidate) {
        List<String> missing = new ArrayList<>();
        if ("ESTIMATED".equals(candidate.get("sourceType"))) missing.add("배기구 실측 좌표·치수");
        if ((long) candidate.get("measurementCount") < 3) missing.add("반복 측정 3회 이상");
        if (candidate.get("averageWindSpeedMS") == null) missing.add("VALID 풍속 측정");
        missing.add("HVAC 운전상태·압력손실·소음 검증");
        return missing;
    }

    private static String[] splitCandidateId(String candidateId) {
        int split = candidateId.indexOf(':');
        if (split <= 0 || split == candidateId.length() - 1) {
            throw new IllegalArgumentException("candidateId 형식은 buildingId:ventId입니다.");
        }
        return new String[]{candidateId.substring(0, split), candidateId.substring(split + 1)};
    }

    private static Double nullableDouble(ResultSet rs, String name) throws SQLException {
        double value = rs.getDouble(name);
        return rs.wasNull() ? null : value;
    }

    private static Boolean nullableBoolean(ResultSet rs, String name) throws SQLException {
        boolean value = rs.getBoolean(name);
        return rs.wasNull() ? null : value;
    }

    private static Map<String, Object> mapOf(Object... values) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < values.length; i += 2) map.put((String) values[i], values[i + 1]);
        return map;
    }
}
