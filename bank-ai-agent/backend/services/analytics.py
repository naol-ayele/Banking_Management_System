from typing import Dict, Any, List, Optional
import numpy as np
import pandas as pd
from sklearn.linear_model import LogisticRegression
from sklearn.preprocessing import StandardScaler

def predict_loan_eligibility(fin: Dict[str, Any], desired_amount: Optional[float] = None) -> Dict[str, Any]:
    df = pd.DataFrame(fin["months"])
    income = df["income"].values.reshape(-1, 1)
    spend = df["spend"].values

    # Label: simulated repayment ability (income > 1.1 * spend)
    y = (df["income"] > df["spend"] * 1.1).astype(int).values

    prob = 0.5  # default probability
    if len(np.unique(y)) < 2:
        # Fallback rule if all labels are the same
        eligible = float(df["income"].mean()) > float(df["spend"].mean())
        base_amount = float(df["income"].mean()) * 6.0
    else:
        # Use Logistic Regression with scaling for robustness
        scaler = StandardScaler()
        X_scaled = scaler.fit_transform(income)
        model = LogisticRegression(random_state=42, max_iter=1000)
        model.fit(X_scaled, y)
        prob = float(model.predict_proba(scaler.transform([[df['income'].iloc[-1]]]))[0][1])

        eligible = prob >= 0.5
        base_amount = float(df["income"].mean()) * (8.0 if prob > 0.7 else 5.0)

    # Adjust by credit score and existing loans
    credit_score = fin.get("credit_score", 650)
    cs_factor = 1.0
    if credit_score < 600:
        cs_factor = 0.6
    elif credit_score < 680:
        cs_factor = 0.8
    elif credit_score > 760:
        cs_factor = 1.2

    existing_loans = fin.get("existing_loans", 0)
    debt_factor = max(0.5, 1.0 - 0.15 * existing_loans)

    max_amount = round(base_amount * cs_factor * debt_factor, 2)

    # APR: adjust by credit score + repayment probability
    apr_percent = round(16.0 - (credit_score - 600) * 0.03 - prob * 2, 2)
    apr_percent = float(np.clip(apr_percent, 8.0, 24.0))

    # Eligibility check against desired amount
    if desired_amount is not None and desired_amount > max_amount:
        eligible = False

    # Reasons
    reasons: List[str] = []
    if eligible:
        reasons.append("Positive income-to-spend trend")
        if prob > 0.7:
            reasons.append("High repayment probability")
        if credit_score >= 700:
            reasons.append("Good credit score")
        if existing_loans == 0:
            reasons.append("No existing loans")
    else:
        if desired_amount and desired_amount > max_amount:
            reasons.append("Requested amount exceeds estimated maximum")
        if credit_score < 640:
            reasons.append("Low credit score")
        if existing_loans > 1:
            reasons.append("Multiple existing loans")
        if float(df['spend'].mean()) > float(df['income'].mean()):
            reasons.append("High spend relative to income")

    return {
        "eligible": bool(eligible),
        "probability": round(prob, 3),
        "max_amount": float(max_amount),
        "apr_percent": float(apr_percent),
        "reasons": reasons or ["Insufficient data"],
    }
