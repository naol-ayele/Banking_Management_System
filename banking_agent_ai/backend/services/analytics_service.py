import io
import pandas as pd
import numpy as np
from sklearn.linear_model import LogisticRegression
from typing import Tuple

def daily_summary_chart_png(transactions: pd.DataFrame) -> io.BytesIO:
    import matplotlib.pyplot as plt
    fig, ax = plt.subplots()
    df = transactions.copy()
    df['date'] = pd.to_datetime(df['date'])
    daily = df.groupby('date')['amount'].sum()
    daily.plot(ax=ax)
    ax.set_title("Transactions by Day")
    ax.set_xlabel("Date")
    ax.set_ylabel("Amount")
    buf = io.BytesIO()
    fig.savefig(buf, format='png', bbox_inches='tight')
    plt.close(fig)
    buf.seek(0)
    return buf

def train_default_risk_model(X: pd.DataFrame, y: pd.Series) -> Tuple[np.ndarray, float]:
    model = LogisticRegression(max_iter=200)
    model.fit(X.values, y.values)
    return model.coef_.ravel(), float(model.intercept_[0])

def demo_default_risk_training() -> Tuple[np.ndarray, float]:
    rng = np.random.default_rng(42)
    n = 400
    income = rng.normal(5000, 1500, n).clip(1000, None)
    loan_amount = rng.normal(12000, 4000, n).clip(1000, None)
    debt_ratio = rng.uniform(0.05, 0.8, n)
    logit = -5 + 0.00015*loan_amount + 4*debt_ratio - 0.0001*income
    p = 1/(1+np.exp(-logit))
    y = rng.binomial(1, p)
    X = pd.DataFrame({"income": income, "loan_amount": loan_amount, "debt_ratio": debt_ratio})
    coefs, intercept = train_default_risk_model(X, pd.Series(y))
    return coefs, intercept
