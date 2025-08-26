from pydantic import BaseModel
from typing import Optional, List

class SignupRequest(BaseModel):
    username: str
    password: str
    role: str = "customer"

class LoginRequest(BaseModel):
    username: str
    password: str

class LoginResponse(BaseModel):
    access_token: str
    token_type: str = "bearer"

class ChatRequest(BaseModel):
    message: str

class ChatResponse(BaseModel):
    reply: str

class AnalyticsResponse(BaseModel):
    info: str
    coefficients: Optional[list] = None
    intercept: Optional[float] = None
