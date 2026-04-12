package com.safepark.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;  // 이걸로 변경!
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "parking_lot")
@Data  // @Getter + @Setter + @ToString 등 모두 포함!
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class ParkingLot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    @Column(length = 200)
    private String address;

    @Column(length = 20)
    private String type;

    private Integer capacity;

    @Column(length = 50)
    private String operatingHours;

    private Integer basicFee;

    private Integer additionalFee;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;
}