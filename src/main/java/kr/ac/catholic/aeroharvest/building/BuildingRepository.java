package kr.ac.catholic.aeroharvest.building;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BuildingRepository extends JpaRepository<Building, String> {
    List<Building> findAllByOrderByPriorityRankAsc();
}

