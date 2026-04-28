-- Fix for MySQL 8.0 (without IF NOT EXISTS for ADD COLUMN)
USE fintrack_db;

-- Add notification columns to users table
ALTER TABLE users ADD COLUMN notification_hour INT DEFAULT 9;
ALTER TABLE users ADD COLUMN notification_minute INT DEFAULT 0;
ALTER TABLE users ADD COLUMN enable_notifications BOOLEAN DEFAULT true;

-- Create notifications table
CREATE TABLE notifications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    recurring_expense_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    message VARCHAR(500),
    status ENUM('UNREAD', 'READ', 'DONE', 'LEFT', 'SKIPPED', 'EXPIRED') DEFAULT 'UNREAD',
    type ENUM('PAYMENT_DUE', 'PAYMENT_OVERDUE', 'RECURRING_CREATED', 'RECURRING_UPDATED'),
    due_date DATE NOT NULL,
    notification_time DATETIME NOT NULL,
    action_taken_at DATETIME,
    action_taken VARCHAR(50),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (recurring_expense_id) REFERENCES recurring_expenses(id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id),
    INDEX idx_status (status),
    INDEX idx_due_date (due_date)
);

-- Verify the changes
DESCRIBE users;
SHOW TABLES;
