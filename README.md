# FinTrack Pro

A smart expense and recurring payment manager with anomaly detection.

## Features

- **User Authentication**: Secure JWT-based authentication
- **Expense Tracking**: Track daily expenses with categories
- **Recurring Expenses**: Manage subscriptions and regular payments
- **Anomaly Detection**: Automatic detection of unusual spending
- **Dashboard**: Visual insights with charts and comparisons
- **Category Management**: Pre-built categories with icons

## Tech Stack

### Backend
- Java 17
- Spring Boot 3.2.0
- Spring Security with JWT
- Spring Data JPA
- MySQL Database

### Frontend
- React 18 with TypeScript
- Vite
- TailwindCSS
- Recharts for visualizations
- React Router v6

## Getting Started

### Prerequisites
- Java 17+
- Node.js 18+
- MySQL 8.0+

### Database Setup

1. Create a MySQL database:
```sql
CREATE DATABASE fintrack_db;
```

2. Update `backend/src/main/resources/application.properties` with your MySQL credentials.

### Backend Setup

```bash
cd backend
./mvnw spring-boot:run
```

The backend will start at http://localhost:8080

### Frontend Setup

```bash
cd frontend
npm install
npm run dev
```

The frontend will start at http://localhost:5173

## API Endpoints

### Authentication
- `POST /api/auth/register` - Register new user
- `POST /api/auth/login` - Login user
- `GET /api/auth/me` - Get current user

### Expenses
- `GET /api/expenses` - Get all expenses
- `POST /api/expenses` - Create expense
- `PUT /api/expenses/:id` - Update expense
- `DELETE /api/expenses/:id` - Delete expense
- `GET /api/expenses/anomalies` - Get anomaly expenses

### Recurring Expenses
- `GET /api/recurring-expenses` - Get all recurring expenses
- `POST /api/recurring-expenses` - Create recurring expense
- `PUT /api/recurring-expenses/:id` - Update recurring expense
- `DELETE /api/recurring-expenses/:id` - Delete recurring expense

### Dashboard
- `GET /api/dashboard/summary` - Get monthly summary
- `GET /api/dashboard/comparison` - Get month comparison
- `GET /api/dashboard/stats` - Get dashboard statistics

### Categories
- `GET /api/categories` - Get all categories

## Project Structure

```
fintech/
├── backend/
│   ├── src/
│   │   └── main/
│   │       ├── java/com/fintrack/
│   │       │   ├── config/
│   │       │   ├── controller/
│   │       │   ├── dto/
│   │       │   ├── model/
│   │       │   ├── repository/
│   │       │   ├── security/
│   │       │   └── service/
│   │       └── resources/
│   └── pom.xml
└── frontend/
    ├── src/
    │   ├── components/
    │   ├── context/
    │   ├── pages/
    │   └── services/
    ├── package.json
    └── vite.config.ts
```

## License

MIT
