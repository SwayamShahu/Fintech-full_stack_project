# QUICK START - IN-APP NOTIFICATION SYSTEM

## 🚀 Get Running in 5 Minutes

### Step 1: Start Backend (2 min)
```bash
cd backend
mvn clean install
mvn spring-boot:run
```
✅ Tables auto-created by Hibernate  
✅ No manual SQL needed  

### Step 2: Start Frontend (2 min)
```bash
cd frontend  
npm install
npm start
```
✅ Opens http://localhost:3000  
✅ Application ready  

### Step 3: Login & Test (1 min)
```
1. Register: http://localhost:3000/register
   - Email: test@example.com
   - Password: Test123!
   - Name: Test User

2. Login: http://localhost:3000/login
   - Use above credentials

3. You're in!
```

---

## 📲 3-Step Demo (2 minutes)

### Create Recurring Expense
```
1. Click "Recurring" in left sidebar
2. Click "+ Add Recurring"
3. Fill form:
   - Amount: 15.99
   - Category: Entertainment
   - Description: Netflix
   - Frequency: Monthly
   - Start Date: Today
4. Click "Add Recurring"
✅ Expense created
```

### Trigger Notification
```
1. Click "Notifications" in sidebar
2. Click "Trigger Check (Demo)" button
3. Wait 1 second...
✅ Notification appears in "Pending Actions"
```

### Take Action & See Result
```
1. Click "Payment Done" on notification
2. See success message
3. Click "Expenses" tab
4. See new transaction: "Netflix (Recurring)"
✅ Complete!
```

---

## ⚙️ Configuration (Optional)

### Set Notification Time
```
1. Go to "Notifications" tab
2. Click Settings icon (gear)
3. Set time: Hour (0-23), Minute (0-59)
   Example: 14:30 = 2:30 PM
4. Click "Save Settings"
```

### Explore Features
- **Unread Count**: Badge showing pending notifications
- **History**: View all processed notifications
- **Status Tracking**: See who did what and when
- **Clear**: Remove old notifications

---

## 🔍 Check It's Working

### Backend Running?
```
http://localhost:8080/api/notifications
→ Should show empty list (or existing notifications)
```

### Frontend Running?
```
http://localhost:3000
→ Should see login page
```

### Database Connected?
```
Backend logs should show:
"Hibernate: SELECT..." messages
```

---

## 🎯 What You Can Test

- [x] Create recurring expenses
- [x] Generate notifications on demand
- [x] Mark payments as "Done"
- [x] Mark payments as "Left for Later"
- [x] See payment history
- [x] Configure notification times
- [x] View generated transactions

---

## 📋 Common Tasks

### I want to test with different payment amounts
```
Create multiple recurring expenses with different amounts
Each will generate its own notification
```

### I want to test different frequencies
```
Create expenses with:
- DAILY (test next day)
- WEEKLY (test next week)
- MONTHLY (default, test next month)
- YEARLY (test next year)
```

### I want to see the auto-scheduling in action
```
Set notification time to current time + 5 minutes
Wait - notification should appear automatically
(Runs every 5 minutes by default)
```

### I want to change when notifications run
```
Edit: backend/src/main/resources/application.properties
Add/modify:
spring.scheduling.pool.size=10
spring.task.scheduling.pool.size=10

Edit NotificationService.java:
Change @Scheduled(fixedRate = 300000) 
From 300000ms (5 min) to your preferred interval
```

---

## 🐛 Troubleshooting

### Notifications don't appear
**Solution:**
1. Refresh page (Ctrl+F5)
2. Check backend logs for errors
3. Verify recurring expense has nextDueDate ≤ today

### Can't find Notifications tab
**Solution:**
1. Clear browser cache
2. Logout and login again
3. Check that frontend built correctly

### Settings won't save
**Solution:**
1. Check browser console (F12)
2. Verify time values are 0-23 (hours) and 0-59 (minutes)
3. Check backend logs for errors

### Expense not showing up
**Solution:**
1. Refresh Expenses tab
2. Look for "(Recurring)" tag in description
3. Check it was marked as "Done" (not "Left for Later")

---

## 📁 Key Files Changed

### Backend
```
✓ model/Notification.java (NEW)
✓ model/NotificationStatus.java (NEW)
✓ model/NotificationType.java (NEW)
✓ model/User.java (UPDATED - notification fields)
✓ repository/NotificationRepository.java (NEW)
✓ service/NotificationService.java (NEW)
✓ controller/NotificationController.java (NEW)
✓ service/RecurringExpenseService.java (UPDATED)
✓ controller/RecurringExpenseController.java (UPDATED)
```

### Frontend
```
✓ pages/Notifications.tsx (NEW)
✓ services/api.ts (UPDATED - notificationApi)
✓ components/Layout.tsx (UPDATED - nav)
✓ App.tsx (UPDATED - route)
```

---

## 🌐 API Reference

### Get All Notifications
```
GET /api/notifications
Response: List of all user's notifications
```

### Get Unread Count
```
GET /api/notifications/count
Response: { unreadCount: 5 }
```

### Take Action on Notification
```
POST /api/notifications/{id}/action
Body: { "action": "DONE" }
Values: DONE | LEFT | SKIPPED
```

### Trigger Demo Check
```
POST /api/notifications/trigger-check
Response: Creates notifications for all due expenses
```

### Update Settings
```
PUT /api/notifications/settings
Body: {
  "notificationHourOfDay": 14,
  "notificationMinute": 30,
  "enableNotifications": true
}
```

---

## ✨ Features at a Glance

| Feature | How | Time |
|---------|-----|------|
| Create Notification | Recurring expense due → auto-create | Immediate |
| Demo Mode | Click "Trigger Check (Demo)" | Instant |
| Approve Payment | Click "Payment Done" → Creates expense | 1 sec |
| Defer Payment | Click "Left for Later" → Keeps pending | 1 sec |
| Set Time | Settings → Configure → Save | 30 sec |
| View History | Switch to History tab | Instant |

---

## 🎓 Learn More

For detailed information, see:
- `NOTIFICATION_SYSTEM_GUIDE.md` - Complete documentation
- `DEMO_SCRIPT.md` - Live demo script
- `IMPLEMENTATION_CHECKLIST.md` - Full implementation details

---

## 🚦 Status

✅ Backend: Ready  
✅ Frontend: Ready  
✅ Database: Auto-migrated  
✅ Demo: Simple & Fast  

**Ready to demonstrate!**

---

## 📞 Need Help?

1. Check backend logs: `tail -f backend.log`
2. Check browser console: F12 → Console
3. Verify MySQL is running: `mysql -u root -p`
4. Clear browser cache: Ctrl+Shift+Delete
5. Restart both services

---

**Let's demo! 🎉**
