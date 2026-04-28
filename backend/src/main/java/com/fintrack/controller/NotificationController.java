package com.fintrack.controller;

import com.fintrack.dto.ApiResponse;
import com.fintrack.dto.NotificationActionRequest;
import com.fintrack.dto.NotificationResponse;
import com.fintrack.dto.NotificationSettingsRequest;
import com.fintrack.security.CustomUserDetails;
import com.fintrack.service.NotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<ApiResponse> getAllNotifications(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        List<NotificationResponse> notifications = notificationService
                .getAllNotifications(userDetails.getId());
        return ResponseEntity.ok(new ApiResponse(true, "Notifications retrieved", notifications));
    }

    @GetMapping("/unread")
    public ResponseEntity<ApiResponse> getUnreadNotifications(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        List<NotificationResponse> notifications = notificationService
                .getUnreadNotifications(userDetails.getId());
        return ResponseEntity.ok(new ApiResponse(true, "Unread notifications retrieved", notifications));
    }

    @GetMapping("/active")
    public ResponseEntity<ApiResponse> getActiveNotifications(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        List<NotificationResponse> notifications = notificationService
                .getActiveNotifications(userDetails.getId());
        return ResponseEntity.ok(new ApiResponse(true, "Active notifications retrieved", notifications));
    }

    @GetMapping("/count")
    public ResponseEntity<ApiResponse> getUnreadCount(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        long count = notificationService.getUnreadCount(userDetails.getId());
        Map<String, Object> response = new HashMap<>();
        response.put("unreadCount", count);
        return ResponseEntity.ok(new ApiResponse(true, "Unread count retrieved", response));
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<ApiResponse> markAsRead(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long id) {
        try {
            NotificationResponse response = notificationService
                    .markNotificationAsRead(userDetails.getId(), id);
            return ResponseEntity.ok(new ApiResponse(true, "Notification marked as read", response));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, e.getMessage()));
        }
    }

    @PostMapping("/{id}/action")
    public ResponseEntity<ApiResponse> takeAction(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long id,
            @Valid @RequestBody NotificationActionRequest request) {
        try {
            NotificationResponse response = notificationService
                    .markNotificationAction(userDetails.getId(), id, request);
            return ResponseEntity.ok(new ApiResponse(true, "Action recorded on notification", response));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, e.getMessage()));
        }
    }

    @PutMapping("/settings")
    public ResponseEntity<ApiResponse> updateNotificationSettings(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody NotificationSettingsRequest request) {
        try {
            notificationService.updateNotificationSettings(userDetails.getId(), request);
            return ResponseEntity.ok(new ApiResponse(true, "Notification settings updated"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, e.getMessage()));
        }
    }

    @DeleteMapping("/clear")
    public ResponseEntity<ApiResponse> clearNotifications(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        try {
            notificationService.clearNotifications(userDetails.getId());
            return ResponseEntity.ok(new ApiResponse(true, "Notifications cleared"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, e.getMessage()));
        }
    }

    @PostMapping("/trigger-check")
    public ResponseEntity<ApiResponse> triggerNotificationCheck() {
        try {
            notificationService.checkAndCreateNotifications();
            return ResponseEntity.ok(new ApiResponse(true, "Notification check triggered (for demo purposes)"));
        } catch (Exception e) {
            String errorMessage = e.getMessage() != null ? e.getMessage() : "Unknown error occurred";
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, "Failed to trigger check: " + errorMessage));
        }
    }
}
