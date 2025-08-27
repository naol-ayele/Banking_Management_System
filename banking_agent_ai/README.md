# AI Banking Sidebar Agent — Full Implementation (Prototype)

This project implements a **role-aware AI assistant** for a banking management system with:
- **Frontend:** Streamlit (login-gated assistant)
- **Backend:** FastAPI (auth, chat, analytics), PostgreSQL persistence
- **AI:** RAG over policy/FAQ docs + basic analytics (pandas/numpy/sklearn)
- **Security:** JWT; the assistant **only** runs after successful login
- **Data:** The AI agent stores chat history in its **own PostgreSQL database**;
  it calls the core banking backend only for **account-related** requests.

> Production SRS targets Spring Boot as the core system. This Python prototype
> shows the integration layer and UX. Replace the mock `account_service` with
> your Spring Boot URLs to go live.

## Quick Start (Local)

1) Install requirements
```bash
python -m venv .venv && source .venv/bin/activate  # (Windows: .venv\Scripts\activate)
pip install -r requirements.txt
```

2) Prepare Postgres (local or Docker). Example with Docker:
```bash
docker compose up -d db adminer
# DB UI: http://localhost:8080  (system: PostgreSQL, server: db, user: aiagent, pass: aiagentpass, db: aiagentdb)
```

3) Set environment variables (or copy `.env.sample` to `.env` and edit)
```bash
export DATABASE_URL="postgresql://aiagent:aiagentpass@localhost:5432/aiagentdb"
export JWT_SECRET="supersecret"
export CORE_BACKEND_URL="http://localhost:8088"  # your Spring Boot base URL (mocked if unset)
```

4) Start FastAPI backend
```bash
uvicorn backend.main:app --reload --port 8000
```

5) Start Streamlit frontend
```bash
streamlit run frontend/app.py
```

## Folders
- `backend/` — FastAPI app, routers, services, Postgres models (SQLAlchemy)
- `frontend/` — Streamlit UI (login → assistant)
- `ai_agent/` — RAG & analytics helpers
- `tests/` — minimal tests
- `docker-compose.yml` — Postgres + Adminer (optional for later)

## Notes
- The assistant stores chats in Postgres and **decides when to call** the core backend.
- Analytics endpoints use **pandas**, **numpy**, **scikit-learn (LogisticRegression)**.
- Matplotlib generates charts for summaries (kept simple for clarity).
