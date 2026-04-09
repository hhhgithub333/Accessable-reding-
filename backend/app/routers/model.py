from fastapi import APIRouter, HTTPException
from pydantic import BaseModel
from typing import Optional
from ..tts.model_manager import ModelManager

router = APIRouter(prefix="/model", tags=["Model Management"])
manager = ModelManager()


class SwitchModelRequest(BaseModel):
    model_name: str


@router.get("/current")
async def get_current_model():
    """获取当前激活的本地模型"""
    return {
        "current_model": manager.current_model,
        "is_ready": manager._service_ready if manager._current_process else False
    }


@router.post("/switch")
async def switch_model(request: SwitchModelRequest):
    """切换本地模型（前端调用）"""
    try:
        success = await manager.ensure_service(request.model_name)
        if success:
            return {
                "status": "ok",
                "message": f"已切换到 {request.model_name}",
                "current_model": manager.current_model
            }
        else:
            raise HTTPException(status_code=500, detail="模型启动超时")
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@router.get("/list")
async def list_local_models():
    """获取所有本地模型列表"""
    return {
        "models": [
            {"name": "xtts_v2", "description": "XTTS-v2", "port": 8001},
            {"name": "voxcpm", "description": "VoxCPM", "port": 8002},
            {"name": "chatterbox", "description": "ChatterBox", "port": 8003},
            {"name": "vibevoice", "description": "VibeVoice", "port": 3000},
            {"name": "indextts", "description": "IndexTTS", "port": 8004}
        ]
    }