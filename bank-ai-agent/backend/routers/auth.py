from fastapi import APIRouter, HTTPException
from pydantic import BaseModel, EmailStr
from ..services.bms_client import register_user_via_bms

router = APIRouter()

class RegisterIn(BaseModel):
    name: str
    email: EmailStr
    password: str

@router.post("/register")
def register_via_agent(body: RegisterIn):
    ok, data = register_user_via_bms(body.model_dump())
    if not ok:
        raise HTTPException(status_code=400, detail=data.get("error", "registration failed"))
    return {"status": "success", "bms": data}
