package com.fintrack.dto;

import com.fintrack.model.Frequency;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class RecurringExpenseRequest {
    
    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    private BigDecimal amount;
    
    @NotNull(message = "Category is required")
    private Long categoryId;
    
    private String description;
    
    private Frequency frequency = Frequency.MONTHLY;
    
    @NotNull(message = "Start date is required")
    private LocalDate startDate;
}
