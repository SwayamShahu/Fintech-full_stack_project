package com.fintrack.service;

import com.fintrack.dto.MonthComparisonResponse;
import com.fintrack.dto.MonthlySummaryResponse;
import com.fintrack.repository.ExpenseRepository;
import com.fintrack.repository.RecurringExpenseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final ExpenseRepository expenseRepository;
    private final RecurringExpenseRepository recurringExpenseRepository;

    public MonthlySummaryResponse getMonthlySummary(Long userId) {
        YearMonth currentMonth = YearMonth.now();
        LocalDate startDate = currentMonth.atDay(1);
        LocalDate endDate = currentMonth.atEndOfMonth();

        BigDecimal totalSpent = expenseRepository.getTotalByUserAndDateRange(userId, startDate, endDate);
        if (totalSpent == null) totalSpent = BigDecimal.ZERO;

        Long transactionCount = expenseRepository.countByUserId(userId);
        BigDecimal avgTransaction = expenseRepository.getAverageByUserId(userId);
        if (avgTransaction == null) avgTransaction = BigDecimal.ZERO;

        List<Object[]> categoryTotals = expenseRepository.getCategoryWiseTotals(userId, startDate, endDate);
        
        List<MonthlySummaryResponse.CategorySummary> categoryBreakdown = new java.util.ArrayList<>();
        String topCategory = null;
        BigDecimal topCategoryAmount = BigDecimal.ZERO;
        
        for (Object[] row : categoryTotals) {
            String catName = (String) row[0];
            BigDecimal amount = (BigDecimal) row[1];
            double percentage = totalSpent.compareTo(BigDecimal.ZERO) > 0 
                    ? amount.multiply(BigDecimal.valueOf(100)).divide(totalSpent, 2, RoundingMode.HALF_UP).doubleValue() 
                    : 0;
            categoryBreakdown.add(MonthlySummaryResponse.CategorySummary.builder()
                    .name(catName)
                    .amount(amount)
                    .percentage(percentage)
                    .build());
            if (topCategory == null || amount.compareTo(topCategoryAmount) > 0) {
                topCategory = catName;
                topCategoryAmount = amount;
            }
        }

        int daysInMonth = currentMonth.lengthOfMonth();
        BigDecimal averageDaily = totalSpent.divide(BigDecimal.valueOf(daysInMonth), 2, RoundingMode.HALF_UP);

        return MonthlySummaryResponse.builder()
                .totalSpent(totalSpent)
                .transactionCount(transactionCount != null ? transactionCount : 0L)
                .averageDaily(averageDaily)
                .categoryBreakdown(categoryBreakdown)
                .topCategory(topCategory)
                .topCategoryAmount(topCategoryAmount)
                .build();
    }

    public MonthComparisonResponse getMonthComparison(Long userId) {
        YearMonth currentMonth = YearMonth.now();
        YearMonth previousMonth = currentMonth.minusMonths(1);

        LocalDate currentStart = currentMonth.atDay(1);
        LocalDate currentEnd = currentMonth.atEndOfMonth();
        LocalDate previousStart = previousMonth.atDay(1);
        LocalDate previousEnd = previousMonth.atEndOfMonth();

        BigDecimal currentTotal = expenseRepository.getTotalByUserAndDateRange(userId, currentStart, currentEnd);
        BigDecimal previousTotal = expenseRepository.getTotalByUserAndDateRange(userId, previousStart, previousEnd);

        if (currentTotal == null) currentTotal = BigDecimal.ZERO;
        if (previousTotal == null) previousTotal = BigDecimal.ZERO;

        BigDecimal difference = currentTotal.subtract(previousTotal);
        Double percentageChange = 0.0;
        if (previousTotal.compareTo(BigDecimal.ZERO) > 0) {
            percentageChange = difference.multiply(BigDecimal.valueOf(100))
                    .divide(previousTotal, 2, RoundingMode.HALF_UP).doubleValue();
        }

        String trend = difference.compareTo(BigDecimal.ZERO) > 0 ? "UP" : 
                      (difference.compareTo(BigDecimal.ZERO) < 0 ? "DOWN" : "STABLE");
        
        String message = trend.equals("UP") 
                ? "You spent " + difference.abs() + " more than last month"
                : (trend.equals("DOWN") 
                    ? "You saved " + difference.abs() + " compared to last month"
                    : "Your spending is the same as last month");

        return MonthComparisonResponse.builder()
                .currentMonthTotal(currentTotal)
                .previousMonthTotal(previousTotal)
                .difference(difference)
                .percentageChange(percentageChange)
                .trend(trend)
                .message(message)
                .build();
    }

    public Map<String, Object> getDashboardStats(Long userId) {
        Map<String, Object> stats = new HashMap<>();
        
        MonthlySummaryResponse summary = getMonthlySummary(userId);
        MonthComparisonResponse comparison = getMonthComparison(userId);
        
        stats.put("summary", summary);
        stats.put("comparison", comparison);
        stats.put("recentAnomalies", getRecentAnomalyCount(userId));
        stats.put("upcomingRecurring", getUpcomingRecurringCount(userId, 7));
        
        return stats;
    }

    private int getRecentAnomalyCount(Long userId) {
        return expenseRepository.findAnomaliesByUserId(userId).size();
    }

    private int getUpcomingRecurringCount(Long userId, int days) {
        LocalDate endDate = LocalDate.now().plusDays(days);
        return (int) recurringExpenseRepository.findByUserIdAndIsActiveTrue(userId)
                .stream()
                .filter(e -> !e.getNextDueDate().isAfter(endDate))
                .count();
    }
}
