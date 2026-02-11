package com.fintrack.dto;

import com.fintrack.model.Frequency;
import com.fintrack.model.RecurringExpense;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class RecurringExpenseResponse {
    private Long id;
    private BigDecimal amount;
    private Long categoryId;
    private String categoryName;
    private String categoryIcon;
    private String categoryColor;
    private String description;
    private Frequency frequency;
    private LocalDate nextDueDate;
    private Boolean isActive;
    private LocalDateTime createdAt;
    
    public RecurringExpenseResponse(RecurringExpense recurring) {
        this.id = recurring.getId();
        this.amount = recurring.getAmount();
        this.categoryId = recurring.getCategory().getId();
        this.categoryName = recurring.getCategory().getName();
        this.categoryIcon = recurring.getCategory().getIcon();
        this.categoryColor = recurring.getCategory().getColor();
        this.description = recurring.getDescription();
        this.frequency = recurring.getFrequency();
        this.nextDueDate = recurring.getNextDueDate();
        this.isActive = recurring.getIsActive();
        this.createdAt = recurring.getCreatedAt();
    }
}
