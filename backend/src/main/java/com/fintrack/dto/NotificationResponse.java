package com.fintrack.dto;

import com.fintrack.model.NotificationStatus;
import com.fintrack.model.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationResponse {
    private Long id;
    private String title;
    private String message;
    private NotificationStatus status;
    private NotificationType type;
    private LocalDate dueDate;
    private LocalDateTime notificationTime;
    private LocalDateTime actionTakenAt;
    private String actionTaken;
    private Long recurringExpenseId;
    private String categoryName;
    private String categoryIcon;
    private String categoryColor;
    private String recurringExpenseDescription;
    private BigDecimal amount;
    private String frequency;
    private LocalDateTime createdAt;
}
