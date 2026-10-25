package kr.ac.catholic.aeroharvest.measurement;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;

public interface MeasurementRepository extends JpaRepository<Measurement, Long> {
    @Query("""
            select m from Measurement m
            where (:buildingId is null or m.building.id = :buildingId)
              and (:fromTime is null or m.measuredAt >= :fromTime)
              and (:toTime is null or m.measuredAt <= :toTime)
            order by m.measuredAt desc
            """)
    Page<Measurement> search(@Param("buildingId") String buildingId,
                             @Param("fromTime") OffsetDateTime fromTime,
                             @Param("toTime") OffsetDateTime toTime,
                             Pageable pageable);
}

