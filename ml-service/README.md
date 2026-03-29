# ML Anomaly Detection Service

Real ML-based anomaly detection for FinTrack Pro using ensemble methods:
- **Isolation Forest**: Global pattern detection (all users)
- **Autoencoder**: Personal pattern detection (per-user)
- **LSTM**: Temporal sequence detection

## Quick Start

### 1. Install Dependencies
```bash
cd ml-service
pip install -r requirements.txt
```

### 2. Train Models
```bash
# Train from database (requires expenses data)
python train.py

# OR train from CSV file
python train.py --csv your_data.csv
```

### 3. Run Service
```bash
python app.py
```

Service runs at `http://localhost:5001`

## API Endpoints

### Health Check
```
GET /health
```

### Predict Anomaly
```
POST /predict
Content-Type: application/json

{
    "user_id": 1,
    "amount": 5000.0,
    "category": "Food & Dining",
    "expense_date": "2026-03-29",
    "payment_mode": "UPI"
}
```

Response:
```json
{
    "is_anomaly": true,
    "anomaly_score": 0.72,
    "anomaly_type": "ML_PERSONAL_DEVIATION",
    "severity": "HIGH",
    "explanation": "Amount is 4.2x your average spending",
    "model_scores": {
        "isolation_forest": 0.45,
        "autoencoder": 0.82,
        "lstm": 0.31
    }
}
```

### Trigger Training
```
POST /train
Content-Type: application/json

{"user_id": 1}  // Optional: train specific user only
```

### User Statistics
```
GET /user/1/stats
```

### Model Status
```
GET /models/status
```

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    ENSEMBLE DETECTOR                         │
├─────────────────────────────────────────────────────────────┤
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │  Isolation   │  │  Autoencoder │  │     LSTM     │      │
│  │    Forest    │  │  (Personal)  │  │  (Temporal)  │      │
│  │   (Global)   │  │              │  │              │      │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘      │
│         │                 │                 │               │
│         └────────────┬────┴─────────────────┘               │
│                      ▼                                       │
│            Dynamic Weighted Ensemble                         │
│         (weights based on user history)                      │
│                      │                                       │
│                      ▼                                       │
│               Final Anomaly Score                            │
└─────────────────────────────────────────────────────────────┘
```

## Files

- `app.py` - Flask REST API
- `train.py` - Model training script
- `detectors.py` - IF, AE, LSTM detector classes
- `ensemble.py` - Weighted ensemble combiner
- `feature_engineering.py` - Feature extraction
- `config.py` - Configuration
- `models/` - Saved trained models

## Integration with Spring Boot

The Spring Boot backend calls this service at `http://localhost:5001/predict` 
when creating new expenses. If the ML service is unavailable, it falls back 
to rule-based detection.
