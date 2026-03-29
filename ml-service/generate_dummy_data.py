import sys
import os
import random
from datetime import datetime, timedelta
import mysql.connector

# Add current path
sys.path.append(os.path.dirname(os.path.abspath(__file__)))
from config import Config

def generate_data():
    conn = mysql.connector.connect(
        host=Config.DB_HOST,
        port=Config.DB_PORT,
        database=Config.DB_NAME,
        user=Config.DB_USER,
        password=Config.DB_PASSWORD
    )
    cursor = conn.cursor()

    users = [1, 2]
    # Categories from 1 to 12
    categories = list(range(1, 13))
    payment_modes = ['Credit Card', 'Debit Card', 'Cash', 'UPI', 'Net Banking']

    insert_query = """
        INSERT INTO expenses (
            amount, created_at, description, expense_date, 
            is_anomaly, is_recurring, payment_mode, category_id, user_id
        ) VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s)
    """

    data = []
    end_date = datetime.now()
    start_date = end_date - timedelta(days=180) # Last 6 months

    print(f"Generating dummy data for {len(users)} users...")
    for user_id in users:
        # Generate 150 transactions per user
        for _ in range(150):
            # Normal amount around 10-500, with a few outliers
            if random.random() < 0.05: # 5% anomalies
                amount = round(random.uniform(2000, 5000), 2)
                is_anomaly = 1
            else:
                amount = round(random.uniform(5, 500), 2)
                is_anomaly = 0

            # Random date in last 6 months
            random_days = random.randint(0, 180)
            expense_date = (start_date + timedelta(days=random_days)).date()
            created_at = datetime.now()

            category_id = random.choice(categories)
            payment_mode = random.choice(payment_modes)
            description = f"Dummy transaction {random.randint(1000, 9999)}"
            is_recurring = 1 if random.random() < 0.1 else 0

            data.append((
                amount,
                created_at,
                description,
                expense_date,
                is_anomaly,
                is_recurring,
                payment_mode,
                category_id,
                user_id
            ))

    cursor.executemany(insert_query, data)
    conn.commit()
    
    print(f"Inserted {cursor.rowcount} expense records successfully.")
    
    cursor.close()
    conn.close()

if __name__ == '__main__':
    generate_data()
