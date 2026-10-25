package kr.ac.catholic.aeroharvest.building;

import jakarta.persistence.Entity;
import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "buildings")
public class Building {
    @Id
    private String id;
    private String name;
    private String code;
    @Column(name = "map_x")
    private double mapX;
    @Column(name = "map_y")
    private double mapY;
    @Column(name = "priority_rank")
    private int priorityRank;
    @Column(name = "model_type")
    private String modelType;
    private String advice;
    @Column(name = "data_status")
    private String dataStatus;

    protected Building() {}

    public String getId() { return id; }
    public String getName() { return name; }
    public String getCode() { return code; }
    public double getMapX() { return mapX; }
    public double getMapY() { return mapY; }
    public int getPriorityRank() { return priorityRank; }
    public String getModelType() { return modelType; }
    public String getAdvice() { return advice; }
    public String getDataStatus() { return dataStatus; }
}

