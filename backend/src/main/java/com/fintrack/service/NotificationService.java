package com.fintrack.service;

import com.fintrack.dto.NotificationActionRequest;
import com.fintrack.dto.NotificationResponse;
import com.fintrack.dto.NotificationSettingsRequest;
import com.fintrack.model.*;
import com.fintrack.repository.NotificationRepository;
import com.fintrack.repository.RecurringExpenseRepository;
import com.fintrack.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final RecurringExpenseRepository recurringExpenseRepository;

    @Transactional
    public void createNotificationForDueRecurringExpense(RecurringExpense recurringExpense, LocalDate dueDate) {
        User user = recurringExpense.getUser();
        
        String title = "Payment Due: " + recurringExpense.getDescription();
        String message = String.format("Your %s payment of $%.2f is due on %s",
                recurringExpense.getCategory().getName(),
                recurringExpense.getAmount(),
                dueDate);

        LocalDateTime notificationTime = LocalDateTime.of(
                dueDate,
                LocalTime.of(user.getNotificationHourOfDay(), user.getNotificationMinute())
        );

        Notification notification = Notification.builder()
                .user(user)
                .recurringExpense(recurringExpense)
                .title(title)
                .message(message)
                .type(NotificationType.PAYMENT_DUE)
                .status(NotificationStatus.UNREAD)
                .dueDate(dueDate)
                .notificationTime(notificationTime)
                .build();

        notificationRepository.save(notification);
        log.info("Created notification for recurring expense ID: {} for user: {}", 
                recurringExpense.getId(), user.getId());
    }

    @Scheduled(fixedRate = 300000) // Run every 5 minutes
    @Transactional
    public void checkAndCreateNotifications() {
        try {
            log.info("Checking for due recurring expenses to create notifications...");
            LocalDate today = LocalDate.now();
            List<RecurringExpense> dueExpenses = recurringExpenseRepository
                    .findByNextDueDateLessThanEqualAndIsActiveTrue(today);

            log.info("Found {} due recurring expenses", dueExpenses.size());

            for (RecurringExpense recurring : dueExpenses) {
                try {
                    // Check if notification already exists for this date
                    long existingNotifications = notificationRepository
                            .findByUserIdAndDueDateAndStatusNot(
                                    recurring.getUser().getId(),
                                    today,
                                    NotificationStatus.EXPIRED
                            ).stream()
                            .filter(n -> n.getRecurringExpense().getId().equals(recurring.getId()))
                            .count();

                    if (existingNotifications == 0) {
                        createNotificationForDueRecurringExpense(recurring, today);
                    }
                } catch (Exception e) {
                    log.error("Error processing recurring expense {}: {}", recurring.getId(), e.getMessage(), e);
                }
            }
            log.info("Notification check completed successfully");
        } catch (Exception e) {
            log.error("Error in checkAndCreateNotifications: {}", e.getMessage(), e);
        }
    }

    public List<NotificationResponse> getAllNotifications(Long userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<NotificationResponse> getUnreadNotifications(Long userId) {
        return notificationRepository.findByUserIdAndStatusOrderByCreatedAtDesc(userId, NotificationStatus.UNREAD)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<NotificationResponse> getActiveNotifications(Long userId) {
        return notificationRepository.findActiveNotifications(userId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public long getUnreadCount(Long userId) {
        return notificationRepository.countByUserIdAndStatus(userId, NotificationStatus.UNREAD);
    }

    @Transactional
    public NotificationResponse markNotificationAsRead(Long userId, Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found"));

        if (!notification.getUser().getId().equals(userId)) {
            throw new RuntimeException("Unauthorized access");
        }

        notification.setStatus(NotificationStatus.READ);
        notification = notificationRepository.save(notification);
        return mapToResponse(notification);
    }

    @Transactional
    public NotificationResponse markNotificationAction(Long userId, Long notificationId, NotificationActionRequest request) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found"));

        if (!notification.getUser().getId().equals(userId)) {
            throw new RuntimeException("Unauthorized access");
        }

        notification.setStatus(request.getAction());
        notification.setActionTaken(request.getAction().toString());
        notification.setActionTakenAt(LocalDateTime.now());
        
        notification = notificationRepository.save(notification);
        return mapToResponse(notification);
    }

    @Transactional
    public void updateNotificationSettings(Long userId, NotificationSettingsRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (request.getNotificationHourOfDay() != null) {
            user.setNotificationHourOfDay(request.getNotificationHourOfDay());
        }
        if (request.getNotificationMinute() != null) {
            user.setNotificationMinute(request.getNotificationMinute());
        }
        if (request.getEnableNotifications() != null) {
            user.setEnableNotifications(request.getEnableNotifications());
        }

        userRepository.save(user);
        log.info("Updated notification settings for user: {}", userId);
    }

    @Transactional
    public void clearNotifications(Long userId) {
        List<Notification> notifications = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
        for (Notification notification : notifications) {
            if (notification.getStatus() != NotificationStatus.UNREAD && 
                notification.getStatus() != NotificationStatus.READ) {
                notificationRepository.delete(notification);
            }
        }
        log.info("Cleared old notifications for user: {}", userId);
    }

    private NotificationResponse mapToResponse(Notification notification) {
        RecurringExpense recurringExpense = notification.getRecurringExpense();
        Category category = recurringExpense.getCategory();

        return NotificationResponse.builder()
                .id(notification.getId())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .status(notification.getStatus())
                .type(notification.getType())
                .dueDate(notification.getDueDate())
                .notificationTime(notification.getNotificationTime())
                .actionTakenAt(notification.getActionTakenAt())
                .actionTaken(notification.getActionTaken())
                .createdAt(notification.getCreatedAt())
                .recurringExpenseId(recurringExpense.getId())
                .categoryName(category.getName())
                .categoryIcon(category.getIcon())
                .categoryColor(category.getColor())
                .recurringExpenseDescription(recurringExpense.getDescription())
                .amount(recurringExpense.getAmount())
                .frequency(recurringExpense.getFrequency().toString())
                .build();
    }
}
