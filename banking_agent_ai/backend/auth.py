import jwt
from datetime import datetime, timedelta
from fastapi import Depends, HTTPException, status
from fastapi.security import HTTPBearer
from passlib.context import CryptContext
from sqlalchemy.orm import Session
from .config import JWT_SECRET, JWT_ALGO
from .db import SessionLocal, User

pwd_context = CryptContext(schemes=["bcrypt"], deprecated="auto")
security = HTTPBearer()

def get_db():
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()

def hash_password(pw: str) -> str:
    return pwd_context.hash(pw)

def verify_password(pw: str, hashed: str) -> bool:
    return pwd_context.verify(pw, hashed)

def create_access_token(user_id: int, role: str) -> str:
    payload = {"sub": str(user_id), "role": role, "exp": datetime.utcnow() + timedelta(hours=2)}
    return jwt.encode(payload, JWT_SECRET, algorithm=JWT_ALGO)

def get_current_user(credentials=Depends(security)) -> dict:
    try:
        payload = jwt.decode(credentials.credentials, JWT_SECRET, algorithms=[JWT_ALGO])
        return payload
    except Exception:
        raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail="Invalid or expired token")
