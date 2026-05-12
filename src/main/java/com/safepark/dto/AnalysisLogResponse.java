package com.safepark.dto;

import com.safepark.entity.AnalysisLog;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AnalysisLogResponse {

    private Long id;
    private Long userId;
    private String username;  // 사용자 이름

    // 위치 정보
    private Float reqLat;
    private Float reqLng;
    private String address;

    // 단속구역 정보
    private Long zoneId;
    private String zoneName;
    private String zoneType;

    // 주차장 정보 (추천)
    private Long lotId;
    private String lotName;
    private String lotAddress;

    // 분석 결과
    private String imagePath;
    private String lineColor;  // 황색이중선/황색단선/백색점선/없음
    private Integer probability;  // 0~100 과태료 확률
    private String riskLevel;  // LOW/MEDIUM/HIGH
    private String reasoning;  // AI 판단 근거

    private LocalDateTime createdAt;

    // Entity -> DTO 변환 (상세 정보 포함)
    public static AnalysisLogResponse fromEntity(AnalysisLog log) {
        AnalysisLogResponse response = new AnalysisLogResponse();
        response.setId(log.getId());

        // 사용자 정보
        if (log.getUser() != null) {
            response.setUserId(log.getUser().getId());
            response.setUsername(log.getUser().getUsername());
        }

        // 위치 정보
        response.setReqLat(log.getReqLat());
        response.setReqLng(log.getReqLng());
        response.setAddress(log.getAddress());

        // 단속구역 정보
        if (log.getZoneId() != null) {
            response.setZoneId(log.getZoneId());
            response.setZoneName(log.getZoneName());
            response.setZoneType(log.getZoneType());
        }

        // 주차장 정보
        if (log.getLot() != null) {
            response.setLotId(log.getLot().getId());
            response.setLotName(log.getLot().getLotName());
            response.setLotAddress(log.getLot().getAddress());
        }

        // 분석 결과
        response.setImagePath(log.getImagePath());
        response.setLineColor(log.getLineColor());
        response.setProbability(log.getProbability());
        response.setRiskLevel(log.getRiskLevel());
        response.setReasoning(log.getReasoning());
        response.setCreatedAt(log.getCreatedAt());

        return response;
    }

    // Entity -> DTO 변환 (목록용 - 간단한 정보만)
    public static AnalysisLogResponse fromEntitySimple(AnalysisLog log) {
        AnalysisLogResponse response = new AnalysisLogResponse();
        response.setId(log.getId());

        if (log.getUser() != null) {
            response.setUserId(log.getUser().getId());
            response.setUsername(log.getUser().getUsername());
        }

        response.setReqLat(log.getReqLat());
        response.setReqLng(log.getReqLng());
        response.setAddress(log.getAddress());
        response.setImagePath(log.getImagePath());
        response.setProbability(log.getProbability());
        response.setRiskLevel(log.getRiskLevel());
        response.setReasoning(log.getReasoning());
        response.setLineColor(log.getLineColor());
        response.setCreatedAt(log.getCreatedAt());

        return response;
    }
}