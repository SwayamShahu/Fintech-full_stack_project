"""
Ensemble Anomaly Detection
Combines Isolation Forest, Autoencoder, and LSTM with dynamic weighting.
"""
import numpy as np
from config import Config


class EnsembleDetector:
    """
    Weighted ensemble combining:
    - Isolation Forest: Global patterns (all users)
    - Autoencoder: Personal patterns (specific user)
    - LSTM: Temporal patterns (sequence-based)
    """
    
    def __init__(self, isolation_forest, user_autoencoders, lstm):
        """
        Args:
            isolation_forest: Trained IsolationForestDetector
            user_autoencoders: Dict of {user_id: AutoencoderDetector}
            lstm: Trained LSTMDetector
        """
        self.isolation_forest = isolation_forest
        self.user_autoencoders = user_autoencoders
        self.lstm = lstm
    
    def _get_weights(self, user_transaction_count, has_personal_model):
        """
        Dynamic weight assignment based on user history.
        
        - New users: Rely more on global patterns (IF)
        - Established users: Rely more on personal patterns (AE)
        """
        if not has_personal_model or user_transaction_count < 10:
            # New user - rely on global and temporal
            return {'if': 0.60, 'ae': 0.10, 'lstm': 0.30}
        elif user_transaction_count < 30:
            # Growing history - balanced
            return {'if': 0.40, 'ae': 0.30, 'lstm': 0.30}
        elif user_transaction_count < 50:
            # Moderate history
            return {'if': 0.30, 'ae': 0.40, 'lstm': 0.30}
        else:
            # Established user - personal patterns most important
            return {'if': 0.20, 'ae': 0.55, 'lstm': 0.25}
    
    def predict_single(self, user_id, X_single, X_history, user_transaction_count):
        """
        Predict anomaly score for a single transaction.
        
        Args:
            user_id: User identifier
            X_single: Feature vector for the transaction (1D array)
            X_history: Recent transaction history including current (2D array)
            user_transaction_count: Total transactions for this user
            
        Returns:
            dict with scores and explanation
        """
        X_single = np.array(X_single).reshape(1, -1)
        
        # 1. Isolation Forest (Global)
        if_score = 0.0
        if self.isolation_forest and self.isolation_forest.is_fitted:
            if_score = self.isolation_forest.predict_score(X_single)[0]
        
        # 2. Autoencoder (Personal)
        ae_score = 0.0
        has_personal_model = False
        if user_id in self.user_autoencoders:
            ae = self.user_autoencoders[user_id]
            if ae and ae.is_fitted:
                ae_score = ae.predict_score(X_single)[0]
                has_personal_model = True
        
        # 3. LSTM (Temporal)
        lstm_score = 0.0
        if self.lstm and self.lstm.is_fitted and len(X_history) >= self.lstm.seq_len:
            lstm_score = self.lstm.predict_score_single(X_history)
        
        # Get dynamic weights
        weights = self._get_weights(user_transaction_count, has_personal_model)
        
        # Weighted combination
        final_score = (
            weights['if'] * if_score +
            weights['ae'] * ae_score +
            weights['lstm'] * lstm_score
        )
        
        # Determine anomaly type and severity
        anomaly_type = self._determine_type(if_score, ae_score, lstm_score)
        severity = self._determine_severity(final_score)
        
        return {
            'final_score': float(final_score),
            'is_anomaly': final_score >= Config.ANOMALY_THRESHOLD,
            'severity': severity,
            'anomaly_type': anomaly_type,
            'model_scores': {
                'isolation_forest': float(if_score),
                'autoencoder': float(ae_score),
                'lstm': float(lstm_score)
            },
            'weights': weights
        }
    
    def _determine_type(self, if_score, ae_score, lstm_score):
        """Determine primary anomaly type based on which model flagged most"""
        scores = {
            'GLOBAL_ANOMALY': if_score,
            'PERSONAL_DEVIATION': ae_score,
            'TEMPORAL_ANOMALY': lstm_score
        }
        
        max_score = max(scores.values())
        if max_score < 0.3:
            return None
        
        # Get the type with highest score
        for atype, score in scores.items():
            if score == max_score:
                return atype
        return None
    
    def _determine_severity(self, score):
        """Determine severity level"""
        if score >= Config.HIGH_ANOMALY_THRESHOLD:
            return 'HIGH'
        elif score >= Config.MODERATE_ANOMALY_THRESHOLD:
            return 'MODERATE'
        elif score >= Config.ANOMALY_THRESHOLD:
            return 'LOW'
        return 'NORMAL'


def generate_explanation(prediction, transaction_data, user_stats):
    """
    Generate human-readable explanation for the anomaly.
    
    Args:
        prediction: Output from EnsembleDetector.predict_single()
        transaction_data: Dict with amount, category, date, etc.
        user_stats: Dict with user's historical stats
    """
    if not prediction['is_anomaly']:
        return None
    
    explanations = []
    scores = prediction['model_scores']
    
    # Amount comparison
    if user_stats and 'mean_amount' in user_stats:
        ratio = transaction_data['amount'] / user_stats['mean_amount']
        if ratio > 2:
            explanations.append(f"Amount is {ratio:.1f}x your average spending")
    
    # Global anomaly
    if scores['isolation_forest'] > 0.4:
        explanations.append("Unusual compared to typical spending patterns")
    
    # Personal deviation
    if scores['autoencoder'] > 0.4:
        explanations.append("Deviates from your personal spending habits")
    
    # Temporal anomaly
    if scores['lstm'] > 0.4:
        explanations.append("Unusual timing or sequence pattern detected")
    
    # Category-specific
    if user_stats and 'category_stats' in user_stats:
        cat = transaction_data.get('category')
        cat_stats = user_stats['category_stats'].get(cat, {})
        if cat_stats.get('count', 0) < 3:
            explanations.append(f"Rare category for you: {cat}")
    
    return "; ".join(explanations) if explanations else "Unusual transaction detected"
