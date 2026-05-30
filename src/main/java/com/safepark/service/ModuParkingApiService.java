package com.safepark.service;

import com.safepark.dto.ModuTicketDto;
import com.safepark.dto.NearbyParkingLotResponse;
import com.safepark.util.DistanceUtils;
import com.safepark.util.GeohashUtils;
import com.safepark.util.KakaoGeocodingUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.*;

/**
 * 모두의주차장 API 연동 서비스
 * GET https://api.modu.cloud/poi/pins?geohash={6자리geohash}&precision=6
 *
 * 응답 구조:
 * {
 *   "data": [
 *     {
 *       "parkinglots": [ { parkinglotSeq, name, latitude, longitude, isFree, qty, calcPrice, ... } ],
 *       "geohash": "..."
 *     }
 *   ]
 * }
 */
@Slf4j
@Service
public class ModuParkingApiService {

    private static final String MODU_API_BASE = "https://api.modu.cloud";
    private static final String MODU_PINS_URL = MODU_API_BASE + "/poi/pins";
    private static final String MODU_TICKETS_URL = MODU_API_BASE + "/poi/available-tickets";
    private static final int PRECISION = 6;

    @Value("${kakao.rest.api.key}")
    private String kakaoApiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * 반경 내 모두의주차장 API 주차장 목록 조회
     * 중심 geohash + 8방향 이웃 셀까지 쿼리하여 경계 누락 방지
     */
    public List<NearbyParkingLotResponse> getNearbyLots(double lat, double lng, double radiusKm) {
        List<NearbyParkingLotResponse> result = new ArrayList<>();
        Set<Integer> seenIds = new HashSet<>(); // parkinglotSeq 기반 중복 제거

        try {
            List<String> geohashes = buildNeighborGeohashes(lat, lng);

            // available-tickets API로 티켓 정보 미리 조회 (seq → tickets 맵)
            Map<Integer, List<ModuTicketDto>> ticketsMap = fetchTicketsMap(geohashes);

            HttpHeaders headers = buildHeaders();
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            for (String geohash : geohashes) {
                try {
                    String url = MODU_PINS_URL + "?geohash=" + geohash + "&precision=" + PRECISION;
                    ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
                    if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null) continue;

                    List<Map<String, Object>> dataList = extractDataList(response.getBody());
                    for (Map<String, Object> dataItem : dataList) {
                        List<Map<String, Object>> parkinglots = extractParkinglots(dataItem);
                        for (Map<String, Object> lot : parkinglots) {
                            Integer seq = toInteger(lot.get("parkinglotSeq"));
                            if (seq != null && seq > 0 && !seenIds.add(seq)) continue;

                            NearbyParkingLotResponse parsed = parseLot(lot, lat, lng, radiusKm);
                            if (parsed != null) {
                                // 티켓 정보 주입
                                if (seq != null && ticketsMap.containsKey(seq)) {
                                    parsed.setTickets(ticketsMap.get(seq));
                                }
                                result.add(parsed);
                            }
                        }
                    }
                } catch (Exception e) {
                    log.debug("모두의주차장 셀 {} 조회 실패: {}", geohash, e.getMessage());
                }
            }

            result.sort(Comparator.comparingDouble(NearbyParkingLotResponse::getDistanceKm));
            long withTickets = result.stream().filter(r -> r.getTickets() != null && !r.getTickets().isEmpty()).count();
            log.info("모두의주차장 API: 반경 {}km 내 주차장 {}개 (티켓 있는 곳: {}개)", radiusKm, result.size(), withTickets);

        } catch (Exception e) {
            log.error("모두의주차장 API 호출 실패: {}", e.getMessage());
        }
        return result;
    }

    /**
     * available-tickets API 호출 → seq별 티켓 목록 맵 반환
     */
    @SuppressWarnings("unchecked")
    private Map<Integer, List<ModuTicketDto>> fetchTicketsMap(List<String> geohashes) {
        Map<Integer, List<ModuTicketDto>> map = new HashMap<>();
        try {
            String geohashParam = String.join(",", geohashes);
            // URI.create() 로 쉼표 인코딩 방지
            URI uri = URI.create(MODU_TICKETS_URL + "?geohash=" + geohashParam);

            HttpHeaders headers = buildHeaders();
            headers.set("Referer", "https://app.modu.kr/");
            headers.set("Origin", "https://app.modu.kr");
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<Map> response = restTemplate.exchange(uri, HttpMethod.GET, entity, Map.class);
            if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null) return map;

            Object dataObj = response.getBody().get("data");
            if (!(dataObj instanceof List)) return map;

            for (Object cellObj : (List<?>) dataObj) {
                if (!(cellObj instanceof Map)) continue;
                Object lotsObj = ((Map<?, ?>) cellObj).get("parkingLots");
                if (!(lotsObj instanceof List)) continue;

                for (Object lotObj : (List<?>) lotsObj) {
                    if (!(lotObj instanceof Map)) continue;
                    Map<String, Object> lot = (Map<String, Object>) lotObj;
                    Integer seq = toInteger(lot.get("seq"));
                    if (seq == null || seq <= 0) continue;

                    Object ticketsObj = lot.get("tickets");
                    if (!(ticketsObj instanceof List)) continue;

                    List<ModuTicketDto> tickets = new ArrayList<>();
                    for (Object ticketObj : (List<?>) ticketsObj) {
                        if (!(ticketObj instanceof Map)) continue;
                        Map<String, Object> t = (Map<String, Object>) ticketObj;
                        String ticketName = getString(t, "name");
                        if (!isUsefulTicket(ticketName)) continue; // 필터링
                        tickets.add(ModuTicketDto.builder()
                                .name(ticketName)
                                .price(toIntegerNullable(t.get("price")))
                                .usagePeriodLabel(getString(t, "usagePeriodLabel"))
                                .build());
                    }
                    if (!tickets.isEmpty()) map.put(seq, tickets);
                }
            }
            log.info("available-tickets API: {}개 주차장 티켓 조회됨", map.size());
        } catch (Exception e) {
            log.warn("available-tickets API 호출 실패 (티켓 없이 진행): {}", e.getMessage());
        }
        return map;
    }

    /**
     * 가격 참고용 티켓만 포함 (종일권, 당일권, 월정기권)
     * 세차권, 단기권(N시간권), 연박권 등은 제외
     */
    private boolean isUsefulTicket(String name) {
        if (name == null) return false;
        // 포함: 종일권, 당일권, 월정기권
        boolean included = name.contains("종일권") || name.contains("당일권") || name.contains("월정기권");
        // 제외: 세차권, N시간권, 연박권, 실속패스, 기간권
        boolean excluded = name.contains("세차권") || name.matches(".*\\d+시간권.*")
                || name.contains("연박권") || name.contains("실속패스") || name.contains("실속 ");
        return included && !excluded;
    }

    /**
     * 중심 좌표의 geohash + 8방향 이웃 geohash 목록 생성
     */
    private List<String> buildNeighborGeohashes(double lat, double lng) {
        double latOff = 0.006;  // ~0.67km
        double lngOff = 0.011;  // ~1.0km

        double[][] offsets = {
            {0, 0},
            {latOff, 0}, {-latOff, 0},
            {0, lngOff}, {0, -lngOff},
            {latOff, lngOff}, {latOff, -lngOff},
            {-latOff, lngOff}, {-latOff, -lngOff}
        };

        List<String> hashes = new ArrayList<>();
        for (double[] off : offsets) {
            String h = GeohashUtils.encode(lat + off[0], lng + off[1], PRECISION);
            if (!hashes.contains(h)) hashes.add(h);
        }
        return hashes;
    }

    private HttpHeaders buildHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("User-Agent", "Mozilla/5.0");
        headers.set("Accept", "application/json");
        return headers;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> extractDataList(Map<String, Object> body) {
        Object dataObj = body.get("data");
        if (dataObj instanceof List) return (List<Map<String, Object>>) dataObj;
        return Collections.emptyList();
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> extractParkinglots(Map<String, Object> dataItem) {
        Object lots = dataItem.get("parkinglots");
        if (lots instanceof List) return (List<Map<String, Object>>) lots;
        return Collections.emptyList();
    }

    @SuppressWarnings("unchecked")
    private NearbyParkingLotResponse parseLot(Map<String, Object> lot, double reqLat, double reqLng, double radiusKm) {
        try {
            double lotLat = toDouble(lot.get("latitude"));
            double lotLng = toDouble(lot.get("longitude"));
            if (lotLat == 0.0 || lotLng == 0.0) return null;

            double distance = DistanceUtils.calculateDistanceKm(reqLat, reqLng, lotLat, lotLng);
            if (distance > radiusKm) return null;

            Integer seq = toInteger(lot.get("parkinglotSeq"));
            String lotName = getString(lot, "name");
            boolean isFree = Boolean.TRUE.equals(lot.get("isFree"));
            boolean isClosed = Boolean.TRUE.equals(lot.get("isClosed"));
            Integer availableSpots = toIntegerNullable(lot.get("qty"));

            Integer feeUnit = null, lotPrice = null, addUnitTime = null, addUnitPrice = null;

            if (!isFree) {
                Object calcPriceObj = lot.get("calcPrice");
                if (calcPriceObj instanceof Map) {
                    Map<String, Object> calcPrice = (Map<String, Object>) calcPriceObj;
                    TreeMap<Integer, Integer> sortedPrice = new TreeMap<>();
                    for (Map.Entry<String, Object> entry : calcPrice.entrySet()) {
                        try {
                            sortedPrice.put(Integer.parseInt(entry.getKey()), toInteger(entry.getValue()));
                        } catch (Exception ignored) {}
                    }
                    if (!sortedPrice.isEmpty()) {
                        Map.Entry<Integer, Integer> first = sortedPrice.firstEntry();
                        feeUnit = first.getKey();
                        lotPrice = first.getValue();
                        if (sortedPrice.size() > 1) {
                            Map.Entry<Integer, Integer> second = sortedPrice.higherEntry(first.getKey());
                            if (second != null) {
                                addUnitTime = second.getKey() - first.getKey();
                                addUnitPrice = second.getValue() - first.getValue();
                            }
                        }
                    }
                }
            }

            String lotType = convertCategory(toInteger(lot.get("category")));

            // 역지오코딩으로 주소 보완
            String address = KakaoGeocodingUtils.getAddress(lotLat, lotLng, kakaoApiKey);

            return NearbyParkingLotResponse.builder()
                    .id(seq != null && seq > 0 ? seq.longValue() : null) // parkinglotSeq를 id로 저장
                    .lotName(lotName)
                    .address(address)
                    .lat(lotLat)
                    .lng(lotLng)
                    .freeYn(isFree)
                    .lotPrice(isFree ? null : lotPrice)
                    .feeUnit(feeUnit)
                    .addUnitTime(addUnitTime)
                    .addUnitPrice(addUnitPrice)
                    .totalSpaces(null)
                    .availableSpots(availableSpots)
                    .operatingHours(isClosed ? "운영 종료" : null)
                    .distanceKm(Math.round(distance * 1000.0) / 1000.0)
                    .source("모두의주차장")
                    .lotType(lotType)
                    .build();

        } catch (Exception e) {
            log.warn("모두의주차장 lot 파싱 실패: {}", e.getMessage());
            return null;
        }
    }

    private String convertCategory(Integer category) {
        if (category == null) return null;
        return switch (category) {
            case 1 -> "공영";
            case 2 -> "민영";
            case 3 -> "노상";
            case 4 -> "부설";
            default -> null;
        };
    }

    private String getString(Map<String, Object> map, String key) {
        Object val = map.get(key);
        return val != null ? val.toString() : null;
    }

    private double toDouble(Object val) {
        if (val == null) return 0.0;
        if (val instanceof Number) return ((Number) val).doubleValue();
        try { return Double.parseDouble(val.toString()); } catch (Exception e) { return 0.0; }
    }

    private int toInteger(Object val) {
        if (val == null) return 0;
        if (val instanceof Number) return ((Number) val).intValue();
        try { return Integer.parseInt(val.toString().replaceAll("[^0-9]", "")); } catch (Exception e) { return 0; }
    }

    private Integer toIntegerNullable(Object val) {
        if (val == null) return null;
        if (val instanceof Number) {
            int v = ((Number) val).intValue();
            return v > 0 ? v : null;
        }
        try {
            int v = Integer.parseInt(val.toString().replaceAll("[^0-9]", ""));
            return v > 0 ? v : null;
        } catch (Exception e) { return null; }
    }
}
