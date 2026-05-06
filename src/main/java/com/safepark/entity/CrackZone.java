package com.safepark.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "crack_zone")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CrackZone {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "zone_name", nullable = false, length = 100)
    private String zoneName;

    @Column(nullable = false)
    private Float lat;

    @Column(nullable = false)
    private Float lng;

    @Column(name = "zone_type", nullable = false, length = 50)
    private String zoneType;  // 버스정류장/스쿨존/횡단보도/일반

    @Column(name = "start_time", length = 10)
    private String startTime;  // 단속시작 (예: 07:00)

    @Column(name = "end_time", length = 10)
    private String endTime;    // 단속종료 (예: 22:00)
}
