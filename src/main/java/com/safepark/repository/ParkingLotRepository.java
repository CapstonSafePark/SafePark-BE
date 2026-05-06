package com.safepark.repository;

import com.safepark.entity.ParkingLot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ParkingLotRepository extends JpaRepository<ParkingLot, Long> {

    // 주차장 이름으로 검색
    List<ParkingLot> findByLotNameContaining(String lotName);

    // 반경 내 주차장 조회 (Haversine 공식, 단위: km)
    @Query(value = """
            SELECT * FROM parking_lot
            WHERE (6371 * acos(
                cos(radians(:lat)) * cos(radians(lat)) *
                cos(radians(lng) - radians(:lng)) +
                sin(radians(:lat)) * sin(radians(lat))
            )) < :radiusKm
            ORDER BY (6371 * acos(
                cos(radians(:lat)) * cos(radians(lat)) *
                cos(radians(lng) - radians(:lng)) +
                sin(radians(:lat)) * sin(radians(lat))
            ))
            """, nativeQuery = true)
    List<ParkingLot> findNearbyLots(
            @Param("lat") float lat,
            @Param("lng") float lng,
            @Param("radiusKm") double radiusKm
    );
}