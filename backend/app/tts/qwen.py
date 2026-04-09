from .base import BaseTTSEngine
import httpx
import base64


class QwenTTSEngine(BaseTTSEngine):

    async def synthesize(self, text: str, voice: str) -> bytes:
        async with httpx.AsyncClient() as client:
            payload = {
                "model": "qwen3-tts-flash-2025-09-18",
                "input": {"text": text},
                "parameters": {"voice": voice, "language_type": "Chinese"}
            }
            headers = {
                "Authorization": f"Bearer {self.api_key}",
                "Content-Type": "application/json"
            }

            response = await client.post(
                "https://dashscope.aliyuncs.com/api/v1/services/aigc/multimodal-generation/generation",
                json=payload,
                headers=headers,
                timeout=60
            )

            if response.status_code != 200:
                raise Exception(f"千问 TTS API 错误: {response.text}")

            result = response.json()
            audio_url = result.get("output", {}).get("audio", {}).get("url")

            if not audio_url:
                audio_data = result.get("output", {}).get("audio", {}).get("data")
                if audio_data:
                    return base64.b64decode(audio_data)
                raise Exception("未获取到音频")

            audio_response = await client.get(audio_url)
            return audio_response.content

    def get_voices(self) -> list:
        return ["Cherry", "Stella", "James", "Bella", "Alex", "Emma", "Liam", "Mia", "Noah", "Olivia"]

    def get_audio_format(self) -> str:
        return "mp3"