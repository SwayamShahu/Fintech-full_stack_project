package com.fintrack.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class ExpenseRequest {
    
    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    private BigDecimal amount;
    
    @NotNull(message = "Category is required")
    private Long categoryId;
    
    private String description;
    
    @NotNull(message = "Expense date is required")
    private LocalDate expenseDate;
    
    private String paymentMode = "Cash";
    
    private Boolean isRecurring = false;
}
