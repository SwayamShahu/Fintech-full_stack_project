# IN-APP NOTIFICATION SYSTEM - SETUP & DEMO GUIDE

## Overview
The notification system allows users to:
1. Receive alerts when recurring payments are due
2. Approve/reject payments from an inbox
3. Set custom notification times
4. Track payment actions

---

## Architecture

### Backend Components
- **Notification Model**: Stores notification records with status tracking
- **NotificationService**: Handles notification creation, actions, and settings
- **NotificationController**: REST API endpoints for notification operations
- **User Enhancement**: Added notification timing fields to User model

### Frontend Components
- **Notifications.tsx**: Full-featured notification inbox UI
- **notificationApi**: TypeScript API client integration
- **Layout.tsx**: Updated navigation with Notifications link

---

## Database Schema

### New Notification Table
```sql
CREATE TABLE notifications (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    recurring_expense_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    message VARCHAR(500),
    status ENUM('UNREAD', 'READ', 'DONE', 'LEFT', 'SKIPPED', 'EXPIRED'),
    type ENUM('PAYMENT_DUE', 'PAYMENT_OVERDUE', 'RECURRING_CREATED', 'RECURRING_UPDATED'),
    due_date DATE NOT NULL,
    notification_time DATETIME NOT NULL,
    action_taken_at DATETIME,
    action_taken VARCHAR(50),
    created_at DATETIME,
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (recurring_expense_id) REFERENCES recurring_expenses(id)
);
```

### Updated User Table
- `notification_hour` (INT) - Hour of day for notifications (0-23, default: 9)
- `notification_minute` (INT) - Minute for notifications (0-59, default: 0)
- `enable_notifications` (BOOLEAN) - Enable/disable notifications (default: true)

---

## How It Works (Workflow)

### 1. User Creates Recurring Expense
```
User fills form → Backend saves RecurringExpense → System ready to notify
```

### 2. Notification Triggered (At Configured Time)
```
Scheduler runs at user's notification time → Checks due recurring expenses
→ Creates Notification record with UNREAD status → User sees in inbox
```

### 3. User Takes Action
```
User clicks "Payment Done" → Status = DONE → Expense created in transactions
       OR
User clicks "Left for Later" → Status = LEFT → Expense NOT created, kept for next cycle
```

### 4. Next Due Date Updated
- System automatically calculates next due date based on frequency
- Process repeats on next cycle

---

## API Endpoints

### Notification Management
```
GET     /api/notifications                  # Get all notifications
GET     /api/notifications/unread           # Get unread notifications
GET     /api/notifications/active           # Get active notifications (UNREAD/READ)
GET     /api/notifications/count            # Get unread count
PATCH   /api/notifications/{id}/read        # Mark as read
POST    /api/notifications/{id}/action      # Take action (DONE/LEFT/SKIPPED)
PUT     /api/notifications/settings         # Update notification settings
DELETE  /api/notifications/clear            # Clear old notifications
POST    /api/notifications/trigger-check    # Manual trigger (DEMO MODE)
```

### Recurring Expense Approval
```
POST    /api/recurring-expenses/{id}/approve?approved={true|false}
        # Approve (true) → creates expense
        # Reject (false) → skips expense, keeps for next cycle
POST    /api/recurring-expenses/process-now # Manual trigger check (DEMO MODE)
```

---

## Demo Setup & Testing

### Quick Setup (5 minutes)

#### Backend Setup
1. Models and repositories auto-create via Hibernate
2. No manual SQL needed - just run the application
3. Existing database will be updated automatically

#### Frontend Setup
1. All new components are already in place
2. Routes automatically added to navigation
3. API integration ready

### Demo Walkthrough (10 minutes)

**Step 1: Create Recurring Expense**
- Go to "Recurring" tab
- Click "Add Recurring"
- Create a test payment (e.g., Netflix $15.99 Monthly)
- Set start date to today
- Click "Add"

**Step 2: Configure Notification Time (Optional)**
- Go to "Notifications" tab
- Click Settings icon
- Set time to current time (e.g., if it's 10:30 AM, set 10:30)
- Click "Save Settings"

**Step 3: Trigger Notification (Demo Mode)**
- Still in "Notifications" tab
- Click "Trigger Check (Demo)" button
- System will immediately create a notification for the due payment

**Step 4: Take Action**
- See the notification appear in "Pending Actions" section
- Click "Payment Done" to record the payment as done
- OR click "Left for Later" to skip this cycle

**Step 5: View History**
- Processed notifications appear in "History" section
- Shows status: DONE, LEFT, SKIPPED, etc.

**Step 6: View Transaction (if "Payment Done" was clicked)**
- Go to "Expenses" tab
- See the new expense created from the recurring payment
- Shows "(Recurring)" tag

---

## Notification Status Flow

```
┌─────────────────────────────────────┐
│  System creates notification        │
│  Status: UNREAD                     │
└────────────┬────────────────────────┘
             │
             ├──→ User marks as read
             │     Status: READ
             │
             └──→ User takes action
                  ├─ "Payment Done"    → Status: DONE    → Expense created
                  ├─ "Left for Later"  → Status: LEFT    → Expense skipped
                  └─ "Skip/Later"      → Status: SKIPPED → Expense skipped
```

---

## Key Features for Demonstration

### 1. **Customizable Notification Time**
- Users can set any time (e.g., 9:00 AM, 5:30 PM)
- System triggers notifications at exactly that time
- Great for showing scheduled behavior

### 2. **Demo Mode Trigger Button**
- "Trigger Check (Demo)" button for instant demonstration
- No need to wait for scheduled times
- Perfect for presentations

### 3. **Action Tracking**
- Shows when and what action was taken
- Displays user decision (Done/Left)
- Full audit trail

### 4. **Visual Status Indicators**
- Color-coded badges for each status
- Easy to understand at a glance
- Icons for quick recognition

### 5. **Two-Way Integration**
- Notifications linked to recurring expenses
- Payment actions reflect in transactions
- Complete audit trail maintained

---

## Scheduled vs Manual Processing

### Automatic Scheduling (Production)
- Runs daily at midnight (configurable)
- Creates notifications for all due payments
- Process: `RecurringExpenseService.processRecurringExpenses()`

### Manual Demo Trigger (Testing)
- "Trigger Check (Demo)" button for immediate results
- Useful for presentations and testing
- Endpoint: `POST /api/notifications/trigger-check`

### Approval Processing
- Triggered when user clicks action button
- Creates expense if "Done"
- Updates next due date regardless
- Process: `RecurringExpenseService.processRecurringExpenseApproval()`

---

## File Structure

### Backend Files Created
```
src/main/java/com/fintrack/
├── model/
│   ├── Notification.java              (NEW)
│   ├── NotificationStatus.java        (NEW)
│   └── NotificationType.java          (NEW)
├── dto/
│   ├── NotificationResponse.java      (NEW)
│   ├── NotificationActionRequest.java (NEW)
│   └── NotificationSettingsRequest.java (NEW)
├── repository/
│   └── NotificationRepository.java    (NEW)
├── service/
│   └── NotificationService.java       (NEW)
└── controller/
    └── NotificationController.java    (NEW)
```

### Frontend Files Created
```
src/
├── pages/
│   └── Notifications.tsx              (NEW)
└── services/
    └── api.ts                         (UPDATED - added notificationApi)
```

### Updated Files
```
Backend:
- model/User.java                      (Added notification settings fields)
- service/RecurringExpenseService.java (Integrated notifications)
- controller/RecurringExpenseController.java (Added approve endpoint)

Frontend:
- components/Layout.tsx                (Added Notifications navigation)
- App.tsx                              (Added Notifications route)
```

---

## Environment Configuration

### Notification Scheduling (Optional)
The system uses Spring's `@Scheduled` annotation. To customize:

**In RecurringExpenseService:**
```java
// Current: runs daily at midnight
@Scheduled(cron = "0 0 0 * * ?")
public void processRecurringExpenses() { ... }

// Example: run every 5 minutes for testing
@Scheduled(fixedRate = 300000)
```

**In NotificationService:**
```java
// Current: checks every 5 minutes
@Scheduled(fixedRate = 300000)
public void checkAndCreateNotifications() { ... }
```

---

## Troubleshooting

### Q: Notifications not appearing?
A: 
1. Check user's notification time is set
2. Click "Trigger Check (Demo)" to manually create
3. Verify recurring expense has nextDueDate ≤ today

### Q: Expense not created after "Payment Done"?
A:
1. Check Expenses tab for "(Recurring)" tag
2. Refresh the page to see latest data
3. Check browser console for API errors

### Q: Notification time not working?
A:
1. Set time in Notification Settings
2. Use "Trigger Check (Demo)" for immediate testing
3. Check backend logs for scheduling details

### Q: Can't find Notifications tab?
A:
1. Ensure you're logged in
2. Clear browser cache
3. Check that both frontend and backend are running

---

## Testing Checklist

- [ ] Create a recurring expense
- [ ] Set notification time
- [ ] Trigger notification via demo button
- [ ] Click "Payment Done" on notification
- [ ] Verify expense created in Expenses tab
- [ ] Try "Left for Later" action
- [ ] Verify next cycle still pending
- [ ] Check notification history
- [ ] Test notification settings update
- [ ] Clear old notifications

---

## Future Enhancements

1. **Email Notifications** - Send emails in addition to in-app
2. **Push Notifications** - Browser/mobile push alerts
3. **Smart Filtering** - Filter by category, status, etc.
4. **Bulk Actions** - Mark multiple as done at once
5. **Notification Templates** - Customizable message formats
6. **Snooze Feature** - Delay notification by X days
7. **Notification Rules** - Smart routing based on user preferences

---

## Summary

The IN-APP Notification System provides:
✅ User-friendly inbox for payment reminders  
✅ Customizable notification times for demonstrations  
✅ Complete action tracking and audit trail  
✅ Demo mode for easy presentations  
✅ Integration with transaction system  
✅ Status-based workflow management  

Perfect for demonstrating recurring payment management in FinTrack Pro!
