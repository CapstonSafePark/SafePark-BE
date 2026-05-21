package com.safepark.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.safepark.dto.NearbyParkingLotResponse;
import com.safepark.util.DistanceUtils;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Slf4j
@Service
public class GyeonggiParkingApiService {

    private static final String REALTIME_API_URL =
            "https://openapigits.gg.go.kr/api/rest/getParkingPlaceAvailabilityInfoList";

    // 실시간 API 지원 시군구 (laeId 매핑) - 필요 시 추가
    private static final Map<String, Integer> LAE_ID_MAP = Map.of(
            "의왕시", 31170
    );

    @Value("${gyeonggi.parking.api.key}")
    private String apiKey;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // 서버 시작 시 로드되는 정적 메타데이터 캐시
    // key: 주차장명 (실시간 API merge용)
    private List<ParkingMeta> metaList = new ArrayList<>();
    private final Map<String, ParkingMeta> metaByName = new HashMap<>();

    public GyeonggiParkingApiService() {
        RestTemplate rt = new RestTemplate();
        rt.getMessageConverters().stream()
                .filter(c -> c instanceof StringHttpMessageConverter)
                .forEach(c -> ((StringHttpMessageConverter) c).setDefaultCharset(StandardCharsets.UTF_8));
        this.restTemplate = rt;
    }

    @PostConstruct
    public void loadMeta() {
        try {
            InputStream is = new ClassPathResource("gyeonggi-parking-meta.json").getInputStream();
            metaList = objectMapper.readValue(is, new TypeReference<List<ParkingMeta>>() {});
            for (ParkingMeta m : metaList) {
                if (m.getName() != null) {
                    // 공백 제거 후 저장 (실시간 API 이름과 매칭용)
                    metaByName.put(m.getName().replaceAll("\\s", ""), m);
                }
            }
            log.info("경기도 주차장 메타데이터 로드 완료: {}개", metaList.size());
        } catch (Exception e) {
            log.error("경기도 주차장 메타데이터 로드 실패: {}", e.getMessage());
        }
    }

    /**
     * 반경 내 경기도 실시간 가용면수 조회 (DB 주차장 enrichment용)
     * key: 주차장명(공백제거), value: 가용면수
     */
    public Map<String, Integer> getRealtimeAvailableMap(double lat, double lng, double radiusKm) {
        Set<String> neededCities = new HashSet<>();
        for (ParkingMeta m : metaList) {
            if (m.getLat() == 0.0 || m.getLng() == 0.0) continue;
            if (m.getAddr() != null && m.getAddr().contains("의왕시")) continue;
            double dist = DistanceUtils.calculateDistanceKm(lat, lng, m.getLat(), m.getLng());
            if (dist <= radiusKm) {
                String city = extractCity(m.getAddr());
                if (city != null && LAE_ID_MAP.containsKey(city)) {
                    neededCities.add(city);
                }
            }
        }
        Map<String, Integer> availableMap = new HashMap<>();
        for (String city : neededCities) {
            fetchRealtimeAvailable(LAE_ID_MAP.get(city), availableMap);
        }
        return availableMap;
    }

    /**
     * 반경 내 경기도 주차장 조회
     * 1) 메타(JSON)에서 거리 필터링
     * 2) 실시간 API로 가용면수 merge (지원 시군만)
     */
    public List<NearbyParkingLotResponse> getNearbyLots(double lat, double lng, double radiusKm) {
        // 1) 거리 필터링 (의왕시는 UiwangParkingApiService가 담당하므로 제외)
        List<ParkingMeta> nearby = new ArrayList<>();
        for (ParkingMeta m : metaList) {
            if (m.getLat() == 0.0 || m.getLng() == 0.0) continue;
            if (m.getAddr() != null && m.getAddr().contains("의왕시")) continue;
            double dist = DistanceUtils.calculateDistanceKm(lat, lng, m.getLat(), m.getLng());
            if (dist <= radiusKm) {
                m.setDistanceKm(Math.round(dist * 1000.0) / 1000.0);
                nearby.add(m);
            }
        }

        if (nearby.isEmpty()) return new ArrayList<>();

        // 2) 실시간 가용면수 fetch (필요한 시군만)
        Set<String> neededCities = new HashSet<>();
        for (ParkingMeta m : nearby) {
            String city = extractCity(m.getAddr());
            if (city != null && LAE_ID_MAP.containsKey(city)) {
                neededCities.add(city);
            }
        }

        // 실시간 데이터: 주차장명 → 가용면수
        Map<String, Integer> availableMap = new HashMap<>();
        for (String city : neededCities) {
            fetchRealtimeAvailable(LAE_ID_MAP.get(city), availableMap);
        }

        // 3) 결과 조합
        List<NearbyParkingLotResponse> result = new ArrayList<>();
        for (ParkingMeta m : nearby) {
            Integer available = availableMap.get(m.getName() != null ? m.getName().replaceAll("\\s", "") : "");

            result.add(NearbyParkingLotResponse.builder()
                    .id(null)
                    .lotName(m.getName())
                    .address(m.getAddr())
                    .lat(m.getLat())
                    .lng(m.getLng())
                    .lotPrice(null)
                    .freeYn(m.isFree())
                    .operatingHours(null)
                    .parkingFeeDesc(null)
                    .totalSpaces(m.getTotal())
                    .availableSpots(available)
                    .distanceKm(m.getDistanceKm())
                    .source("경기도")
                    .build());
        }

        result.sort(Comparator.comparingDouble(NearbyParkingLotResponse::getDistanceKm));
        log.info("경기도: 반경 {}km 내 주차장 {}개 (실시간 {}개)", radiusKm, result.size(), availableMap.size());
        return result;
    }

    private void fetchRealtimeAvailable(int laeId, Map<String, Integer> availableMap) {
        try {
            String url = REALTIME_API_URL + "?serviceKey=" + apiKey + "&laeId=" + laeId;
            String xml = restTemplate.getForObject(url, String.class);
            if (xml == null) return;

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
            doc.getDocumentElement().normalize();

            NodeList items = doc.getElementsByTagName("itemList");
            for (int i = 0; i < items.getLength(); i++) {
                Element item = (Element) items.item(i);
                String name = getText(item, "pkplcNm");
                String avbl = getText(item, "avblPklotCnt");
                if (name != null && avbl != null) {
                    try {
                        // 공백 제거 후 저장 (JSON 메타 이름과 매칭용)
                        availableMap.put(name.replaceAll("\\s", ""), Integer.parseInt(avbl.trim()));
                    } catch (NumberFormatException ignored) {}
                }
            }
        } catch (Exception e) {
            log.error("경기도 실시간 API 실패 laeId={}: {}", laeId, e.getMessage());
        }
    }

    private String extractCity(String addr) {
        if (addr == null) return null;
        for (String city : LAE_ID_MAP.keySet()) {
            if (addr.contains(city)) return city;
        }
        return null;
    }

    private String getText(Element el, String tagName) {
        NodeList nl = el.getElementsByTagName(tagName);
        if (nl.getLength() == 0) return null;
        String text = nl.item(0).getTextContent();
        return (text == null || text.isBlank()) ? null : text.trim();
    }

    // 정적 메타데이터 DTO
    public static class ParkingMeta {
        private String id;
        private String name;
        private String addr;
        private double lat;
        private double lng;
        private Integer total;
        private boolean free;
        private double distanceKm;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getAddr() { return addr; }
        public void setAddr(String addr) { this.addr = addr; }
        public double getLat() { return lat; }
        public void setLat(double lat) { this.lat = lat; }
        public double getLng() { return lng; }
        public void setLng(double lng) { this.lng = lng; }
        public Integer getTotal() { return total; }
        public void setTotal(Integer total) { this.total = total; }
        public boolean isFree() { return free; }
        public void setFree(boolean free) { this.free = free; }
        public double getDistanceKm() { return distanceKm; }
        public void setDistanceKm(double distanceKm) { this.distanceKm = distanceKm; }
    }
}
