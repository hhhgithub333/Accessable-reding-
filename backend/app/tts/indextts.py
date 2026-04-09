import os
import asyncio
import json
import tempfile
import subprocess
from concurrent.futures import ThreadPoolExecutor
from .base import BaseTTSEngine

CONDA_BASE = r"E:\miniconda3"
# 注意：需要创建 indextts 虚拟环境
PYTHON_PATH = os.path.join(CONDA_BASE, "envs", "indextts", "python.exe")

SCRIPT_PATH = os.path.join(
    os.path.dirname(__file__), ".." , "scripts", "indextts_worker.py"
)
SCRIPT_PATH = os.path.abspath(SCRIPT_PATH)

_executor = ThreadPoolExecutor(max_workers=2)


class IndexTTSEngine(BaseTTSEngine):
    def __init__(self):
        super().__init__()

    def get_voices(self) -> list:
        return ["default"]

    def get_audio_format(self) -> str:
        return "wav"

    async def synthesize(self, text: str, voice: str = None) -> bytes:
        def _sync_synthesize():
            with tempfile.NamedTemporaryFile(suffix=".wav", delete=False) as tmp:
                output_path = tmp.name

            cmd = [
                PYTHON_PATH,
                SCRIPT_PATH,
                "--text", text,
                "--output", output_path
            ]

            result = subprocess.run(
                cmd,
                capture_output=True,
                text=True,
                encoding='utf-8',
                errors='ignore'
            )

            try:
                for line in result.stdout.split("\n"):
                    if line.strip().startswith("{"):
                        res = json.loads(line)
                        if res.get("success"):
                            with open(output_path, "rb") as f:
                                audio_data = f.read()
                            os.unlink(output_path)
                            return audio_data
                        else:
                            raise Exception(res.get("error", "未知错误"))
            except json.JSONDecodeError:
                pass

            if os.path.exists(output_path):
                os.unlink(output_path)

            raise Exception(f"IndexTTS 合成失败: {result.stderr}")

        loop = asyncio.get_event_loop()
        return await loop.run_in_executor(_executor, _sync_synthesize)