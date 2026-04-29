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
    private Long userId;  // 필수! 사용자 ID

    @Column(name = "image_path", length = 255)
    private String imagePath;

    @Column(columnDefinition = "TEXT")
    private String result;

    @Column(name = "risk_score")
    private Integer riskScore;

    @Column(name = "risk_level", length = 10)
    private String riskLevel;  // HIGH, MEDIUM, LOW

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}