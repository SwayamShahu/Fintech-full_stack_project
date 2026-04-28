# MySQL Fix for Notification System

# 1. Add missing columns to users table
ALTER TABLE users ADD COLUMN IF NOT EXISTS notification_hour INT DEFAULT 9;
ALTER TABLE users ADD COLUMN IF NOT EXISTS notification_minute INT DEFAULT 0;
ALTER TABLE users ADD COLUMN IF NOT EXISTS enable_notifications BOOLEAN DEFAULT true;

# 2. Create Notification table
CREATE TABLE IF NOT EXISTS notifications (
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

# 3. Verify tables exist
SHOW TABLES;

# 4. Check users table structure
DESCRIBE users;

# 5. Check notifications table structure  
DESCRIBE notifications;
