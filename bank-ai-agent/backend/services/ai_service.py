import os
from .db import get_db
from ..models import ChatSession, Message
from typing import Optional

# Gemini optional
GEMINI_API_KEY = os.getenv("GEMINI_API_KEY", "")
if GEMINI_API_KEY:
    try:
        import google.generativeai as genai
        genai.configure(api_key=GEMINI_API_KEY)
        GEMINI_MODEL = genai.GenerativeModel("gemini-2.0-flash")
    except Exception as e:
        GEMINI_API_KEY = ""
        GEMINI_MODEL = None
else:
    GEMINI_MODEL = None

SYSTEM_HINT = (
    "You are an AI Banking Agent. Be concise, helpful, and safe. "
    "If user asks about eligibility or summaries, you may suggest using the analytics tools in the app."
)

def _ensure_session(db, user_id: Optional[int], session_id: Optional[int]):
    # Create an ad-hoc session if missing
    if session_id:
        sess = db.get(ChatSession, session_id)
        if sess:
            return sess
    sess = ChatSession(user_id=user_id)
    db.add(sess)
    db.commit()
    db.refresh(sess)
    return sess

def chat_with_ai(message: str, user_id: Optional[int] = None, session_id: Optional[int] = None) -> str:
    with get_db() as db:
        sess = _ensure_session(db, user_id, session_id)
        db.add(Message(session_id=sess.id, role="user", content=message))
        db.commit()

        if GEMINI_MODEL:
            try:
                prompt = SYSTEM_HINT + "\nUser: " + message
                rsp = GEMINI_MODEL.generate_content(prompt)
                text = rsp.text or "I'm here to help with your banking questions."
            except Exception as e:
                text = f"Gemini error: {e}. Fallback: For loan checks, use the 'Loan Eligibility' tab."
        else:
            # Simple heuristic fallback
            m = message.lower()
            if "loan" in m and ("eligible" in m or "qualify" in m):
                text = "I can estimate your loan eligibility. In the app, open the 'Loan Eligibility' tab, or provide your user_id."
            elif "summary" in m or "spending" in m or "saving" in m:
                text = "I can analyze your spending and savings trends. Use the 'Financial Summary' tab with your user_id."
            else:
                text = "Hello! I can help register, guide you in the BMS, and analyze your finances. Try: 'Check my loan eligibility'."

        db.add(Message(session_id=sess.id, role="assistant", content=text))
        db.commit()
        return text
