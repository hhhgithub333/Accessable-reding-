from pydantic import BaseModel
from datetime import datetime
from typing import Optional


class UserCreate(BaseModel):
    """注册请求"""
    username: str
    password: str
    email: Optional[str] = None


class UserLogin(BaseModel):
    """登录请求"""
    username: str
    password: str


class UserResponse(BaseModel):
    """用户信息响应"""
    id: int
    username: str
    email: Optional[str] = None
    created_at: datetime

    class Config:
        from_attributes = True


class Token(BaseModel):
    """Token 响应"""
    access_token: str
    token_type: str
    user: UserResponse


class TTSRequest(BaseModel):
    """TTS 请求"""
    text: str
    voice: str
    engine: str