package com.fintrack.repository;

import com.fintrack.model.Notification;
import com.fintrack.model.NotificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<Notification> findByUserIdAndStatusOrderByCreatedAtDesc(Long userId, NotificationStatus status);
    List<Notification> findByUserIdAndDueDateAndStatusNot(Long userId, LocalDate dueDate, NotificationStatus status);
    
    @Query("SELECT n FROM Notification n WHERE n.user.id = :userId AND n.status IN ('UNREAD', 'READ')")
    List<Notification> findActiveNotifications(Long userId);
    
    @Query("SELECT n FROM Notification n WHERE n.status = 'UNREAD' AND n.notificationTime <= :now")
    List<Notification> findUnreadNotificationsReadyToSend(LocalDateTime now);
    
    long countByUserIdAndStatus(Long userId, NotificationStatus status);
}
