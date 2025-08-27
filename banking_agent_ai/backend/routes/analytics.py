from fastapi import APIRouter, Depends
from sqlalchemy.orm import Session
from ..auth import get_current_user, get_db
from ..schemas import AnalyticsResponse
from ..services import analytics_service as AS
import pandas as pd

router = APIRouter(prefix="/analytics", tags=["analytics"])

@router.get("/default-risk", response_model=AnalyticsResponse)
def default_risk_demo(user=Depends(get_current_user), db: Session = Depends(get_db)):
    coefs, intercept = AS.demo_default_risk_training()
    return {"info": "Trained logistic regression on synthetic data.", "coefficients": coefs.tolist(), "intercept": intercept}

@router.post("/summary-chart")
def summary_chart(transactions: list[dict], user=Depends(get_current_user)):
    df = pd.DataFrame(transactions)
    buf = AS.daily_summary_chart_png(df)
    return {"png_bytes": buf.getvalue().hex()}
