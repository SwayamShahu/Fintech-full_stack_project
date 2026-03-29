package com.fintrack.dto;

import com.fintrack.model.Expense;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class ExpenseResponse {
    private Long id;
    private BigDecimal amount;
    private Long categoryId;
    private String categoryName;
    private String categoryIcon;
    private String categoryColor;
    private String description;
    private LocalDate expenseDate;
    private String paymentMode;
    private Boolean isRecurring;
    private Boolean isAnomaly;
    private BigDecimal anomalyScore;
    private String anomalyExplanation;
    private String anomalyType;
    private LocalDateTime createdAt;
    
    public ExpenseResponse(Expense expense) {
        this.id = expense.getId();
        this.amount = expense.getAmount();
        this.categoryId = expense.getCategory().getId();
        this.categoryName = expense.getCategory().getName();
        this.categoryIcon = expense.getCategory().getIcon();
        this.categoryColor = expense.getCategory().getColor();
        this.description = expense.getDescription();
        this.expenseDate = expense.getExpenseDate();
        this.paymentMode = expense.getPaymentMode();
        this.isRecurring = expense.getIsRecurring();
        this.isAnomaly = expense.getIsAnomaly();
        this.anomalyScore = expense.getAnomalyScore();
        this.anomalyExplanation = expense.getAnomalyExplanation();
        this.anomalyType = expense.getAnomalyType();
        this.createdAt = expense.getCreatedAt();
    }
}
