import os
import sys
import asyncio
import tempfile
import subprocess
import logging

from .base import BaseTTSEngine

logger = logging.getLogger("xtts_v2")

# xtts_v2 conda 环境路径
CONDA_BASE = r"E:\miniconda3"
PYTHON_PATH = os.path.join(CONDA_BASE, "envs", "xtts_v2", "python.exe")
SCRIPT_PATH = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "scripts", "xtts_v2_worker.py"))

# 默认参考音频
DEFAULT_VOICE_FILE = r"D:\Python\Project\xtts_v2\samples\zh-cn-sample.wav"


class XTTSv2Engine(BaseTTSEngine):
    """XTTS-v2 引擎，每次请求启动新进程"""

    def get_voices(self) -> list:
        """获取可用音色列表"""
        return []

    def get_audio_format(self) -> str:
        """获取音频格式"""
        return "wav"

    async def synthesize(self, text: str, voice: str = None) -> bytes:
        logger.info(f"XTTS-v2 合成请求: {text[:30]}..." if len(text) > 30 else f"XTTS-v2 合成请求: {text}")

        # 使用参考音频
        voice_file = DEFAULT_VOICE_FILE

        # 确保参考音频存在
        if not os.path.exists(voice_file):
            raise FileNotFoundError(f"参考音频不存在: {voice_file}")

        try:
            # 创建临时输出文件
            with tempfile.NamedTemporaryFile(suffix=".wav", delete=False) as tmp:
                output_path = tmp.name

            # 构建命令
            cmd = [
                PYTHON_PATH,
                SCRIPT_PATH,
                "--text", text,
                "--voice_file", voice_file,
                "--output", output_path
            ]

            # 在线程池中执行同步的 subprocess 调用
            loop = asyncio.get_event_loop()
            audio_data = await loop.run_in_executor(
                None,
                self._run_subprocess,
                cmd,
                output_path
            )

            return audio_data

        except Exception as e:
            logger.error(f"XTTS-v2 合成失败: {e}")
            raise

    def _run_subprocess(self, cmd: list, output_path: str) -> bytes:
        """在子进程中执行 TTS 合成"""
        try:
            # 执行命令
            result = subprocess.run(
                cmd,
                capture_output=True,
                text=True
            )

            if result.returncode != 0:
                error_msg = result.stderr or result.stdout
                raise RuntimeError(f"TTS Worker 错误: {error_msg}")

            # 读取输出文件
            if not os.path.exists(output_path):
                raise FileNotFoundError(f"输出文件未生成: {output_path}")

            with open(output_path, "rb") as f:
                audio_data = f.read()

            # 清理临时文件
            try:
                os.unlink(output_path)
            except Exception:
                pass

            logger.info(f"XTTS-v2 合成成功，音频大小: {len(audio_data)} bytes")
            return audio_data

        except subprocess.TimeoutExpired:
            raise TimeoutError("TTS 合成超时")
        except Exception as e:
            # 清理临时文件
            if os.path.exists(output_path):
                try:
                    os.unlink(output_path)
                except Exception:
                    pass
            raise


