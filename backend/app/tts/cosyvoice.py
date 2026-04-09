from .base import BaseTTSEngine
import httpx
import json
import websockets
import asyncio


class CosyVoiceEngine(BaseTTSEngine):

    def get_model_name(self) -> str:
        return "cosyvoice-v3-flash"

    def get_api_url(self) -> str:
        # CosyVoice 使用 WebSocket
        return "wss://dashscope.aliyuncs.com/api-ws/v1/inference"

    def get_voices(self) -> list:
        return [
            "longanhuan", "longanyang", "longanqin", "longhuhu_v3",
            "longpaopao_v3", "longjielidou_v3", "longxian_v3", "longling_v3",
            "longshanshan_v3", "longniuniu_v3", "longjiaxin_v3", "longjiayi_v3",
            "longanyue_v3", "longlaotie_v3", "longshange_v3", "longanmin_v3",
            "loongkyong_v3", "loongriko_v3", "loongtomoka_v3", "longfei_v3"
        ]

    async def synthesize(self, text: str, voice: str) -> bytes:
        """通过 WebSocket 合成语音"""
        import uuid
        import base64

        task_id = str(uuid.uuid4())

        # 构建 run-task 指令
        run_task = {
            "header": {
                "action": "run-task",
                "task_id": task_id,
                "streaming": "duplex"
            },
            "payload": {
                "task_group": "audio",
                "task": "tts",
                "function": "SpeechSynthesizer",
                "model": "cosyvoice-v3-flash",
                "parameters": {
                    "text_type": "PlainText",
                    "voice": voice,
                    "format": "mp3",
                    "sample_rate": 22050,
                    "volume": 50,
                    "rate": 1,
                    "pitch": 1,
                    "enable_ssml": False
                },
                "input": {}
            }
        }

        # 构建 continue-task 指令
        continue_task = {
            "header": {
                "action": "continue-task",
                "task_id": task_id,
                "streaming": "duplex"
            },
            "payload": {
                "input": {"text": text}
            }
        }

        # 构建 finish-task 指令
        finish_task = {
            "header": {
                "action": "finish-task",
                "task_id": task_id,
                "streaming": "duplex"
            },
            "payload": {"input": {}}
        }

        audio_data = bytearray()

        async with websockets.connect(
                self.get_api_url(),
                extra_headers={
                    "Authorization": f"Bearer {self.api_key}",
                    "X-DashScope-DataInspection": "enable"
                }
        ) as ws:
            # 发送 run-task
            await ws.send(json.dumps(run_task))

            # 接收 task-started
            response = await ws.recv()
            print(f"收到: {response}")

            # 发送 continue-task
            await ws.send(json.dumps(continue_task))

            # 发送 finish-task
            await ws.send(json.dumps(finish_task))

            # 接收音频数据
            while True:
                try:
                    message = await asyncio.wait_for(ws.recv(), timeout=10)

                    if isinstance(message, bytes):
                        # 二进制音频数据
                        audio_data.extend(message)
                    else:
                        # JSON 事件
                        msg = json.loads(message)
                        if msg.get("header", {}).get("event") == "task-finished":
                            break
                        elif msg.get("header", {}).get("event") == "task-failed":
                            error = msg.get("header", {}).get("error_message", "未知错误")
                            raise Exception(f"合成失败: {error}")

                except asyncio.TimeoutError:
                    break

        return bytes(audio_data)

    def get_audio_format(self) -> str:
        return "mp3"