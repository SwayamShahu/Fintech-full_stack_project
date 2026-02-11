package com.fintrack.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonthComparisonResponse {
    private BigDecimal currentMonthTotal;
    private BigDecimal previousMonthTotal;
    private BigDecimal difference;
    private Double percentageChange;
    private String trend; // "UP", "DOWN", "STABLE"
    private String message;
}
