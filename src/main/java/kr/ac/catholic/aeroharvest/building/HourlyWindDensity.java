package kr.ac.catholic.aeroharvest.building;

import jakarta.persistence.Entity;
import jakarta.persistence.Column;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "hourly_wind_densities")
public class HourlyWindDensity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "building_id")
    private Building building;
    @Column(name = "hour_of_day")
    private int hourOfDay;
    @Column(name = "density_w_m2")
    private double densityWM2;
    @Column(name = "source_type")
    private String sourceType;
    @Column(name = "density_unit")
    private String densityUnit;
    @Column(name = "source_title")
    private String sourceTitle;
    @Column(name = "source_document")
    private String sourceDocument;
    @Column(name = "data_status")
    private String dataStatus;
    @Column(name = "derivation_method")
    private String derivationMethod;

    protected HourlyWindDensity() {}

    public int getHourOfDay() { return hourOfDay; }
    public double getDensityWM2() { return densityWM2; }
    public String getSourceType() { return sourceType; }
    public String getDensityUnit() { return densityUnit; }
    public String getSourceTitle() { return sourceTitle; }
    public String getSourceDocument() { return sourceDocument; }
    public String getDataStatus() { return dataStatus; }
    public String getDerivationMethod() { return derivationMethod; }
}

