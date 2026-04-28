# SYSTEM ARCHITECTURE & FLOW DIAGRAM

## 🏗️ Architecture Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                     FINTECH PRO APPLICATION                     │
└─────────────────────────────────────────────────────────────────┘

┌────────────────────────────┐         ┌──────────────────────────┐
│      FRONTEND (React)      │◄───────►│   BACKEND (Spring)       │
│  ┌──────────────────────┐  │         │ ┌────────────────────────┐
│  │ Notifications.tsx    │  │         │ │ NotificationController │
│  │ - Inbox UI           │  │         │ │ - REST API Endpoints   │
│  │ - Action Buttons     │  │         │ │ - Error Handling       │
│  │ - Settings Modal     │  │         │ │ - Auth Validation      │
│  │ - History View       │  │         │ │                        │
│  └──────────────────────┘  │         │ ├────────────────────────┤
│  ┌──────────────────────┐  │         │ │ NotificationService    │
│  │ api.ts               │  │         │ │ - Create Notifications │
│  │ - notificationApi    │  │         │ │ - Handle Actions       │
│  │ - HTTP Requests      │  │         │ │ - Update Settings      │
│  │ - JWT Auth Token     │  │         │ │ - Scheduler (5 min)    │
│  └──────────────────────┘  │         │ │                        │
│  ┌──────────────────────┐  │         │ ├────────────────────────┤
│  │ Layout.tsx           │  │         │ │ RecurringExpenseService│
│  │ - Navigation Updated │  │         │ │ - Process Approvals    │
│  │ - Notifications Tab  │  │         │ │ - Calculate Due Dates  │
│  └──────────────────────┘  │         │ │ - Create Expenses      │
│                            │         │ └────────────────────────┘
└────────────────────────────┘         │ ┌────────────────────────┐
                                       │ │ NotificationRepository │
                                       │ │ - Query Notifications  │
                                       │ │ - Update Status        │
                                       │ │ - Count Unread         │
                                       │ └────────────────────────┘
                                       └──────────────────────────┘
                                              ▲
                                              │
                                       ┌──────┴──────┐
                                       │   MySQL    │
                                       │ notifications table
                                       │ (Auto-created)
                                       └────────────┘
```

---

## 📊 Data Flow Diagram

### Creating a Notification

```
┌────────────────┐
│ Recurring      │
│ Expense        │
│ Created        │
└────────┬───────┘
         │
         ▼
┌────────────────────────────────────┐
│ Scheduler runs (every 5 minutes)   │
│ OR                                 │
│ Admin triggers "/trigger-check"    │
└────────┬───────────────────────────┘
         │
         ▼
┌────────────────────────────────────┐
│ Check due recurring expenses       │
│ WHERE nextDueDate ≤ today          │
│ AND isActive = true                │
└────────┬───────────────────────────┘
         │
         ▼
┌────────────────────────────────────┐
│ For each due expense:              │
│ 1. Create Notification record      │
│ 2. Set status = UNREAD             │
│ 3. Set notificationTime =          │
│    today + user's preferred time   │
│ 4. Save to database                │
└────────┬───────────────────────────┘
         │
         ▼
┌────────────────────────────────────┐
│ Frontend polling detects new       │
│ notification via GET /notifications│
└────────┬───────────────────────────┘
         │
         ▼
┌────────────────────────────────────┐
│ User sees notification in:         │
│ "Pending Actions" section          │
│ Status: UNREAD (blue badge)        │
└────────────────────────────────────┘
```

### Processing a Notification (User Action)

```
┌─────────────────────┐
│ User sees           │
│ notification        │
└────────┬────────────┘
         │
         ├─────────────────────────────┬──────────────────────────┐
         │                             │                          │
         ▼                             ▼                          ▼
    ┌─────────┐                  ┌──────────┐            ┌────────────┐
    │ Clicks  │                  │ Clicks   │            │ Clicks     │
    │ "Done"  │                  │ "Left"   │            │ "Mark Read"│
    │         │                  │ for Later│            │ (optional) │
    └────┬────┘                  └─────┬────┘            └────┬───────┘
         │                             │                      │
         ▼                             ▼                      ▼
    ┌──────────────────────────┐  ┌──────────┐         ┌───────────┐
    │ POST /notifications/     │  │ POST     │         │ PATCH /   │
    │ {id}/action              │  │ /notif   │         │ notif/{id}│
    │ { action: "DONE" }       │  │ /{id}/   │         │ /read     │
    │                          │  │ action   │         │           │
    │ { action: "LEFT" }       │  │ {        │         └─────┬─────┘
    └────────┬─────────────────┘  │ action:  │               │
             │                    │ "LEFT"   │               ▼
             │                    │ }        │         Status: READ
             │                    └────┬─────┘         (gray badge)
             │                         │
             ▼                         ▼
    ┌──────────────────────┐  ┌──────────────┐
    │ Backend:             │  │ Backend:     │
    │ 1. Update status to  │  │ 1. Update    │
    │    "DONE"            │  │    status to │
    │ 2. Record action_    │  │    "LEFT"    │
    │    taken = now()     │  │ 2. Record    │
    │ 3. Call            │  │    action     │
    │    processRecurring │  │    _taken     │
    │    ExpenseApproval( │  │ 3. Update    │
    │      true)          │  │    next due  │
    │ 4. Create Expense   │  │    date      │
    │    entry            │  │ 4. No Expense│
    │ 5. Update next due  │  │    created   │
    │    date             │  └──────┬───────┘
    │                              │
    └────────────┬────────────────┘
                 │
                 ▼
    ┌────────────────────────────────┐
    │ Notification moved to:         │
    │ "History" section              │
    │ Status: DONE / LEFT (colored)  │
    │                                │
    │ If DONE:                       │
    │ ✓ Expense visible in tab       │
    │ ✓ Shows "(Recurring)" tag      │
    │ ✓ Amount recorded              │
    │                                │
    │ If LEFT:                       │
    │ ✓ No expense created           │
    │ ✓ Pending for next cycle       │
    └────────────────────────────────┘
```

---

## 🔄 Status Transitions

```
    ┌─────────────────────────────────────────────────────┐
    │                 Notification Created                │
    │                   Status: UNREAD                    │
    └──────────────────────────┬──────────────────────────┘
                               │
              ┌────────────────┼────────────────┐
              │                │                │
              ▼                ▼                ▼
         ┌─────────┐      ┌──────────┐    ┌──────────┐
         │ Mark    │      │ Take     │    │  Wait   │
         │ as Read │      │ Action   │    │ (Expire)│
         └────┬────┘      └────┬─────┘    └────┬────┘
              │                │              │
              ▼                │              ▼
         ┌──────────┐          │         ┌──────────┐
         │ Status:  │          │         │ Status:  │
         │ READ ──┐ │          │         │ EXPIRED  │
         └────────┼─┘          │         └──────────┘
                  │            │
    ┌─────────────┴────────────┴──────────────┐
    │         (User takes action)             │
    │                                         │
    ├──────────────┬────────────┬──────────────┤
    │              │            │              │
    ▼              ▼            ▼              ▼
┌────────┐  ┌───────────┐  ┌────────┐  ┌─────────┐
│ DONE   │  │ LEFT      │  │SKIPPED │  │ EXPIRED │
│(blue)  │  │(orange)   │  │(yellow)│  │ (gray) │
└────────┘  └───────────┘  └────────┘  └─────────┘
    │              │            │         │
    └──────┬───────┴────────────┴────────┬┘
           │                             │
           └────────┬───────────────────┘
                    │
                    ▼
         ┌────────────────────────┐
         │ Moved to History       │
         │ Visible in Inbox       │
         │ (Not deleted - audited)│
         └────────────────────────┘
```

---

## 🎯 Class Relationships

```
User
  ├─ has many Notifications (1:N)
  │   └─ Each has notificationHourOfDay, notificationMinute
  │
  └─ has many RecurringExpenses (1:N)
       └─ Each has many Notifications (1:N)

RecurringExpense
  ├─ belongs to User (N:1)
  ├─ belongs to Category (N:1)
  └─ has many Notifications (1:N)

Notification
  ├─ belongs to User (N:1)
  ├─ belongs to RecurringExpense (N:1)
  ├─ has status: UNREAD|READ|DONE|LEFT|SKIPPED|EXPIRED
  ├─ has type: PAYMENT_DUE|PAYMENT_OVERDUE|...
  └─ tracks: title, message, dueDate, actionTaken, actionTakenAt

Expense
  ├─ belongs to User (N:1)
  ├─ belongs to Category (N:1)
  ├─ created from RecurringExpense (via notification approval)
  └─ has isRecurring = true flag (marks source)
```

---

## 📱 UI Component Tree

```
App.tsx
├── Layout.tsx
│   ├── Sidebar Navigation (updated)
│   │   ├── Dashboard
│   │   ├── Expenses
│   │   ├── Recurring
│   │   └── ► Notifications (NEW)
│   │
│   └── Main Content Area (Outlet)
│       └── <Route>
│           └── Notifications.tsx (NEW)
│               ├── Header
│               │   ├── Title: "Notifications Inbox"
│               │   ├── Unread count badge
│               │   ├── "Trigger Check (Demo)" button
│               │   └── Settings button (gear icon)
│               │
│               ├── Settings Modal (conditionally rendered)
│               │   ├── Hour input (0-23)
│               │   ├── Minute input (0-59)
│               │   ├── Time display: "HH:MM"
│               │   └── Save/Cancel buttons
│               │
│               ├── Active Notifications Section
│               │   └── Notification Card (repeated)
│               │       ├── Category icon (with color)
│               │       ├── Title & Message
│               │       ├── Status badge
│               │       ├── Metadata (due date, amount, frequency)
│               │       ├── "Payment Done" button (green)
│               │       ├── "Left for Later" button (orange)
│               │       └── "Mark Read" button (optional)
│               │
│               ├── Empty State (if no notifications)
│               │   ├── Bell icon
│               │   └── "No notifications" message
│               │
│               └── History Section
│                   ├── "History" title
│                   ├── "Clear" button
│                   └── History Item (repeated)
│                       ├── Category icon
│                       ├── Description & amount
│                       ├── Status badge
│                       └── Date

API Service Layer (services/api.ts)
└── notificationApi object
    ├── getAll()
    ├── getUnread()
    ├── getActive()
    ├── getUnreadCount()
    ├── markAsRead(id)
    ├── takeAction(id, action)
    ├── updateSettings(settings)
    ├── clearAll()
    └── triggerCheck()
```

---

## 🔌 API Endpoint Map

```
BASE URL: http://localhost:8080/api/notifications

Retrieval Endpoints (GET)
├── /                              → All notifications
├── /unread                         → Only unread
├── /active                         → Active (UNREAD/READ)
└── /count                          → Unread count

Action Endpoints
├── PATCH /{id}/read                → Mark as read
├── POST /{id}/action               → Record user action
│   ├── Body: { action: "DONE" }
│   ├── Body: { action: "LEFT" }
│   └── Body: { action: "SKIPPED" }
├── PUT /settings                   → Update preferences
│   └── Body: { notificationHourOfDay, notificationMinute }
├── DELETE /clear                   → Clear old notifications
└── POST /trigger-check             → Demo: Create notifications

Related Endpoints
├── POST /recurring-expenses/{id}/approve?approved=true
├── POST /recurring-expenses/{id}/approve?approved=false
└── POST /recurring-expenses/process-now
```

---

## 💾 Database Schema (Visual)

```
users (already exists)
├── id (PK)
├── name
├── email
├── password
├── phone
├── notification_hour (NEW) ◄─────┐
├── notification_minute (NEW) ◄──┐ │
├── enable_notifications (NEW) ◄┐ │ │
├── created_at                  │ │ │
└── updated_at                  │ │ │
                               │ │ │
recurring_expenses             │ │ │
├── id (PK)                    │ │ │
├── user_id (FK) ──┐           │ │ │
├── amount         │           │ │ │
├── category_id    │           │ │ │
├── description    │           │ │ │
├── frequency      │           │ │ │
├── next_due_date  │           │ │ │
├── is_active      │           │ │ │
├── created_at     │           │ │ │
└── updated_at     │           │ │ │
                  │           │ │ │
notifications (NEW)            │ │ │
├── id (PK)       │           │ │ │
├── user_id (FK) ───┼───────────┘ │ │
├── recurring_expense_id (FK)┐    │ │
│   └─────────────────────────┴────┼─┤
├── title              │    │ │
├── message            │    │ │
├── status (ENUM)      │    │ │
├── type (ENUM)        │    │ │
├── due_date           │    │ │
├── notification_time  │    │ │
├── action_taken_at    │    │ │
├── action_taken       │    │ │
├── created_at         │    │ │
└── updated_at         │    │ │
                       │    │ │
expenses (already exists) ◄──┘ │
├── id (PK)          │
├── user_id (FK) ─────┼────────┘
├── amount
├── category_id
├── description
├── expense_date
├── is_recurring ◄────── (Links back to notification)
├── is_anomaly
├── created_at
└── updated_at
```

---

## 🎬 Interaction Sequence

```
User              Frontend          Backend          Database
 │                  │                 │                 │
 │ 1. View Notif.   │                 │                 │
 ├─────────────────►│                 │                 │
 │                  │ 2. GET /notif   │                 │
 │                  ├────────────────►│                 │
 │                  │                 │ 3. Query        │
 │                  │                 ├────────────────►│
 │                  │                 │◄────────────────┤
 │                  │ 4. JSON array   │                 │
 │◄─────────────────┤◄────────────────┤                 │
 │ 5. Display inbox │                 │                 │
 │                  │                 │                 │
 │ 6. Click "Done"  │                 │                 │
 ├─────────────────►│                 │                 │
 │                  │ 7. POST /notif/ │                 │
 │                  │    {id}/action  │                 │
 │                  ├────────────────►│                 │
 │                  │                 │ 8. Update       │
 │                  │                 ├────────────────►│
 │                  │                 │ 9. Create       │
 │                  │                 │    Expense      │
 │                  │                 ├────────────────►│
 │                  │ 10. Response OK │                 │
 │◄─────────────────┤◄────────────────┤                 │
 │ 11. Show success │                 │                 │
 │                  │ 12. Refresh     │                 │
 │                  ├────────────────►│                 │
 │                  │                 │ 13. Query       │
 │                  │                 ├────────────────►│
 │                  │ 14. Updated     │◄────────────────┤
 │◄─────────────────┤◄────────────────┤                 │
 │ 15. See History  │                 │                 │
 │                  │                 │                 │
```

---

## 🎯 Quick Navigation

```
Want to...                          See File...
─────────────────────────────────────────────────────────────
Understand the whole system         IMPLEMENTATION_SUMMARY.md
Set up and run quickly              QUICK_START.md
Demo to stakeholders               DEMO_SCRIPT.md
Learn all details                  NOTIFICATION_SYSTEM_GUIDE.md
Check implementation status        IMPLEMENTATION_CHECKLIST.md
View code for notifications        Notification.java
View API endpoints                 NotificationController.java
View business logic                NotificationService.java
View frontend UI                   Notifications.tsx
View API integration               api.ts (notificationApi)
```

---

**Diagram Version**: 1.0  
**Last Updated**: April 28, 2026  
**Status**: ✅ Complete & Ready
