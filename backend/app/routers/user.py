from fastapi import APIRouter, HTTPException, Depends
from sqlalchemy.orm import Session
from datetime import datetime
from ..models.database import get_db, User
from ..models.schemas import UserCreate, UserLogin, UserResponse, Token
from ..models.auth import verify_password, get_password_hash, create_access_token

router = APIRouter(prefix="/user", tags=["用户"])


@router.post("/register", response_model=UserResponse)
def register(user: UserCreate, db: Session = Depends(get_db)):
    """用户注册"""
    # 检查用户名是否已存在
    existing = db.query(User).filter(User.username == user.username).first()
    if existing:
        raise HTTPException(status_code=400, detail="用户名已存在")

    # 创建新用户
    db_user = User(
        username=user.username,
        password=get_password_hash(user.password),
        email=user.email
    )
    db.add(db_user)
    db.commit()
    db.refresh(db_user)

    return db_user


@router.post("/login", response_model=Token)
def login(user: UserLogin, db: Session = Depends(get_db)):
    """用户登录"""
    # 查找用户
    db_user = db.query(User).filter(User.username == user.username).first()
    if not db_user:
        raise HTTPException(status_code=401, detail="用户名或密码错误")

    # 验证密码
    if not verify_password(user.password, db_user.password):
        raise HTTPException(status_code=401, detail="用户名或密码错误")

    # 更新最后登录时间
    db_user.last_login = datetime.utcnow()
    db.commit()

    # 创建 token
    access_token = create_access_token(data={"sub": db_user.username})

    return {
        "access_token": access_token,
        "token_type": "bearer",
        "user": db_user
    }
# |_backend
#     |_app
#         |_models
#             |_ __init__.py
#             |_ auth.py
#             |_ database.py
#             |_ schemas.py
#         |_routers
#             |_ __init__.py
#             |_ TTS.py
#             |_ user.py
#         |_tts
#             |_ __init__.py
#             |_ base.py
#             |_ cosyvoice.py
#             |_ qwen.py
#             |_ sambert.py
#     |_ .env
#     |_ requirement.txt
#     |_ tts.db