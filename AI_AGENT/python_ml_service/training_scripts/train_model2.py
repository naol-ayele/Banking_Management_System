import pandas as pd
from sklearn.model_selection import train_test_split
from sklearn.ensemble import RandomForestClassifier # Switched to a more powerful model
from sklearn.metrics import accuracy_score
import joblib
import os
import numpy as np

# --- 1. Create More Realistic and Feature-Rich Sample Data ---
# We've added 'employment_duration_months' and 'num_existing_loans'.
data = {
    'income_monthly': np.random.randint(2000, 15000, 100),
    'spend_monthly': np.random.randint(1000, 10000, 100),
    'credit_score': np.random.randint(500, 850, 100),
    'employment_duration_months': np.random.randint(6, 120, 100),
    'num_existing_loans': np.random.randint(0, 5, 100)
}
df = pd.DataFrame(data)

# --- 2. Feature Engineering ---
# Create a new, highly predictive feature: debt_to_income_ratio.
df['debt_to_income_ratio'] = df['spend_monthly'] / df['income_monthly']

# --- 3. Create a Realistic Target Variable ---
# Define conditions for loan approval based on the features.
conditions = [
    (df['credit_score'] > 720) & (df['debt_to_income_ratio'] < 0.4) & (df['employment_duration_months'] > 24),
    (df['credit_score'] > 680) & (df['debt_to_income_ratio'] < 0.5) & (df['num_existing_loans'] <= 1),
    (df['credit_score'] < 600) | (df['debt_to_income_ratio'] > 0.6)
]
outcomes = [1, 1, 0] # 1 for approved, 0 for denied
df['loan_approved'] = np.select(conditions, outcomes, default=np.random.randint(0, 2))


# --- 4. Train the Improved Model ---
# We now include our new engineered feature in the training data.
features = ['income_monthly', 'spend_monthly', 'credit_score', 'employment_duration_months', 'num_existing_loans', 'debt_to_income_ratio']
X = df[features]
y = df['loan_approved']

X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=0.2, random_state=42)

# Use RandomForestClassifier instead of LogisticRegression
model = RandomForestClassifier(n_estimators=100, random_state=42, class_weight='balanced')
model.fit(X_train, y_train)

# --- 5. Evaluate and Save ---
predictions = model.predict(X_test)
accuracy = accuracy_score(y_test, predictions)
print(f"Improved Model Accuracy: {accuracy * 100:.0f}%")

output_dir = "trained_models"
script_dir = os.path.dirname(__file__)
model_dir = os.path.join(script_dir, '..', output_dir)
os.makedirs(model_dir, exist_ok=True)

model_path = os.path.join(model_dir, "loan_predictor_v2.joblib")
joblib.dump(model, model_path)

print(f"New, more accurate model trained and saved to {model_path}")