package com.fintrack.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonthlySummaryResponse {
    private BigDecimal totalSpent;
    private BigDecimal averageDaily;
    private Long transactionCount;
    private String topCategory;
    private BigDecimal topCategoryAmount;
    private List<CategorySummary> categoryBreakdown;
    private Map<String, BigDecimal> dailyTotals;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategorySummary {
        private String name;
        private String icon;
        private String color;
        private BigDecimal amount;
        private Double percentage;
    }
}
