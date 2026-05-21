package com.safepark.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.safepark.dto.NearbyParkingLotResponse;
import com.safepark.util.DistanceUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class SeoulParkingApiService {

    private static final String SEARCH_URL = "https://parking.seoul.go.kr/SearchParking.do";

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 서울시 parking.seoul.go.kr SearchParking.do API로 반경 내 실시간 주차장 조회
     * que_status == "1" (실시간 연계)인 주차장만 반환
     *
     * @param lat      사용자 위도
     * @param lng      사용자 경도
     * @param radiusKm 검색 반경 (km)
     */
    public List<NearbyParkingLotResponse> getNearbyLots(double lat, double lng, double radiusKm) {
        List<NearbyParkingLotResponse> result = new ArrayList<>();
        try {
            int rangeMeters = (int) (radiusKm * 1000);

            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("LAT", String.valueOf(lat));
            params.add("LON", String.valueOf(lng));
            params.add("index", "1");
            params.add("range", String.valueOf(rangeMeters));
            params.add("Type", "63");
            params.add("Rule", "0");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headers.set("Referer", "https://parking.seoul.go.kr/");
            headers.set("Origin", "https://parking.seoul.go.kr");

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);
            ResponseEntity<String> response = restTemplate.exchange(
                    SEARCH_URL, HttpMethod.POST, request, String.class);

            String body = response.getBody();
            if (body == null || body.isBlank()) {
                log.warn("서울시 SearchParking.do 응답이 비어있습니다.");
                return result;
            }

            // 디버그: 원본 응답 앞 500자 로깅
            log.debug("서울시 raw 응답: {}", body.length() > 500 ? body.substring(0, 500) : body);

            JsonNode root = objectMapper.readTree(body);

            // 응답 구조: { "res_value": { "parking_list": [...] }, "result_state": "0000" }
            JsonNode items = root.path("res_value").path("parking_list");
            if (items.isMissingNode() || !items.isArray()) {
                log.warn("서울시 API 응답에서 parking_list를 찾을 수 없습니다. result_state={}", root.path("result_state").asText());
                return result;
            }

            for (JsonNode item : items) {
                // que_status == "1" : 실시간 연계 주차장만 반환
                String queStatus = item.path("que_status").asText("");
                if (!"1".equals(queStatus)) continue;

                // 좌표 추출 (position_list[0].lat, .lng)
                JsonNode positionList = item.path("position_list");
                if (!positionList.isArray() || positionList.isEmpty()) continue;
                JsonNode pos = positionList.get(0);

                double lotLat, lotLng;
                try {
                    lotLat = Double.parseDouble(pos.path("lat").asText("0"));
                    lotLng = Double.parseDouble(pos.path("lng").asText("0"));
                } catch (NumberFormatException e) {
                    continue;
                }
                if (lotLat == 0.0 || lotLng == 0.0) continue;

                double distance = DistanceUtils.calculateDistanceKm(lat, lng, lotLat, lotLng);
                if (distance > radiusKm) continue;

                String parkingName = item.path("parking_name").asText(null);
                String address = item.path("new_juso").asText(null);
                Integer totalSpaces = parseIntSafe(item.path("capacity").asText(null));
                Integer availableSpots = parseIntSafe(item.path("cur_parking").asText(null));
                String payYn = item.path("pay_yn").asText("Y");
                boolean freeYn = "N".equalsIgnoreCase(payYn);
                Integer lotPrice = parseIntSafe(item.path("rates").asText(null));

                result.add(NearbyParkingLotResponse.builder()
                        .id(null)
                        .lotName(parkingName)
                        .address(address)
                        .lat(lotLat)
                        .lng(lotLng)
                        .lotPrice(lotPrice)
                        .freeYn(freeYn)
                        .operatingHours(null)
                        .parkingFeeDesc(null)
                        .totalSpaces(totalSpaces)
                        .availableSpots(availableSpots)
                        .distanceKm(Math.round(distance * 1000.0) / 1000.0)
                        .source("서울시")
                        .build());
            }

            log.info("서울시 SearchParking.do: 반경 {}km 내 실시간 주차장 {}개 조회됨", radiusKm, result.size());

        } catch (Exception e) {
            log.error("서울시 주차장 API 호출 실패: {}", e.getMessage());
        }
        return result;
    }

    private Integer parseIntSafe(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
