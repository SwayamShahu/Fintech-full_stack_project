# =============================================================================
# FINTRACK PRO - ENSEMBLE ANOMALY DETECTION TRAINING
# =============================================================================
# Run this on Kaggle to train ML models for anomaly detection
# =============================================================================

# Install dependencies
# !pip install scikit-learn tensorflow lime joblib pandas numpy --quiet

import numpy as np
import pandas as pd
import matplotlib.pyplot as plt
import warnings
import os
import json
import joblib
from datetime import datetime

warnings.filterwarnings('ignore')

from sklearn.ensemble import IsolationForest
from sklearn.preprocessing import StandardScaler, LabelEncoder, MinMaxScaler
from sklearn.metrics import precision_score, recall_score, f1_score, roc_auc_score

import tensorflow as tf
from tensorflow.keras.models import Model, Sequential
from tensorflow.keras.layers import Input, Dense, LSTM, Dropout, RepeatVector, TimeDistributed

print("Libraries imported!")
print("TensorFlow version:", tf.__version__)

# =============================================================================
# CONFIGURATION
# =============================================================================
class Config:
    FEATURE_COLS = [
        'amount', 'category_encoded', 'day_of_week', 'day_of_month',
        'month', 'is_weekend', 'payment_mode_encoded', 'location_encoded',
        'amount_zscore', 'amount_to_median', 'cat_zscore'
    ]
    MIN_USER_TRANSACTIONS = 5
    MIN_GLOBAL_TRANSACTIONS = 50
    MODEL_DIR = './models'
    
os.makedirs(Config.MODEL_DIR, exist_ok=True)

# =============================================================================
# STEP 1: LOAD DATA
# =============================================================================
df = pd.read_csv('/kaggle/input/synthetic-indian-expenses/synthetic_indian_expenses.csv')

print("Dataset:", len(df), "transactions,", df['user_id'].nunique(), "users")
print("Anomalies:", df['is_anomaly'].sum(), "(", round(df['is_anomaly'].mean()*100, 1), "%)")
print("Columns:", list(df.columns))
print("Locations:", df['location'].nunique(), "unique")
print("Categories:", df['category'].unique().tolist())

# Preview
df.head(10)

# =============================================================================
# STEP 2: FEATURE ENGINEERING
# =============================================================================
class FeatureEngineer:
    def __init__(self):
        self.category_encoder = LabelEncoder()
        self.payment_mode_encoder = LabelEncoder()
        self.location_encoder = LabelEncoder()
        self.user_stats = {}
        self.category_stats = {}
        self.is_fitted = False
    
    def fit(self, df):
        df = df.copy()
        df['date'] = pd.to_datetime(df['date'])
        
        self.category_encoder.fit(df['category'].astype(str))
        self.payment_mode_encoder.fit(df['payment_mode'].astype(str))
        self.location_encoder.fit(df['location'].astype(str))
        
        for user_id, group in df.groupby('user_id'):
            self.user_stats[user_id] = {
                'mean': float(group['amount'].mean()),
                'std': float(group['amount'].std()) if len(group) > 1 else 1.0,
                'median': float(group['amount'].median()),
                'count': int(len(group))
            }
            if self.user_stats[user_id]['std'] == 0 or np.isnan(self.user_stats[user_id]['std']):
                self.user_stats[user_id]['std'] = 1.0
        
        for cat, group in df.groupby('category'):
            self.category_stats[cat] = {
                'mean': float(group['amount'].mean()),
                'std': float(group['amount'].std()) if len(group) > 1 else 1.0
            }
        
        self.is_fitted = True
        return self
    
    def transform_batch(self, df):
        df = df.copy()
        df['date'] = pd.to_datetime(df['date'])
        
        df['day_of_week'] = df['date'].dt.dayofweek
        df['day_of_month'] = df['date'].dt.day
        df['month'] = df['date'].dt.month
        df['is_weekend'] = df['day_of_week'].isin([5, 6]).astype(int)
        
        df['category_encoded'] = self.category_encoder.transform(df['category'].astype(str))
        df['payment_mode_encoded'] = self.payment_mode_encoder.transform(df['payment_mode'].astype(str))
        df['location_encoded'] = self.location_encoder.transform(df['location'].astype(str))
        
        df['user_mean'] = df['user_id'].map(lambda x: self.user_stats.get(x, {}).get('mean', df['amount'].mean()))
        df['user_std'] = df['user_id'].map(lambda x: self.user_stats.get(x, {}).get('std', 1.0))
        df['user_median'] = df['user_id'].map(lambda x: self.user_stats.get(x, {}).get('median', df['amount'].median()))
        
        df['amount_zscore'] = (df['amount'] - df['user_mean']) / df['user_std']
        df['amount_to_median'] = df['amount'] / (df['user_median'] + 1)
        
        df['cat_mean'] = df['category'].map(lambda x: self.category_stats.get(x, {}).get('mean', df['amount'].mean()))
        df['cat_std'] = df['category'].map(lambda x: self.category_stats.get(x, {}).get('std', 1.0))
        df['cat_zscore'] = (df['amount'] - df['cat_mean']) / (df['cat_std'] + 1)
        
        return df[Config.FEATURE_COLS].values
    
    def save(self, path):
        joblib.dump({
            'category_encoder': self.category_encoder,
            'payment_mode_encoder': self.payment_mode_encoder,
            'location_encoder': self.location_encoder,
            'user_stats': self.user_stats,
            'category_stats': self.category_stats,
            'is_fitted': self.is_fitted
        }, path)

print("\nFeature Engineering...")
feature_engineer = FeatureEngineer()
feature_engineer.fit(df)
X_all = feature_engineer.transform_batch(df)
y_all = df['is_anomaly'].values

print("Features:", X_all.shape[1], "dimensions")
feature_engineer.save(f'{Config.MODEL_DIR}/feature_engineer.pkl')
print("Saved feature_engineer.pkl")

# =============================================================================
# STEP 3: ISOLATION FOREST (GLOBAL)
# =============================================================================
class IsolationForestDetector:
    def __init__(self, contamination=0.05):
        self.model = IsolationForest(contamination=contamination, n_estimators=150, random_state=42)
        self.scaler = StandardScaler()
        self.train_min = None
        self.train_max = None
        self.is_fitted = False
    
    def fit(self, X):
        X_scaled = self.scaler.fit_transform(X)
        self.model.fit(X_scaled)
        scores = self.model.decision_function(X_scaled)
        self.train_min = scores.min()
        self.train_max = scores.max()
        self.is_fitted = True
        return self
    
    def predict_score(self, X):
        X_scaled = self.scaler.transform(X)
        scores = self.model.decision_function(X_scaled)
        normalized = 1 - (scores - self.train_min) / (self.train_max - self.train_min + 1e-10)
        return np.clip(normalized, 0, 1)
    
    def save(self, path):
        joblib.dump({
            'model': self.model,
            'scaler': self.scaler,
            'train_min': self.train_min,
            'train_max': self.train_max,
            'is_fitted': self.is_fitted
        }, path)

print("\nTraining Isolation Forest (Global Patterns)...")
isolation_forest = IsolationForestDetector(contamination=0.05)
isolation_forest.fit(X_all)
isolation_forest.save(f'{Config.MODEL_DIR}/isolation_forest.pkl')
print("Trained on", len(X_all), "transactions from", df['user_id'].nunique(), "users")

# =============================================================================
# STEP 4: LSTM (TEMPORAL)
# =============================================================================
class LSTMDetector:
    def __init__(self, n_features, seq_len=5):
        self.seq_len = seq_len
        self.n_features = n_features
        self.scaler = MinMaxScaler()
        self.model = self._build(n_features, seq_len)
        self.threshold = None
        self.is_fitted = False
    
    def _build(self, n_features, seq_len):
        model = Sequential([
            LSTM(64, input_shape=(seq_len, n_features), return_sequences=True),
            Dropout(0.2),
            LSTM(32, return_sequences=False),
            RepeatVector(seq_len),
            LSTM(32, return_sequences=True),
            Dropout(0.2),
            LSTM(64, return_sequences=True),
            TimeDistributed(Dense(n_features))
        ])
        model.compile(optimizer='adam', loss='mse')
        return model
    
    def _create_sequences(self, X):
        seqs = []
        for i in range(len(X) - self.seq_len + 1):
            seqs.append(X[i:i+self.seq_len])
        return np.array(seqs) if seqs else np.array([])
    
    def fit(self, X, epochs=50, verbose=0):
        X_scaled = self.scaler.fit_transform(X)
        seqs = self._create_sequences(X_scaled)
        if len(seqs) < 10:
            self.threshold = 0.5
            return self
        self.model.fit(seqs, seqs, epochs=epochs, batch_size=32, validation_split=0.1, verbose=verbose)
        recon = self.model.predict(seqs, verbose=0)
        mse = np.mean((seqs - recon)**2, axis=(1,2))
        self.threshold = np.mean(mse) + 2 * np.std(mse)
        self.is_fitted = True
        return self
    
    def save(self, path):
        base_path = path.replace('.pkl', '')
        self.model.save(f"{base_path}_model.h5")
        joblib.dump({
            'scaler': self.scaler,
            'threshold': self.threshold,
            'seq_len': self.seq_len,
            'n_features': self.n_features,
            'is_fitted': self.is_fitted
        }, f"{base_path}_meta.pkl")

print("\nTraining LSTM (Temporal Patterns)...")
lstm = LSTMDetector(n_features=X_all.shape[1], seq_len=5)
lstm.fit(X_all, epochs=30, verbose=1)
lstm.save(f'{Config.MODEL_DIR}/lstm.pkl')
print("LSTM trained")

# =============================================================================
# STEP 5: AUTOENCODERS (PER-USER)
# =============================================================================
class AutoencoderDetector:
    def __init__(self, input_dim):
        self.input_dim = input_dim
        self.scaler = MinMaxScaler()
        self.model = self._build(input_dim)
        self.threshold = None
        self.is_fitted = False
    
    def _build(self, dim):
        inp = Input(shape=(dim,))
        x = Dense(32, activation='relu')(inp)
        x = Dropout(0.2)(x)
        x = Dense(16, activation='relu')(x)
        x = Dense(8, activation='relu')(x)
        x = Dense(16, activation='relu')(x)
        x = Dropout(0.2)(x)
        x = Dense(32, activation='relu')(x)
        out = Dense(dim, activation='sigmoid')(x)
        model = Model(inp, out)
        model.compile(optimizer='adam', loss='mse')
        return model
    
    def fit(self, X, epochs=100, verbose=0):
        if len(X) < 10:
            return self
        X_scaled = self.scaler.fit_transform(X)
        self.model.fit(X_scaled, X_scaled, epochs=epochs, batch_size=min(16, len(X)//2), 
                      validation_split=0.1, verbose=verbose)
        recon = self.model.predict(X_scaled, verbose=0)
        mse = np.mean((X_scaled - recon)**2, axis=1)
        self.threshold = np.mean(mse) + 2 * np.std(mse)
        self.is_fitted = True
        return self
    
    def save(self, path):
        base_path = path.replace('.pkl', '')
        self.model.save(f"{base_path}_model.h5")
        joblib.dump({
            'scaler': self.scaler,
            'threshold': self.threshold,
            'input_dim': self.input_dim,
            'is_fitted': self.is_fitted
        }, f"{base_path}_meta.pkl")

print("\nTraining Autoencoders (Personal Patterns)...")
user_models_trained = 0

for user_id in df['user_id'].unique():
    user_df = df[df['user_id'] == user_id]
    
    if len(user_df) < Config.MIN_USER_TRANSACTIONS:
        continue
    
    X_user = feature_engineer.transform_batch(user_df)
    ae = AutoencoderDetector(input_dim=X_user.shape[1])
    ae.fit(X_user, epochs=100, verbose=0)
    
    if ae.is_fitted:
        safe_user_id = str(user_id).replace('/', '_').replace('\\', '_')
        ae.save(f'{Config.MODEL_DIR}/autoencoder_user_{safe_user_id}.pkl')
        user_models_trained += 1
        
    if user_models_trained % 10 == 0:
        print(f"   Trained {user_models_trained} models...")

print("Trained", user_models_trained, "personal models")

# =============================================================================
# STEP 6: EVALUATE
# =============================================================================
print("\n" + "="*60)
print("MODEL EVALUATION")
print("="*60)

if_scores = isolation_forest.predict_score(X_all)
if_pred = (if_scores > 0.35).astype(int)

print("\nIsolation Forest Performance:")
print("   Precision:", round(precision_score(y_all, if_pred, zero_division=0), 4))
print("   Recall:", round(recall_score(y_all, if_pred, zero_division=0), 4))
print("   F1-Score:", round(f1_score(y_all, if_pred, zero_division=0), 4))

if len(np.unique(y_all)) > 1:
    print("   AUC-ROC:", round(roc_auc_score(y_all, if_scores), 4))

print("\nScore Distribution:")
print("   Normal avg score:", round(if_scores[~y_all.astype(bool)].mean(), 3))
print("   Anomaly avg score:", round(if_scores[y_all.astype(bool)].mean(), 3))

# =============================================================================
# STEP 7: SAVE METADATA
# =============================================================================
metadata = {
    'trained_at': datetime.now().isoformat(),
    'total_transactions': int(len(df)),
    'total_users': int(df['user_id'].nunique()),
    'user_models_trained': user_models_trained,
    'feature_count': int(X_all.shape[1]),
    'feature_columns': Config.FEATURE_COLS,
    'categories': df['category'].unique().tolist(),
    'locations': df['location'].unique().tolist(),
    'payment_modes': df['payment_mode'].unique().tolist(),
    'anomaly_rate': float(df['is_anomaly'].mean())
}

with open(f'{Config.MODEL_DIR}/training_metadata.json', 'w') as f:
    json.dump(metadata, f, indent=2)

print("\n" + "="*60)
print("TRAINING COMPLETE!")
print("="*60)
print("Models saved to:", Config.MODEL_DIR)
print("Transactions:", len(df))
print("Users:", df['user_id'].nunique())
print("Personal models:", user_models_trained)

print("\nSaved files:")
for f in sorted(os.listdir(Config.MODEL_DIR)):
    size = os.path.getsize(f'{Config.MODEL_DIR}/{f}')
    print(f"   {f} ({round(size/1024, 1)} KB)")

# =============================================================================
# STEP 8: CREATE ZIP
# =============================================================================
import shutil
shutil.make_archive('trained_models', 'zip', Config.MODEL_DIR)
print("\nCreated trained_models.zip - DOWNLOAD THIS FILE!")

print("""
============================================================
DOWNLOAD INSTRUCTIONS
============================================================

1. Download 'trained_models.zip' from the Output panel (right side)

2. Extract to: Fintech/ml-service/models/

3. Start the ML service:
   cd Fintech/ml-service
   pip install -r requirements.txt
   python app.py

============================================================
""")
