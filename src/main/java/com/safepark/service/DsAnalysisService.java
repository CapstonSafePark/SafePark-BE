package com.safepark.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class DsAnalysisService {

    @Value("${ds.api.url:http://localhost:5000}")
    private String dsApiUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * DS 모델에 이미지를 보내서 차선 분석 결과를 받아옴
     * @param imageFile 분석할 이미지 파일
     * @return 분석 결과 (lineColor, riskLevel, probability, reasoning)
     */
    public Map<String, Object> analyzeImage(File imageFile) {
        Map<String, Object> result = new HashMap<>();

        try {
            // multipart/form-data로 DS API 호출
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("image", new FileSystemResource(imageFile));

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    dsApiUrl + "/ds/line-detect",
                    HttpMethod.POST,
                    requestEntity,
                    Map.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> dsResult = response.getBody();
                String lineType = (String) dsResult.get("line_type");

                // DS line_type → 한글 lineColor 변환
                String lineColor = convertLineType(lineType);

                // lineColor 기반으로 위험도와 확률 계산
                String riskLevel = calculateRiskLevel(lineType);
                int probability = calculateProbability(lineType);
                String reasoning = generateReasoning(lineType, dsResult);

                result.put("lineColor", lineColor);
                result.put("riskLevel", riskLevel);
                result.put("probability", probability);
                result.put("reasoning", reasoning);
                result.put("success", true);

                log.info("DS 분석 완료: lineType={}, lineColor={}, riskLevel={}, probability={}",
                        lineType, lineColor, riskLevel, probability);
            } else {
                log.error("DS API 응답 오류: status={}", response.getStatusCode());
                result.putAll(getDefaultResult("DS 서버 응답 오류"));
            }

        } catch (Exception e) {
            log.error("DS API 호출 실패: {}", e.getMessage());
            result.putAll(getDefaultResult("DS 서버 연결 실패: " + e.getMessage()));
        }

        return result;
    }

    /**
     * DS line_type → 한글 차선 색상 변환
     */
    private String convertLineType(String lineType) {
        if (lineType == null) return "없음";
        return switch (lineType) {
            case "yellow_double" -> "황색이중선";
            case "yellow_single" -> "황색단선";
            case "white_dotted" -> "백색점선";
            case "none" -> "없음";
            default -> "없음";
        };
    }

    /**
     * 차선 종류에 따른 위험도 계산
     * - 황색이중선: 절대 주차 금지 → HIGH
     * - 황색단선: 주차 금지 → HIGH
     * - 백색점선: 주차 가능 → LOW
     * - 없음: 판단 불가 → MEDIUM
     */
    private String calculateRiskLevel(String lineType) {
        if (lineType == null) return "MEDIUM";
        return switch (lineType) {
            case "yellow_double" -> "HIGH";
            case "yellow_single" -> "HIGH";
            case "white_dotted" -> "LOW";
            case "none" -> "MEDIUM";
            default -> "MEDIUM";
        };
    }

    /**
     * 차선 종류에 따른 과태료 확률 계산
     */
    private int calculateProbability(String lineType) {
        if (lineType == null) return 50;
        return switch (lineType) {
            case "yellow_double" -> 95;
            case "yellow_single" -> 85;
            case "white_dotted" -> 10;
            case "none" -> 50;
            default -> 50;
        };
    }

    /**
     * 분석 결과에 대한 판단 근거 생성
     */
    private String generateReasoning(String lineType, Map<String, Object> dsResult) {
        if (lineType == null) return "차선 분석에 실패했습니다.";

        String lineColor = convertLineType(lineType);
        int yellowCount = dsResult.get("valid_yellow_count") != null ?
                ((Number) dsResult.get("valid_yellow_count")).intValue() : 0;
        int whiteCount = dsResult.get("white_count") != null ?
                ((Number) dsResult.get("white_count")).intValue() : 0;

        return switch (lineType) {
            case "yellow_double" -> String.format(
                    "AI 이미지 분석 결과, 황색이중선(%d개)이 감지되었습니다. " +
                    "황색이중선 구역은 주정차 절대 금지 구역으로, 과태료 부과 확률이 매우 높습니다. " +
                    "즉시 차량을 이동하시기 바랍니다.", yellowCount);
            case "yellow_single" -> String.format(
                    "AI 이미지 분석 결과, 황색단선(%d개)이 감지되었습니다. " +
                    "황색단선 구역은 주차 금지 구역으로, 과태료 부과 확률이 높습니다. " +
                    "5분 이내 탄력 정차만 가능합니다.", yellowCount);
            case "white_dotted" -> String.format(
                    "AI 이미지 분석 결과, 백색점선(%d개)이 감지되었습니다. " +
                    "백색점선 구역은 주차가 허용되는 구역입니다. " +
                    "안전하게 주차하셔도 됩니다.", whiteCount);
            case "none" -> "AI 이미지 분석 결과, 뚜렷한 차선이 감지되지 않았습니다. " +
                    "주변 표지판이나 노면 표시를 직접 확인하시기 바랍니다.";
            default -> "차선 분석 결과를 판단할 수 없습니다.";
        };
    }

    /**
     * DS 서버 연결 실패 시 기본값 반환
     */
    private Map<String, Object> getDefaultResult(String errorMsg) {
        Map<String, Object> result = new HashMap<>();
        result.put("lineColor", "없음");
        result.put("riskLevel", "MEDIUM");
        result.put("probability", 50);
        result.put("reasoning", "DS 분석 서버에 연결할 수 없어 임시 결과를 표시합니다. " + errorMsg);
        result.put("success", false);
        return result;
    }
}
