from fastapi import APIRouter, Depends
from sqlalchemy.orm import Session
from ..schemas import ChatRequest, ChatResponse
from ..auth import get_current_user, get_db
from ..db import Chat
from ai_agent.rag.retriever import SimpleRAG

router = APIRouter(prefix="/chat", tags=["chat"])
retriever = SimpleRAG(docs_dir="ai_agent/rag/docs")

@router.post("/", response_model=ChatResponse)
def chat(req: ChatRequest, user=Depends(get_current_user), db: Session = Depends(get_db)):
    text = req.message.lower()
    if any(k in text for k in ["balance", "transaction", "transfer", "account"]):
        reply = "This looks account-related. The assistant would call the core backend securely."
    else:
        hits = retriever.query(req.message, top_k=2)
        if not hits:
            reply = "I couldn't find any policy info. Please rephrase."
        else:
            reply = "Here are relevant policy snippets:\n" + "\n\n".join([f"[{n}] {s}" for n, s, _ in hits])

    uid = int(user["sub"])
    c = Chat(user_id=uid, message=req.message, response=reply)
    db.add(c)
    db.commit()
    return {"reply": reply}
