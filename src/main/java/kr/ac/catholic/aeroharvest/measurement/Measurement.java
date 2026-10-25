package kr.ac.catholic.aeroharvest.measurement;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import kr.ac.catholic.aeroharvest.building.Building;

import java.time.OffsetDateTime;

@Entity
@Table(name = "measurements")
public class Measurement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "building_id")
    private Building building;
    @Column(name = "vent_id")
    private String ventId;
    @Column(name = "measured_at")
    private OffsetDateTime measuredAt;
    private String timezone;
    @Column(name = "wind_speed_m_s")
    private double windSpeedMS;
    @Column(name = "vent_area_m2")
    private double ventAreaM2;
    @Column(name = "sensor_id")
    private String sensorId;
    @Enumerated(EnumType.STRING)
    @Column(name = "quality_flag")
    private QualityFlag qualityFlag;
    @Enumerated(EnumType.STRING)
    @Column(name = "operating_state")
    private OperatingState operatingState;
    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    protected Measurement() {}

    public Measurement(Building building, String ventId, OffsetDateTime measuredAt, String timezone,
                       double windSpeedMS, double ventAreaM2, String sensorId,
                       QualityFlag qualityFlag, OperatingState operatingState) {
        this.building = building;
        this.ventId = ventId;
        this.measuredAt = measuredAt;
        this.timezone = timezone;
        this.windSpeedMS = windSpeedMS;
        this.ventAreaM2 = ventAreaM2;
        this.sensorId = sensorId;
        this.qualityFlag = qualityFlag;
        this.operatingState = operatingState;
    }

    @PrePersist
    void setCreatedAt() { createdAt = OffsetDateTime.now(); }

    public Long getId() { return id; }
    public Building getBuilding() { return building; }
    public String getVentId() { return ventId; }
    public OffsetDateTime getMeasuredAt() { return measuredAt; }
    public String getTimezone() { return timezone; }
    public double getWindSpeedMS() { return windSpeedMS; }
    public double getVentAreaM2() { return ventAreaM2; }
    public String getSensorId() { return sensorId; }
    public QualityFlag getQualityFlag() { return qualityFlag; }
    public OperatingState getOperatingState() { return operatingState; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}

