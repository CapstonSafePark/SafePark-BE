package com.safepark.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "analysis_log")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class AnalysisLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;

    // 위치 정보
    @Column(name = "req_lat")
    private Float reqLat;

    @Column(name = "req_lng")
    private Float reqLng;

    @Column(length = 255)
    private String address;

    // 단속구역 정보 (인라인 저장)
    @Column(name = "zone_id")
    private Long zoneId;

    @Column(name = "zone_name", length = 100)
    private String zoneName;

    @Column(name = "zone_type", length = 50)
    private String zoneType;

    // 추천 주차장 정보
    @Column(name = "lot_id")
    private Long lotId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lot_id", insertable = false, updatable = false)
    private ParkingLot lot;

    // 분석 결과
    @Column(name = "image_path", length = 255)
    private String imagePath;

    @Column(name = "line_color", length = 50)
    private String lineColor;  // 황색이중선/황색단선/백색점선/없음

    @Column(name = "probability")
    private Integer probability;  // 0~100 과태료 확률

    @Column(columnDefinition = "TEXT")
    private String result;

    @Column(name = "risk_score")
    private Integer riskScore;

    @Column(name = "risk_level", length = 10)
    private String riskLevel;  // HIGH, MEDIUM, LOW

    @Column(columnDefinition = "TEXT")
    private String reasoning;  // AI 판단 근거

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
