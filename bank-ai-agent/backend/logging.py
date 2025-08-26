from .db import get_db
from ..models import AnalyticsLog

def log_analytics_event(user_id: int, kind: str, payload, result, duration_ms: int):
    with get_db() as db:
        log = AnalyticsLog(user_id=user_id, kind=kind, payload=payload, result=result, duration_ms=duration_ms)
        db.add(log)
        db.commit()
