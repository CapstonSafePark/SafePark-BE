package com.safepark.util;

import com.safepark.entity.CrackZone;

import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;

public class RiskCalculator {

    // 24시간 절대 주정차 금지 구역 타입 (추후 소화전, 횡단보도 등 추가 가능)
    private static final List<String> ABSOLUTE_BAN_ZONES = Arrays.asList("버스정류장", "스쿨존");

    /**
     * 현재 KST 시각이 단속 시간 내인지 확인
     * - null이면 시간 정보 없음 → 안전하게 상시 단속으로 간주
     * - 자정 교차 처리 (예: 22:00 ~ 06:00)
     */
    public static boolean isWithinEnforcementTime(String startTime, String endTime) {
        if (startTime == null || endTime == null) return true;
        try {
            LocalTime start = LocalTime.parse(startTime);
            LocalTime end = LocalTime.parse(endTime);
            LocalTime now = ZonedDateTime.now(ZoneId.of("Asia/Seoul")).toLocalTime();

            if (start.isAfter(end)) {
                // 자정 교차 (예: 22:00 ~ 06:00)
                return !now.isBefore(start) || now.isBefore(end);
            } else {
                return !now.isBefore(start) && now.isBefore(end);
            }
        } catch (Exception e) {
            return true; // 파싱 실패 시 안전하게 단속 중으로 처리
        }
    }

    /**
     * 24시간 절대 주정차 금지 구역 여부
     */
    public static boolean isAbsoluteBanZone(String zoneType) {
        return zoneType != null && ABSOLUTE_BAN_ZONES.contains(zoneType);
    }

    /**
     * zone_type → 사용자 표시 명칭 변환
     */
    private static String displayName(String zoneType) {
        if ("스쿨존".equals(zoneType)) return "어린이보호구역(스쿨존)";
        return zoneType;
    }

    /**
     * 위험도 계산 결과
     */
    public static class RiskResult {
        public final String riskLevel;
        public final int probability;
        public final String reasoning;

        public RiskResult(String riskLevel, int probability, String reasoning) {
            this.riskLevel = riskLevel;
            this.probability = probability;
            this.reasoning = reasoning;
        }
    }

    /**
     * 이미지 업로드 경로: lineColor + zone + 현재시각 조합으로 최종 위험도 계산
     */
    public static RiskResult calculateWithLineColor(String lineColor, CrackZone zone) {
        boolean hasZone = zone != null;
        boolean hasTimeInfo = hasZone && zone.getStartTime() != null && zone.getEndTime() != null;
        String timeInfo = hasTimeInfo
                ? String.format("(%s~%s)", zone.getStartTime(), zone.getEndTime())
                : "";
        boolean withinTime = !hasZone || isWithinEnforcementTime(
                zone != null ? zone.getStartTime() : null,
                zone != null ? zone.getEndTime() : null);

        // ── 황색 이중선: 24시간 절대 금지 ──────────────────────────────────
        if ("황색이중선".equals(lineColor) || "yellow_double".equals(lineColor)) {
            return new RiskResult("HIGH", 95,
                    "황색 복선 구간입니다. 절대 주정차 금지 구역입니다.");
        }

        // ── 황색 점선: 주차 금지, 5분 정차만 가능 ────────────────────────────
        if ("황색점선".equals(lineColor) || "yellow_dashed".equals(lineColor)) {
            if (hasZone && isAbsoluteBanZone(zone.getZoneType())) {
                String zoneDesc = zoneDesc(zone);
                String extra = "스쿨존".equals(zone.getZoneType()) ? "과태료 확률이 매우 높습니다." : "주차 불가입니다.";
                return new RiskResult("HIGH", 92,
                        String.format("반경 100m 내 %s 단속구역이 존재합니다. %s", zoneDesc, extra));
            }
            return new RiskResult("HIGH", 80,
                    "황색 점선 구간입니다. 5분 이내 정차만 가능합니다.");
        }

        // ── 황색 단선: 탄력적 허용, zone + 시간 확인 ──────────────────────────
        if ("황색단선".equals(lineColor) || "yellow_single".equals(lineColor)) {
            if (hasZone && isAbsoluteBanZone(zone.getZoneType())) {
                String zoneDesc = zoneDesc(zone);
                String extra = "스쿨존".equals(zone.getZoneType()) ? "과태료 확률이 매우 높습니다." : "과태료 확률이 매우 높습니다.";
                return new RiskResult("HIGH", 90,
                        String.format("반경 100m 내 %s 단속구역이 존재합니다. %s", zoneDesc, extra));
            }
            if (hasZone && "주정차금지".equals(zone.getZoneType())) {
                if (!hasTimeInfo) {
                    return new RiskResult("MEDIUM", 55,
                            "황색 실선 + 주정차금지 구역입니다. 주변 표지판을 확인하세요.");
                }
                if (withinTime) {
                    return new RiskResult("HIGH", 85,
                            String.format("황색 실선 + 주정차금지 구역입니다. 현재 단속시간%s 내입니다.", timeInfo));
                } else {
                    return new RiskResult("LOW", 20,
                            String.format("황색 실선 구간이나 현재 단속시간%s 외입니다.", timeInfo));
                }
            }
            return new RiskResult("MEDIUM", 50,
                    "황색 실선 구간입니다. 주변 보조 표지판을 확인하세요.");
        }

        // ── CCTV 단속 카메라 구역 ─────────────────────────────────────────────
        if (hasZone && "CCTV".equals(zone.getZoneType())) {
            return new RiskResult("MEDIUM", 55,
                    "단속 CCTV가 설치된 구역입니다. 위반 시 즉시 적발됩니다.");
        }

        // ── 백색 점선 or 없음(감지 불가): zone 우선 ──────────────────────────
        if (hasZone && isAbsoluteBanZone(zone.getZoneType())) {
            String zoneDesc = zoneDesc(zone);
            String extra = "스쿨존".equals(zone.getZoneType()) ? "과태료 확률이 매우 높습니다." : "과태료 확률이 매우 높습니다.";
            return new RiskResult("HIGH", 90,
                    String.format("반경 100m 내 %s 단속구역이 존재합니다. %s", zoneDesc, extra));
        }
        if (hasZone && "주정차금지".equals(zone.getZoneType())) {
            if (!hasTimeInfo) {
                return new RiskResult("MEDIUM", 50,
                        "주정차금지 구역입니다. 단속시간 정보가 없습니다. 주의가 필요합니다.");
            }
            if (withinTime) {
                return new RiskResult("MEDIUM", 60,
                        String.format("주정차금지 구역입니다. 현재 단속시간%s 내입니다.", timeInfo));
            } else {
                return new RiskResult("LOW", 10,
                        String.format("주정차금지 구역이나 현재 단속시간%s 외입니다.", timeInfo));
            }
        }
        return new RiskResult("LOW", 10, "주변에 단속구역이 확인되지 않습니다.");
    }

    /**
     * 위치만 경로(재분석): zone + 현재시각만으로 위험도 계산
     */
    public static RiskResult calculateWithZoneOnly(CrackZone zone) {
        if (zone == null) {
            return new RiskResult("LOW", 10, "반경 100m 내 단속구역이 확인되지 않습니다.");
        }

        boolean hasTimeInfo = zone.getStartTime() != null && zone.getEndTime() != null;
        String timeInfo = hasTimeInfo
                ? String.format("(%s~%s)", zone.getStartTime(), zone.getEndTime())
                : "";

        if (isAbsoluteBanZone(zone.getZoneType())) {
            String zoneDesc = zoneDesc(zone);
            String extra = "스쿨존".equals(zone.getZoneType()) ? "과태료 확률이 매우 높습니다." : "과태료 확률이 매우 높습니다.";
            return new RiskResult("HIGH", 90,
                    String.format("반경 100m 내 %s 단속구역이 존재합니다. %s", zoneDesc, extra));
        }

        if ("CCTV".equals(zone.getZoneType())) {
            return new RiskResult("MEDIUM", 55,
                    "단속 CCTV가 설치된 구역입니다. 위반 시 즉시 적발됩니다.");
        }

        if ("주정차금지".equals(zone.getZoneType())) {
            if (!hasTimeInfo) {
                return new RiskResult("MEDIUM", 50,
                        "주정차금지 구역입니다. 단속시간 정보가 없습니다. 주의가 필요합니다.");
            }
            if (isWithinEnforcementTime(zone.getStartTime(), zone.getEndTime())) {
                return new RiskResult("MEDIUM", 60,
                        String.format("주정차금지 구역입니다. 현재 단속시간%s 내입니다.", timeInfo));
            } else {
                return new RiskResult("LOW", 15,
                        String.format("주정차금지 구역이나 현재 단속시간%s 외입니다.", timeInfo));
            }
        }

        return new RiskResult("LOW", 10, "반경 100m 내 단속구역이 확인되지 않습니다.");
    }

    /**
     * zone 표시 문자열: "어린이보호구역(스쿨존)(잠원초등학교)" 형태
     */
    private static String zoneDesc(CrackZone zone) {
        String name = displayName(zone.getZoneType());
        if (zone.getZoneName() != null && !zone.getZoneName().isBlank()) {
            return name + "(" + zone.getZoneName() + ")";
        }
        return name;
    }
}
