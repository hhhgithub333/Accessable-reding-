from abc import ABC, abstractmethod
import os
from dotenv import load_dotenv

load_dotenv()


class BaseTTSEngine(ABC):
    """TTS 引擎基类"""

    def __init__(self):
        """初始化，从环境变量读取 API Key"""
        self.api_key = os.getenv("DASHSCOPE_API_KEY")
        # 本地模型不需要 API Key，所以不检查
        # if not self.api_key:
        #     raise ValueError("请设置 DASHSCOPE_API_KEY 环境变量")

    @abstractmethod
    async def synthesize(self, text: str, voice: str = None, voice_file: str = None) -> bytes:
        """
        合成语音，返回音频数据

        Args:
            text: 要合成的文本
            voice: 音色代码（云端模型使用）
            voice_file: 参考音频文件路径（本地克隆模型使用）

        Returns:
            bytes: 音频数据（MP3 或 WAV 格式）
        """
        pass

    @abstractmethod
    def get_voices(self) -> list:
        """
        获取该引擎支持的所有音色列表

        Returns:
            list: 音色代码列表，如果没有预设音色则返回空列表
        """
        pass

    def get_audio_format(self) -> str:
        """
        获取返回的音频格式

        Returns:
            str: "mp3" 或 "wav"
        """
        return "mp3"