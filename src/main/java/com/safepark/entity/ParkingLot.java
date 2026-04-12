package com.safepark.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "parking_lot")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class ParkingLot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "lot_name", nullable = false, length = 100)
    private String lotName;  // 변경: name → lotName

    @Column(length = 255)
    private String address;

    @Column(nullable = false)
    private Float lat;  // 변경: latitude → lat

    @Column(nullable = false)
    private Float lng;  // 변경: longitude → lng

    @Column(name = "lot_type", length = 20)
    private String lotType;  // 변경: type → lotType (공영/민영/노상)

    @Column(name = "lot_price")
    private Integer lotPrice;  // 변경: basicFee → lotPrice (10분당 요금)

    @Column(name = "free_yn", columnDefinition = "INTEGER DEFAULT 0")
    private Integer freeYn;  // 추가: 0=유료, 1=무료

    @Column(name = "operating_hours", length = 50)
    private String operatingHours;  // 변경: operatingHours → operating_hours

    @Column(name = "total_spaces")
    private Integer totalSpaces;  // 변경: capacity → totalSpaces

    @Column(name = "available_spaces")
    private Integer availableSpaces;  // 추가: 현재 가능 면수

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}