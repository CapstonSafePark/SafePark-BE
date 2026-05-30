package com.safepark.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModuTicketDto {
    private String name;           // 티켓명 (예: "평일 종일권")
    private Integer price;         // 가격 (원)
    private String usagePeriodLabel; // 사용 기간 (예: "당일 23:59까지")
}
