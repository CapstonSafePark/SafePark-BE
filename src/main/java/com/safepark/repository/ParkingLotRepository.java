package com.safepark.repository;

import com.safepark.entity.ParkingLot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ParkingLotRepository extends JpaRepository<ParkingLot, Long> {

    // 주차장 이름으로 검색
    List<ParkingLot> findByLotNameContaining(String lotName);  // 변경: name → lotName
}