import random, datetime as dt
from typing import Dict, Any
from ..config import CORE_BACKEND_URL

# In production, replace these mocks with real requests to CORE_BACKEND_URL endpoints.
def fetch_account_overview(user_id: int) -> Dict[str, Any]:
    balance = round(random.uniform(100, 5000), 2)
    return {"userId": user_id, "balance": balance, "currency": "USD"}

def fetch_transactions(user_id: int, days: int = 30):
    start = dt.datetime.utcnow() - dt.timedelta(days=days)
    tx = []
    for i in range(days):
        d = start + dt.timedelta(days=i)
        for _ in range(random.randint(1, 4)):
            amt = round(random.uniform(-200, 800), 2)
            tx.append({"date": d.date().isoformat(), "amount": amt, "type": "credit" if amt>0 else "debit"})
    return {"transactions": tx}
