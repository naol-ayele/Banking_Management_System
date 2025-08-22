from fastapi import FastAPI
from .db import init_db
from .routes import auth, chat, analytics

app = FastAPI(title="AI Banking Agent Backend")

@app.on_event("startup")
def on_startup():
    init_db()

app.include_router(auth.router)
app.include_router(chat.router)
app.include_router(analytics.router)
