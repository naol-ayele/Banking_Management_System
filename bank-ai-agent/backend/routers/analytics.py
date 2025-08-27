import time
from fastapi import APIRouter, HTTPException, Query
from pydantic import BaseModel
from typing import Optional, List, Dict, Any
from ..services.bms_client import get_user_financials
from ..services.analytics import summarize_finances, predict_loan_eligibility
from ..services.logging import log_analytics_event

router = APIRouter()

class SummaryOut(BaseModel):
    income_monthly: float
    spend_monthly: float
    savings_rate: float
    top_categories: Dict[str, float]

@router.get("/financial-summary", response_model=SummaryOut)
def financial_summary(user_id: int = Query(..., description="BMS user id")):
    t0 = time.time()
    ok, data = get_user_financials(user_id)
    if not ok:
        raise HTTPException(status_code=400, detail=data.get("error", "cannot fetch user financials"))

    summary = summarize_finances(data)
    log_analytics_event(user_id=user_id, kind="financial_summary", payload=data, result=summary, duration_ms=int((time.time()-t0)*1000))
    return summary

class EligibilityOut(BaseModel):
    eligible: bool
    max_amount: float
    apr_percent: float
    reasons: List[str]

@router.get("/loan-eligibility", response_model=EligibilityOut)
def loan_eligibility(user_id: int, desired_amount: Optional[float] = None):
    t0 = time.time()
    ok, data = get_user_financials(user_id)
    if not ok:
        raise HTTPException(status_code=400, detail=data.get("error", "cannot fetch user financials"))

    result = predict_loan_eligibility(data, desired_amount=desired_amount)
    log_analytics_event(user_id=user_id, kind="loan_eligibility", payload={"desired_amount": desired_amount}, result=result, duration_ms=int((time.time()-t0)*1000))
    return result
