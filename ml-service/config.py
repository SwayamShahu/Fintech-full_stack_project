"""
Configuration for ML Anomaly Detection Service
"""
import os
from dotenv import load_dotenv

load_dotenv()

class Config:
    # Flask
    DEBUG = os.getenv('DEBUG', 'False').lower() == 'true'
    PORT = int(os.getenv('ML_SERVICE_PORT', 5001))
    
    # Database (same as Spring Boot backend)
    DB_HOST = os.getenv('DB_HOST', 'localhost')
    DB_PORT = int(os.getenv('DB_PORT', 3306))
    DB_NAME = os.getenv('DB_NAME', 'fintrack_db')
    DB_USER = os.getenv('DB_USER', 'root')
    DB_PASSWORD = os.getenv('DB_PASSWORD', 'swayam2004')
    
    # Model paths
    MODEL_DIR = os.path.join(os.path.dirname(__file__), 'models')
    
    # Feature columns (must match training - includes location)
    FEATURE_COLS = [
        'amount', 'category_encoded', 'day_of_week', 'day_of_month',
        'month', 'is_weekend', 'payment_mode_encoded', 'location_encoded',
        'amount_zscore', 'amount_to_median', 'cat_zscore'
    ]
    
    # Anomaly detection thresholds
    ANOMALY_THRESHOLD = 0.35
    HIGH_ANOMALY_THRESHOLD = 0.6
    MODERATE_ANOMALY_THRESHOLD = 0.4
    
    # Minimum data requirements
    MIN_USER_TRANSACTIONS = 5
    MIN_GLOBAL_TRANSACTIONS = 50
