from .base import BaseTTSEngine
import json
import websockets
import asyncio
import uuid


class SambertEngine(BaseTTSEngine):

    def _get_model_by_voice(self, voice: str) -> str:
        model_map = {
            "zhinan": "sambert-zhinan-v1",
            "zhiqi": "sambert-zhiqi-v1",
            "zhichu": "sambert-zhichu-v1",
            "zhide": "sambert-zhide-v1",
            "zhijia": "sambert-zhijia-v1",
            "zhiru": "sambert-zhiru-v1",
            "zhiqian": "sambert-zhiqian-v1",
            "zhixiang": "sambert-zhixiang-v1",
            "zhiwei": "sambert-zhiwei-v1",
            "zhihao": "sambert-zhihao-v1",
            "zhijing": "sambert-zhijing-v1",
            "zhiming": "sambert-zhiming-v1",
            "zhimo": "sambert-zhimo-v1",
            "zhina": "sambert-zhina-v1",
            "zhishu": "sambert-zhishu-v1",
            "zhistella": "sambert-zhistella-v1",
            "zhiting": "sambert-zhiting-v1",
            "zhixiao": "sambert-zhixiao-v1",
            "zhiya": "sambert-zhiya-v1",
            "zhiye": "sambert-zhiye-v1",
            "zhiying": "sambert-zhiying-v1",
            "zhiyuan": "sambert-zhiyuan-v1",
            "zhiyue": "sambert-zhiyue-v1",
            "zhigui": "sambert-zhigui-v1",
            "zhishuo": "sambert-zhishuo-v1",
            "zhimiao-emo": "sambert-zhimiao-emo-v1",
            "zhimao": "sambert-zhimao-v1"
        }
        return model_map.get(voice, "sambert-zhichu-v1")

    def _get_sample_rate(self, voice: str) -> int:
        high_rate_voices = [
            "zhinan", "zhiqi", "zhichu", "zhide", "zhijia",
            "zhiru", "zhiqian", "zhixiang", "zhiwei"
        ]
        return 48000 if voice in high_rate_voices else 16000

    async def synthesize(self, text: str, voice: str) -> bytes:
        task_id = str(uuid.uuid4())
        model = self._get_model_by_voice(voice)
        sample_rate = self._get_sample_rate(voice)

        run_task = {
            "header": {
                "action": "run-task",
                "task_id": task_id,
                "streaming": "out"
            },
            "payload": {
                "model": model,
                "task_group": "audio",
                "task": "tts",
                "function": "SpeechSynthesizer",
                "input": {"text": text},
                "parameters": {
                    "text_type": "PlainText",
                    "voice": voice,
                    "format": "wav",  # 改成 wav 格式
                    "sample_rate": sample_rate,
                    "volume": 50,
                    "rate": 1,
                    "pitch": 1
                }
            }
        }

        audio_data = bytearray()

        async with websockets.connect(
                "wss://dashscope.aliyuncs.com/api-ws/v1/inference",
                extra_headers={
                    "Authorization": f"Bearer {self.api_key}",
                    "X-DashScope-DataInspection": "enable"
                }
        ) as ws:
            await ws.send(json.dumps(run_task))

            while True:
                message = await ws.recv()

                if isinstance(message, bytes):
                    audio_data.extend(message)
                else:
                    msg = json.loads(message)
                    event = msg.get("header", {}).get("event")
                    if event == "task-finished":
                        break
                    elif event == "task-failed":
                        error = msg.get("header", {}).get("error_message", "未知错误")
                        raise Exception(f"Sambert 合成失败: {error}")

        return bytes(audio_data)

    def get_voices(self) -> list:
        return [
            "zhinan", "zhiqi", "zhichu", "zhide", "zhijia",
            "zhiru", "zhiqian", "zhixiang", "zhiwei", "zhihao",
            "zhijing", "zhiming", "zhimo", "zhina", "zhishu",
            "zhistella", "zhiting", "zhixiao", "zhiya", "zhiye",
            "zhiying", "zhiyuan", "zhiyue", "zhigui", "zhishuo",
            "zhimiao-emo", "zhimao"
        ]

    def get_audio_format(self) -> str:
        return "mp3"