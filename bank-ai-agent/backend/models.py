from sqlalchemy import Column, Integer, String, DateTime, Text, ForeignKey, JSON, func, Boolean, Float
from sqlalchemy.orm import relationship
from .database import Base

class User(Base):
    __tablename__ = "users"
    id = Column(Integer, primary_key=True, index=True)
    bms_user_id = Column(Integer, index=True, nullable=True)
    name = Column(String(120))
    email = Column(String(120), index=True)
    created_at = Column(DateTime, server_default=func.now())

    sessions = relationship("ChatSession", back_populates="user")

class ChatSession(Base):
    __tablename__ = "chat_sessions"
    id = Column(Integer, primary_key=True, index=True)
    user_id = Column(Integer, ForeignKey("users.id"))
    started_at = Column(DateTime, server_default=func.now())
    is_active = Column(Boolean, default=True)

    user = relationship("User", back_populates="sessions")
    messages = relationship("Message", back_populates="session")

class Message(Base):
    __tablename__ = "messages"
    id = Column(Integer, primary_key=True, index=True)
    session_id = Column(Integer, ForeignKey("chat_sessions.id"))
    role = Column(String(20))  # 'user' or 'assistant'
    content = Column(Text)
    created_at = Column(DateTime, server_default=func.now())

    session = relationship("ChatSession", back_populates="messages")

class AnalyticsLog(Base):
    __tablename__ = "analytics_logs"
    id = Column(Integer, primary_key=True, index=True)
    user_id = Column(Integer, index=True)
    kind = Column(String(50))  # 'loan_eligibility', 'financial_summary', etc.
    payload = Column(JSON)
    result = Column(JSON)
    duration_ms = Column(Integer)
    created_at = Column(DateTime, server_default=func.now())

class EligibilityModelMeta(Base):
    __tablename__ = "eligibility_model_meta"
    id = Column(Integer, primary_key=True, index=True)
    version = Column(String(50))
    accuracy = Column(Float)
    trained_at = Column(DateTime, server_default=func.now())
