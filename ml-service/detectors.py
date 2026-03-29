"""
Anomaly Detection Models
- Isolation Forest (Global patterns)
- Autoencoder (Personal patterns)  
- LSTM (Temporal patterns)
"""
import numpy as np
import joblib
import os
from sklearn.ensemble import IsolationForest
from sklearn.preprocessing import StandardScaler, MinMaxScaler
import tensorflow as tf
from tensorflow.keras.models import Model, Sequential, load_model
from tensorflow.keras.layers import Input, Dense, LSTM, Dropout, RepeatVector, TimeDistributed

from config import Config


class IsolationForestDetector:
    """
    Global anomaly detector - trained on ALL users' transactions.
    Detects patterns unusual compared to the entire user base.
    """
    
    def __init__(self, contamination=0.05):
        self.model = IsolationForest(
            contamination=contamination, 
            n_estimators=150, 
            random_state=42,
            n_jobs=-1
        )
        self.scaler = StandardScaler()
        self.train_min = None
        self.train_max = None
        self.is_fitted = False
    
    def fit(self, X):
        """Train on global transaction data"""
        X_scaled = self.scaler.fit_transform(X)
        self.model.fit(X_scaled)
        
        # Get score range for normalization
        scores = self.model.decision_function(X_scaled)
        self.train_min = scores.min()
        self.train_max = scores.max()
        self.is_fitted = True
        return self
    
    def predict_score(self, X):
        """Return anomaly scores normalized to [0, 1]"""
        if not self.is_fitted:
            return np.zeros(len(X))
        
        X_scaled = self.scaler.transform(X)
        scores = self.model.decision_function(X_scaled)
        
        # Normalize: lower decision_function = more anomalous
        # Convert to [0, 1] where 1 = most anomalous
        normalized = 1 - (scores - self.train_min) / (self.train_max - self.train_min + 1e-10)
        return np.clip(normalized, 0, 1)
    
    def save(self, path):
        """Save model to disk"""
        joblib.dump({
            'model': self.model,
            'scaler': self.scaler,
            'train_min': self.train_min,
            'train_max': self.train_max,
            'is_fitted': self.is_fitted
        }, path)
    
    @classmethod
    def load(cls, path):
        """Load model from disk"""
        if not os.path.exists(path):
            return None
        data = joblib.load(path)
        detector = cls()
        detector.model = data['model']
        detector.scaler = data['scaler']
        detector.train_min = data['train_min']
        detector.train_max = data['train_max']
        detector.is_fitted = data['is_fitted']
        return detector


class AutoencoderDetector:
    """
    Personal anomaly detector - trained on SPECIFIC user's transactions.
    Learns individual spending patterns and flags deviations.
    """
    
    def __init__(self, input_dim=None):
        self.input_dim = input_dim
        self.scaler = MinMaxScaler()
        self.model = None
        self.threshold = None
        self.is_fitted = False
        
        if input_dim:
            self.model = self._build_model(input_dim)
    
    def _build_model(self, dim):
        """Build autoencoder architecture"""
        inp = Input(shape=(dim,))
        
        # Encoder
        x = Dense(32, activation='relu')(inp)
        x = Dropout(0.2)(x)
        x = Dense(16, activation='relu')(x)
        encoded = Dense(8, activation='relu')(x)  # Bottleneck
        
        # Decoder
        x = Dense(16, activation='relu')(encoded)
        x = Dropout(0.2)(x)
        x = Dense(32, activation='relu')(x)
        decoded = Dense(dim, activation='sigmoid')(x)
        
        model = Model(inp, decoded)
        model.compile(optimizer='adam', loss='mse')
        return model
    
    def fit(self, X, epochs=100, verbose=0):
        """Train on user's transaction history"""
        if len(X) < 10:
            # Not enough data
            self.is_fitted = False
            return self
        
        if self.model is None:
            self.input_dim = X.shape[1]
            self.model = self._build_model(self.input_dim)
        
        X_scaled = self.scaler.fit_transform(X)
        
        self.model.fit(
            X_scaled, X_scaled,
            epochs=epochs,
            batch_size=min(16, len(X) // 2),
            validation_split=0.1,
            verbose=verbose
        )
        
        # Calculate threshold from training data
        reconstructed = self.model.predict(X_scaled, verbose=0)
        mse = np.mean((X_scaled - reconstructed) ** 2, axis=1)
        self.threshold = np.mean(mse) + 2 * np.std(mse)
        self.is_fitted = True
        return self
    
    def predict_score(self, X):
        """Return anomaly scores normalized to [0, 1]"""
        if not self.is_fitted or self.model is None:
            return np.zeros(len(X))
        
        X_scaled = self.scaler.transform(X)
        reconstructed = self.model.predict(X_scaled, verbose=0)
        mse = np.mean((X_scaled - reconstructed) ** 2, axis=1)
        
        # Normalize by threshold
        scores = mse / (self.threshold * 2 + 1e-10)
        return np.clip(scores, 0, 1)
    
    def save(self, path):
        """Save model to disk"""
        base_path = path.replace('.pkl', '')
        
        # Save Keras model
        if self.model:
            self.model.save(f"{base_path}_model.h5")
        
        # Save other components
        joblib.dump({
            'scaler': self.scaler,
            'threshold': self.threshold,
            'input_dim': self.input_dim,
            'is_fitted': self.is_fitted
        }, f"{base_path}_meta.pkl")
    
    @classmethod
    def load(cls, path):
        """Load model from disk"""
        base_path = path.replace('.pkl', '')
        meta_path = f"{base_path}_meta.pkl"
        model_path = f"{base_path}_model.h5"
        
        if not os.path.exists(meta_path):
            return None
        
        meta = joblib.load(meta_path)
        detector = cls(input_dim=meta['input_dim'])
        detector.scaler = meta['scaler']
        detector.threshold = meta['threshold']
        detector.is_fitted = meta['is_fitted']
        
        if os.path.exists(model_path):
            detector.model = load_model(model_path)
        
        return detector


class LSTMDetector:
    """
    Temporal anomaly detector - trained on ALL users' sequences.
    Detects unusual patterns in transaction sequences over time.
    """
    
    def __init__(self, n_features=None, seq_len=5):
        self.seq_len = seq_len
        self.n_features = n_features
        self.scaler = MinMaxScaler()
        self.model = None
        self.threshold = None
        self.is_fitted = False
        
        if n_features:
            self.model = self._build_model(n_features, seq_len)
    
    def _build_model(self, n_features, seq_len):
        """Build LSTM autoencoder"""
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
        """Create sliding window sequences"""
        sequences = []
        for i in range(len(X) - self.seq_len + 1):
            sequences.append(X[i:i + self.seq_len])
        return np.array(sequences) if sequences else np.array([])
    
    def fit(self, X, epochs=50, verbose=0):
        """Train on all users' transaction sequences"""
        if len(X) < self.seq_len + 10:
            self.is_fitted = False
            return self
        
        if self.model is None:
            self.n_features = X.shape[1]
            self.model = self._build_model(self.n_features, self.seq_len)
        
        X_scaled = self.scaler.fit_transform(X)
        sequences = self._create_sequences(X_scaled)
        
        if len(sequences) < 10:
            self.is_fitted = False
            return self
        
        self.model.fit(
            sequences, sequences,
            epochs=epochs,
            batch_size=32,
            validation_split=0.1,
            verbose=verbose
        )
        
        # Calculate threshold
        reconstructed = self.model.predict(sequences, verbose=0)
        mse = np.mean((sequences - reconstructed) ** 2, axis=(1, 2))
        self.threshold = np.mean(mse) + 2 * np.std(mse)
        self.is_fitted = True
        return self
    
    def predict_score_single(self, X_history):
        """
        Score a single transaction given recent history.
        X_history should include the transaction to score at the end.
        """
        if not self.is_fitted or len(X_history) < self.seq_len:
            return 0.0
        
        X_scaled = self.scaler.transform(X_history)
        sequence = X_scaled[-self.seq_len:].reshape(1, self.seq_len, -1)
        
        reconstructed = self.model.predict(sequence, verbose=0)
        mse = np.mean((sequence - reconstructed) ** 2)
        
        score = mse / (self.threshold * 2 + 1e-10)
        return min(1.0, score)
    
    def save(self, path):
        """Save model to disk"""
        base_path = path.replace('.pkl', '')
        
        if self.model:
            self.model.save(f"{base_path}_model.h5")
        
        joblib.dump({
            'scaler': self.scaler,
            'threshold': self.threshold,
            'seq_len': self.seq_len,
            'n_features': self.n_features,
            'is_fitted': self.is_fitted
        }, f"{base_path}_meta.pkl")
    
    @classmethod
    def load(cls, path):
        """Load model from disk"""
        base_path = path.replace('.pkl', '')
        meta_path = f"{base_path}_meta.pkl"
        model_path = f"{base_path}_model.h5"
        
        if not os.path.exists(meta_path):
            return None
        
        meta = joblib.load(meta_path)
        detector = cls(n_features=meta['n_features'], seq_len=meta['seq_len'])
        detector.scaler = meta['scaler']
        detector.threshold = meta['threshold']
        detector.is_fitted = meta['is_fitted']
        
        if os.path.exists(model_path):
            detector.model = load_model(model_path)
        
        return detector
