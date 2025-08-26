from sqlalchemy.orm import Session
from ..db import User
from ..auth import hash_password, verify_password, create_access_token

def signup(db: Session, username: str, password: str, role: str) -> int:
    if db.query(User).filter(User.username == username).first():
        raise ValueError("Username already exists")
    u = User(username=username, password_hash=hash_password(password), role=role)
    db.add(u)
    db.commit()
    db.refresh(u)
    return u.id

def login(db: Session, username: str, password: str) -> str:
    u = db.query(User).filter(User.username == username).first()
    if not u or not verify_password(password, u.password_hash):
        raise ValueError("Invalid credentials")
    return create_access_token(u.id, u.role)
