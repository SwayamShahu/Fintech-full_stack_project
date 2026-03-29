# FinTrack Pro

A smart expense and recurring payment manager with **ML-powered anomaly detection**.

## Features

- **User Authentication**: Secure JWT-based authentication
- **Expense Tracking**: Track daily expenses with categories
- **Recurring Expenses**: Manage subscriptions and regular payments
- **🤖 ML Anomaly Detection**: Ensemble ML models for intelligent anomaly detection
  - Isolation Forest (global patterns)
  - Autoencoder (personal spending patterns)
  - LSTM (temporal sequence patterns)
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

### ML Service (Optional)
- Python 3.10+
- TensorFlow/Keras
- scikit-learn
- Flask REST API

## Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                         FINTRACK PRO                             │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│   Frontend (React :5173)                                         │
│        ↓                                                         │
│   Spring Boot Backend (:8080)                                    │
│        ↓                                                         │
│   ┌──────────────────────────────────────┐                      │
│   │  Anomaly Detection:                   │                      │
│   │  1. ML Service (if available)         │                      │
│   │  2. Rule-based fallback               │                      │
│   └──────────────────────────────────────┘                      │
│        ↓                                                         │
│   Python ML Service (:5001) - Optional                          │
│   • Isolation Forest (global)                                   │
│   • Autoencoder (personal)                                      │
│   • LSTM (temporal)                                             │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

## Getting Started

### Prerequisites
- Java 17+
- Node.js 18+
- MySQL 8.0+
- Python 3.10+ (optional, for ML service)

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

### ML Service Setup (Optional)

The ML service provides advanced anomaly detection. Without it, the system uses rule-based detection.

```bash
cd ml-service
pip install -r requirements.txt

# Train models (requires expense data in database)
python train.py

# OR train on Kaggle using train_kaggle.ipynb

# Start the service
python app.py
```

The ML service will start at http://localhost:5001

## Anomaly Detection

### Rule-Based (Fallback)
- HIGH_COST: Amount > 2.5× average
- CATEGORY_SPIKE: Amount > 2× category average
- HIGH_FREQUENCY: Multiple same-category expenses/day
- POTENTIAL_DUPLICATE: Similar amounts same day
- WEEKLY_FREQUENCY_SPIKE: Too many in 7 days
- UNUSUAL_CATEGORY: Rare category usage

### ML-Based (When ML Service Running)
- ML_GLOBAL_PATTERN: Unusual vs all users (Isolation Forest)
- ML_PERSONAL_DEVIATION: Unusual for this user (Autoencoder)
- ML_TEMPORAL_PATTERN: Unusual sequence (LSTM)

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
- `GET /api/expenses/ml-status` - Get ML service status

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
│   │       │       ├── ExpenseService.java
│   │       │       └── MLAnomalyService.java  ← ML Integration
│   │       └── resources/
│   └── pom.xml
├── frontend/
│   ├── src/
│   │   ├── components/
│   │   ├── context/
│   │   ├── pages/
│   │   └── services/
│   ├── package.json
│   └── vite.config.ts
└── ml-service/                    ← ML Microservice
    ├── app.py                     ← Flask API
    ├── train.py                   ← Training script
    ├── train_kaggle.ipynb         ← Kaggle notebook
    ├── detectors.py               ← IF, AE, LSTM
    ├── ensemble.py                ← Weighted ensemble
    ├── feature_engineering.py     ← Feature extraction
    ├── config.py                  ← Configuration
    ├── requirements.txt
    └── models/                    ← Trained models
```

## License

MIT
