import subprocess
import asyncio
import os
import signal
import time
from typing import Optional
import aiohttp


class ModelManager:
    """本地模型管理器：按需自动启停服务"""

    _instance = None
    _current_model: Optional[str] = None
    _current_process: Optional[subprocess.Popen] = None
    _service_ready: bool = False

    # 模型配置
    MODEL_CONFIGS = {
        "xtts_v2": {
            "conda_env": "xtts_v2",
            "port": 8001,
            "script": "tts_services/xtts_v2_service.py",
            "vram_mb": 3500
        },
        "voxcpm": {
            "conda_env": "VoxCPM",
            "port": 8002,
            "script": "tts_services/voxcpm_service.py",
            "vram_mb": 3500
        },
        "chatterbox": {
            "conda_env": "chatterbox",
            "port": 8003,
            "script": "tts_services/chatterbox_service.py",
            "vram_mb": 4000
        },
        "vibevoice": {
            "conda_env": "vibevoice310",
            "port": 3000,
            "script": "tts_services/vibevoice_service.py",
            "vram_mb": 3000
        },
        "indextts": {
            "conda_env": "indextts",
            "port": 8004,
            "script": "tts_services/indextts_service.py",
            "vram_mb": 6000
        }
    }

    def __new__(cls):
        if cls._instance is None:
            cls._instance = super().__new__(cls)
        return cls._instance

    @property
    def current_model(self) -> Optional[str]:
        return self._current_model

    async def ensure_service(self, model_name: str) -> bool:
        """确保指定模型的服务已启动"""
        if model_name not in self.MODEL_CONFIGS:
            return False

        # 如果已经是当前模型且服务正常运行
        if self._current_model == model_name and self._service_ready:
            if await self._check_service_ready(self.MODEL_CONFIGS[model_name]["port"]):
                return True

        # 停止当前服务
        await self._stop_current_service()

        # 启动新服务
        success = await self._start_service(model_name)

        if success:
            self._current_model = model_name
            self._service_ready = True

        return success

    async def _stop_current_service(self):
        """停止当前运行的服务"""
        if self._current_process is not None:
            try:
                self._current_process.terminate()
                await asyncio.sleep(2)
                if self._current_process.poll() is None:
                    self._current_process.kill()
                self._current_process = None
            except Exception as e:
                print(f"停止服务失败: {e}")

        self._current_model = None
        self._service_ready = False

    async def _start_service(self, model_name: str) -> bool:
        """启动模型服务"""
        config = self.MODEL_CONFIGS[model_name]

        # 获取 backend 根目录
        backend_root = os.path.dirname(os.path.dirname(os.path.dirname(__file__)))
        script_path = os.path.join(backend_root, config["script"])

        # 构建启动命令
        cmd = f'conda run -n {config["conda_env"]} python "{script_path}" --port {config["port"]}'

        print(f"启动服务: {model_name}, 端口: {config['port']}")

        try:
            self._current_process = subprocess.Popen(
                cmd,
                shell=True,
                stdout=subprocess.PIPE,
                stderr=subprocess.PIPE
            )

            # 等待服务启动（最多 60 秒）
            for i in range(60):
                await asyncio.sleep(1)
                if await self._check_service_ready(config["port"]):
                    print(f"服务启动成功: {model_name}")
                    return True
                if i % 10 == 0:
                    print(f"等待服务启动... {i}s")

            print(f"服务启动超时: {model_name}")
            return False

        except Exception as e:
            print(f"启动服务失败: {e}")
            return False

    async def _check_service_ready(self, port: int) -> bool:
        """检查服务是否就绪"""
        try:
            async with aiohttp.ClientSession() as session:
                async with session.get(f"http://127.0.0.1:{port}/health", timeout=2) as resp:
                    return resp.status == 200
        except:
            return False

    def get_api_url(self, model_name: str) -> str:
        """获取模型 API 地址"""
        config = self.MODEL_CONFIGS.get(model_name)
        if config:
            return f"http://127.0.0.1:{config['port']}/synthesize"
        return None