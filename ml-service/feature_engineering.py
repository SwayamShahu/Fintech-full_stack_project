"""
Feature Engineering for Anomaly Detection
Transforms raw transaction data into ML-ready features.
"""
import numpy as np
import pandas as pd
from sklearn.preprocessing import LabelEncoder
import joblib
import os
from config import Config


class FeatureEngineer:
    """
    Handles feature extraction and transformation for transactions.
    Must be fitted on training data to learn encodings.
    """
    
    def __init__(self):
        self.category_encoder = LabelEncoder()
        self.payment_mode_encoder = LabelEncoder()
        self.location_encoder = LabelEncoder()
        self.user_stats = {}  # {user_id: {mean, std, median, count}}
        self.category_stats = {}  # {category: {mean, std}}
        self.is_fitted = False
    
    def fit(self, df):
        """
        Fit encoders and compute statistics from training data.
        
        Args:
            df: DataFrame with columns [user_id, amount, category, payment_mode, date, location]
        """
        df = df.copy()
        df['date'] = pd.to_datetime(df['date'])
        
        # Fit encoders
        self.category_encoder.fit(df['category'].astype(str))
        self.payment_mode_encoder.fit(df['payment_mode'].astype(str))
        
        # Handle location if present
        if 'location' in df.columns:
            self.location_encoder.fit(df['location'].astype(str))
        else:
            self.location_encoder.fit(['Unknown'])
        
        # Compute user statistics (handles string user_ids like "U001")
        user_groups = df.groupby('user_id')['amount'].agg(['mean', 'std', 'median', 'count'])
        for user_id, row in user_groups.iterrows():
            self.user_stats[user_id] = {
                'mean': float(row['mean']),
                'std': float(row['std']) if pd.notna(row['std']) and row['std'] > 0 else 1.0,
                'median': float(row['median']),
                'count': int(row['count'])
            }
        
        # Compute category statistics
        cat_groups = df.groupby('category')['amount'].agg(['mean', 'std'])
        for cat, row in cat_groups.iterrows():
            self.category_stats[cat] = {
                'mean': float(row['mean']),
                'std': float(row['std']) if pd.notna(row['std']) and row['std'] > 0 else 1.0
            }
        
        self.is_fitted = True
        return self
    
    def transform_single(self, transaction, user_id):
        """
        Transform a single transaction into feature vector.
        
        Args:
            transaction: Dict with amount, category, payment_mode, expense_date, location (optional)
            user_id: User identifier (can be string like "U001" or integer)
            
        Returns:
            numpy array of features
        """
        # Parse date
        date = pd.to_datetime(transaction.get('expense_date') or transaction.get('date'))
        
        # Time features
        day_of_week = date.dayofweek
        day_of_month = date.day
        month = date.month
        is_weekend = 1 if day_of_week in [5, 6] else 0
        
        # Encode category
        category = str(transaction['category'])
        try:
            category_encoded = self.category_encoder.transform([category])[0]
        except ValueError:
            # Unknown category - use -1
            category_encoded = -1
        
        # Encode payment mode
        payment_mode = str(transaction.get('payment_mode', 'Cash'))
        try:
            payment_mode_encoded = self.payment_mode_encoder.transform([payment_mode])[0]
        except ValueError:
            payment_mode_encoded = 0
        
        # Encode location
        location = str(transaction.get('location', 'Unknown'))
        try:
            location_encoded = self.location_encoder.transform([location])[0]
        except ValueError:
            location_encoded = 0
        
        amount = float(transaction['amount'])
        
        # User-relative features
        user_stat = self.user_stats.get(user_id, {'mean': amount, 'std': 1, 'median': amount})
        amount_zscore = (amount - user_stat['mean']) / user_stat['std']
        amount_to_median = amount / (user_stat['median'] + 1)
        
        # Category-relative features
        cat_stat = self.category_stats.get(category, {'mean': amount, 'std': 1})
        cat_zscore = (amount - cat_stat['mean']) / (cat_stat['std'] + 1)
        
        # Build feature vector (must match FEATURE_COLS order in config)
        features = np.array([
            amount,
            category_encoded,
            day_of_week,
            day_of_month,
            month,
            is_weekend,
            payment_mode_encoded,
            location_encoded,
            amount_zscore,
            amount_to_median,
            cat_zscore
        ], dtype=np.float32)
        
        return features
    
    def transform_batch(self, df):
        """Transform a DataFrame of transactions"""
        df = df.copy()
        df['date'] = pd.to_datetime(df['date'])
        
        features_list = []
        for _, row in df.iterrows():
            transaction = {
                'amount': row['amount'],
                'category': row['category'],
                'payment_mode': row.get('payment_mode', 'Cash'),
                'expense_date': row['date'],
                'location': row.get('location', 'Unknown')
            }
            features = self.transform_single(transaction, row['user_id'])
            features_list.append(features)
        
        return np.array(features_list)
    
    def update_user_stats(self, user_id, transactions_df):
        """Update statistics for a specific user with new data"""
        if len(transactions_df) == 0:
            return
        
        amounts = transactions_df['amount'].values
        self.user_stats[user_id] = {
            'mean': float(np.mean(amounts)),
            'std': float(np.std(amounts)) if len(amounts) > 1 else 1.0,
            'median': float(np.median(amounts)),
            'count': len(amounts)
        }
    
    def get_user_stats(self, user_id):
        """Get statistics for a user"""
        return self.user_stats.get(user_id, None)
    
    def get_user_category_stats(self, user_id, df):
        """Get per-category stats for a user"""
        user_df = df[df['user_id'] == user_id]
        cat_stats = {}
        for cat, group in user_df.groupby('category'):
            cat_stats[cat] = {
                'mean': group['amount'].mean(),
                'count': len(group)
            }
        return cat_stats
    
    def save(self, path):
        """Save to disk"""
        joblib.dump({
            'category_encoder': self.category_encoder,
            'payment_mode_encoder': self.payment_mode_encoder,
            'location_encoder': self.location_encoder,
            'user_stats': self.user_stats,
            'category_stats': self.category_stats,
            'is_fitted': self.is_fitted
        }, path)
    
    @classmethod
    def load(cls, path):
        """Load from disk"""
        if not os.path.exists(path):
            return None
        
        data = joblib.load(path)
        fe = cls()
        fe.category_encoder = data['category_encoder']
        fe.payment_mode_encoder = data['payment_mode_encoder']
        fe.location_encoder = data.get('location_encoder', LabelEncoder().fit(['Unknown']))
        fe.user_stats = data['user_stats']
        fe.category_stats = data['category_stats']
        fe.is_fitted = data['is_fitted']
        return fe
