-- Add notification columns to users table
ALTER TABLE users ADD COLUMN notification_hour INT DEFAULT 9;
ALTER TABLE users ADD COLUMN notification_minute INT DEFAULT 0;
ALTER TABLE users ADD COLUMN enable_notifications BOOLEAN DEFAULT true;
