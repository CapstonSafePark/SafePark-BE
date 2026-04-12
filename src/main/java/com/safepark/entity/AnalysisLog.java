package com.safepark.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
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
@Builder
@EntityListeners(AuditingEntityListener.class)
public class AnalysisLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;  // 사용자

    @Column(nullable = false)
    private Double latitude;  // 분석 위치 위도

    @Column(nullable = false)
    private Double longitude;  // 분석 위치 경도

    @Column(length = 50)
    private String lineColor;  // 주차선 색상 (황색이중선, 백색점선 등)

    private Integer probability;  // 과태료 확률 (0~100)

    @Column(length = 20)
    private String riskLevel;  // 위험도 (HIGH, MEDIUM, LOW)

    @Column(columnDefinition = "TEXT")
    private String reasoning;  // LLM 생성 설명

    @Column(length = 500)
    private String imagePath;  // 이미지 저장 경로

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;
}