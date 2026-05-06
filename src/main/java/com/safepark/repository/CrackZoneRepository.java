package com.safepark.repository;

import com.safepark.entity.CrackZone;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CrackZoneRepository extends JpaRepository<CrackZone, Long> {

    // 반경 내 단속구역 조회 (Haversine 공식 사용, 단위: km)
    @Query(value = """
            SELECT * FROM crack_zone
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
    List<CrackZone> findNearbyZones(
            @Param("lat") float lat,
            @Param("lng") float lng,
            @Param("radiusKm") double radiusKm
    );
}
