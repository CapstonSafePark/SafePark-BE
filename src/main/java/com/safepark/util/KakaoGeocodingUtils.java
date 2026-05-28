package com.safepark.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 카카오 역지오코딩 유틸
 * GET https://dapi.kakao.com/v2/local/geo/coord2address.json?x={lng}&y={lat}
 */
@Slf4j
public class KakaoGeocodingUtils {

    private static final String COORD2ADDRESS_URL =
            "https://dapi.kakao.com/v2/local/geo/coord2address.json";

    // 좌표 → 주소 캐시 (같은 좌표 중복 호출 방지)
    private static final Map<String, String> addressCache = new ConcurrentHashMap<>();

    private static final RestTemplate restTemplate = new RestTemplate();

    /**
     * 위경도 → 도로명/지번 주소 변환
     * @return 도로명 주소 우선, 없으면 지번 주소, 실패 시 null
     */
    public static String getAddress(double lat, double lng, String apiKey) {
        String cacheKey = String.format("%.6f,%.6f", lat, lng);
        if (addressCache.containsKey(cacheKey)) {
            return addressCache.get(cacheKey);
        }

        try {
            String url = COORD2ADDRESS_URL + "?x=" + lng + "&y=" + lat;

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "KakaoAK " + apiKey);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
            if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null) return null;

            String address = parseAddress(response.getBody());
            if (address != null) {
                addressCache.put(cacheKey, address);
            }
            return address;

        } catch (Exception e) {
            log.debug("역지오코딩 실패 ({}, {}): {}", lat, lng, e.getMessage());
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private static String parseAddress(Map<String, Object> body) {
        try {
            Object documentsObj = body.get("documents");
            if (!(documentsObj instanceof java.util.List)) return null;
            java.util.List<Map<String, Object>> documents = (java.util.List<Map<String, Object>>) documentsObj;
            if (documents.isEmpty()) return null;

            Map<String, Object> doc = documents.get(0);

            // 도로명 주소 우선
            Map<String, Object> roadAddress = (Map<String, Object>) doc.get("road_address");
            if (roadAddress != null) {
                String addr = (String) roadAddress.get("address_name");
                if (addr != null && !addr.isBlank()) return addr;
            }

            // 없으면 지번 주소
            Map<String, Object> address = (Map<String, Object>) doc.get("address");
            if (address != null) {
                String addr = (String) address.get("address_name");
                if (addr != null && !addr.isBlank()) return addr;
            }

        } catch (Exception e) {
            log.debug("주소 파싱 실패: {}", e.getMessage());
        }
        return null;
    }
}
