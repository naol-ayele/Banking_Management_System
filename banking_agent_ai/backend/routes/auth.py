from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session
from ..schemas import SignupRequest, LoginRequest, LoginResponse
from ..auth import get_db
from ..services import user_service

router = APIRouter(prefix="/auth", tags=["auth"])

@router.post("/signup", response_model=LoginResponse)
def signup(req: SignupRequest, db: Session = Depends(get_db)):
    try:
        user_id = user_service.signup(db, req.username, req.password, req.role)
        token = user_service.login(db, req.username, req.password)
        return {"access_token": token, "token_type": "bearer"}
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))

@router.post("/login", response_model=LoginResponse)
def login(req: LoginRequest, db: Session = Depends(get_db)):
    try:
        token = user_service.login(db, req.username, req.password)
        return {"access_token": token, "token_type": "bearer"}
    except ValueError:
        raise HTTPException(status_code=401, detail="Invalid credentials")
