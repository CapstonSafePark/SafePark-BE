package com.safepark.repository;

import com.safepark.entity.ParkingLot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ParkingLotRepository extends JpaRepository<ParkingLot, Long> {

    // 주차장명으로 검색
    List<ParkingLot> findByNameContaining(String name);

    // 위치 기반 검색 (위도, 경도 범위)
    List<ParkingLot> findByLatitudeBetweenAndLongitudeBetween(
            Double minLat, Double maxLat,
            Double minLng, Double maxLng
    );

}