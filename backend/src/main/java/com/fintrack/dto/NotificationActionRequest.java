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
public class NotificationActionRequest {
    private NotificationStatus action; // DONE, LEFT, SKIPPED
}
