from fastapi import APIRouter, HTTPException
from pydantic import BaseModel
from typing import Optional
from ..services.ai_service import chat_with_ai

router = APIRouter()

class ChatIn(BaseModel):
    message: str
    user_id: Optional[int] = None
    session_id: Optional[int] = None

@router.post("")
def chat(body: ChatIn):
    if not body.message:
        raise HTTPException(status_code=400, detail="message is required")
    reply = chat_with_ai(body.message, user_id=body.user_id, session_id=body.session_id)
    return {"response": reply}
