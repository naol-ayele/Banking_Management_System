import os, random, datetime as dt
import requests

BASE = os.getenv("BMS_BASE_URL", "http://localhost:8080")
API_KEY = os.getenv("BMS_API_KEY", "BMS-TEST-APIKEY-12345")
MOCK = os.getenv("MOCK_BMS", "true").lower() == "true"

def _hdr():
    return {"X-API-KEY": API_KEY, "content-type": "application/json"}

def register_user_via_bms(payload: dict):
    """
    Registers a new user in the BMS backend.
    No API Key or JWT required.
    """
    if MOCK:
        return True, {
            "id": random.randint(1000, 9999),
            "email": payload.get("email"),
            "status": "registered_mock"
        }

    try:
        r = requests.post(f"{BASE}/api/auth/register",
                          json=payload, timeout=10)
        if r.status_code >= 400:
            return False, {"error": r.text}
        return True, r.json()
    except Exception as e:
        return False, {"error": str(e)}
def get_user_financials(user_id: int):
    if MOCK:
        # Generate 12 months of synthetic income + spending + categories
        today = dt.date.today()
        months = []
        for i in range(12):
            d = (today.replace(day=1) - dt.timedelta(days=30*i))
            income = random.randint(12000, 30000)
            spend = random.randint(6000, 22000)
            categories = {
                "food": round(spend*random.uniform(0.15, 0.35), 2),
                "rent": round(spend*random.uniform(0.25, 0.45), 2),
                "utilities": round(spend*random.uniform(0.05, 0.15), 2),
                "travel": round(spend*random.uniform(0.05, 0.20), 2),
                "other": 0.0
            }
            categories["other"] = round(spend - sum([v for k,v in categories.items() if k != "other"]), 2)
            months.append({"month": d.strftime("%Y-%m"), "income": income, "spend": spend, "categories": categories})
        credit_score = random.randint(520, 820)
        on_time_rate = round(random.uniform(0.7, 1.0), 2)
        existing_loans = random.randint(0, 3)
        return True, {"user_id": user_id, "months": list(reversed(months)), "credit_score": credit_score, "on_time_rate": on_time_rate, "existing_loans": existing_loans}
    try:
        r = requests.get(f"{BASE}/analytics/financials?user_id={user_id}", headers=_hdr(), timeout=15)
        if r.status_code >= 400:
            return False, {"error": r.text}
        return True, r.json()
    except Exception as e:
        return False, {"error": str(e)}
