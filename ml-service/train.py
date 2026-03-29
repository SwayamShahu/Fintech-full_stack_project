"""
Training Script for Anomaly Detection Models
Trains models using data from MySQL database.
"""
import os
import sys
import numpy as np
import pandas as pd
import mysql.connector
from datetime import datetime

from config import Config
from feature_engineering import FeatureEngineer
from detectors import IsolationForestDetector, AutoencoderDetector, LSTMDetector

# Suppress TensorFlow warnings
os.environ['TF_CPP_MIN_LOG_LEVEL'] = '2'


def get_db_connection():
    """Connect to MySQL database"""
    return mysql.connector.connect(
        host=Config.DB_HOST,
        port=Config.DB_PORT,
        database=Config.DB_NAME,
        user=Config.DB_USER,
        password=Config.DB_PASSWORD
    )


def load_data_from_db():
    """Load expense data from MySQL"""
    print("📊 Loading data from database...")
    
    conn = get_db_connection()
    
    query = """
    SELECT 
        e.id,
        e.user_id,
        e.amount,
        c.name as category,
        e.expense_date as date,
        e.payment_mode,
        e.is_anomaly
    FROM expenses e
    JOIN categories c ON e.category_id = c.id
    ORDER BY e.user_id, e.expense_date
    """
    
    df = pd.read_sql(query, conn)
    conn.close()
    
    print(f"✅ Loaded {len(df)} transactions from {df['user_id'].nunique()} users")
    return df


def train_models(df=None):
    """
    Train all anomaly detection models.
    
    Args:
        df: Optional DataFrame. If None, loads from database.
    """
    if df is None:
        df = load_data_from_db()
    
    if len(df) < Config.MIN_GLOBAL_TRANSACTIONS:
        print(f"⚠️ Not enough data to train ({len(df)} < {Config.MIN_GLOBAL_TRANSACTIONS})")
        print("   Models will use fallback rule-based detection")
        return False
    
    # Ensure model directory exists
    os.makedirs(Config.MODEL_DIR, exist_ok=True)
    
    # 1. Feature Engineering
    print("\n🔧 Feature Engineering...")
    feature_engineer = FeatureEngineer()
    feature_engineer.fit(df)
    
    # Transform all data
    X_all = feature_engineer.transform_batch(df)
    print(f"   Generated {X_all.shape[1]} features for {len(X_all)} transactions")
    
    # Save feature engineer
    feature_engineer.save(os.path.join(Config.MODEL_DIR, 'feature_engineer.pkl'))
    print("   ✅ Saved feature_engineer.pkl")
    
    # 2. Isolation Forest (Global)
    print("\n🌲 Training Isolation Forest (Global Patterns)...")
    isolation_forest = IsolationForestDetector(contamination=0.05)
    isolation_forest.fit(X_all)
    isolation_forest.save(os.path.join(Config.MODEL_DIR, 'isolation_forest.pkl'))
    print(f"   ✅ Trained on {len(X_all)} transactions")
    
    # 3. LSTM (Temporal - All Users)
    print("\n⏱️ Training LSTM (Temporal Patterns)...")
    lstm = LSTMDetector(n_features=X_all.shape[1], seq_len=5)
    lstm.fit(X_all, epochs=30, verbose=0)
    lstm.save(os.path.join(Config.MODEL_DIR, 'lstm.pkl'))
    print(f"   ✅ Learned temporal patterns from all users")
    
    # 4. Autoencoders (Per-User Personal Models)
    print("\n🧠 Training Autoencoders (Personal Patterns)...")
    user_models_trained = 0
    
    for user_id in df['user_id'].unique():
        user_df = df[df['user_id'] == user_id]
        
        if len(user_df) < Config.MIN_USER_TRANSACTIONS:
            continue
        
        # Get user's features
        X_user = feature_engineer.transform_batch(user_df)
        
        # Train autoencoder for this user
        ae = AutoencoderDetector(input_dim=X_user.shape[1])
        ae.fit(X_user, epochs=100, verbose=0)
        
        if ae.is_fitted:
            ae.save(os.path.join(Config.MODEL_DIR, f'autoencoder_user_{user_id}.pkl'))
            user_models_trained += 1
    
    print(f"   ✅ Trained {user_models_trained} personal models")
    
    # Save training metadata
    metadata = {
        'trained_at': datetime.now().isoformat(),
        'total_transactions': len(df),
        'total_users': df['user_id'].nunique(),
        'user_models_trained': user_models_trained,
        'feature_count': X_all.shape[1]
    }
    
    import json
    with open(os.path.join(Config.MODEL_DIR, 'training_metadata.json'), 'w') as f:
        json.dump(metadata, f, indent=2)
    
    print("\n" + "="*50)
    print("🎉 TRAINING COMPLETE!")
    print("="*50)
    print(f"   📊 Transactions processed: {len(df)}")
    print(f"   👥 Users: {df['user_id'].nunique()}")
    print(f"   🧠 Personal models: {user_models_trained}")
    print(f"   📁 Models saved to: {Config.MODEL_DIR}")
    
    return True


def train_user_model(user_id, df=None):
    """
    Train/retrain autoencoder for a specific user.
    Called when user gets enough transaction history.
    """
    print(f"\n🧠 Training personal model for user {user_id}...")
    
    # Load feature engineer
    fe_path = os.path.join(Config.MODEL_DIR, 'feature_engineer.pkl')
    feature_engineer = FeatureEngineer.load(fe_path)
    
    if feature_engineer is None:
        print("   ⚠️ Feature engineer not found. Run full training first.")
        return False
    
    if df is None:
        df = load_data_from_db()
    
    user_df = df[df['user_id'] == user_id]
    
    if len(user_df) < Config.MIN_USER_TRANSACTIONS:
        print(f"   ⚠️ Not enough data ({len(user_df)} transactions)")
        return False
    
    # Update user stats
    feature_engineer.update_user_stats(user_id, user_df)
    feature_engineer.save(fe_path)
    
    # Transform and train
    X_user = feature_engineer.transform_batch(user_df)
    ae = AutoencoderDetector(input_dim=X_user.shape[1])
    ae.fit(X_user, epochs=100, verbose=0)
    
    if ae.is_fitted:
        ae.save(os.path.join(Config.MODEL_DIR, f'autoencoder_user_{user_id}.pkl'))
        print(f"   ✅ Model saved for user {user_id}")
        return True
    
    return False


if __name__ == '__main__':
    import argparse
    
    parser = argparse.ArgumentParser(description='Train anomaly detection models')
    parser.add_argument('--user', type=int, help='Train model for specific user only')
    parser.add_argument('--csv', type=str, help='Train from CSV file instead of database')
    
    args = parser.parse_args()
    
    if args.csv:
        print(f"📂 Loading from CSV: {args.csv}")
        df = pd.read_csv(args.csv)
        # Rename columns if needed
        if 'expense_date' in df.columns and 'date' not in df.columns:
            df['date'] = df['expense_date']
    else:
        df = None
    
    if args.user:
        train_user_model(args.user, df)
    else:
        train_models(df)
