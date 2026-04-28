package com.fintrack.dto;

import com.fintrack.model.NotificationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationSettingsRequest {
    private Integer notificationHourOfDay; // 0-23, e.g., 9 for 9 AM
    private Integer notificationMinute; // 0-59
    private Boolean enableNotifications; // true/false
}
