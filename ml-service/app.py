"""
Flask API for ML Anomaly Detection Service
Provides REST endpoints for the Spring Boot backend.
"""
import os
import sys
import numpy as np
import pandas as pd
from flask import Flask, request, jsonify
from flask_cors import CORS
import logging
import mysql.connector
from datetime import datetime, timedelta

# Suppress TensorFlow warnings
os.environ['TF_CPP_MIN_LOG_LEVEL'] = '2'

from config import Config
from feature_engineering import FeatureEngineer
from detectors import IsolationForestDetector, AutoencoderDetector, LSTMDetector
from ensemble import EnsembleDetector, generate_explanation

# Configure logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

app = Flask(__name__)
CORS(app)

# Global model instances
feature_engineer = None
isolation_forest = None
lstm = None
user_autoencoders = {}
ensemble = None


def get_db_connection():
    """Get MySQL database connection"""
    return mysql.connector.connect(
        host=Config.DB_HOST,
        port=Config.DB_PORT,
        database=Config.DB_NAME,
        user=Config.DB_USER,
        password=Config.DB_PASSWORD
    )


def load_models():
    """Load all trained models"""
    global feature_engineer, isolation_forest, lstm, user_autoencoders, ensemble
    
    logger.info("Loading ML models...")
    
    # Load feature engineer
    fe_path = os.path.join(Config.MODEL_DIR, 'feature_engineer.pkl')
    feature_engineer = FeatureEngineer.load(fe_path)
    if feature_engineer:
        logger.info("✅ Feature engineer loaded")
    else:
        logger.warning("⚠️ Feature engineer not found - will use defaults")
        feature_engineer = FeatureEngineer()
    
    # Load Isolation Forest
    if_path = os.path.join(Config.MODEL_DIR, 'isolation_forest.pkl')
    isolation_forest = IsolationForestDetector.load(if_path)
    if isolation_forest:
        logger.info("✅ Isolation Forest loaded")
    else:
        logger.warning("⚠️ Isolation Forest not found")
    
    # Load LSTM
    lstm_path = os.path.join(Config.MODEL_DIR, 'lstm.pkl')
    lstm = LSTMDetector.load(lstm_path)
    if lstm:
        logger.info("✅ LSTM loaded")
    else:
        logger.warning("⚠️ LSTM not found")
    
    # Load user autoencoders (handles both integer and string user_ids like "U001")
    user_autoencoders = {}
    if os.path.exists(Config.MODEL_DIR):
        for filename in os.listdir(Config.MODEL_DIR):
            if filename.startswith('autoencoder_user_') and filename.endswith('_meta.pkl'):
                # Extract user_id (could be "U001" or "1")
                user_id_str = filename.replace('autoencoder_user_', '').replace('_meta.pkl', '')
                ae_path = os.path.join(Config.MODEL_DIR, f'autoencoder_user_{user_id_str}.pkl')
                ae = AutoencoderDetector.load(ae_path)
                if ae:
                    # Try to convert to int, otherwise keep as string
                    try:
                        user_id = int(user_id_str)
                    except ValueError:
                        user_id = user_id_str
                    user_autoencoders[user_id] = ae
    
    logger.info(f"✅ Loaded {len(user_autoencoders)} user autoencoders")
    
    # Create ensemble
    ensemble = EnsembleDetector(isolation_forest, user_autoencoders, lstm)
    logger.info("✅ Ensemble detector ready")


def get_user_history(user_id, limit=20):
    """Get user's recent transaction history from database"""
    try:
        conn = get_db_connection()
        cursor = conn.cursor(dictionary=True)
        
        query = """
        SELECT 
            e.amount,
            c.name as category,
            e.expense_date as date,
            e.payment_mode
        FROM expenses e
        JOIN categories c ON e.category_id = c.id
        WHERE e.user_id = %s
        ORDER BY e.expense_date DESC
        LIMIT %s
        """
        
        cursor.execute(query, (user_id, limit))
        rows = cursor.fetchall()
        cursor.close()
        conn.close()
        
        return rows[::-1]  # Reverse to chronological order
    except Exception as e:
        logger.error(f"Error fetching history: {e}")
        return []


def get_user_transaction_count(user_id):
    """Get total transaction count for user"""
    try:
        conn = get_db_connection()
        cursor = conn.cursor()
        cursor.execute("SELECT COUNT(*) FROM expenses WHERE user_id = %s", (user_id,))
        count = cursor.fetchone()[0]
        cursor.close()
        conn.close()
        return count
    except Exception as e:
        logger.error(f"Error getting count: {e}")
        return 0


# ============================================================================
# API ENDPOINTS
# ============================================================================

@app.route('/health', methods=['GET'])
def health_check():
    """Health check endpoint"""
    models_status = {
        'feature_engineer': feature_engineer is not None and feature_engineer.is_fitted,
        'isolation_forest': isolation_forest is not None and isolation_forest.is_fitted,
        'lstm': lstm is not None and lstm.is_fitted,
        'user_autoencoders_count': len(user_autoencoders)
    }
    
    all_ready = models_status['isolation_forest'] or models_status['lstm']
    
    return jsonify({
        'status': 'healthy' if all_ready else 'degraded',
        'models': models_status,
        'message': 'ML service is running'
    })


@app.route('/predict', methods=['POST'])
def predict_anomaly():
    """
    Main prediction endpoint.
    
    Request body:
    {
        "user_id": 1,
        "amount": 5000.0,
        "category": "Food & Dining",
        "category_id": 1,
        "expense_date": "2026-03-29",
        "payment_mode": "UPI"
    }
    
    Response:
    {
        "is_anomaly": true,
        "anomaly_score": 0.72,
        "anomaly_type": "PERSONAL_DEVIATION",
        "severity": "HIGH",
        "explanation": "Amount is 4.2x your average spending",
        "model_scores": {...}
    }
    """
    try:
        data = request.json
        
        # Validate required fields
        required = ['user_id', 'amount', 'category', 'expense_date']
        for field in required:
            if field not in data:
                return jsonify({'error': f'Missing required field: {field}'}), 400
        
        user_id = int(data['user_id'])
        
        # Check if models are loaded
        if ensemble is None or feature_engineer is None:
            return jsonify({
                'error': 'Models not loaded',
                'fallback': True
            }), 503
        
        # Transform transaction to features
        transaction = {
            'amount': float(data['amount']),
            'category': data['category'],
            'payment_mode': data.get('payment_mode', 'Cash'),
            'expense_date': data['expense_date']
        }
        
        X_single = feature_engineer.transform_single(transaction, user_id)
        
        # Get user history for LSTM
        history = get_user_history(user_id, limit=15)
        X_history = []
        for txn in history:
            X_history.append(feature_engineer.transform_single(txn, user_id))
        X_history.append(X_single)  # Add current transaction
        X_history = np.array(X_history)
        
        # Get user transaction count
        user_count = get_user_transaction_count(user_id)
        
        # Make prediction
        prediction = ensemble.predict_single(
            user_id=user_id,
            X_single=X_single,
            X_history=X_history,
            user_transaction_count=user_count
        )
        
        # Generate explanation
        user_stats = feature_engineer.get_user_stats(user_id)
        if user_stats:
            user_stats['mean_amount'] = user_stats.get('mean', data['amount'])
        
        explanation = generate_explanation(prediction, transaction, user_stats)
        
        # Map anomaly type to match Spring Boot expectations
        anomaly_type_mapping = {
            'GLOBAL_ANOMALY': 'ML_GLOBAL_PATTERN',
            'PERSONAL_DEVIATION': 'ML_PERSONAL_DEVIATION',
            'TEMPORAL_ANOMALY': 'ML_TEMPORAL_PATTERN'
        }
        
        ml_anomaly_type = anomaly_type_mapping.get(
            prediction['anomaly_type'],
            'ML_DETECTED'
        ) if prediction['is_anomaly'] else None

        response_data = {
            'is_anomaly': bool(prediction['is_anomaly']),
            'anomaly_score': float(prediction['final_score']),
            'anomaly_type': ml_anomaly_type,
            'severity': prediction['severity'],
            'explanation': explanation,
            'model_scores': {
                'isolation_forest': float(prediction['model_scores']['isolation_forest']),
                'autoencoder': float(prediction['model_scores']['autoencoder']),
                'lstm': float(prediction['model_scores']['lstm'])
            },
            'weights_used': prediction['weights']
        }
        
        # Log the detailed prediction to the terminal before sending it to the backend
        print("\n" + "="*50)
        print(f"📊 ML PREDICTION FOR USER {user_id} - ${data['amount']}")
        print("="*50)
        import json
        print(json.dumps(response_data, indent=4))
        print("="*50 + "\n")

        return jsonify(response_data)

    except Exception as e:
        logger.error(f"Prediction error: {e}", exc_info=True)
        return jsonify({
            'error': str(e),
            'fallback': True
        }), 500


@app.route('/train', methods=['POST'])
def trigger_training():
    """
    Trigger model retraining.
    
    Request body (optional):
    {
        "user_id": 1  // If specified, only retrain this user's model
    }
    """
    try:
        data = request.json or {}
        user_id = data.get('user_id')
        
        if user_id:
            # Train specific user model
            from train import train_user_model
            success = train_user_model(int(user_id))
            
            if success:
                # Reload the user's model
                ae_path = os.path.join(Config.MODEL_DIR, f'autoencoder_user_{user_id}.pkl')
                ae = AutoencoderDetector.load(ae_path)
                if ae:
                    user_autoencoders[int(user_id)] = ae
                
                return jsonify({
                    'success': True,
                    'message': f'Model retrained for user {user_id}'
                })
            else:
                return jsonify({
                    'success': False,
                    'message': f'Failed to train model for user {user_id}'
                }), 400
        else:
            # Full retraining
            from train import train_models
            success = train_models()
            
            if success:
                # Reload all models
                load_models()
                return jsonify({
                    'success': True,
                    'message': 'All models retrained successfully'
                })
            else:
                return jsonify({
                    'success': False,
                    'message': 'Training failed - not enough data'
                }), 400
                
    except Exception as e:
        logger.error(f"Training error: {e}", exc_info=True)
        return jsonify({'error': str(e)}), 500


@app.route('/user/<int:user_id>/stats', methods=['GET'])
def get_user_statistics(user_id):
    """Get anomaly statistics for a user"""
    try:
        stats = feature_engineer.get_user_stats(user_id) if feature_engineer else None
        has_personal_model = user_id in user_autoencoders
        
        return jsonify({
            'user_id': user_id,
            'has_personal_model': has_personal_model,
            'statistics': stats,
            'model_ready': has_personal_model and user_autoencoders[user_id].is_fitted
        })
    except Exception as e:
        return jsonify({'error': str(e)}), 500


@app.route('/models/status', methods=['GET'])
def models_status():
    """Get detailed status of all models"""
    return jsonify({
        'feature_engineer': {
            'loaded': feature_engineer is not None,
            'fitted': feature_engineer.is_fitted if feature_engineer else False,
            'users_tracked': len(feature_engineer.user_stats) if feature_engineer else 0
        },
        'isolation_forest': {
            'loaded': isolation_forest is not None,
            'fitted': isolation_forest.is_fitted if isolation_forest else False
        },
        'lstm': {
            'loaded': lstm is not None,
            'fitted': lstm.is_fitted if lstm else False,
            'sequence_length': lstm.seq_len if lstm else None
        },
        'autoencoders': {
            'count': len(user_autoencoders),
            'user_ids': list(user_autoencoders.keys())
        }
    })


# ============================================================================
# STARTUP
# ============================================================================

# Load models on startup
load_models()


if __name__ == '__main__':
    print("\n" + "="*60)
    print("🚀 ML Anomaly Detection Service")
    print("="*60)
    print(f"   Port: {Config.PORT}")
    print(f"   Model directory: {Config.MODEL_DIR}")
    print(f"   Database: {Config.DB_HOST}:{Config.DB_PORT}/{Config.DB_NAME}")
    print("="*60 + "\n")
    
    app.run(host='0.0.0.0', port=Config.PORT, debug=Config.DEBUG)
