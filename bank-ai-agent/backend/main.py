from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from dotenv import load_dotenv
from .database import init_db
from .routers import auth, analytics, chat

load_dotenv()

app = FastAPI(title="AI Banking Agent", version="1.0.0")

# CORS for Streamlit and embeddings
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(auth.router, prefix="/agent", tags=["agent-auth"])
app.include_router(analytics.router, prefix="/analytics", tags=["analytics"])
app.include_router(chat.router, prefix="/chat", tags=["chat"])

@app.on_event("startup")
def on_startup():
    init_db()

@app.get("/health")
def health():
    return {"status": "ok"}
